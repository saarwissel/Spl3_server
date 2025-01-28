#ifndef STOMP_MESSAGE_BUILDER_H
#define STOMP_MESSAGE_BUILDER_H

#include <string>
#include <stdexcept>


class StompMessageBuilder {
public:
    std::string buildConnectMessage(const std::string& host, const std::string& port, const std::string& username, const std::string& password);
    std::string buildSubscribeMessage(const std::string& destination, int id);
    std::string buildUnsubscribeMessage(int id);
    std::string buildSendMessage(const std::string& destination, const std::string& messageBody);
    std::string buildDisconnectMessage();
};

#endif // STOMP_MESSAGE_BUILDER_H
