package org.arabellan.mud;

import com.google.common.eventbus.EventBus;
import lombok.extern.slf4j.Slf4j;
import org.arabellan.mud.network.MessageProcessor;
import org.arabellan.mud.network.SocketProcessor;
import org.arabellan.mud.network.TelnetListener;
import org.arabellan.mud.network.Socket;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
public class Server {

    public static void main(String[] args) {
        new Server().run();
    }

    private void run() {
        Queue<Socket> socketQueue = new ConcurrentLinkedQueue<>();
        EventBus eventBus = new EventBus();

        Thread telnetListenerThread = new Thread(new TelnetListener(socketQueue));
        Thread socketProcessingThread = new Thread(new SocketProcessor(socketQueue, eventBus));
        Thread messageProcessingThread = new Thread(new MessageProcessor(eventBus));

        messageProcessingThread.start();
        socketProcessingThread.start();
        telnetListenerThread.start();
    }
}
