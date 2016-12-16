package org.arabellan.utils;

import java.nio.ByteBuffer;

public class ConversionUtils {

    public static int convertByteToInt(byte b) {
        return b & 0xFF;
    }

    public static byte convertIntToByte(int i) {
        return (byte) i;
    }

    public static String convertBufferToString(ByteBuffer buffer) {
        if (buffer.remaining() == 0) return "";
        byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes);
        return new String(bytes);
    }

    public static ByteBuffer convertStringToBuffer(String string) {
        ByteBuffer buffer = ByteBuffer.allocate(string.getBytes().length);
        buffer.put(string.getBytes());
        buffer.flip();
        return buffer;
    }

    public static String convertByteToString(byte b) {
        return new String(new byte[]{b});
    }
}
