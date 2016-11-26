package org.arabellan.mud.network;

import lombok.Value;

import java.nio.ByteBuffer;

@Value
class IncomingMessageEvent {
    int id;
    ByteBuffer buffer;
}
