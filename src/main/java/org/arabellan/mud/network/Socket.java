package org.arabellan.mud.network;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;

@Slf4j
@Getter
public class Socket {
    int id;
    State state;
    Protocol protocol;
    SocketChannel socketChannel;
    SelectionKey readSelectionKey;
    SelectionKey writeSelectionKey;
    Queue<ByteBuffer> outgoingQueue = new LinkedList<>();

    Socket(SocketChannel socketChannel, Protocol protocol) {
        this.id = socketChannel.hashCode();
        this.protocol = protocol;
        this.socketChannel = socketChannel;
        setState(State.OPEN);
    }

    void setState(State state) {
        log.debug("Socket " + id + " set to " + state.name());
        this.state = state;
    }

    void addReadSelector(Selector selector) {
        try {
            readSelectionKey = socketChannel.register(selector, OP_READ, this);
            log.trace("Socket " + id + " added read selector");
        } catch (ClosedChannelException e) {
            setState(State.CLOSED);
        }
    }

    void addWriteSelector(Selector selector) {
        try {
            if (writeSelectionKey == null) {
                writeSelectionKey = socketChannel.register(selector, OP_WRITE, this);
                log.trace("Socket " + id + " added write selector");
            }
        } catch (ClosedChannelException e) {
            setState(State.CLOSED);
        }
    }

    void removeWriteSelector() {
        writeSelectionKey.attach(null);
        writeSelectionKey.cancel();
        writeSelectionKey = null;
        log.trace("Socket " + id + " removed write selector");
    }

    void setNonBlockingMode() {
        try {
            socketChannel.configureBlocking(false);
            log.debug("Socket " + id + " set to non-blocking mode");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void send(ByteBuffer buffer, Selector writeSelector) {
        ByteBuffer clone = ByteBuffer.allocate(buffer.remaining());
        clone.put(buffer);
        buffer.rewind();
        clone.flip();
        outgoingQueue.add(clone);
        addWriteSelector(writeSelector);
    }

    enum State {
        OPEN, CLOSED
    }

    enum Protocol {
        TELNET
    }
}
