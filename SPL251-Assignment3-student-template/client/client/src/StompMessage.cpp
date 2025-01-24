#include <iostream>
#include <sstream>
#include <string>
#include <stdexcept>

// מחלקה לבניית ההודעות
class StompMessageBuilder {
public:
    // פונקציה לבניית הודעת CONNECT
    std::string buildConnectMessage(const std::string& host, const std::string& port, const std::string& username, const std::string& password) {
        // טיפול בשגיאות
        if (host.empty() || port.empty() || username.empty() || password.empty()) {
            throw std::invalid_argument("Host, port, username, and password cannot be empty for CONNECT");
        }

        std::ostringstream oss;
        oss << "CONNECT" << std::endl;
        oss << "accept-version:1.2" << std::endl;
        oss << "host:" << host << std::endl;
        oss << "login:" << username << std::endl;
        oss << "passcode:" << password << std::endl;
        oss << "^@" << std::endl;  // סוף ההודעה
        return oss.str();
    }

    // פונקציה לבניית הודעת SUBSCRIBE
    std::string buildSubscribeMessage(const std::string& destination, int id) {
        // destination ריק, לא נוכל לשלוח את ההודעה
        if (destination.empty()) {
            throw std::invalid_argument("Destination cannot be empty for SUBSCRIBE");
        }

        std::ostringstream oss;
        oss << "SUBSCRIBE" << std::endl;
        oss << "destination:/" << destination << std::endl;
        oss << "id:" << id << std::endl;
        oss << "^@" << std::endl;  // סוף ההודעה
        return oss.str();
    }

    // פונקציה לבניית הודעת UNSUBSCRIBE
    std::string buildUnsubscribeMessage(int id) {
        //  אם id לא חוקי (שלילי או אפס), לא נוכל לשלוח את ההודעה
        if (id <= 0) {
            throw std::invalid_argument("ID must be greater than 0 for UNSUBSCRIBE");
        }

        std::ostringstream oss;
        oss << "UNSUBSCRIBE" << std::endl;
        oss << "id:" << id << std::endl;
        oss << "^@" << std::endl;  // סוף ההודעה
        return oss.str();
    }

    // פונקציה לבניית הודעת SEND
    std::string buildSendMessage(const std::string& destination, const std::string& messageBody) {
        // אם destination או body ריקים, לא נוכל לשלוח את ההודעה
        if (destination.empty()) {
            throw std::invalid_argument("Destination cannot be empty for SEND");
        }
        if (messageBody.empty()) {
            throw std::invalid_argument("Message body cannot be empty for SEND");
        }

        std::ostringstream oss;
        oss << "SEND" << std::endl;
        oss << "destination:/" << destination << std::endl;
        oss << std::endl;  // רווח לפני גוף ההודעה
        oss << messageBody << std::endl;  // גוף ההודעה
        oss << "^@" << std::endl;  // סוף ההודעה
        return oss.str();
    }

    // פונקציה לבניית הודעת DISCONNECT
    std::string buildDisconnectMessage() {
        std::ostringstream oss;
        oss << "DISCONNECT" << std::endl;
        oss << "^@" << std::endl;  // סוף ההודעה
        return oss.str();
    }
};

