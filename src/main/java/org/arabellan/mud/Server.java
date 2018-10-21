package org.arabellan.mud;

import lombok.AllArgsConstructor;
import org.arabellan.mud.network.MessageProcessor;
import org.arabellan.mud.network.SocketListener;
import org.arabellan.mud.network.SocketProcessor;

import javax.inject.Inject;

@AllArgsConstructor(onConstructor=@__(@Inject))
class Server {

    private final SocketListener socketListener;
    private final SocketProcessor socketProcessor;
    private final MessageProcessor messageProcessor;

    void run() {
        Thread telnetListenerThread = new Thread(socketListener);
        Thread socketProcessingThread = new Thread(socketProcessor);
        Thread messageProcessingThread = new Thread(messageProcessor);

        messageProcessingThread.start();
        socketProcessingThread.start();
        telnetListenerThread.start();
    }

}
