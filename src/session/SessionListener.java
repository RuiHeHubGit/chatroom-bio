package session;

/**
 * Created by HeRui on 2018/11/3.
 */
public interface SessionListener<T> {
    void onOpen(Session<T> session);
    void onMessage(Session<T> session, T msg);
    void onError(Exception e);
    void onClone(Session<T> session);
}
