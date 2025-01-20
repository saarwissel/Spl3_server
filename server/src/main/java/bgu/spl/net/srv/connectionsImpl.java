package bgu.spl.net.srv;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class connectionsImpl<T> implements Connections {

    private ConcurrentMap<Integer, ConnectionHandler<T>> activeClients;
    private ConcurrentMap<String, BlockingQueue <Integer>> subscriptions;
    int messageID;


    connectionsImpl(){
        this.activeClients= new ConcurrentHashMap<>();
        this.subscriptions=new ConcurrentHashMap<>();
        this.messageID=0;
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
        // TODO Auto-generated method stub
    }

    @Override
    public void send(String channel, Object msg) {
        if(subscriptions.get(channel)!=null){
            synchronized(subscriptions.get(channel)){
            for(Integer id: subscriptions.get(channel)){
                this.send(id, msg);
            }
            }
        }
    }

    @Override
    public void disconnect(int connectionId) {
        if(connectionId>=0){
            activeClients.remove(connectionId);
            for (String channel : subscriptions.keySet()) {
                subscriptions.get(channel).remove(connectionId);
            }
        }
    }
}
