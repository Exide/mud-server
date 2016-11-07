package org.arabellan.mud.network;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Responsibility:
 * Listen for incoming Telnet connections and place them on the queue
 */

@Slf4j
public class TelnetListener implements Runnable {

    private static final int PORT = 23;

    private final ConcurrentLinkedQueue<Socket> socketQueue;
    private ServerSocket listenerSocket;
    private boolean listening = true;

    public TelnetListener(ConcurrentLinkedQueue<Socket> socketQueue) {
        this.socketQueue = socketQueue;
    }

    public void run() {
        openListenerSocket();
        log.info("Listening on " + listenerSocket.getInetAddress() + ":" + PORT);

        while (listening) {
            Socket socket = acceptClientSocket(listenerSocket);
            log.info("Accepting connection from " + socket.getInetAddress().getHostAddress());
            socketQueue.add(socket);
        }

        closeListenerSocket();
    }

    public void stop() {
        listening = false;
    }

    private void openListenerSocket() {
        try {
            listenerSocket = new ServerSocket(PORT);
        } catch (IOException e) {
            throw new RuntimeException("Error opening listener socket", e);
        }
    }

    private Socket acceptClientSocket(ServerSocket listenerSocket) {
        try {
            return listenerSocket.accept();
        } catch (IOException e) {
            throw new RuntimeException("Error accepting socket", e);
        }
    }

    private void closeListenerSocket() {
        try {
            listenerSocket.close();
        } catch (IOException e) {
            throw new RuntimeException("Error closing listener socket", e);
        }
    }
}
