package org.arabellan.mud.network;

import lombok.Value;

import java.nio.ByteBuffer;

@Value
class OutgoingMessageEvent {
    int id;
    ByteBuffer buffer;
}
