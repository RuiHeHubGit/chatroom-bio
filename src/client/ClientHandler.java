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
    private SocketChannel socketChannel;
    private Selector selector;
    private SessionListener<T> listener;
    private Encoder encode;
    private Decoder decode;
    private Session<T> session;
    private volatile boolean run;

    public ClientHandler(SocketChannel socketChannel, Selector selector, SessionListener<T> listener) {
        this(socketChannel, selector, listener, null, null);
    }

    public ClientHandler(SocketChannel socketChannel, Selector selector, SessionListener<T> listener, Encoder encode, Decoder decode) {
        this.socketChannel = socketChannel;
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
                run = false;
                break;
            }
        }
        // 关闭
        close();
    }

    private void close() {
        if(socketChannel != null) {
            try {
                socketChannel.close();
            } catch (IOException e) {
                listener.onError(e);
            }
            socketChannel = null;
        }

        if(selector != null) {
            try {
                selector.close();
            } catch (IOException e) {
                listener.onError(e);
            }
            selector = null;
        }
        listener.onClone(session);
    }

    private void handleKey(SelectionKey key) throws IOException {
        if(key.isValid()) {
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
        }
    }
}
