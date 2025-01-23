#include"../include/StompProtocol.h"
#include <iostream>
#include <string>
#include <unordered_map>
#include <mutex>

class StompProtocol {
    private:
        bool isConnect;
        bool error;
        std::string username;
        std::string host;
        std::unordered_map<std::string, int> subscriptionIds;
        std::mutex protocolMutex;
        std::mutex ocolMutex;//checkkkkkk




    StompProtocol(string username,string host) :  isConnect(false), error(false), username(username), host(host) {}
    
    
    bool connect()











}