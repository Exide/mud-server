package org.arabellan.mud.network;

import lombok.Value;

import java.nio.ByteBuffer;

@Value
public class OutgoingMessage {

    boolean protocolSpecific;
    ByteBuffer buffer;

    OutgoingMessage(ByteBuffer buffer) {
        this(buffer, false);
    }

    OutgoingMessage(ByteBuffer buffer, boolean protocolSpecific) {
        this.buffer = buffer;
        this.protocolSpecific = protocolSpecific;
    }
}
