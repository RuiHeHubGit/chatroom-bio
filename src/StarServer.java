import client.Client;
import server.Server;

/**
 * Created by HeRui on 2018/11/7.
 */
public class StarServer {
    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
}
