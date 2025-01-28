#include "../include/StompProtocol.h"
#include <iostream>
#include <fstream>



StompProtocol::StompProtocol(ConnectionHandler& handler)
    : isConnected(false), shouldTerminateConnection(false), connectionHandler(handler) {}

bool StompProtocol::connect(const std::string& host, const std::string& username, const std::string& password) {
    std::lock_guard<std::mutex> lock(protocolMutex);
    if (isConnected) {
        std::cout << "The client is already logged in. Log out before trying again." << std::endl;
        return false;
    }

    // Build and send CONNECT message
    std::string connectMessage = messageBuilder.buildConnectMessage(host, "7777", username, password);
    
    std::cout << "CONNECT message sent:\n" << connectMessage << std::endl;///check if the message is correct
    
    connectionHandler.sendLine(connectMessage);  // Send CONNECT message
    if (!connectionHandler.sendLine(connectMessage)) {
        std::cerr << "Failed to send CONNECT message." << std::endl;
        return false;
    }

    // Handle server response
    std::string response;
    if (!connectionHandler.getLine(response)) {
        std::cerr << "Failed to receive CONNECT response." << std::endl;
        return false;
    }

    if (response.find("CONNECTED") != std::string::npos) {
        isConnected = true;
        this->username = username;
        std::cout << "Login successful." << std::endl;
        return true;
    } else {
        std::cerr << "Login failed. Server response: " << response << std::endl;
        return false;
    }
}

void StompProtocol::subscribe(const std::string& topic) {
    std::lock_guard<std::mutex> lock(protocolMutex);
    if (!isConnected) {
        std::cerr << "Cannot subscribe. Client is not connected." << std::endl;
        return;
    }

    int subscriptionId = subscriptionIds.size() + 1;
    subscriptionIds[topic] = subscriptionId;

    std::string subscribeMessage = messageBuilder.buildSubscribeMessage(topic, subscriptionId);

    if (!connectionHandler.sendLine(subscribeMessage)) {
        std::cerr << "Failed to send SUBSCRIBE message." << std::endl;
    } else {
        std::cout << "Subscribed to topic: " << topic << std::endl;
    }
}

void StompProtocol::unsubscribe(const std::string& topic) {
    std::lock_guard<std::mutex> lock(protocolMutex);
    if (!isConnected || subscriptionIds.find(topic) == subscriptionIds.end()) {
        std::cerr << "Cannot UNSUBSCRIBE. Either not connected or topic not found." << std::endl;
        return;
    }

    int subscriptionId = subscriptionIds[topic];
    std::string unsubscribeMessage = messageBuilder.buildUnsubscribeMessage(subscriptionId);
    connectionHandler.sendLine(unsubscribeMessage); // Send UNSUBSCRIBE message
    if (!connectionHandler.sendLine(unsubscribeMessage)) {
        std::cerr << "Failed to send UNSUBSCRIBE message." << std::endl;
    } else {
        std::cout << "Unsubscribed from topic: " << topic << std::endl;
        subscriptionIds.erase(topic);
    }
}

void StompProtocol::send(const std::string& topic, const std::string& messageBody) {
    std::lock_guard<std::mutex> lock(protocolMutex);
    if (!isConnected || subscriptionIds.find(topic) == subscriptionIds.end()) {
        std::cerr << "Cannot send message. Either not connected or not subscribed to topic." << std::endl;
        return;
    }

    std::string sendMessage = messageBuilder.buildSendMessage(topic, messageBody);
    if (connectionHandler.sendLine(sendMessage)) {
        eventsByChannel[topic].push_back(messageBody);
        std::cout << "Message sent to topic: " << topic << std::endl;
    } else {
        std::cerr << "Failed to send message." << std::endl;
    }
}

void StompProtocol::disconnect() {
    std::lock_guard<std::mutex> lock(protocolMutex);
    if (!isConnected) {
        std::cerr << "Cannot disconnect. Client is not connected." << std::endl;
        return;
    }

    std::string disconnectMessage = messageBuilder.buildDisconnectMessage();
    connectionHandler.sendLine(disconnectMessage);  // Send DISCONNECT message
    if (!connectionHandler.sendLine(disconnectMessage)) {
        std::cerr << "Failed to send DISCONNECT message." << std::endl;
    } else {
        std::cout << "Disconnected from server." << std::endl;
        isConnected = false;
        subscriptionIds.clear();
    }
}

void StompProtocol::process(const std::string& message) {
    std::lock_guard<std::mutex> lock(protocolMutex);
    if (message.find("CONNECTED") != std::string::npos) {
        std::cout << "Server responded with CONNECTED. Connection established." << std::endl;
        }    
                   else if (message.find("MESSAGE") != std::string::npos) {
            std::size_t subPos = message.find("subscription:");
            std::size_t msgIdPos = message.find("message-id:");
            std::size_t destPos = message.find("destination:");
            std::size_t bodyStart = message.find("\n\n") + 2;

            if (subPos != std::string::npos && msgIdPos != std::string::npos && destPos != std::string::npos) {
                std::string subscription = message.substr(subPos + 13, message.find('\n', subPos) - subPos - 13);
                std::string messageId = message.substr(msgIdPos + 11, message.find('\n', msgIdPos) - msgIdPos - 11);
                std::string destination = message.substr(destPos + 12, message.find('\n', destPos) - destPos - 12);
                std::string messageBody = message.substr(bodyStart);

                std::cout << "MESSAGE received:" << std::endl;
                std::cout << "Subscription ID: " << subscription << std::endl;
                std::cout << "Message ID: " << messageId << std::endl;
                std::cout << "Destination: " << destination << std::endl;
                std::cout << "Message Body: " << messageBody << std::endl;
            }
        } 
        else if (message.find("RECEIPT") != std::string::npos) {
            std::size_t idPos = message.find("receipt-id:");
            if (idPos != std::string::npos) {
                std::string receiptId = message.substr(idPos + 11, message.find('\n', idPos) - idPos - 11);
                std::cout << "RECEIPT received with ID: " << receiptId << std::endl;
            }
        } 
        else if (message.find("ERROR") != std::string::npos) {
            std::size_t msgPos = message.find("message:");
            if (msgPos != std::string::npos) {
                std::string errorMessage = message.substr(msgPos + 8, message.find('\n', msgPos) - msgPos - 8);
                std::cerr << "ERROR received: " << errorMessage << std::endl;
            } else {
                std::cerr << "ERROR received: " << message << std::endl;
            }
        } 
        else {
            std::cerr << "Unknown message type received: " << message << std::endl;
        }
    }


void StompProtocol::summarize(const std::string& channelName, const std::string& user, const std::string& outputFileName) {
    std::lock_guard<std::mutex> lock(protocolMutex);
    if (eventsByChannel.find(channelName) == eventsByChannel.end()) {
        std::cerr << "No events found for channel: " << channelName << std::endl;
        return;
    }

    const auto& events = eventsByChannel[channelName];
    int totalReports = 0;
    int activeCount = 0;
    int forcesArrivalCount = 0;

    std::vector<std::string> reportLines;
    for (const auto& event : events) {
        if (event.find("user:" + user) != std::string::npos) {
            totalReports++;
            if (event.find("active:true") != std::string::npos) {
                activeCount++;
            }
            if (event.find("forces_arrival_at_scene:true") != std::string::npos) {
                forcesArrivalCount++;
            }

            size_t descriptionStart = event.find("description:") + 12;
            size_t descriptionEnd = event.find("\n", descriptionStart);
            std::string description = event.substr(descriptionStart, descriptionEnd - descriptionStart);
            if (description.length() > 27) {
                description = description.substr(0, 27) + "...";
            }

            reportLines.push_back("Report_" + std::to_string(totalReports) + ":\n" + event);
        }
    }

    std::ofstream outFile(outputFileName);
    if (!outFile.is_open()) {
        std::cerr << "Failed to open file: " << outputFileName << std::endl;
        return;
    }

    outFile << "Channel " << channelName << "\n";
    outFile << "Stats:\n";
    outFile << "Total: " << totalReports << "\n";
    outFile << "active: " << activeCount << "\n";
    outFile << "forces arrival at scene: " << forcesArrivalCount << "\n";
    outFile << "\nEvent Reports:\n";
    for (const auto& line : reportLines) {
        outFile << line << "\n";
    }

    outFile.close();
    std::cout << "Summary written to file: " << outputFileName << std::endl;
}

bool StompProtocol::shouldTerminate() const {
    return shouldTerminateConnection;
}
