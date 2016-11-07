package org.arabellan.mud.server;

import com.google.common.eventbus.EventBus;
import lombok.extern.slf4j.Slf4j;
import org.arabellan.mud.server.network.Connection;
import org.arabellan.mud.server.network.ConnectionWorker;
import org.arabellan.mud.server.network.TelnetConnection;
import org.arabellan.mud.server.network.TelnetListener;

import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.arabellan.mud.server.utils.ThreadUtils.setThreadName;
import static org.arabellan.mud.server.utils.ThreadUtils.setThreadNameWithID;

@Slf4j
public class Server {

    private static final int MAX_CONNECTIONS = 10;

    private final ExecutorService listenerThread = Executors.newSingleThreadExecutor(setThreadName("listener"));
    private final ExecutorService workerThread = Executors.newSingleThreadExecutor(setThreadName("worker"));
    private final ExecutorService connectionThreadPool = Executors.newFixedThreadPool(MAX_CONNECTIONS, setThreadNameWithID("connection"));
    private final ConcurrentLinkedQueue<Socket> socketQueue;

    private Server() {
        this.socketQueue = new ConcurrentLinkedQueue<>();
    }

    public static void main(String[] args) {
        try {
            new Server().run();
        } catch (Exception e) {
            throw new RuntimeException("An error occured that we can't recover from", e);
        }
    }

    private void run() {
        EventBus eventBus = new EventBus();
        workerThread.execute(new ConnectionWorker(eventBus));
        listenerThread.execute(new TelnetListener(socketQueue));

        while (true) {
            if (socketQueue.size() > 0) {
                Socket socket = socketQueue.remove();
                Connection connection = new TelnetConnection(socket, eventBus);
                connectionThreadPool.execute(connection);
            }
        }
    }
}
