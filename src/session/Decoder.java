package session;

import java.io.IOException;

public interface Decoder<T> {
    T decode(byte[] bytes) throws IOException;
}
