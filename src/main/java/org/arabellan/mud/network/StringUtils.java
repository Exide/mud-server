package org.arabellan.mud.network;

import java.nio.ByteBuffer;

class StringUtils {

    static String fromByteBuffer(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.limit()];
        buffer.rewind();
        buffer.get(bytes);
        buffer.rewind();
        String output = new String(bytes);
        return output.trim();
    }
}
