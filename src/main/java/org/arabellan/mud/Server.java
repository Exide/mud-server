package org.arabellan.mud;

import com.google.common.eventbus.EventBus;
import lombok.extern.slf4j.Slf4j;
import org.arabellan.mud.network.Connection;
import org.arabellan.mud.network.MessageProcessor;
import org.arabellan.mud.network.SocketProcessor;
import org.arabellan.mud.network.SocketListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
public class Server {

    public static void main(String[] args) {
        new Server().run();
    }

    private void run() {
        Properties config = loadConfigFile("/config.properties");
        validateConfig(config);

        Queue<Connection> connectionQueue = new ConcurrentLinkedQueue<>();
        Map<Integer, Connection> connectionMap = new ConcurrentHashMap<>();
        EventBus eventBus = new EventBus();

        int telnetPort = Integer.valueOf(config.getProperty("telnetPort"));
        Thread telnetListenerThread = new Thread(new SocketListener(telnetPort, connectionQueue));
        Thread socketProcessingThread = new Thread(new SocketProcessor(connectionQueue, connectionMap, eventBus));
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
