package org.arabellan.mud.network;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import static java.util.Objects.nonNull;
import static org.arabellan.utils.ConversionUtils.convertBufferToString;

@Slf4j
public class SocketProcessor implements Runnable {

    private final Queue<Connection> connectionQueue;
    private final Map<Integer, Connection> connectionMap;
    private final EventBus eventBus;

    private Selector readSelector;
    private Selector writeSelector;

    @Inject
    public SocketProcessor(Queue<Connection> connectionQueue, Map<Integer, Connection> connectionMap, EventBus eventBus) {
        this.connectionQueue = connectionQueue;
        this.connectionMap = connectionMap;
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
        connectionMap.entrySet().removeIf(entry -> {
            boolean isClosed = entry.getValue().isClosed();
            if (isClosed) log.info("Connection closed: " + entry.getValue().getId());
            return isClosed;
        });
    }

    private void acceptNewSockets() {
        Connection connection = connectionQueue.poll();
        while (connection != null) {
            log.info("Connection accepted: " + connection.getId());
            connection.setNonBlockingMode();
            connection.setReadSelector(readSelector);
            connection.setWriteSelector(writeSelector);
            connection.queueForRead();
            connectionMap.put(connection.getId(), connection);
            connection = connectionQueue.poll();
        }
    }

    private void readSockets() {
        try {
            int keysReady = readSelector.selectNow();
            if (keysReady > 0) {
                Set<SelectionKey> keys = readSelector.selectedKeys();

                for (SelectionKey key : keys) {
                    Connection connection = (Connection) key.attachment();
                    connection.read();

                    // post all incoming messages to the event bus
                    String message = connection.getIncomingQueue().poll();
                    while (nonNull(message)) {
                        eventBus.post(new IncomingMessageEvent(connection.getId(), message));
                        message = connection.getIncomingQueue().poll();
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
                    Connection connection = (Connection) key.attachment();
                    connection.write();
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
            Connection connection = connectionMap.get(event.getId());
            String message = convertBufferToString(event.getBuffer());
            connection.send(message);
        }
    }

    private class BroadcastMessageHandler {
        @Subscribe
        public void handle(GossipEvent event) {
            log.trace("Handling GossipEvent");
            for (Connection connection : connectionMap.values()) {
                connection.send(event.getMessage());
            }
        }
    }
}
