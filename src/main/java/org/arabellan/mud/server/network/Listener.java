package org.arabellan.mud.server.network;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class Listener {

    private static final int PORT = 23;
    private static final short WORKER_THREADS = 50;

    private ServerSocket listenerSocket;
    private ExecutorService executor = Executors.newFixedThreadPool(WORKER_THREADS);

    public void start() {
        try {
            listenerSocket = new ServerSocket(PORT);
            log.info("Listener started on " + listenerSocket.getInetAddress() + ":" + PORT);

            while (!listenerSocket.isClosed()) {
                Socket socket = listenerSocket.accept();
                executor.execute(new SocketWorker(socket));
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot open a listening socket on port " + PORT, e);
        }
    }

    public void stop() {
        try {
            executor.shutdown();
            listenerSocket.close();
            log.info("Listener shutdown.");
        } catch (IOException e) {
            throw new RuntimeException("Cannot close the listening socket", e);
        }
    }
}
