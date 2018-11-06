package session;

import java.io.IOException;

public class DefStringDecoder implements Decoder<String> {

    @Override
    public String decode(byte[] bytes) throws IOException {
        if(bytes == null) {
            return null;
        }
        return new String(bytes, "utf-8");
    }
}
