package bgu.spl.net.impl.stomp;

import java.util.HashMap;
import java.util.Map;

import bgu.spl.net.api.StompMessagingProtocol;
import bgu.spl.net.srv.Connections;
import bgu.spl.net.srv.NonBlockingConnectionHandler;
import bgu.spl.net.srv.connectionsImpl;

public class StompMessagingProtocolImpl <T>implements StompMessagingProtocol <T>{
    int connectionId;
    connectionsImpl<T> connections;

    @Override
    public void start(int connectionId, Connections<T> connections) {
        this.connections=(connectionsImpl<T>) connections;
        this.connectionId=connectionId;
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

        }
        else if(command.equals("SUBSCRIBE")){

        }
        else if(command.equals("DISCONNECT")){

        }
        else{
            System.out.println("Wrong messege sent , not in the protocol");
        }
                return null; 
               
    }

    @Override
    public boolean shouldTerminate() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'shouldTerminate'");
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


    private void handleConnect(Map<String, String> headers) {
        String acceptVersion = headers.get("accept-version");
        String host = headers.get("host");
        String login = headers.get("login");
        String passcode = headers.get("passcode");
        if (acceptVersion == null || !acceptVersion.equals("1.2")) {
            connections.send(this.getConnectionId(), "ERROR\nmessage:Unsupported STOMP version\n^@");
            return;
        }
        else if (host == null || !host.equals("stomp.cs.bgu.ac.il")) {
            connections.send(this.getConnectionId(), "ERROR\nmessage:Invalid host\n^@");
            return;
        }
        else if (login == null || passcode == null) {
            connections.send(this.getConnectionId(), "ERROR\nmessage:Missing authentication details\n^@");
            return;
        }
        else {
            connections.addClient(connectionId, );//////new conection handler
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


