#include "ConnectionHandler.h"
#include "../src/StompProtocol.cpp"
#include <iostream>
#include <string>
#include <thread>

int main(int argc, char *argv[]) {
    if (argc != 4) {
        std::cerr << "Usage: " << argv[0] << " <host> <port> <username>" << std::endl;
        return 1;
    }

    std::string host = argv[1];
    short port = std::stoi(argv[2]);
    std::string username = argv[3];

    ConnectionHandler connectionHandler(host, port);
    
    if (!connectionHandler.connect()) {
        std::cerr << "Could not connect to server at " << host << ":" << port << std::endl;
        return 1;
    }

    StompProtocol protocol(connectionHandler);


    std::string password;
    std::cout << "Enter password for user " << username << ": ";
    std::cin >> password;
    
    protocol.connect(host, username, password);

    if (!protocol.connect(host, username, password)) {
        std::cerr << "Failed to log in to the server." << std::endl;
        return 1;
    }

    std::cout << "Logged in successfully!" << std::endl;

    // Simple interaction loop
    std::string command;
    while (!protocol.shouldTerminate()) {
        std::cout << "> ";
        std::getline(std::cin, command);

        if (command.rfind("join", 0) == 0) {
            std::string topic = command.substr(10); // Extract topic after "subscribe "
            protocol.subscribe(topic);
        } else if (command.rfind("exit", 0) == 0) {
            std::string topic = command.substr(12); // Extract topic after "unsubscribe "
            protocol.unsubscribe(topic);
        } else if (command.rfind("report", 0) == 0) {
            size_t spacePos = command.find(' ', 5); // Find the space after "send "
            if (spacePos != std::string::npos) {
                std::string topic = command.substr(5, spacePos - 5);
                std::string message = command.substr(spacePos + 1);
                protocol.send(topic, message);
            } else {
                std::cerr << "Usage: send <topic> <message>" << std::endl;
            }
        } else if (command == "DISCONNECT") {
            protocol.disconnect();
        } else if (!command.empty()) {
            std::cerr << "Unknown command." << std::endl;
        }
    }

    std::cout << "Client terminated." << std::endl;
    return 0;
}
