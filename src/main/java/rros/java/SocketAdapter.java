package rros.java;
////////////////////////////////////////////////////////////////////////////////

import rros.SocketListener;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by namnguyen on 3/9/15.
 */
public abstract class SocketAdapter implements Closeable{

    public SocketAdapter(){
        listeners = Collections.synchronizedSet(new HashSet<>());
    }
    public abstract void send(String message);
    public void addListener(SocketListener listener){
        listeners.add(listener);
    }
    public void removeListener(SocketListener listener){
        listeners.remove(listener);
    }
    public Set<SocketListener> getListeners() { return listeners; }

    public rros.SocketAdapter toScalaSocketAdapter() {
        if (this.socketAdapter==null) {
            class Temp implements rros.SocketAdapter{

                @Override
                public void send(String message) {
                    SocketAdapter.this.send(message);
                }

                @Override
                public void close() {
                    try {
                        SocketAdapter.this.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void $plus$eq(SocketListener socketListener) {
                    SocketAdapter.this.addListener(socketListener);
                }

                @Override
                public void $minus$eq(SocketListener socketListener) {
                    SocketAdapter.this.removeListener(socketListener);
                }

                @Override
                public scala.collection.immutable.Set<SocketListener> socketListeners() {
                    scala.collection.Set<SocketListener> set =
                            new scala.collection.immutable.HashSet<>();

                    for (SocketListener listener : SocketAdapter.this.getListeners()) {
                        set = set.$plus(listener);
                    }
                    return (scala.collection.immutable.Set<SocketListener>) set;
                }
                //@Override //maintain compatibility with Scala
                public void rros$SocketAdapter$$_socketListeners_$eq(scala.collection.immutable.Set<rros.SocketListener> listenerSet) {

                }
                //@Override //maintain compatibility with Scala
                public scala.collection.immutable.Set<SocketListener> rros$SocketAdapter$$_socketListeners() {
                    scala.collection.Set<SocketListener> set =
                            new scala.collection.immutable.HashSet<>();

                    for (SocketListener listener : SocketAdapter.this.getListeners()) {
                        set = set.$plus(listener);
                    }
                    return (scala.collection.immutable.Set<SocketListener>) set;
                }
            }
            socketAdapter = new Temp();
        }
        return this.socketAdapter;
    }

    private Set<SocketListener> listeners;
    private rros.SocketAdapter socketAdapter = null;

}
////////////////////////////////////////////////////////////////////////////////