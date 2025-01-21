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
    public T process(Object message) {
        if (!(message instanceof String)) {
            throw new IllegalArgumentException("Invalid message type: " + message.getClass());
        }
        String msg = (String) message;
        Map<String, String> headers = parseHeaders(msg);
        String[] lines = msg.split("\n");
        String command = lines[0];

        if(command.equals("CONNECT")){
            this.handleConnect(headers);
        }
        else if(command.equals("SEND")){
            this.handleSend(headers);
        }
        else if(command.equals("SUBSCRIBE")){
            this.handleSub(headers);
        }
        else if(command.equals("DISCONNECT")){
            this.handleDisconnect(headers);
        }
        else{
            connections.send(this.getConnectionId(), "ERROR\nmessage:Invalid MESSAGE\n^@");
            System.out.println("Wrong messege sent , not in the protocol");

        }
                return null; 
               
    }

    @Override
    public boolean shouldTerminate() {
        return Terminate;
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

    private void handleDisconnect(Map<String, String> headers) {
        if(!shouldTerminate()) {
            String receipt = headers.get("receipt");
            if (!connections.getActiveClients().containsKey(receipt)) {
                connections.send(this.getConnectionId(), "ERROR\nmessage:ID not found: " + receipt + "\n^@");
            } else {
                connections.disconnect(this.getConnectionId());
                int messageId = connections.getMessageID();
                String message = String.format(
                        "SUBSCRIBED\nid:%s\ndestination:%s\n\n^@"
                        , messageId
                );
                Terminate = true;
                connections.send(this.getConnectionId(), message);
            }
        }
        else{
            System.out.println("Alredy terminated");
        }
    }


    private void handleSub(Map<String, String> headers) {
        String destination=headers.get("destination");
        String id=headers.get("id");
        if (!connections.getChannels().containsKey(destination)) {
            connections.send(this.getConnectionId(), "ERROR\nmessage:Topic not found: " + destination + "\n^@");
        }
        else if (!connections.getActiveClients().containsKey(id)) {
            connections.send(this.getConnectionId(), "ERROR\nmessage:ID not found: " + id + "\n^@");
        }
        else{
            connections.subscribeChanel(destination,this.getConnectionId());
            int messageId = connections.getMessageID();
            String message = String.format(
                    "SUBSCRIBED\nid:%s\ndestination:%s\n\n^@",
                    id,
                    destination
            );
            connections.send(this.getConnectionId(), message);
        }
    }

        private void handleSend(Map<String, String> headers) {
        String destination = headers.get("destination");
        if (!connections.getChannels().containsKey(destination)) {
            connections.send(this.getConnectionId(), "ERROR\nmessage:Topic not found: " + destination + "\n^@");
            return;
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
    }


    private void handleConnect(Map<String, String> headers) {
        String acceptVersion = headers.get("accept-version");
        String host = headers.get("host");
        String login = headers.get("login");
        String passcode = headers.get("passcode");
        if (acceptVersion == null || !acceptVersion.equals("1.2")) {
            connections.send(this.getConnectionId(), "ERROR\nmessage:Unsupported STOMP version\n^@");
        }
        else if (host == null || !host.equals("stomp.cs.bgu.ac.il")) {
            connections.send(this.getConnectionId(), "ERROR\nmessage:Invalid host\n^@");
        }
        else if (login == null || passcode == null) {
            connections.send(this.getConnectionId(), "ERROR\nmessage:Missing authentication details\n^@");
        }
        else {
            connections.send(this.getConnectionId(), "CONNECTED\nversion:1.2\n^@");
        }


    }

    public int getConnectionId() {
        return connectionId;
    }

    public connectionsImpl<T> getConnections() {
        return connections;
    }
}


