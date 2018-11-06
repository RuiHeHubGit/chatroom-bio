package session;

import java.io.IOException;

public class DefStringEncoder implements Encoder<String> {

    @Override
    public byte[] encode(String data) throws IOException {
        if(data == null)
            return new byte[0];
        return data.getBytes("utf-8");
    }
}
