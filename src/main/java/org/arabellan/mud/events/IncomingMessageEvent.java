package org.arabellan.mud.events;

import lombok.Value;

@Value
public class IncomingMessageEvent {
    int id;
    String message;
}
