package bgu.spl.net.srv;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

public class connectionsImpl<T> implements  Connections<T> {
    
    private static connectionsImpl<?> instance;

    private ConcurrentMap<Integer, ConnectionHandler<T>> activeClients;
    private ConcurrentMap<String, LinkedBlockingQueue <Integer>>  channels;
    String method;
    int messageID;


    connectionsImpl(){
        this.activeClients= new ConcurrentHashMap<>();
        this.channels=new ConcurrentHashMap<>();
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
            for(Integer id: channels.get(channel)){
                this.send(id, msg);
            }
            }
        }
    }

    @Override
    public void disconnect(int connectionId) {
        ConnectionHandler handler=activeClients.get(connectionId);
        if(handler!=null){
            try {
                activeClients.get(connectionId).close();
                activeClients.remove(connectionId);
             for (String channel : channels.keySet()) {
                channels.get(channel).remove(connectionId);
                }
            } catch (IOException e) {
                System.out.println("No handler exist");
            }
            
        }
        
    }
    public void addClient(int connectionId, ConnectionHandler<T> handler) {
        activeClients.put(connectionId, handler);
    }

    public void subscribeChanel(String chanel,int connectionId){
        if(channels.get(chanel)!=null){
            channels.get(chanel).add(connectionId);
        }
        else{
            LinkedBlockingQueue<Integer> subs=new LinkedBlockingQueue<>();
            subs.add(connectionId);
            channels.put(chanel,subs); 
        }
    }
    public ConcurrentMap<String, LinkedBlockingQueue<Integer>> getChannels() {
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

    public void setMessageID() {
        this.messageID = messageID++;
    }
}
