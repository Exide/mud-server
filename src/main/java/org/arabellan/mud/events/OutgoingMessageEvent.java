package org.arabellan.mud.events;

import lombok.Value;

@Value
public class OutgoingMessageEvent {
    int id;
    String message;
}
