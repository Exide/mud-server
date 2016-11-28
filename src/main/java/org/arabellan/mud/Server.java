package org.arabellan.mud;

import com.google.common.eventbus.EventBus;
import lombok.extern.slf4j.Slf4j;
import org.arabellan.mud.network.MessageProcessor;
import org.arabellan.mud.network.SocketProcessor;
import org.arabellan.mud.network.SocketListener;
import org.arabellan.mud.network.Socket;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
public class Server {

    public static void main(String[] args) {
        new Server().run();
    }

    private void run() {
        Properties config = loadConfigFile("/config.properties");
        validateConfig(config);

        Queue<Socket> socketQueue = new ConcurrentLinkedQueue<>();
        EventBus eventBus = new EventBus();

        int telnetPort = Integer.valueOf(config.getProperty("telnetPort"));
        Thread telnetListenerThread = new Thread(new SocketListener(telnetPort, socketQueue));
        Thread socketProcessingThread = new Thread(new SocketProcessor(socketQueue, eventBus));
        Thread messageProcessingThread = new Thread(new MessageProcessor(eventBus));

        messageProcessingThread.start();
        socketProcessingThread.start();
        telnetListenerThread.start();
    }

    private Properties loadConfigFile(String filename) {
        try {
            Properties properties = new Properties();
            InputStream inputStream = Server.class.getResourceAsStream(filename);
            properties.load(inputStream);
            return properties;
        } catch (IOException e) {
            throw new RuntimeException("cannot load config file: " + filename, e);
        }
    }

    private void validateConfig(Properties config) {
        if (config.get("telnetPort") == null)
            throw new RuntimeException("missing \"telnetPort\" setting");
    }
}
