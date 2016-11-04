package org.arabellan.mud.server.network;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;

@Slf4j
class SocketWorker implements Runnable {

    private static final int IAC = 255;
    private static final int DONT = 254;
    private static final int DO = 253;
    private static final int WONT = 252;
    private static final int WILL = 251;

    private final Socket socket;

    SocketWorker(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            log.info("Socket open: " + getClientAddress());

            BufferedInputStream input = new BufferedInputStream(socket.getInputStream());
            BufferedOutputStream output = new BufferedOutputStream(socket.getOutputStream());

            negotiateTelnetOptions(input, output);

            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output));

            writer.write("Connected!\r\n");
            writer.flush();

            String message;
            while ((message = reader.readLine()) != null) log.debug(message);

            socket.close();

            log.info("Socket closed: " + getClientAddress());

    } catch (IOException e) {
            throw new RuntimeException("An error occurred communicating with " + getClientAddress(), e);
        }
    }

    private void negotiateTelnetOptions(InputStream input, OutputStream output) throws IOException {
        log.debug("Telnet negotiation started.");

        if (!input.markSupported()) throw new IllegalArgumentException("input must support mark/reset");

        boolean negotiating = true;
        while (negotiating) {

            // only negotiate if theres data available
            if (input.available() < 1) {
                negotiating = false;
                continue;
            }

            // only negotiate if we receive IAC
            input.mark(1);
            int nextByte = input.read();
            if (nextByte != IAC) {
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

        log.debug("Telnet negotiation complete.");
    }

    private String getClientAddress() {
        return socket.getInetAddress().getHostAddress();
    }
}
