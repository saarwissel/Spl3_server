#pragma once

#include "../include/ConnectionHandler.h"
#include "../include/StompMessageBuilder.h"
#include <string>
#include <unordered_map>
#include <vector>
#include <mutex>

class StompProtocol {

private:
    bool isConnected;
    bool shouldTerminateConnection;
    std::string username;
    std::unordered_map<std::string, int> subscriptionIds;
    std::mutex protocolMutex;
    ConnectionHandler& connectionHandler;
    StompMessageBuilder messageBuilder; // חשוב לכלול את ה-builder הזה
    std::unordered_map<std::string, std::vector<std::string>> eventsByChannel;

public:
    StompProtocol(ConnectionHandler& handler);

    bool connect(const std::string& host, const std::string& username, const std::string& password);
    void subscribe(const std::string& topic);
    void unsubscribe(const std::string& topic);
    void send(const std::string& topic, const std::string& messageBody);
    void disconnect();
    void process(const std::string& message);
    void summarize(const std::string& channelName, const std::string& user, const std::string& outputFileName);
    bool shouldTerminate() const;
};
