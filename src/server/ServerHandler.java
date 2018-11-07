package server;

import session.Session;
import session.Decoder;
import session.Encoder;
import session.SessionListener;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by HeRui on 2018/11/6.
 */
public class ServerHandler<T> implements Runnable{
    private HashMap<SocketChannel, Session<T>> sessionMap;
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private SessionListener<T> listener;
    private Encoder encode;
    private Decoder decode;
    private volatile boolean run;

    public ServerHandler(ServerSocketChannel serverSocketChannel, Selector selector, SessionListener<T> listener) {
        this(serverSocketChannel, selector, listener, null, null);
    }

    public ServerHandler(ServerSocketChannel serverSocketChannel, Selector selector, SessionListener<T> listener, Encoder encode, Decoder decode) {
        this.serverSocketChannel = serverSocketChannel;
        this.selector = selector;
        this.listener = listener;
        this.encode = encode;
        this.decode = decode;
        this.sessionMap = new HashMap<>();
    }

    public synchronized void start() {
        if(run) return;
        run = true;
        new Thread(this).start();
    }

    public synchronized void stop() {
        run = false;
    }

    @Override
    public void run() {
        while (run) {
            try {
                selector.select(1000);
                Set<SelectionKey> selectionKeySet = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeySet.iterator();
                SelectionKey key;
                while (iterator.hasNext()) {
                    key = iterator.next();
                    iterator.remove();
                    handleKey(key);
                }
            } catch (IOException e) {
                listener.onError(e);
            }
        }
        close();
    }

    private void close() {
        if(serverSocketChannel != null) {
            try {
                serverSocketChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            serverSocketChannel = null;
        }

        if(selector != null) {
            try {
                selector.close();
            } catch (IOException e) {
                listener.onError(e);
            }
            selector = null;
        }
    }

    private void handleKey(SelectionKey key) throws IOException {
        if(key.isValid()) {
            if(key.isAcceptable()) {
                ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
                SocketChannel sc = ssc.accept();
                if(sc != null) {
                    sc.configureBlocking(false);
                    sc.register(selector, SelectionKey.OP_READ);
                    Session<T> session = new Session(sc, listener, encode, decode);
                    sessionMap.put(sc, session);
                    listener.onOpen(session);
                }
            }

            if(key.isReadable()) {
                SocketChannel sc = (SocketChannel) key.channel();
                if(!sessionMap.get(sc).decode()) {
                    sessionMap.remove(sc);
                }
            }
        }
    }
}
