package org.arabellan.mud.network;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;
import org.arabellan.mud.events.BroadcastEvent;
import org.arabellan.mud.events.GossipEvent;
import org.arabellan.mud.events.IncomingMessageEvent;
import org.arabellan.mud.events.LookRoomEvent;
import org.arabellan.mud.events.OutgoingMessageEvent;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import static java.util.Objects.nonNull;

@Slf4j
public class SocketProcessor implements Runnable {

    private final Queue<Connection> connectionQueue;
    private final Map<Integer, Connection> connectionMap;
    private final EventBus eventBus;

    private Selector readSelector;
    private Selector writeSelector;

    public SocketProcessor(Queue<Connection> connectionQueue, Map<Integer, Connection> connectionMap, EventBus eventBus) {
        this.connectionQueue = connectionQueue;
        this.connectionMap = connectionMap;
        this.eventBus = eventBus;
        this.eventBus.register(new OutgoingMessageHandler());
        this.eventBus.register(new BroadcastHandler());
        this.eventBus.register(new GossipHandler());
        this.eventBus.register(new LookRoomHandler());
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
            connection.initialize(readSelector, writeSelector);
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
            connection.sendString(event.getMessage());
        }
    }

    private class BroadcastHandler {
        @Subscribe
        public void handle(BroadcastEvent event) {
            log.trace("Handling BroadcastEvent");
            for (Connection connection : connectionMap.values()) {
                connection.sendString(event.getMessage());
            }
        }
    }

    private class GossipHandler {
        @Subscribe
        public void handle(GossipEvent event) {
            log.trace("Handling GossipEvent");
            for (Connection connection : connectionMap.values()) {
                connection.sendString(event.getMessage());
            }
        }
    }

    private class LookRoomHandler {
        @Subscribe
        public void handle(LookRoomEvent event) {
            log.trace("Handling LookRoomEvent");
            Connection connection = connectionMap.get(event.getId());
            connection.sendString("Room Name\r\nObvious exits: none");
        }
    }
}
