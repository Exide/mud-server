package org.arabellan.mud.network;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import static org.arabellan.mud.network.Socket.State.CLOSED;

/**
 * Handle non-blocking socket reads and writes using the Telnet specification
 *
 * @see <a href="https://tools.ietf.org/html/rfc854">RFC 854: Telnet Protocol Specification</a>
 * @see <a href="https://tools.ietf.org/html/rfc855">RFC 855: Telnet Option Specification</a>
 */

@Slf4j
public class SocketProcessor implements Runnable {

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

    private final Queue<Socket> socketQueue;
    private final Map<Integer, Socket> socketMap;
    private final EventBus eventBus;

    private Selector readSelector;
    private Selector writeSelector;

    public SocketProcessor(Queue<Socket> socketQueue, EventBus eventBus) {
        this.socketQueue = socketQueue;
        this.socketMap = new HashMap<>();
        this.eventBus = eventBus;
        this.eventBus.register(new OutgoingMessageHandler());
        this.eventBus.register(new BroadcastMessageHandler());
    }

    public void run() {
        try {
            this.readSelector = Selector.open();
            this.writeSelector = Selector.open();

            while (true) {
                cullClosedSockets();
                acceptNewSockets();
                readSockets();
                writeSockets();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void cullClosedSockets() {
        socketMap.entrySet().removeIf(entry -> {
            boolean isClosed = entry.getValue().getState() == CLOSED;
            if (isClosed) log.info("Socket closed: " + entry.getValue().getId());
            return isClosed;
        });
    }

    private void acceptNewSockets() {
        Socket socket = socketQueue.poll();
        while (socket != null) {
            log.info("Socket accepted: " + socket.getId());
            socket.setNonBlockingMode();
            socket.addReadSelector(readSelector);
            socketMap.put(socket.getId(), socket);
            socket = socketQueue.poll();
        }
    }

    private void readSockets() {
        try {
            int keysReady = readSelector.selectNow();
            if (keysReady > 0) {
                Set<SelectionKey> keys = readSelector.selectedKeys();
                for (SelectionKey key : keys) {
                    Socket socket = (Socket) key.attachment();
                    ByteBuffer buffer = ByteBuffer.allocate(1024);

                    int bytesRead = socket.getSocketChannel().read(buffer);
                    while (bytesRead > 0) {
                        bytesRead = socket.getSocketChannel().read(buffer);
                    }

                    if (bytesRead == -1) {
                        key.attach(null);
                        key.cancel();
                        socket.setState(CLOSED);
                    }

                    buffer.flip();

                    byte[] bytes = new byte[buffer.limit()];
                    buffer.get(bytes);
                    buffer.rewind();
                    String input = new String(bytes);
                    String trimmedInput = input.trim();
                    log.debug("From " + socket.getId() + ": " + trimmedInput);

                    while (buffer.hasRemaining()) {
                        if (nextByteIAC(buffer)) {
                            ByteBuffer response = handleTelnetCommand(buffer);
                            socket.getOutgoingQueue().add(response);
                        } else {
                            ByteBuffer clonedBuffer = ByteBuffer.allocate(buffer.remaining());
                            clonedBuffer.put(buffer);
                            clonedBuffer.flip();
                            IncomingMessageEvent message = new IncomingMessageEvent(socket.getId(), clonedBuffer);
                            eventBus.post(message);
                        }
                    }

                    if (socket.getOutgoingQueue().size() > 0) {
                        socket.addWriteSelector(writeSelector);
                    }
                }

                keys.clear();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeSockets() {
        try {
            int keysReady = writeSelector.selectNow();
            if (keysReady > 0) {
                Set<SelectionKey> keys = writeSelector.selectedKeys();

                for (SelectionKey key : keys) {
                    Socket socket = (Socket) key.attachment();
                    ByteBuffer buffer = socket.getOutgoingQueue().poll();

                    if (buffer != null && buffer.hasRemaining()) {

                        byte[] bytes = new byte[buffer.limit()];
                        buffer.get(bytes);
                        buffer.rewind();
                        String output = new String(bytes);
                        String trimmedOutput = output.trim();
                        log.debug("To " + socket.getId() + ": " + trimmedOutput);

                        socket.getSocketChannel().write(buffer);
                    }

                    if (socket.getOutgoingQueue().size() == 0) {
                        socket.removeWriteSelector();
                    }
                }

                keys.clear();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
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

    private int convertByteToInt(byte b) {
        return b & 0xFF;
    }

    private byte convertIntToByte(int i) {
        return (byte) i;
    }

    private class OutgoingMessageHandler {
        @Subscribe
        public void handle(OutgoingMessageEvent event) {
            log.trace("Handling OutgoingMessageEvent");
            Socket socket = socketMap.get(event.getId());
            socket.send(event.getBuffer(), writeSelector);
        }
    }

    private class BroadcastMessageHandler {
        @Subscribe
        public void handle(BroadcastMessageEvent event) {
            log.trace("Handling BroadcastMessageEvent");
            for (Socket socket : socketMap.values()) {
                socket.send(event.getBuffer(), writeSelector);
            }
        }
    }
}
