package org.arabellan.mud.network;

import lombok.Value;

@Value
class IncomingMessageEvent {
    int id;
    String message;
}
