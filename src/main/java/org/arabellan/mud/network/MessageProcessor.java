package org.arabellan.mud.network;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;

import static org.arabellan.utils.ConversionUtils.convertBufferToString;

@Slf4j
public class MessageProcessor implements Runnable {

    private final EventBus eventBus;

    public MessageProcessor(EventBus eventBus) {
        this.eventBus = eventBus;
        this.eventBus.register(new IncomingMessageHandler());
    }

    public void run() {
        while (true) {
        }
    }

    private class IncomingMessageHandler {
        @Subscribe
        public void handle(IncomingMessageEvent event) {
            log.trace("Handling IncomingMessageEvent");
            ByteBuffer buffer = event.getBuffer();
            String message = convertBufferToString(buffer);

            // parse gossip messages
            if (GossipEvent.match(message)) {
                eventBus.post(new GossipEvent(event.getId(), message));
            }
        }
    }
}
