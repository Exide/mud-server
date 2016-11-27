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
            log.debug("Read selector added");
        } catch (ClosedChannelException e) {
            setState(State.CLOSED);
        }
    }

    void addWriteSelector(Selector selector) {
        try {
            if (writeSelectionKey == null) {
                writeSelectionKey = socketChannel.register(selector, OP_WRITE, this);
                log.debug("Write selector added");
            }
        } catch (ClosedChannelException e) {
            setState(State.CLOSED);
        }
    }

    void removeWriteSelector() {
        writeSelectionKey.attach(null);
        writeSelectionKey.cancel();
        writeSelectionKey = null;
        log.debug("Write selector removed");
    }

    void setNonBlockingMode() {
        try {
            socketChannel.configureBlocking(false);
            log.debug("Socket " + id + " set to non-blocking mode");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    enum State {
        OPEN, CLOSED
    }

    enum Protocol {
        TELNET
    }
}
