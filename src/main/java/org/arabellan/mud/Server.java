package org.arabellan.mud;

import com.google.common.eventbus.EventBus;
import lombok.extern.slf4j.Slf4j;
import org.arabellan.mud.network.MessageProcessor;
import org.arabellan.mud.network.SocketProcessor;
import org.arabellan.mud.network.SocketListener;
import org.arabellan.mud.network.Socket;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
public class Server {

    private static final int TELNET_PORT = 23;

    public static void main(String[] args) {
        new Server().run();
    }

    private void run() {
        Queue<Socket> socketQueue = new ConcurrentLinkedQueue<>();
        EventBus eventBus = new EventBus();

        Thread telnetListenerThread = new Thread(new SocketListener(TELNET_PORT, socketQueue));
        Thread socketProcessingThread = new Thread(new SocketProcessor(socketQueue, eventBus));
        Thread messageProcessingThread = new Thread(new MessageProcessor(eventBus));

        messageProcessingThread.start();
        socketProcessingThread.start();
        telnetListenerThread.start();
    }
}
