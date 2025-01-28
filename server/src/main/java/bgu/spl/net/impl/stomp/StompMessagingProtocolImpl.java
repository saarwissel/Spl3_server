package bgu.spl.net.impl.stomp;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import bgu.spl.net.api.StompMessagingProtocol;
import bgu.spl.net.srv.Connections;
import bgu.spl.net.srv.NonBlockingConnectionHandler;
import bgu.spl.net.srv.connectionsImpl;

public class StompMessagingProtocolImpl <T>implements StompMessagingProtocol <T>{
    int connectionId;
    connectionsImpl<T> connections;
    boolean Terminate;

    @Override
    public void start(int connectionId, Connections<T> connections) {
        this.connections=(connectionsImpl<T>) connections;
        this.connectionId=connectionId;
        Terminate=false;
        }

    @Override
    public synchronized T  process(Object message) {
        if (!(message instanceof String)) {
            throw new IllegalArgumentException("Invalid message type: " + message.getClass());
        }
        String msg = (String) message;
        Map<String, String> headers = parseHeaders(msg);
        String[] lines = msg.split("\n");
        String command = lines[0];
        T response = null;

        if(command.equals("CONNECT")){
            System.out.println("CONNECT");
            response=(T)handleConnect(headers);
            System.out.println("response: "+response);
        }
        else if(command.equals("SEND")){
            response=(T)handleSend(headers);
        }
        else if(command.equals("SUBSCRIBE")){
            response= (T)handleSub(headers);
        }
        else if(command.equals("UNSUBSCRIBE")){
            response =(T)this.handleUnsub(headers);
        }
        else if(command.equals("DISCONNECT")){/////++++++++++++
            response=(T)this.handleDisconnect(headers);
        }
        else{
            connections.disconnect(this.getConnectionId());
            System.out.println("Wrong messege sent , not in the protocol");
            response= (T)"ERROR\nmessage:Invalid MESSAGE\n^@";

        }
        return response;               
    }

    @Override
    public boolean shouldTerminate() {
        return Terminate;
    }



    private String handleUnsub(Map<String, String> headers) {
        String idSub=headers.get("id");
        String destination = connections.getIDchannel().get(idSub);
        String username = connections.getSubID().get(idSub);
        if (username == null) {
            connections.disconnect(this.getConnectionId());
            return"ERROR\nmessage:ID not found: " + connectionId + "\n^@";
        }
        else {
            connections.getChannels().get(destination).remove(username);
            connections.getSubID().remove(idSub);
            connections.getIDchannel().remove(idSub);
            int Id = this.connectionId;
            String message = String.format(
                    "UNSUBSCRIBED\nid:%s\n\n^@",
                    Id
            );
            return message;
        }
    }



    private Map<String, String> parseHeaders(String msg) {
        Map<String, String> headers = new HashMap<>();
        String[] lines = msg.split("\n");
        for (int i = 1; i < lines.length; i++) { 
            if (lines[i].isEmpty()) break; 
            String[] parts = lines[i].split(":", 2);
            if (parts.length == 2) {
                headers.put(parts[0], parts[1]);
            }
        }
        return headers;
    }

    private String handleDisconnect(Map<String, String> headers) {
        if(!shouldTerminate()) {
            String receipt = headers.get("receipt");
            if (!connections.getActiveClients().containsKey(receipt)) {
                connections.disconnect(this.getConnectionId());
                return"ERROR\nmessage:ID not found: " + receipt + "\n^@";
            } else {
                connections.disconnect(this.getConnectionId());
                int Id = this.connectionId;
                String message = String.format(
                        "SUBSCRIBED\nid:%s\ndestination:%s\n\n^@"
                        , Id
                );
                Terminate = true;
                return message;
            }
        }
        else{
            System.out.println("Alredy terminated");
            return "ERROR\nmessage:Alredy terminated\n^@";
        }
    }


    private String handleSub(Map<String, String> headers) {
        String destination=headers.get("destination");
        String idSub=headers.get("id");
        String username = connections.getLoginID().get(this.getConnectionId());
        if (!connections.getChannels().containsKey(destination)) {
            String chanel = destination;
            LinkedBlockingQueue subs = new LinkedBlockingQueue();
            subs.add(username);
            connections.getChannels().put(chanel, subs);
            connections.getSubID().put(idSub, username);
            connections.subscribeChanel(destination, username);
            connections.getIDchannel().put(idSub, destination);
        }
        else{
        connections.getSubID().put(idSub,username);
        connections.subscribeChanel(destination,username);
        connections.getIDchannel().put(idSub,destination);
        }
        int messageId = connections.getMessageID();
        String message = String.format(
                    "SUBSCRIBED\nid:%s\ndestination:%s\n\n^@",
                    messageId,
                    destination
            );
            return message;
        }
    

        private String handleSend(Map<String, String> headers) {
        String destination = headers.get("destination");
        if (!connections.getChannels().containsKey(destination)) {
            connections.disconnect(this.getConnectionId());
            return "ERROR\nmessage:Topic not found: " + destination + "\n^@";

        }
        String body = headers.get("body");
        connections.setMessageID();
        int messageId = connections.getMessageID();
        String message = String.format(
                "MESSAGE\nsubscription:%d\nmessage-id:%d\ndestination:%s\n\n%s\n^@",
                this.getConnectionId(),
                messageId,
                destination,
                body != null ? body : ""
        );
        connections.send(destination, message);
        return message;
    }


    private String handleConnect(Map<String, String> headers) {
        String acceptVersion = headers.get("accept-version");
        String host = headers.get("host");
        String login = headers.get("login");
        String passcode = headers.get("passcode");
    

        if (acceptVersion == null || !acceptVersion.equals("1.2")) {
            connections.disconnect(this.getConnectionId());
            return "ERROR\nmessage:Unsupported STOMP version\n^@";            
        }

        if (host == null || !host.equals("127.0.0.1")) {
            connections.disconnect(this.getConnectionId());
            return"ERROR\nmessage:Invalid host\n^@";

        }
    
        if (login == null || passcode == null) {
            connections.disconnect(this.getConnectionId());
            return"ERROR\nmessage:Missing authentication details\n^@";
        }
    
        if (connectionsImpl.getInstance().getUsers().get(login) == null) {
            connections.getUsers().put(login, passcode);
        }
        connections.getUserID().put(login, this.getConnectionId());
        connections.getLoginID().put(this.getConnectionId(), login);
        return "CONNECTED\nversion:1.2\n^@";
    }

    
    public int getConnectionId() {
        return connectionId;
    }

    public connectionsImpl<T> getConnections() {
        return connections;
    }
}


