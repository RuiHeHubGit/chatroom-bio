package session;

import java.io.IOException;

public interface Encoder<T> {
    byte[] encode(T data) throws IOException;
}
