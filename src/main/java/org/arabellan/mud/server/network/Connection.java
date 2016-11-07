package org.arabellan.mud.server.network;

import lombok.Getter;

import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class Connection implements Runnable {

    @Getter
    protected final ConcurrentLinkedQueue<String> incomingMessageQueue = new ConcurrentLinkedQueue<>();

    @Getter
    protected final ConcurrentLinkedQueue<String> outgoingMessageQueue = new ConcurrentLinkedQueue<>();

    public abstract String getClientAddress();
}
