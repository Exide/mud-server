package org.arabellan.mud.network;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;
import org.arabellan.mud.events.ConnectionClosed;
import org.arabellan.mud.events.ConnectionOpened;

import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Responsibility:
 * Take messages off connection queues and parse them
 */

@Slf4j
public class ConnectionWorker implements Runnable {

    private boolean running = true;
    private final EventBus eventBus;
    private final Set<Connection> connections = ConcurrentHashMap.newKeySet();

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
                    log.info(connection.getClientAddress() + ": " + message);
                }
            }
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
