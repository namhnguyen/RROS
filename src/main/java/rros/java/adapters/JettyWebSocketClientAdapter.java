package rros.java.adapters;


import org.eclipse.jetty.websocket.api.CloseStatus;
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
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
////////////////////////////////////////////////////////////////////////////////
/**
 * Created by namnguyen on 3/9/15.
 */
@WebSocket
public class JettyWebSocketClientAdapter extends SocketAdapter{
    private static final Logger LOG =
            Logger.getLogger(JettyWebSocketClientAdapter.class.getName());
    private static final long INITIAL_RETRY_DELAY = 5000;
    private static final double RETRY_DELAY_PUSHBACK_FACTOR = 1.1;
    private static final long RETRY_DELAY_MAX_VALUE = 30000;
    private static final long PING_TIME_OUT = rros.GlobalConfig.PING_TIME_OUT(); //  10000;
    private static final long PING_DURATION = rros.GlobalConfig.PING_DURATION(); // 2000
    private Timer timer;
    volatile private long lastMessageReceivedTime = System.currentTimeMillis();
    volatile private long lastPing = System.currentTimeMillis();

    public JettyWebSocketClientAdapter(URI uri){
        this.uri = uri;
        this.client = new WebSocketClient();
        this.client.setMaxIdleTimeout(Long.MAX_VALUE);
        this.client.setAsyncWriteTimeout(5000);
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
            if (this.session!=null&&this.session.isOpen())
                this.session.close();
            this.session = null;
            try{

                retryDelay *= RETRY_DELAY_PUSHBACK_FACTOR;
                if (retryDelay>RETRY_DELAY_MAX_VALUE)
                    retryDelay = RETRY_DELAY_MAX_VALUE;
                long delayInSecond = retryDelay/1000;

                LOG.log(Level.INFO,"Reconnecting, wait time "+delayInSecond+" second(s)...");

                //client.start();
                ClientUpgradeRequest request = new ClientUpgradeRequest();

                client.connect(this,this.uri,request);
                connectOK = true;
            }catch(IOException t){
                connectOK = false;
                System.out.println("Retry to connect to server... "+t.toString());
                try {
                    Thread.sleep(retryDelay);
                }catch(Exception exc) { }
            }catch (IllegalStateException t){
                LOG.log(Level.SEVERE,t.getMessage(),t);
                break;
            }
        }
    }
    @OnWebSocketError
    public void onError(Session session,Throwable error){
        //System.out.println(error.toString());
        LOG.log(Level.WARNING,error.getMessage());
        if (!(error instanceof IllegalStateException))
            if(timer!=null) timer.cancel();{
            if (!this.closing) {
                try {
                    Thread.sleep(retryDelay);
                } catch (Exception exc) {
                }
                this.reconnect();
            }
        }
    }
    @OnWebSocketConnect
    public void onConnect(Session session){
        this.lastMessageReceivedTime = System.currentTimeMillis();
        this.lastPing = System.currentTimeMillis();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                long durationFromLastMessageReceived = currentTime-lastMessageReceivedTime;
                if (durationFromLastMessageReceived>PING_TIME_OUT){
                    session.close(5,CloseStatus.trimMaxReasonLength("Cannot receive ping from other end!!!"));
                }else {
                    long durationFromLastPing = currentTime - lastPing;
                    if (durationFromLastPing>PING_DURATION){
                        lastPing = currentTime;
                        send("\0");
                    }
                }
            }
        }, 0, 1000);
        this.retryDelay = INITIAL_RETRY_DELAY;
        LOG.log(Level.INFO, session.toString());
        this.session = session;
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason){
        LOG.log(Level.INFO, "Close because of ["+reason+"]");
        if(timer!=null) timer.cancel();
        if (!this.closing){
            try {
                Thread.sleep(retryDelay);
            } catch (Exception exc) {
            }
            this.reconnect();
            //this.connect();
        }else {
            //make sure it close
            if (this.session.isOpen()) {
                this.session.close();
            }
            this.session = null;
            try {
                this.client.stop();
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }
    }

    @OnWebSocketMessage
    public void onMessage(String msg){
        this.lastMessageReceivedTime = System.currentTimeMillis();
        if (!msg.equals("\0")) {
            for (SocketListener listener : this.getListeners()) {
                listener.onReceived(msg);
            }
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
        LOG.log(Level.INFO, "Explicitly Close");
        if (this.session!=null&&this.session.isOpen())
            this.session.close(0,"Normal Close");
        try {
            this.client.stop();
        } catch(IOException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }
    private final WebSocketClient client;
    private boolean closing = false;
    private long retryDelay = INITIAL_RETRY_DELAY;

}
////////////////////////////////////////////////////////////////////////////////