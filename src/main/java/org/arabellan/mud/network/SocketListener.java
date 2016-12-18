package org.arabellan.mud.network;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Queue;

@Slf4j
public class SocketListener implements Runnable {

    private final int port;
    private final Queue<Connection> connectionQueue;

    public SocketListener(int port, Queue<Connection> connectionQueue) {
        this.port = port;
        this.connectionQueue = connectionQueue;
    }

    public void run() {
        try {
            ServerSocketChannel listenerSocket = ServerSocketChannel.open();
            InetSocketAddress socketAddress = new InetSocketAddress(port);
            listenerSocket.bind(socketAddress);
            log.info("Listening on " + socketAddress.toString());

            while (true) {
                SocketChannel socketChannel = listenerSocket.accept();
                Connection connection = new Connection(socketChannel);
                log.info("Connection accepted: " + connection.getId());
                connectionQueue.add(connection);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
