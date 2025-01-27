#include "../include/StompProtocol.h"
#include "StompMessage.cpp"
#include "ConnectionHandler.h"
#include <iostream>
#include <unordered_map>
#include <mutex>
#include <string>
#include <fstream>

class StompProtocol {
private:
    bool isConnected;
    bool shouldTerminateConnection;
    std::string username;
    std::unordered_map<std::string, int> subscriptionIds; // cannel+subID  Map topic to subscription ID
    std::mutex protocolMutex;
    ConnectionHandler& connectionHandler;
    StompMessageBuilder messageBuilder;
    std::unordered_map<std::string, std::vector<std::string>> eventsByChannel;// cannel+event

public:
    StompProtocol(ConnectionHandler& handler)
        : isConnected(false), shouldTerminateConnection(false), connectionHandler(handler) {}

        
    bool connect(const std::string& host, const std::string& username, const std::string& password) {
        std::lock_guard<std::mutex> lock(protocolMutex);
        if (isConnected) {
            std::cout << "The client is already logged in. Log out before trying again." << std::endl;
            return false;
        }

        // Build and send CONNECT message
        std::string connectMessage = messageBuilder.buildConnectMessage(host, "7777", username, password);
        connectionHandler.sendLine(connectMessage);  // Send CONNECT message
        if (!connectionHandler.sendLine(connectMessage)) {
            std::cerr << "Failed to send CONNECT message." << std::endl;
            return false;
        }

        // Handle server response
        std::string response;
        if (!connectionHandler.getLine(response)) {//reed the answer from the server and put it in the response
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

    void subscribe(const std::string& topic) {
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

    void unsubscribe(const std::string& topic) {
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

    void send(const std::string& topic, const std::string& messageBody) {
        std::lock_guard<std::mutex> lock(protocolMutex);
        if (!isConnected || subscriptionIds.find(topic) == subscriptionIds.end()) {
            std::cerr << "Cannot send message. Either not connected or not subscribed to topic." << std::endl;
            return;
        }

        std::string sendMessage = messageBuilder.buildSendMessage(topic, messageBody);
        if(connectionHandler.sendLine(sendMessage)){
            eventsByChannel[topic].push_back(messageBody);
            std::cout << "Message sent to topic: " << topic << std::endl;
        }
        else 
         {
            std::cerr << "Failed to send message." << std::endl;
        } 
    }

    void disconnect() {
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

    void process(const std::string& message) {
        std::lock_guard<std::mutex> lock(protocolMutex);
        // Parse incoming message and handle it accordingly
        if (message.find("MESSAGE") != std::string::npos) {
            // Extract message-id and display it
            std::size_t idPos = message.find("message-id:");
            if (idPos != std::string::npos) {
                std::size_t idEnd = message.find('\n', idPos);
                std::string messageId = message.substr(idPos + 11, idEnd - idPos - 11);
                std::cout << "Received message with ID: " << messageId << std::endl;
            }
            std::cout << "Processed message: " << message << std::endl;
        } else if (message.find("ERROR") != std::string::npos) {
            std::cerr << "Error received: " << message << std::endl;
        }
    }


    void summarize(const std::string& channelName, const std::string& user, const std::string& outputFileName) {
        std::lock_guard<std::mutex> lock(protocolMutex);
    
        if (eventsByChannel.find(channelName) == eventsByChannel.end()) {
        std::cerr << "No events found for channel: " << channelName << std::endl;
            return;
        }

        const auto& events = eventsByChannel[channelName];
        int totalReports = 0;
        int activeCount = 0;
        int forcesArrivalCount = 0;

         // ניתוח האירועים
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

                // יצירת תקציר של תיאור האירוע
                size_t descriptionStart = event.find("description:") + 12;
                size_t descriptionEnd = event.find("\n", descriptionStart);
                std::string description = event.substr(descriptionStart, descriptionEnd - descriptionStart);
                if (description.length() > 27) {
                description = description.substr(0, 27) + "...";
                }

                // הוספת פרטי האירוע
                reportLines.push_back("Report_" + std::to_string(totalReports) + ":\n" + event);
            }
        }

        // כתיבת הדוח לקובץ
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

    bool shouldTerminate() const {
        return shouldTerminateConnection;
    }
};
