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
            handler = new ClientHandler(socketChannel, selector, this);
            handler.start();
        } catch (IOException e) {
            onError(e);
        }
    }

    @Override
    public void onOpen(Session<String> session) {
        System.out.println("client connect:");
        session.setId(null);
        new Thread(()->{
            Scanner scanner = new Scanner(System.in);
            System.out.println("输入昵称：");
            String line =scanner.nextLine();
            session.getProperty().put("username", line);
            session.send(line);
            while (true){
                line = scanner.nextLine();
                if("exit".equals(line)) {
                    handler.stop();
                    System.exit(0);
                }
                if(!line.isEmpty()) {
                    session.send(line);
                }
            }
        }).start();
    }

    @Override
    public void onMessage(Session<String> session, String msg) {
        if(session.getId() == null) {
            session.setId(msg);
            System.out.println(session);
            return;
        }
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
