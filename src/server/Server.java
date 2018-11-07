package server;

import session.Session;
import session.SessionListener;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

/**
 * Created by HeRui on 2018/11/6.
 */
public class Server implements SessionListener<String>{
    public final static int DEFAULT_PORT = 9666;
    private Selector selector;
    private ServerSocketChannel serverChannel;
    private volatile boolean started;
    private ServerHandler handler;
    private Vector<Session> sessions;

    public void start() {
        start(DEFAULT_PORT);
    }

    private synchronized void start(int port) {
        if(started) {
            return;
        }

        sessions = new Vector<>();
        try {
            // 创建选择器
            selector = Selector.open();
            // 打开监听通道
            serverChannel = ServerSocketChannel.open();
            // 设置通道为非阻塞模式
            serverChannel.configureBlocking(false);
            // 设置socket监听端口为port,设置连接队列最大为1024
            serverChannel.socket().bind(new InetSocketAddress(port), 1024);
            // 监听连接
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            handler = new ServerHandler(serverChannel, selector, this);
            handler.start();
            started = true;
            System.out.println("server start on port:"+port);
        } catch (IOException e) {
            started = false;
            System.out.println("server start failed on port:"+port);
            e.printStackTrace();
        }
    }

    private static String getNowTimeString(String fmt) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(fmt);
        return simpleDateFormat.format(new Date());
    }

    @Override
    public void onOpen(Session<String> session) {
        System.out.println("client connected");
    }

    @Override
    public void onMessage(Session<String> session, String msg) {
        Object username = session.getProperty().get("username");
        if(username == null) {
            username = msg;
            session.getProperty().put("username", username);
            msg = getNowTimeString("MM-dd HH:mm:ss")+"\n["+username+"] "+"加入了聊天室！";
            sessions.add(session);
            session.send(session.getId());
            System.out.println(session);
        } else {
            msg = "["+username+"] "+ getNowTimeString("MM-dd HH:mm:ss")+"\n"+msg;
        }
        System.out.println(msg);
        for (Session s : sessions) {
            s.send(msg);
        }
    }

    @Override
    public void onError(Exception e) {
        System.out.println(getNowTimeString("yyyy-MM-dd HH:mm:ss")+"[error]:");
        System.out.println(e.getLocalizedMessage());
    }

    @Override
    public void onClone(Session<String> session) {
        sessions.remove(session);
        System.out.println("client disconnect:"+session);
        for (Session s : sessions) {
            s.send("["+session.getProperty().get("username")+"] "+ getNowTimeString("MM-dd HH:mm:ss")+":离开了聊天室！");
        }
    }
}
