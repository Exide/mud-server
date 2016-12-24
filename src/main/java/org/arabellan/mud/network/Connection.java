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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static java.util.Objects.nonNull;
import static org.arabellan.utils.ConversionUtils.convertBufferToString;
import static org.arabellan.utils.ConversionUtils.convertByteListToByteArray;
import static org.arabellan.utils.ConversionUtils.convertByteListToString;
import static org.arabellan.utils.ConversionUtils.convertByteToBuffer;
import static org.arabellan.utils.ConversionUtils.convertByteToInt;
import static org.arabellan.utils.ConversionUtils.convertByteToString;
import static org.arabellan.utils.ConversionUtils.convertStringToBuffer;
import static org.arabellan.utils.DebugUtils.traceCollectionSize;

/**
 * Socket communication utilizing the Telnet specification
 *
 * @see <a href="https://tools.ietf.org/html/rfc854">RFC 854: Telnet Protocol Specification</a>
 * @see <a href="https://tools.ietf.org/html/rfc855">RFC 855: Telnet Option Specification</a>
 */

@Slf4j
public class Connection {

    private static final byte BACKSPACE = (byte) 8;
    private static final byte LINE_FEED = (byte) 10;
    private static final byte CARRIAGE_RETURN = (byte) 13;
    private static final byte SPACE = (byte) 32;
    private static final byte DELETE = (byte) 127;

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

    private static final byte OPTION_ECHO = (byte) 1;
    private static final byte OPTION_SUPRESS_GO_AHEAD = (byte) 3;
    private static final byte OPTION_TERMINAL_TYPE = (byte) 24;
    private static final byte OPTION_NEGOTIATE_ABOUT_WINDOW_SIZE = (byte) 31;
    private static final byte OPTION_TERMINAL_SPEED = (byte) 32;
    private static final byte OPTION_LINEMODE = (byte) 34;
    private static final byte OPTION_ENVIRONMENT_VARIABLE = (byte) 36;
    private static final byte OPTION_NEW_ENVIRONMENT_VARIABLE = (byte) 39;

    @Getter
    private int id;

    private SocketChannel socketChannel;

    @Getter
    private Queue<OutgoingMessage> outgoingQueue = new LinkedList<>();

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

    private List<Byte> characterBuffer = new ArrayList<>();
    private String prompt = "[HP=0/KAI=0]:";

    Connection(SocketChannel socketChannel) {
        this.id = socketChannel.hashCode();
        this.socketChannel = socketChannel;
        this.isClosed = false;
        traceClass();
    }

    void initialize(Selector readSelector, Selector writeSelector) {
        setNonBlockingMode();
        setReadSelector(readSelector);
        setWriteSelector(writeSelector);
        queueForRead();

        sendBytes(INTERPRET_AS_COMMAND, DONT, OPTION_ECHO);
        sendBytes(INTERPRET_AS_COMMAND, WILL, OPTION_ECHO);
        sendBytes(INTERPRET_AS_COMMAND, DO, OPTION_SUPRESS_GO_AHEAD);
        sendBytes(INTERPRET_AS_COMMAND, WILL, OPTION_SUPRESS_GO_AHEAD);
    }

    void sendBytes(byte... bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
        buffer.put(bytes);
        buffer.flip();
        OutgoingMessage message = new OutgoingMessage(buffer, true);
        outgoingQueue.add(message);
        queueForWrite();
    }

    void sendString(String text) {
        ByteBuffer buffer = convertStringToBuffer(text);
        OutgoingMessage message = new OutgoingMessage(buffer);
        outgoingQueue.add(message);
        queueForWrite();

    }

    private void close() {
        dequeueForRead();
        dequeueForWrite();
        isClosed = true;
    }

    private void queueForRead() {
        try {
            readSelectionKey = socketChannel.register(readSelector, OP_READ, this);
            log.trace("Connection " + id + " added read selector");
        } catch (ClosedChannelException e) {
            close();
        }
    }

    private void queueForWrite() {
        try {
            if (writeSelectionKey == null) {
                writeSelectionKey = socketChannel.register(writeSelector, OP_WRITE, this);
                log.trace("Connection " + id + " added write selector");
            }
        } catch (ClosedChannelException e) {
            close();
        }
    }

    private void dequeueForRead() {
        if (nonNull(readSelectionKey)) {
            readSelectionKey.attach(null);
            readSelectionKey.cancel();
            readSelectionKey = null;
            log.trace("Connection " + id + " removed from read selector");
        }
    }

    private void dequeueForWrite() {
        if (nonNull(writeSelectionKey)) {
            writeSelectionKey.attach(null);
            writeSelectionKey.cancel();
            writeSelectionKey = null;
            log.trace("Connection " + id + " removed from write selector");
        }
    }

    private void setNonBlockingMode() {
        try {
            socketChannel.configureBlocking(false);
            log.debug("Connection " + id + " set to non-blocking mode");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

                if (isNextByte(INTERPRET_AS_COMMAND, buffer)) {
                    log.trace("input: telnet command");
                    log.debug("From " + id + ": " + convertTelnetCommandToString(buffer));
                    ByteBuffer response = handleTelnetCommand(buffer);
                    OutgoingMessage message = new OutgoingMessage(response, true);
                    outgoingQueue.add(message);
                } else if (isNextByte(DELETE, buffer)) {
                    log.trace("input: delete");
                    if (!characterBuffer.isEmpty()) {
                        byte b = buffer.get();
                        characterBuffer.remove(characterBuffer.size() - 1);
                        ByteBuffer response = convertByteToBuffer(b);
                        OutgoingMessage message = new OutgoingMessage(response, true);
                        outgoingQueue.add(message);
                    } else {
                        buffer.position(buffer.position() + 1);
                    }
                } else if (isNextByte(CARRIAGE_RETURN, buffer)) {
                    log.trace("input: carriage return");

                    String request = convertByteListToString(characterBuffer);
                    characterBuffer.clear();
                    incomingQueue.add(request);
                    log.debug("From " + id + ": " + request);

                    byte b = buffer.get();
                    ByteBuffer response = convertByteToBuffer(b);
                    OutgoingMessage message = new OutgoingMessage(response, true);
                    outgoingQueue.add(message);
                } else if (isNextByte(LINE_FEED, buffer)) {
                    log.trace("input: line feed");
                    byte b = buffer.get();
                    ByteBuffer response = convertByteToBuffer(b);
                    OutgoingMessage message = new OutgoingMessage(response, true);
                    outgoingQueue.add(message);
                } else {
                    log.trace("input: character");
                    byte b = buffer.get();
                    log.debug("From " + id + ": " + convertByteToString(b));
                    characterBuffer.add(b);
                    ByteBuffer response = convertByteToBuffer(b);
                    OutgoingMessage message = new OutgoingMessage(response, true);
                    outgoingQueue.add(message);
                }

                traceClass();
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
            OutgoingMessage message = outgoingQueue.poll();

            if (message != null && message.getBuffer().hasRemaining()) {

                if (isNextByte(INTERPRET_AS_COMMAND, message.getBuffer())) {
                    log.debug("To " + id + ": " + convertTelnetCommandToString(message.getBuffer()));
                } else {
                    log.debug("To " + id + ": " + convertBufferToString(message.getBuffer()));
                }

                if (message.isProtocolSpecific()) {
                    socketChannel.write(message.getBuffer());
                } else {
                    byte[] eraser = new byte[prompt.length() + characterBuffer.size()];
                    Arrays.fill(eraser, SPACE);

                    int bufferSize = 0;
                    bufferSize += 1; // carriage return
                    bufferSize += eraser.length;
                    bufferSize += 1; // carriage return
                    bufferSize += message.getBuffer().limit();
                    bufferSize += 2; // line ending
                    bufferSize += prompt.getBytes().length;
                    bufferSize += characterBuffer.size();

                    ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
                    buffer.put(CARRIAGE_RETURN);
                    buffer.put(eraser);
                    buffer.put(CARRIAGE_RETURN);
                    buffer.put(message.getBuffer());
                    buffer.put(CARRIAGE_RETURN);
                    buffer.put(LINE_FEED);
                    buffer.put(prompt.getBytes());
                    buffer.put(convertByteListToByteArray(characterBuffer));
                    buffer.flip();

                    socketChannel.write(buffer);
                }

                traceClass();
            }

            if (outgoingQueue.size() == 0) {
                dequeueForWrite();
            }
        } catch (IOException e) {
            throw new RuntimeException("error occured writing to socket", e);
        }

    }

    private void traceClass() {
        traceCollectionSize("characterBuffer", characterBuffer);
        traceCollectionSize("incomingQueue", incomingQueue);
        traceCollectionSize("outgoingQueue", outgoingQueue);
    }

    private boolean isNextByte(byte isByte, ByteBuffer buffer) {
        if (buffer.remaining() == 0)
            throw new IllegalArgumentException("buffer is empty");

        buffer.mark();
        byte b = buffer.get();
        buffer.reset();
        return b == isByte;
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
            response = handleOptionRequest(command, option);
        } else {
            log.debug("Ignoring telnet command: " + convertByteToInt(command));
            response = ByteBuffer.allocate(2);
            response.put(CARRIAGE_RETURN);
            response.put(LINE_FEED);
        }

        return response;
    }

    private String convertTelnetCommandToString(ByteBuffer buffer) {
        buffer.mark();
        byte iac = buffer.get();
        byte command = buffer.get();
        byte option = buffer.get();
        buffer.reset();
        return convertTelnetCommandToString(command, option);
    }

    private String convertTelnetCommandToString(byte command, byte option) {
        String commandAsString = convertCommandByteToString(command);
        String optionAsString = convertOptionByteToString(option);
        return String.format("IAC %s %s", commandAsString, optionAsString);
    }

    private String convertCommandByteToString(byte command) {
        switch (command) {
            case WILL:
                return "WILL";
            case WONT:
                return "WONT";
            case DO:
                return "DO";
            case DONT:
                return "DONT";
            default:
                return String.valueOf(convertByteToInt(command));
        }
    }

    private String convertOptionByteToString(byte option) {
        switch (option) {
            case OPTION_ECHO:
                return "ECHO";
            case OPTION_SUPRESS_GO_AHEAD:
                return "SUPRESS-GO-AHEAD";
            case OPTION_TERMINAL_TYPE:
                return "TERMINAL-TYPE";
            case OPTION_NEGOTIATE_ABOUT_WINDOW_SIZE:
                return "NEGOTIATE-ABOUT-WINDOW-SIZE";
            case OPTION_TERMINAL_SPEED:
                return "TERMINAL-SPEED";
            case OPTION_LINEMODE:
                return "LINEMODE";
            case OPTION_ENVIRONMENT_VARIABLE:
                return "ENVIRONMENT-VARIABLE";
            case OPTION_NEW_ENVIRONMENT_VARIABLE:
                return "NEW-ENVIRONMENT-VARIABLE";
            default:
                return String.valueOf(convertByteToInt(option));
        }
    }

    private boolean isOptionRequest(byte command) {
        return command == WILL || command == WONT || command == DO || command == DONT;
    }

    private ByteBuffer handleOptionRequest(byte command, byte option) {
        ByteBuffer output = ByteBuffer.allocate(3);
        output.put(INTERPRET_AS_COMMAND);

        if (option == OPTION_ECHO && command == WILL) {
            output.put(DONT);
        } else if (option == OPTION_ECHO && command == DO) {
            output.put(WILL);
        } else if (option == OPTION_SUPRESS_GO_AHEAD && command == WILL) {
            output.put(DO);
        } else if (option == OPTION_SUPRESS_GO_AHEAD && command == DO) {
            output.put(WILL);
        } else {
            if (command == DO) output.put(WONT);
            if (command == WILL) output.put(DONT);
        }

        output.put(option);
        output.flip();

        return output;
    }

}
