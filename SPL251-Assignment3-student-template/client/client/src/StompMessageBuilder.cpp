#include "StompMessageBuilder.h"
#include <sstream>

std::string StompMessageBuilder::buildConnectMessage(const std::string& host, const std::string& port, const std::string& username, const std::string& password) {
    if (host.empty() || port.empty() || username.empty() || password.empty()) {
        throw std::invalid_argument("Host, port, username, and password cannot be empty for CONNECT");
    }

    std::ostringstream oss;
    oss << "CONNECT" << std::endl;
    oss << "accept-version:1.2" << std::endl;
    oss << "host:" << host << std::endl;
    oss << "login:" << username << std::endl;
    oss << "passcode:" << password << std::endl;
    oss << "^@" << std::endl; // סוף ההודעה
    return oss.str();
}

std::string StompMessageBuilder::buildSubscribeMessage(const std::string& destination, int id) {
    if (destination.empty()) {
        throw std::invalid_argument("Destination cannot be empty for SUBSCRIBE");
    }

    std::ostringstream oss;
    oss << "SUBSCRIBE" << std::endl;
    oss << "destination:/" << destination << std::endl;
    oss << "id:" << id << std::endl;
    oss << "^@" << std::endl; // סוף ההודעה
    return oss.str();
}

std::string StompMessageBuilder::buildUnsubscribeMessage(int id) {
    if (id <= 0) {
        throw std::invalid_argument("ID must be greater than 0 for UNSUBSCRIBE");
    }

    std::ostringstream oss;
    oss << "UNSUBSCRIBE" << std::endl;
    oss << "id:" << id << std::endl;
    oss << "^@" << std::endl; // סוף ההודעה
    return oss.str();
}

// פונקציה לבניית הודעת SEND
std::string StompMessageBuilder::buildSendMessage(const std::string& destination, const std::string& messageBody) {
    if (destination.empty()) {
        throw std::invalid_argument("Destination cannot be empty for SEND");
    }
    if (messageBody.empty()) {
        throw std::invalid_argument("Message body cannot be empty for SEND");
    }

    std::ostringstream oss;
    oss << "SEND" << std::endl;
    oss << "destination:/" << destination << std::endl;
    oss << std::endl; // רווח לפני גוף ההודעה
    oss << messageBody << std::endl; // גוף ההודעה
    oss << "^@" << std::endl; // סוף ההודעה
    return oss.str();
}

// פונקציה לבניית הודעת DISCONNECT
std::string StompMessageBuilder::buildDisconnectMessage() {
    std::ostringstream oss;
    oss << "DISCONNECT" << std::endl;
    oss << "^@" << std::endl; // סוף ההודעה
    return oss.str();
}
