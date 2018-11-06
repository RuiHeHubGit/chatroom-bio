package client;

import session.Session;
import session.SessionListener;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Scanner;

/**
 * Created by HeRui on 2018/11/7.
 */
public class Client implements SessionListener<String>{
    public final static String DEFAULT_HOST = "127.0.0.1";
    public final static int DEFAULT_SERVER_PORT = 9666;
    private String host;
    private int port;
    private SocketChannel socketChannel;
    private Selector selector;
    private ClientHandler handler;

    public void start() {
        start(DEFAULT_HOST, DEFAULT_SERVER_PORT);
    }

    public synchronized void start(String host, int port) {
        this.host = host;
        this.port = port;

        try {
            selector = Selector.open();
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            socketChannel.connect(new InetSocketAddress(host,port));
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
        new Thread(()->{
            Scanner scanner = new Scanner(System.in);
            String line;
            do {
                line = scanner.nextLine();
                if(!line.isEmpty()) {
                    session.send(line);
                }
            } while (line != "exit");
            handler.stop();
            scanner.close();
        }).start();
    }

    @Override
    public void onMessage(Session<String> session, String msg) {
        System.out.println(msg);
    }

    @Override
    public void onError(Exception e) {
        System.out.println(e.getLocalizedMessage());
        e.printStackTrace();
    }

    @Override
    public void onClone(Session<String> session) {
        System.out.println("client disconnect,session:"+session);
    }
}
