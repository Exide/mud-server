package org.arabellan.mud;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;
import org.arabellan.mud.events.BroadcastEvent;
import org.arabellan.mud.events.CommandEvent;
import org.arabellan.mud.events.IncomingMessageEvent;
import org.arabellan.mud.events.OutgoingMessageEvent;

import java.time.Duration;
import java.time.LocalTime;

import static java.util.Objects.nonNull;

@Slf4j
public class CommandProcessor implements Runnable {

    private final CommandRepository repository;
    private final EventBus eventBus;

    public CommandProcessor(CommandRepository repository, EventBus eventBus) {
        this.repository = repository;
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
                eventBus.post(new BroadcastEvent("tick!"));
                lastTick = now;
            }
        }
    }

    private class IncomingMessageHandler {
        @Subscribe
        public void handle(IncomingMessageEvent event) {
            log.trace("Handling IncomingMessageEvent");

            Command command = repository.get(event.getMessage());
            if (nonNull(command)) {
                eventBus.post(new CommandEvent(command));
            } else {
                eventBus.post(new OutgoingMessageEvent(event.getId(), "Your command had no effect."));
            }
        }
    }
}
