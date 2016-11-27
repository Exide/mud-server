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

@Slf4j
public class SocketProcessor implements Runnable {

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

                    log.debug("Socket " + socket.getId() + " read selected");

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

                    while (buffer.remaining() != 0) {
                        if (TelnetProtocol.nextByteIAC(buffer)) {
                            ByteBuffer request = TelnetProtocol.extractTelnetCommand(buffer);
                            ByteBuffer response = TelnetProtocol.parseTelnetCommand(request);
                            socket.addWriteSelector(writeSelector);
                            socket.getOutgoingQueue().add(response);
                        } else {
                            byte[] bytes = new byte[buffer.limit()];
                            buffer.get(bytes);
                            String output = new String(bytes);
                            String trimmedOutput = output.trim();
                            log.debug("From " + socket.getId() + ": " + trimmedOutput);
                            trimmedOutput += "\r\n";
                            ByteBuffer outputBuffer = ByteBuffer.allocate(trimmedOutput.length());
                            outputBuffer.put(trimmedOutput.getBytes());
                            IncomingMessageEvent message = new IncomingMessageEvent(socket.getId(), outputBuffer);
                            eventBus.post(message);
                        }
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
                    ByteBuffer message = socket.getOutgoingQueue().poll();

                    if (message != null && message.remaining() > 0) {

                        if (socket.getProtocol() == Socket.Protocol.TELNET && !TelnetProtocol.nextByteIAC(message)) {
                            log.debug("To " + socket.getId() + ": " + StringUtils.fromByteBuffer(message));
                        }

                        socket.getSocketChannel().write(message);
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

    private class OutgoingMessageHandler {
        @Subscribe
        public void handle(OutgoingMessageEvent event) {
            log.trace("Handling OutgoingMessageEvent");
            Socket socket = socketMap.get(event.getId());
            socket.addWriteSelector(writeSelector);
            socket.getOutgoingQueue().add(event.getBuffer());
        }
    }

    private class BroadcastMessageHandler {
        @Subscribe
        public void handle(BroadcastMessageEvent event) {
            log.trace("Handling BroadcastMessageEvent");
            for (Socket socket : socketMap.values()) {
                socket.addWriteSelector(writeSelector);
                socket.getOutgoingQueue().add(event.getBuffer());
            }
        }
    }
}
