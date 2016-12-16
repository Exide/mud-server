package org.arabellan.mud.network;

import lombok.Value;

@Value
class OutgoingMessageEvent {
    int id;
    String message;
}
