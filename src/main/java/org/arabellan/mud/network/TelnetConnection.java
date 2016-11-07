package org.arabellan.mud.network;

import com.google.common.eventbus.EventBus;
import lombok.extern.slf4j.Slf4j;
import org.arabellan.mud.events.ConnectionClosed;
import org.arabellan.mud.events.ConnectionOpened;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

/**
 * Responsibility:
 * Communicate with the socket using the Telnet protocol
 */

@Slf4j
public class TelnetConnection extends Connection {

    private static final int IAC = 255;
    private static final int DONT = 254;
    private static final int DO = 253;
    private static final int WONT = 252;
    private static final int WILL = 251;

    private final Socket socket;
    private final BufferedInputStream input;
    private final BufferedOutputStream output;
    private final EventBus eventBus;
    private boolean active = true;

    public TelnetConnection(Socket socket, EventBus eventBus) {
        this.socket = socket;
        this.eventBus = eventBus;

        try {
            this.input = new BufferedInputStream(socket.getInputStream());
            this.output = new BufferedOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException("Socket closed prematurely", e);
        }
    }

    public void run() {
        log.info("Connection opened");
        negotiateTelnetOptions();
        eventBus.post(new ConnectionOpened(this));

        while (active) {
            if (isClosedRemotely()) {
                log.trace("Closed remotely.");
                active = false;
                continue;
            }

            read();
            write();
        }

        eventBus.post(new ConnectionClosed(this));
        log.info("Connection closed");
    }

    private boolean isClosedRemotely() {
        try {
            input.mark(1);
            int value = input.read();
            input.reset();
            return value == -1;
        } catch (IOException e) {
            throw new RuntimeException("Error checking for remote closure on socket " + getClientAddress(), e);
        }
    }

    private void read() {
        try {
            InputStreamReader inputStreamReader = new InputStreamReader(input);
            BufferedReader reader = new BufferedReader(inputStreamReader);
            while (reader.ready()) {
                String message = reader.readLine();
                log.debug("Incoming message: " + message);
                incomingMessageQueue.add(message);
            }
        } catch (IOException e) {
            log.warn("Error reading from socket " + getClientAddress(), e);
            active = false;
        }
    }

    private void write() {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(output);
            BufferedWriter writer = new BufferedWriter(outputStreamWriter);
            while (outgoingMessageQueue.size() > 0) {
                String message = outgoingMessageQueue.remove();
                writer.write(message + "\r\n");
            }
            writer.flush();
        } catch (IOException e) {
            log.warn("Error writing to socket " + getClientAddress(), e);
            active = false;
        }
    }

    private void negotiateTelnetOptions() {
        try {
            log.trace("Telnet negotiation started");

            boolean negotiating = true;
            while (negotiating) {

                // only negotiate if theres data available
                if (input.available() < 1) {
                    log.debug("No data available");
                    negotiating = false;
                    continue;
                }

                // only negotiate if we receive IAC
                input.mark(1);
                int nextByte = input.read();
                if (nextByte != IAC) {
                    log.debug("No IAC received");
                    input.reset();
                    negotiating = false;
                    continue;
                }

                int command = input.read();
                int option = input.read();

                output.write(IAC);

                if (command == DO) output.write(WONT);
                if (command == WILL) output.write(DONT);

                output.write(option);
                output.flush();

                log.debug("Ignored telnet option: " + option);
            }

            log.trace("Telnet negotiation complete");

        } catch (IOException e) {
            throw new RuntimeException("Error negotiating Telnet options with socket " + getClientAddress(), e);
        }
    }

    public String getClientAddress() {
        return socket.getInetAddress().getHostAddress();
    }
}
