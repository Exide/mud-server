package org.arabellan.mud.network;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Queue;

@Slf4j
public class TelnetListener implements Runnable {

    private static final int TELNET_PORT = 2323;

    private final Queue<Socket> socketQueue;

    public TelnetListener(Queue<Socket> socketQueue) {
        this.socketQueue = socketQueue;
    }

    public void run() {
        try {
            ServerSocketChannel listenerSocket = ServerSocketChannel.open();
            InetSocketAddress socketAddress = new InetSocketAddress(TELNET_PORT);
            listenerSocket.bind(socketAddress);
            log.info("Listening on " + socketAddress.toString());

            while (true) {
                SocketChannel socketChannel = listenerSocket.accept();
                Socket socket = new Socket(socketChannel, Socket.Protocol.TELNET);
                log.info("Socket opened: " + socket.getId());
                socketQueue.add(socket);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
