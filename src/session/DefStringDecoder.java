package session;

import java.io.IOException;

public class DefStringDecoder implements Decoder<String> {

    @Override
    public String decode(byte[] bytes) throws IOException {
        return new String(bytes, "utf-8");
    }
}
