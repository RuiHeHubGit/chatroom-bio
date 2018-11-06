package client;

import session.Session;
import session.SessionListener;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

/**
 * Created by HeRui on 2018/11/7.
 */
public class Client implements SessionListener<String>{
    public final static String DEFAULT_URL = "127.0.0.1";
    public final static int DEFAULT_SERVER_PORT = 9666;
    private String url;
    private int port;
    private SocketChannel socketChannel;
    private Selector selector;
    private ClientHandler handler;

    public void start() {
        start(DEFAULT_URL, DEFAULT_SERVER_PORT);
    }

    public synchronized void start(String url, int port) {
        this.url = url;
        this.port = port;

        try {
            selector = Selector.open();
            socketChannel = SocketChannel.open();
            socketChannel.connect(new InetSocketAddress(url, port));
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_CONNECT);
            handler = new ClientHandler(selector, this);
            handler.start();
        } catch (IOException e) {
            onError(e);
        }
    }

    @Override
    public void onOpen(Session<String> session) {
        System.out.println("client connect:"+session);
        Scanner scanner = new Scanner(System.in);
        String line = null;
        do {
            line = scanner.nextLine();
            if(!line.isEmpty()) {
                session.send(line);
            }
        } while (line != "exit");
        handler.stop();
    }

    @Override
    public void onMessage(Session<String> session, String msg) {
        System.out.println(msg);
    }

    @Override
    public void onError(Exception e) {
        System.out.println(e.getLocalizedMessage());
    }

    @Override
    public void onClone(Session<String> session) {
        System.out.println("client disconnect,session:"+session);
    }
}
