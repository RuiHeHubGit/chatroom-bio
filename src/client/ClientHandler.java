package client;

import session.Decoder;
import session.Encoder;
import session.Session;
import session.SessionListener;

import java.io.IOException;
import java.net.ConnectException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by HeRui on 2018/11/7.
 */
public class ClientHandler<T>implements Runnable{
    private Selector selector;
    private SessionListener<T> listener;
    private Encoder encode;
    private Decoder decode;
    private Session<T> session;
    private volatile boolean run;

    public ClientHandler(Selector selector, SessionListener<T> listener) {
        this(selector, listener, null, null);
    }

    public ClientHandler(Selector selector, SessionListener<T> listener, Encoder encode, Decoder decode) {
        this.selector = selector;
        this.listener = listener;
        this.encode = encode;
        this.decode = decode;
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
            SelectionKey key = null;
            try {
                selector.select(1000);
                Set<SelectionKey> selectionKeySet = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeySet.iterator();
                while (iterator.hasNext()) {
                    key = iterator.next();
                    iterator.remove();
                    handleKey(key);
                }
            } catch (IOException e) {
                listener.onError(e);
                if(key != null) {
                    try {
                        ((SocketChannel)key.channel()).register(null, SelectionKey.OP_CONNECT);
                    } catch (ClosedChannelException e1) {
                        listener.onError(e1);
                    }
                }
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            selector.close();
        } catch (IOException e) {
            listener.onError(e);
        }
    }

    private void handleKey(SelectionKey key) {
        if(key.isValid()) {
            try {
                if(key.isConnectable()) {
                    SocketChannel sc = (SocketChannel) key.channel();
                    if(sc.finishConnect()) {
                        sc.register(selector, SelectionKey.OP_READ);
                        session = new Session(sc, listener, encode, decode);
                        listener.onOpen(session);
                    } else {
                        listener.onError(new ConnectException("failed connection"));
                        System.exit(1);
                    }
                }

                if(key.isReadable()) {
                    session.decode();
                }
            } catch (IOException e) {
                listener.onError(e);
            }
        }
    }
}
