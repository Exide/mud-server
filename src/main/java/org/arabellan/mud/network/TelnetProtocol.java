package org.arabellan.mud.network;

import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;

/**
 * Responsibility:
 * Detect and parse telnet commands (e.g. IAC DO n)
 */

@Slf4j
class TelnetProtocol {

    private static final int IAC = 255;
    private static final int DONT = 254;
    private static final int DO = 253;
    private static final int WONT = 252;
    private static final int WILL = 251;

    static boolean startsWithTelnetCommand(ByteBuffer buffer) {
        byte b = buffer.get();
        buffer.rewind();
        return b == convertIntToByte(IAC);
    }

    static ByteBuffer extractTelnetCommand(ByteBuffer buffer) {
        byte[] bytes = new byte[3];
        buffer.get(bytes, 0, 3);
        ByteBuffer command = ByteBuffer.allocate(3);
        command.put(bytes);
        command.flip();
        return command;
    }

    static ByteBuffer parseTelnetCommand(ByteBuffer buffer) {
        log.trace("Parsing telnet command");

        ByteBuffer output = ByteBuffer.allocate(3);

        int command = convertByteToInt(buffer.get(1));
        int option = convertByteToInt(buffer.get(2));

        output.put(convertIntToByte(IAC));

        if (command == DO) output.put(convertIntToByte(WONT));
        if (command == WILL) output.put(convertIntToByte(DONT));

        output.put(convertIntToByte(option));
        output.flip();

        log.debug("Ignoring telnet command: " + option);
        return output;
    }

    private static int convertByteToInt(byte b) {
        return b & 0xFF;
    }

    private static byte convertIntToByte(int i) {
        return (byte) i;
    }
}
