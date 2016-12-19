package org.arabellan.utils;

import java.nio.ByteBuffer;
import java.util.List;

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
        buffer.mark();
        buffer.get(bytes);
        buffer.reset();
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

    public static ByteBuffer convertByteToBuffer(byte b) {
        ByteBuffer buffer = ByteBuffer.allocate(1);
        buffer.put(b);
        buffer.flip();
        return buffer;
    }

    public static String convertByteListToString(List<Byte> collection) {
        return new String(convertByteListToByteArray(collection));
    }

    public static byte[] convertByteListToByteArray(List<Byte> collection) {
        byte[] bytes = new byte[collection.size()];
        for (int i = 0; i < collection.size(); ++i) {
            bytes[i] = collection.get(i);
        }
        collection.clear();
        return bytes;
    }
}
