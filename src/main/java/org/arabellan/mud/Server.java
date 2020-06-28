package org.arabellan.mud;

import com.google.common.eventbus.EventBus;
import lombok.extern.slf4j.Slf4j;
import org.arabellan.mud.network.Connection;
import org.arabellan.mud.network.SocketListener;
import org.arabellan.mud.network.SocketProcessor;

import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.arabellan.utils.PropertiesUtils.loadPropertiesFile;

@Slf4j
public class Server {

    public static void main(String[] args) {
        new Server().run();
    }

    private void run() {
        Properties config = loadPropertiesFile("/config.properties");
        validateConfig(config);

        Queue<Connection> connectionQueue = new ConcurrentLinkedQueue<>();
        Map<Integer, Connection> connectionMap = new ConcurrentHashMap<>();
        EventBus eventBus = new EventBus();

        int telnetPort = Integer.valueOf(config.getProperty("telnetPort"));
        Thread telnetListenerThread = new Thread(new SocketListener(telnetPort, connectionQueue));
        Thread socketProcessingThread = new Thread(new SocketProcessor(connectionQueue, connectionMap, eventBus));
        Thread messageProcessingThread = new Thread(new CommandProcessor(new CommandRepository(), eventBus));

        messageProcessingThread.start();
        socketProcessingThread.start();
        telnetListenerThread.start();
    }

    private void validateConfig(Properties config) {
        if (config.get("telnetPort") == null)
            throw new RuntimeException("missing \"telnetPort\" setting");
    }
}
