package org.arabellan.mud.network;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalTime;
import java.util.regex.Matcher;

@Slf4j
public class MessageProcessor implements Runnable {

    private final EventBus eventBus;

    public MessageProcessor(EventBus eventBus) {
        this.eventBus = eventBus;
        this.eventBus.register(new IncomingMessageHandler());
    }

    public void run() {
        boolean threeSecondsPassed;
        LocalTime lastTick = LocalTime.now();

        while (true) {
            LocalTime now = LocalTime.now();
            threeSecondsPassed = Duration.between(lastTick, now).getSeconds() >= 3;
            if (threeSecondsPassed) {
                eventBus.post(new BroadcastEvent("\btick!"));
                lastTick = now;
            }
        }
    }

    private class IncomingMessageHandler {
        @Subscribe
        public void handle(IncomingMessageEvent event) {
            log.trace("Handling IncomingMessageEvent");

            // parse gossip messages
            Matcher matcher = GossipEvent.GOSSIP_PATTERN.matcher(event.getMessage());
            if (matcher.matches()) {
                String gossip = matcher.group(1);
                eventBus.post(new GossipEvent(event.getId(), gossip));
            } else if (event.getMessage().length() > 0) {
                eventBus.post(new OutgoingMessageEvent(event.getId(), "Your command had no effect."));
            } else if (event.getMessage().length() == 0) {
                eventBus.post(new OutgoingMessageEvent(event.getId(), ""));
            }
        }
    }
}
