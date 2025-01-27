#include "ConnectionHandler.h"
#include "StompProtocol.cpp"
#include <iostream>
#include <thread>
#include <atomic>

std::atomic<bool> terminateProgram(false);

void inputThread(ConnectionHandler &connectionHandler, StompProtocol &protocol) {
    std::string command;
    while (!terminateProgram.load() && !protocol.shouldTerminate()) {
        std::cout << "> ";
        std::getline(std::cin, command);

        if (command.rfind("join", 0) == 0) {
            std::string topic = command.substr(10); // Extract topic after "join "
            protocol.subscribe(topic);
        } else if (command.rfind("exit", 0) == 0) {
            std::string topic = command.substr(12); // Extract topic after "exit "
            protocol.unsubscribe(topic);
        } else if (command.rfind("report", 0) == 0) {
            size_t spacePos = command.find(' ', 5); // Find the space after "report "
            if (spacePos != std::string::npos) {
                std::string topic = command.substr(7, spacePos - 7);
                std::string message = command.substr(spacePos + 1);
                protocol.send(topic, message);
            } else {
                std::cerr << "Usage: report <topic> <message>" << std::endl;
            }
        } else if (command.rfind("summary ", 0) == 0) {
            size_t firstSpace = command.find(' ', 8);
            size_t secondSpace = command.find(' ', firstSpace + 1);
            if (firstSpace != std::string::npos && secondSpace != std::string::npos) {
                std::string channelName = command.substr(8, firstSpace - 8);
                std::string user = command.substr(firstSpace + 1, secondSpace - firstSpace - 1);
                std::string fileName = command.substr(secondSpace + 1);
                protocol.summarize(channelName, user, fileName);
            } else {
                std::cerr << "Usage: summary <channel_name> <user> <file>" << std::endl;
            }
        } else if (command == "logout") {
            protocol.disconnect();
            terminateProgram.store(true);
        } else {
            std::cerr << "Unknown command." << std::endl;
        }
    }
}

void socketThread(ConnectionHandler &connectionHandler, StompProtocol &protocol) {
    while (!terminateProgram.load()) {
        std::string response;
        if (!connectionHandler.getLine(response)) {
            std::cerr << "Disconnected from server." << std::endl;
            terminateProgram.store(true);
            break;
        }

        if (response.find("CONNECTED") != std::string::npos) {
            std::cout << "Login successful." << std::endl;
        } else if (response.find("ERROR") != std::string::npos) {
            std::cerr << "Login failed. Server response: " << response << std::endl;
            terminateProgram.store(true);
        } else {
            protocol.process(response);
        }
    }
}

int main(int argc, char *argv[]) {
    if (argc != 4) {
        std::cerr << "Usage: " << argv[0] << " <host> <port> <username>" << std::endl;
        return 1;
    }

    std::string host = argv[1];
    short port = std::stoi(argv[2]);
    std::string username = argv[3];

    ConnectionHandler connectionHandler(host, port);
    StompProtocol protocol(connectionHandler);

    // ניסיון להתחבר לשרת
    if (!connectionHandler.connect()) {
        std::cerr << "Could not connect to server at " << host << ":" << port << std::endl;
        return 1;
    }

    // בקשת סיסמה והתחברות עם פרוטוקול STOMP
    std::string password;
    std::cout << "Enter password for user " << username << ": ";
    std::cin >> password;
    std::cin.ignore();  // Clear newline from the input buffer

    if (!protocol.connect(host, username, password)) {
        std::cerr << "Failed to log in to the server." << std::endl;
        return 1;
    }

    std::cout << "Logged in successfully!" << std::endl;

    // הפעלת התרדים לאחר חיבור והתחברות תקינים
    std::thread input(inputThread, std::ref(connectionHandler), std::ref(protocol));
    std::thread socket(socketThread, std::ref(connectionHandler), std::ref(protocol));

    // המתנה לסיום התרדים
    input.join();
    socket.join();

    std::cout << "Client terminated." << std::endl;
    return 0;
}

