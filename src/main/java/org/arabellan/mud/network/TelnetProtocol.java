package org.arabellan.mud.network;

import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;

/**
 * Responsibility:
 * Detect and parse telnet commands (e.g. IAC DO n)
 *
 * Resources:
 * - http://tools.ietf.org/html/rfc854
 * - http://www.iana.org/assignments/telnet-options/telnet-options.xhtml
 */

@Slf4j
class TelnetProtocol {

    private static final int SUBNEGOTIATION_END = 240;
    private static final int NO_OPERATION = 241;
    private static final int DATA_MARK = 242;
    private static final int BREAK = 243;
    private static final int INTERRUPT_PROCESS = 244;
    private static final int ABORT_OUTPUT = 245;
    private static final int ARE_YOU_THERE = 246;
    private static final int ERASE_CHARACTER = 247;
    private static final int ERASE_LINE = 248;
    private static final int GO_AHEAD = 249;
    private static final int SUBNEGOTIATION_BEGIN = 250;
    private static final int WILL = 251;
    private static final int WONT = 252;
    private static final int DO = 253;
    private static final int DONT = 254;
    private static final int INTERPRET_AS_COMMAND = 255;

    static boolean nextByteIAC(ByteBuffer buffer) {
        if (buffer.remaining() == 0)
            throw new IllegalArgumentException("the buffer provided has no bytes remaining");

        buffer.mark();
        byte b = buffer.get();
        buffer.reset();
        return b == convertIntToByte(INTERPRET_AS_COMMAND);
    }

    static ByteBuffer extractTelnetCommand(ByteBuffer buffer) {
        if (buffer.remaining() < 3)
            throw new IllegalArgumentException("the buffer provided doesn't have enough bytes to be a Telnet command");

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

        output.put(convertIntToByte(INTERPRET_AS_COMMAND));

        if (command == DO) output.put(convertIntToByte(WONT));
        if (command == WILL) output.put(convertIntToByte(DONT));

        output.put(convertIntToByte(option));
        output.flip();

        log.debug("Ignoring telnet option: " + option);
        return output;
    }

    private static int convertByteToInt(byte b) {
        return b & 0xFF;
    }

    private static byte convertIntToByte(int i) {
        return (byte) i;
    }
}
