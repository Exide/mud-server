package org.arabellan.mud;

import com.google.common.eventbus.EventBus;
import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.arabellan.mud.network.Connection;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class GuiceModule extends AbstractModule {

    @Override
    protected void configure() {

    }

    @Provides
    @Singleton
    public Configuration provideConfiguration(Gson gson) throws IOException {
        String json = loadFileAsString("/config.json");
        return gson.fromJson(json, Configuration.class);
    }

    @Provides
    @Singleton
    public Queue<Connection> provideConnectionQueue() {
        return new ConcurrentLinkedQueue<>();
    }

    @Provides
    @Singleton
    public Map<Integer, Connection> provideConnectionMap() {
        return new ConcurrentHashMap<>();
    }

    @Provides
    @Singleton
    public EventBus provideEventBus() {
        return new EventBus();
    }

    private String loadFileAsString(String path) throws IOException {
        InputStream inputStream = this.getClass().getResourceAsStream(path);
        if (inputStream == null) throw new FileNotFoundException(path);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, length);
        }
        return baos.toString(StandardCharsets.UTF_8.name());
    }

}
