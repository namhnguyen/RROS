package rros.java.adapters;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import rros.SocketListener;
import rros.java.SocketAdapter;

import java.io.IOException;
import java.net.URI;
////////////////////////////////////////////////////////////////////////////////
/**
 * Created by namnguyen on 3/9/15.
 */
@WebSocket
public class JettyWebSocketClientAdapter extends SocketAdapter{

    public JettyWebSocketClientAdapter(URI uri){
        this.uri = uri;
        this.client = new WebSocketClient();
    }
    public void connect(){

        try{
            client.setConnectTimeout(5000);
            client.start();
            ClientUpgradeRequest request = new ClientUpgradeRequest();
            client.connect(this,this.uri,request);
            System.out.println("Connecting");
        }catch(Throwable t){
            t.printStackTrace();
            this.reconnect();
        }
    }
    private void reconnect(){
        boolean connectOK =false;
        while (!connectOK && !this.closing){
            this.session = null;
            try{
                client.start();
                ClientUpgradeRequest request = new ClientUpgradeRequest();
                client.connect(this,this.uri,request);
                connectOK = true;
            }catch(Throwable t){
                connectOK = false;
                System.out.println("Retry to connect to server... "+t.toString());
                try {
                    Thread.sleep(1000);
                }catch(Exception exc) { }
            }
        }
    }
    @OnWebSocketError
    public void onError(Session session,Throwable error){
        System.out.println(error.toString());
        try {
            Thread.sleep(1000);
        }catch(Exception exc){ }
        this.reconnect();
    }
    @OnWebSocketConnect
    public void onConnect(Session session){
        System.out.println("Connected");
        this.session = session;
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason){
        System.out.println("Close");
        if (!this.closing){
            this.reconnect();
        }else {
            try {
                this.client.stop();
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }
    }

    @OnWebSocketMessage
    public void onMessage(String msg){
        for(SocketListener listener : this.getListeners()){
            listener.onReceived(msg);
        }
    }

    @SuppressWarnings("unused")
    private Session session;
    private URI uri;

    @Override
    public void send(String message) {
        if (session!=null) {
            try {
                session.getRemote().sendString(message);
            }catch (Exception exc){
                throw new RuntimeException(exc);
            }
        }
    }

    /**
     * Closes this stream and releases any system resources associated
     * with it. If the stream is already closed then invoking this
     * method has no effect.
     * <p>
     * <p> As noted in {@link AutoCloseable#close()}, cases where the
     * close may fail require careful attention. It is strongly advised
     * to relinquish the underlying resources and to internally
     * <em>mark</em> the {@code Closeable} as closed, prior to throwing
     * the {@code IOException}.
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        this.closing = true;

    }
    private final WebSocketClient client;
    private boolean closing = false;
}
////////////////////////////////////////////////////////////////////////////////