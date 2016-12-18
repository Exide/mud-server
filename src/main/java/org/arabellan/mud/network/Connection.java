package org.arabellan.mud.network;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static java.util.Objects.nonNull;
import static org.arabellan.utils.ConversionUtils.convertBufferToString;
import static org.arabellan.utils.ConversionUtils.convertByteToInt;
import static org.arabellan.utils.ConversionUtils.convertByteToString;
import static org.arabellan.utils.ConversionUtils.convertStringToBuffer;

/**
 * Socket communication utilizing the Telnet specification
 *
 * @see <a href="https://tools.ietf.org/html/rfc854">RFC 854: Telnet Protocol Specification</a>
 * @see <a href="https://tools.ietf.org/html/rfc855">RFC 855: Telnet Option Specification</a>
 */

@Slf4j
public class Connection {

    private static final byte LINE_FEED = (byte) 10;
    private static final byte CARRIAGE_RETURN = (byte) 13;

    private static final byte SUBNEGOTIATION_END = (byte) 240;
    private static final byte NO_OPERATION = (byte) 241;
    private static final byte DATA_MARK = (byte) 242;
    private static final byte BREAK = (byte) 243;
    private static final byte INTERRUPT_PROCESS = (byte) 244;
    private static final byte ABORT_OUTPUT = (byte) 245;
    private static final byte ARE_YOU_THERE = (byte) 246;
    private static final byte ERASE_CHARACTER = (byte) 247;
    private static final byte ERASE_LINE = (byte) 248;
    private static final byte GO_AHEAD = (byte) 249;
    private static final byte SUBNEGOTIATION_BEGIN = (byte) 250;
    private static final byte WILL = (byte) 251;
    private static final byte WONT = (byte) 252;
    private static final byte DO = (byte) 253;
    private static final byte DONT = (byte) 254;
    private static final byte INTERPRET_AS_COMMAND = (byte) 255;

    @Getter
    private int id;

    private SocketChannel socketChannel;

    @Getter
    private Queue<String> outgoingQueue = new LinkedList<>();

    @Getter
    private Queue<String> incomingQueue = new LinkedList<>();

    @Getter
    private boolean isClosed;

    @Setter
    private Selector readSelector;

    @Setter
    private Selector writeSelector;

    private SelectionKey readSelectionKey;
    private SelectionKey writeSelectionKey;

    Connection(SocketChannel socketChannel) {
        this.id = socketChannel.hashCode();
        this.socketChannel = socketChannel;
        this.isClosed = false;
    }

    void close() {
        dequeueForRead();
        dequeueForWrite();
        isClosed = true;
    }

    void queueForRead() {
        try {
            readSelectionKey = socketChannel.register(readSelector, OP_READ, this);
            log.trace("Connection " + id + " added read selector");
        } catch (ClosedChannelException e) {
            close();
        }
    }

    void queueForWrite() {
        try {
            if (writeSelectionKey == null) {
                writeSelectionKey = socketChannel.register(writeSelector, OP_WRITE, this);
                log.trace("Connection " + id + " added write selector");
            }
        } catch (ClosedChannelException e) {
            close();
        }
    }

    void dequeueForRead() {
        if (nonNull(readSelectionKey)) {
            readSelectionKey.attach(null);
            readSelectionKey.cancel();
            readSelectionKey = null;
            log.trace("Connection " + id + " removed from read selector");
        }
    }

    void dequeueForWrite() {
        if (nonNull(writeSelectionKey)) {
            writeSelectionKey.attach(null);
            writeSelectionKey.cancel();
            writeSelectionKey = null;
            log.trace("Connection " + id + " removed from write selector");
        }
    }

    void setNonBlockingMode() {
        try {
            socketChannel.configureBlocking(false);
            log.debug("Connection " + id + " set to non-blocking mode");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void send(String message) {
        message += "\r\n";
        outgoingQueue.add(message);
        queueForWrite();
    }

    void read() {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(1024);

            int bytesRead = socketChannel.read(buffer);
            while (bytesRead > 0) {
                bytesRead = socketChannel.read(buffer);
            }

            if (bytesRead == -1) {
                close();
            }

            buffer.flip();

            while (buffer.hasRemaining()) {
                if (nextByteIAC(buffer)) {
                    ByteBuffer response = handleTelnetCommand(buffer);
                    String message = convertBufferToString(response);
                    outgoingQueue.add(message);
                } else {
                    byte[] bytes = new byte[buffer.limit()];
                    buffer.get(bytes);
                    String input = new String(bytes);
                    String trimmedInput = input.trim();
                    log.debug("From " + id + ": " + trimmedInput);
                    incomingQueue.add(trimmedInput);
                }
            }

            if (outgoingQueue.size() > 0) {
                queueForWrite();
            }
        } catch (IOException e) {
            throw new RuntimeException("error occured reading from socket", e);
        }
    }

    void write() {
        try {
            String message = outgoingQueue.poll();

            if (message != null) {

                if (!message.startsWith(convertByteToString(INTERPRET_AS_COMMAND))) {
                    String trimmedMessage = message.trim();
                    log.debug("To " + id + ": " + trimmedMessage);
                }

                message += ":";

                ByteBuffer buffer = convertStringToBuffer(message);
                socketChannel.write(buffer);
            }

            if (outgoingQueue.size() == 0) {
                dequeueForWrite();
            }
        } catch (IOException e) {
            throw new RuntimeException("error occured writing to socket", e);
        }

    }

    private boolean nextByteIAC(ByteBuffer buffer) {
        if (buffer.remaining() == 0)
            throw new IllegalArgumentException("buffer is empty");

        buffer.mark();
        byte b = buffer.get();
        buffer.reset();
        return b == INTERPRET_AS_COMMAND;
    }

    private ByteBuffer handleTelnetCommand(ByteBuffer buffer) {
        if (buffer.remaining() == 0)
            throw new IllegalArgumentException("buffer is empty");

        if (buffer.remaining() == 1)
            throw new IllegalArgumentException("buffer doesn't have enough bytes");

        byte iac = buffer.get();
        byte command = buffer.get();

        ByteBuffer response;

        if (isOptionRequest(command)) {
            byte option = buffer.get();
            log.debug("Ignoring telnet command: " + convertByteToInt(command) + " " + convertByteToInt(option));
            response = handleOptionRequest(command, option);
        } else {
            log.debug("Ignoring telnet command: " + convertByteToInt(command));
            response = ByteBuffer.allocate(2);
            response.put(CARRIAGE_RETURN);
            response.put(LINE_FEED);
        }

        return response;
    }

    private boolean isOptionRequest(byte command) {
        return command == WILL || command == WONT || command == DO || command == DONT;
    }

    private ByteBuffer handleOptionRequest(byte command, byte option) {
        ByteBuffer output = ByteBuffer.allocate(3);
        output.put(INTERPRET_AS_COMMAND);

        if (command == DO) output.put(WONT);
        if (command == WILL) output.put(DONT);

        output.put(option);
        output.flip();

        return output;
    }
}
