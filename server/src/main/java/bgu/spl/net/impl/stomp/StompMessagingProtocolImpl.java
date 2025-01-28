package bgu.spl.net.impl.stomp;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import bgu.spl.net.srv.StompMessageBuilder;
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
        else if(command.equals("DISCONNECT")){
            response=(T)this.handleDisconnect(headers);
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
            return StompMessageBuilder.builderrotMessage(connections.getMessageID(),"ID not found");
        }
        else {
            connections.getChannels().get(destination).remove(username);
            connections.getSubID().remove(idSub);
            connections.getIDchannel().remove(idSub);
            int Id = this.connectionId;
            return StompMessageBuilder.buildUnsubscribeMessage(Id + "");
        
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
                return StompMessageBuilder.builderrotMessage(connections.getMessageID(),"ID not found");

            } else {
                connections.disconnect(this.getConnectionId());
                int Id = this.connectionId;
                Terminate = true;
                return StompMessageBuilder.buildUnsubscribeMessage(Id + "");
            }
        }
        else{
            System.out.println("Alredy terminated");
            return StompMessageBuilder.builderrotMessage(connections.getMessageID(),"message:Alredy terminated");

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
            connections.getIDchannel().put(idSub, destination);
        }
        else{
            connections.getSubID().put(idSub,username);
            connections.subscribeChanel(destination,username);
            connections.getIDchannel().put(idSub,destination);
        }
        String id=this.getConnectionId()+"";
        return StompMessageBuilder.buildSubscribeedMessage(id);
            
        }
    

        private String handleSend(Map<String, String> headers) {
        String destination = headers.get("destination");
        if (!connections.getChannels().containsKey(destination)) {
            connections.disconnect(this.getConnectionId());
            return StompMessageBuilder.builderrotMessage(connections.getMessageID(),"Topic not found");


        }
        String body = headers.get("body");
        connections.setMessageID();
        int messageId = connections.getMessageID();
        String message=StompMessageBuilder.buildSendMessage(this.getConnectionId(), messageId, destination, body);
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
            return StompMessageBuilder.builderrotMessage(connections.getMessageID(),"Unsupported STOMP version");
          
        }
        if (host == null || !host.equals("127.0.0.1")) {
            connections.disconnect(this.getConnectionId());
            return StompMessageBuilder.builderrotMessage(connections.getMessageID(),"nvalid host");
        }
    
        if (login == null || passcode == null) {
            connections.disconnect(this.getConnectionId());
            return StompMessageBuilder.builderrotMessage(connections.getMessageID(),"Missing authentication details");
        }
    
        if (connectionsImpl.getInstance().getUsers().get(login) == null) {
            connections.getUsers().put(login, passcode);
        }
        connections.getUserID().put(login, this.getConnectionId());
        connections.getLoginID().put(this.getConnectionId(), login);
        String accepetVrsion="1.2";
        return StompMessageBuilder.buildConnectedMessage(accepetVrsion);
    }

    
    public int getConnectionId() {
        return connectionId;
    }

    public connectionsImpl<T> getConnections() {
        return connections;
    }
}


