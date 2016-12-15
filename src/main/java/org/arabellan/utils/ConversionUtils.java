package org.arabellan.utils;

public class ConversionUtils {

    public static int convertByteToInt(byte b) {
        return b & 0xFF;
    }

    public static byte convertIntToByte(int i) {
        return (byte) i;
    }
}
