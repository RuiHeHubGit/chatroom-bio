package session;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

public class Session<T> {
    private String id;
    private HashMap property;
    private SocketChannel socketChannel;
    private SessionListener<T> listener;
    private Date createTime;
    private Encoder<T> encode;
    private Decoder<T> decode;
    private static ByteBuffer readBuffer = ByteBuffer.allocate(1024);

    public Session(SocketChannel socketChannel, SessionListener<T> listener) {
        this(socketChannel, listener, null, null);
    }

    public Session(SocketChannel socketChannel, SessionListener<T> listener, Encoder<T> encode, Decoder<T> decode) {
        if(socketChannel == null) {
            throw new IllegalArgumentException("must set socketChannel");
        }

        this.socketChannel = socketChannel;
        this.listener = listener;

        this.id = UUID.randomUUID().toString();
        this.property = new HashMap();
        this.createTime = new Date();
        try {
            if(decode == null) {
                this.decode = (Decoder<T>) new DefStringDecoder();
            } else {
                this.decode = decode;
            }
            if(encode == null) {
                this.encode = (Encoder<T>) new DefStringEncoder();
            } else {
                this.encode = encode;
            }
        } catch (ClassCastException e) {
            new IllegalArgumentException("No suitable encoder and decoder are set.");
        }

    }
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public HashMap getProperty() {
        return property;
    }

    public Encoder<T> getEncode() {
        return encode;
    }

    public Decoder<T> getDecode() {
        return decode;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public void setSocketChannel(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    @Override
    public String toString() {
        return "Session{" +
                "id='" + id + '\'' +
                ", property=" + property +
                ", createTime=" + createTime +
                '}';
    }

    public void send(T data) {
        try {
            byte[] bytes = encode.encode(data);
            ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
            //将字节数组复制到缓冲区
            writeBuffer.put(bytes);
            //flip操作
            writeBuffer.flip();
            //发送缓冲区的字节数组
            socketChannel.write(writeBuffer);
        } catch (IOException e) {
            listener.onError(e);
            listener.onClone(this);
        }
    }

    public boolean decode() {
        try {
            int len = socketChannel.read(readBuffer);
            if(len > 0) {
                readBuffer.flip();
                //根据缓冲区可读字节数创建字节数组
                byte[] bytes = new byte[readBuffer.remaining()];
                //将缓冲区可读字节数组复制到新建的数组中
                readBuffer.get(bytes);
                readBuffer.clear();
                listener.onMessage(this, decode.decode(bytes));
            }
            return true;
        } catch (IOException e) {
            try {
                socketChannel.close();
            } catch (IOException e1) {
                listener.onError(e1);
            }
            listener.onError(e);
            listener.onClone(this);
        }
        return false;
    }

    public boolean isConnected() {
        return socketChannel.isConnected();
    }
}
