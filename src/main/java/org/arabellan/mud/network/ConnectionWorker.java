package org.arabellan.mud.network;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;
import org.arabellan.mud.events.ConnectionClosed;
import org.arabellan.mud.events.ConnectionOpened;

import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Responsibility:
 * Take messages off connection queues and parse them
 */

@Slf4j
public class ConnectionWorker implements Runnable {

    private final EventBus eventBus;
    private final Set<Connection> connections = ConcurrentHashMap.newKeySet();
    private boolean running = true;

    public ConnectionWorker(EventBus eventBus) {
        this.eventBus = eventBus;
        this.eventBus.register(new RegisterConnection());
        this.eventBus.register(new DeregisterConnection());
    }

    public void run() {
        while (running) {
            for (Connection connection : connections) {
                Queue<String> incomingMessages = connection.getIncomingMessageQueue();
                while (incomingMessages.size() > 0) {
                    String message = incomingMessages.remove();
                    List<String> tokens = Arrays.stream(message.trim().split("\\s+")).collect(Collectors.toList());

                    // gossip hack
                    if (tokens.size() < 2) continue;
                    String command = tokens.remove(0);
                    String arguments = tokens.stream().collect(Collectors.joining(" "));
                    if ("gossip".contains(command)) {
                        sendToEveryone(arguments);
                    }
                }
            }
        }
    }

    private void sendToEveryone(String message) {
        log.trace("Sending to everyone: " + message);
        for (Connection connection : connections) {
            Queue<String> outgoingMessages = connection.getOutgoingMessageQueue();
            outgoingMessages.add(message);
        }
    }

    public void stop() {
        running = false;
    }

    private class RegisterConnection {
        @Subscribe
        public void register(ConnectionOpened event) {
            connections.add(event.getConnection());
            log.trace("Connection registered: " + event.getConnection().getClientAddress());
        }
    }

    private class DeregisterConnection {
        @Subscribe
        public void deregister(ConnectionClosed event) {
            connections.remove(event.getConnection());
            log.trace("Connection deregistered: " + event.getConnection().getClientAddress());
        }
    }
}
