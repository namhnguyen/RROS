package rros.java.adapters;

import org.glassfish.tyrus.core.CloseReasons;
import rros.SocketListener;
import rros.java.SocketAdapter;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
////////////////////////////////////////////////////////////////////////////////
/**
 * Created by namnguyen on 3/17/15.
 */
@ClientEndpoint
public class GrizzlyWebSocketClientAdapter extends SocketAdapter {
    private static final Logger LOG =
            Logger.getLogger(GrizzlyWebSocketClientAdapter.class.getName());
    private static final long INITIAL_RETRY_DELAY = 5000;
    private static final double RETRY_DELAY_PUSHBACK_FACTOR = 1.1;
    private static final long RETRY_DELAY_MAX_VALUE = 30000;
    private static final long PING_TIME_OUT = 10000;
    private static final long PING_DURATION = 2000;
    private Timer timer;
    volatile private long lastMessageReceivedTime = System.currentTimeMillis();
    volatile private long lastPing = System.currentTimeMillis();

    public GrizzlyWebSocketClientAdapter(URI uri){
        this.uri = uri;
    }

    public void connect()  {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, this.uri);
        }catch(Exception exc){
            LOG.log(Level.WARNING,exc.getMessage(),exc);
            try {
                Thread.sleep(retryDelay);
            }catch(Exception ignore){ }
            reconnect();
        }
    }

    private void reconnect()  {
        boolean connectOK =false;
        while (!connectOK && !this.closing){
            if (this.session!=null&&this.session.isOpen()){
                try {
                    this.session.close();
                }catch(IOException exc){  }
            }

            this.session = null;
            try{

                retryDelay *= RETRY_DELAY_PUSHBACK_FACTOR;
                if (retryDelay>RETRY_DELAY_MAX_VALUE)
                    retryDelay = RETRY_DELAY_MAX_VALUE;
                long delayInSecond = retryDelay/1000;

                LOG.log(Level.INFO,"Reconnecting, wait time "+delayInSecond+" second(s)...");

                WebSocketContainer container = ContainerProvider.getWebSocketContainer();
                container.connectToServer(this,this.uri);

                connectOK = true;
            }catch(IOException | DeploymentException t){
                LOG.log(Level.WARNING,t.getMessage(),t);
                //t.printStackTrace();
                connectOK = false;
                System.out.println("Retry to connect to server... "+t.toString());
                try {
                    Thread.sleep(retryDelay);
                }catch(Exception exc) { }
            }catch (Exception exc) {
                LOG.log(Level.SEVERE,exc.getMessage(),exc);
                break;
            }
        }
    }

    @OnOpen
    public void onOpen(Session userSession) {
        LOG.log(Level.INFO, userSession.toString());
        System.out.println("On Connect");
        this.session = userSession;
        this.lastMessageReceivedTime = System.currentTimeMillis();
        this.lastPing = System.currentTimeMillis();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                long durationFromLastMessageReceived = currentTime-lastMessageReceivedTime;
                if (durationFromLastMessageReceived>PING_TIME_OUT){
                    try {
                        session.close(
                                new CloseReason(CloseReason.CloseCodes.GOING_AWAY
                                        , "Cannot receive ping from other end!!!"));
                    }catch(IOException exc){
                        LOG.log(Level.INFO,exc.getMessage(),exc);
                    }
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

    }

    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
        LOG.log(Level.INFO, "Close because of ["+reason.getReasonPhrase()+"]");
        if(timer!=null) timer.cancel();
        if (!this.closing){
            try {
                Thread.sleep(retryDelay);
            } catch (Exception exc) {
                LOG.log(Level.INFO,exc.getMessage(),exc);
            }
            this.reconnect();
            //this.connect();
        }else {
            //make sure it close
            if (this.session.isOpen()) {
                try {
                    this.session.close();
                }catch(Exception exc){
                    LOG.log(Level.INFO,exc.getMessage(),exc);
                }
            }
            this.session = null;
        }
    }
    @OnMessage
    public void onMessage(String message) {
        this.lastMessageReceivedTime = System.currentTimeMillis();
        if (!(message==null||message.equals("\0"))) {
            for (SocketListener listener : this.getListeners()) {
                listener.onReceived(message);
            }
        }
    }
    @OnError
    public void onError(Throwable error) {
        LOG.log(Level.WARNING, error.getMessage(),error);

//        if(timer!=null) timer.cancel();
//        if (!this.closing) {
//            try {
//                Thread.sleep(retryDelay);
//            } catch (Exception exc) {
//            }
//            this.reconnect();
//        }
    }
    @Override
    public void send(String message) {
        if (this.session!=null) {
            this.session.getAsyncRemote().sendText(message);
        } else {
            throw new RuntimeException("Connection fail");
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
            this.session.close(CloseReasons.NORMAL_CLOSURE.getCloseReason());
    }
    private Session session;
    private URI uri;
    private boolean closing = false;
    private long retryDelay = INITIAL_RETRY_DELAY;
}
////////////////////////////////////////////////////////////////////////////////