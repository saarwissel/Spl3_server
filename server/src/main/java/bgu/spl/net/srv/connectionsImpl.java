package bgu.spl.net.srv;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

public class connectionsImpl<T> implements  Connections<T> {
    
    private static connectionsImpl<?> instance;

    private ConcurrentMap<Integer, ConnectionHandler<T>> activeClients; // connectionID+handler
    private ConcurrentMap<String,String> users;//// username+password
    private ConcurrentMap<String,Integer> userID;// username+conectionID
    private ConcurrentMap<Integer,String> loginID;//// conectionID+username
    private ConcurrentMap<String,String> SubID; // subID + username
    private ConcurrentMap<String,String> IDchannel; // subID + chanel
    private ConcurrentMap<String, LinkedBlockingQueue <String>>  channels;//chanale + list of usernemas

    String method;
    int messageID;


    connectionsImpl(){
        this.activeClients= new ConcurrentHashMap<>();
        this.channels=new ConcurrentHashMap<>();
        this.users=new ConcurrentHashMap<>();
        this.userID=new ConcurrentHashMap<>();
        this.loginID=new ConcurrentHashMap<>();
        this.SubID=new ConcurrentHashMap<>();
        this.IDchannel=new ConcurrentHashMap<>();
        method  ="";
        this.messageID=0;
    }
    public static synchronized <T> connectionsImpl<T> getInstance() {
        if (instance == null) {
            instance = new connectionsImpl<>();
        }
        return (connectionsImpl<T>) instance;
    }

    @Override
    public boolean send(int connectionId, Object msg) {
        ConnectionHandler handler=activeClients.get(connectionId);
        if(handler!=null){
        synchronized(handler){
            handler.send((T)msg);
            return true;
            }
        }
        else
        {
        return false;
        } 
    }

    @Override
    public void send(String channel, Object msg) {
        if(channels.get(channel)!=null){
            synchronized(channels.get(channel)){
            for(String login: channels.get(channel)){
                if(userID.get(login)!=null){
                    send(userID.get(login),msg);
                }
                }
            }
        }
    }

    @Override
    public void disconnect(int connectionId) {
        ConnectionHandler handler=activeClients.get(connectionId);
        if(handler!=null){
            synchronized(handler){
                String username=loginID.get(connectionId);
                userID.remove(connectionId, username);
                loginID.remove(username,connectionId);
                activeClients.remove(connectionId);    
            }
        }     
    }
    public void addClient(int connectionId, ConnectionHandler<T> handler) {
        activeClients.put(connectionId, handler);
    }

    public void subscribeChanel(String chanel,String username){
        if(channels.get(chanel)!=null){

            channels.get(chanel).add(username);
        }
        else{
            LinkedBlockingQueue<String> subs=new LinkedBlockingQueue<>();
            subs.add(username);
            channels.put(chanel,subs); 
        }
    }
    public ConcurrentMap<String, LinkedBlockingQueue<String>> getChannels() {
        return channels;
    }
    public ConcurrentMap<Integer, ConnectionHandler<T>> getActiveClients() {
        return activeClients;
    }
    public String getMethod() {
        return method;
    }

    public int getMessageID() {
        return messageID;
    }

    public ConcurrentMap<String, Integer> getUserID() {
        return userID;
    }

    public ConcurrentMap<String, String> getUsers() {
        return users;
    }

    public ConcurrentMap<Integer, String> getLoginID() {
        return loginID;
    }

    public ConcurrentMap<String, String> getSubID() {
        return SubID;
    }

    public void setMessageID() {
        this.messageID = messageID++;
    }

    public ConcurrentMap<String, String> getIDchannel() {
        return IDchannel;
    }
}
