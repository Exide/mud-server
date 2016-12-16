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
}
