package org.arabellan.mud.network;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;

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
            BroadcastMessageEvent broadcast = new BroadcastMessageEvent(event.getBuffer());
            eventBus.post(broadcast);
        }
    }
}
