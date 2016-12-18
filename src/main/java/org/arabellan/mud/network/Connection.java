package org.arabellan.mud.network;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static java.util.Objects.nonNull;

@Slf4j
@Data
public abstract class Connection {

    protected int id;
    protected SocketChannel socketChannel;
    protected Queue<String> outgoingQueue = new LinkedList<>();
    protected Queue<String> incomingQueue = new LinkedList<>();

    private boolean isClosed;
    private Selector readSelector;
    private Selector writeSelector;
    private SelectionKey readSelectionKey;
    private SelectionKey writeSelectionKey;

    Connection(SocketChannel socketChannel) {
        this.id = socketChannel.hashCode();
        this.socketChannel = socketChannel;
        this.isClosed = false;
    }

    abstract void write();

    abstract void read();

    void close() {
        dequeueForRead();
        dequeueForWrite();
        isClosed = true;
    }

    void queueForRead() {
        try {
            readSelectionKey = socketChannel.register(readSelector, OP_READ, this);
            log.trace("Connection " + id + " added read selector");
        } catch (ClosedChannelException e) {
            close();
        }
    }

    void queueForWrite() {
        try {
            if (writeSelectionKey == null) {
                writeSelectionKey = socketChannel.register(writeSelector, OP_WRITE, this);
                log.trace("Connection " + id + " added write selector");
            }
        } catch (ClosedChannelException e) {
            close();
        }
    }

    void dequeueForRead() {
        if (nonNull(readSelectionKey)) {
            readSelectionKey.attach(null);
            readSelectionKey.cancel();
            readSelectionKey = null;
            log.trace("Connection " + id + " removed from read selector");
        }
    }

    void dequeueForWrite() {
        if (nonNull(writeSelectionKey)) {
            writeSelectionKey.attach(null);
            writeSelectionKey.cancel();
            writeSelectionKey = null;
            log.trace("Connection " + id + " removed from write selector");
        }
    }

    void setNonBlockingMode() {
        try {
            socketChannel.configureBlocking(false);
            log.debug("Connection " + id + " set to non-blocking mode");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void send(String message) {
        message += "\r\n";
        outgoingQueue.add(message);
        queueForWrite();
    }
}
