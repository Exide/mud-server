package org.arabellan.mud.server;

import org.arabellan.mud.server.network.Listener;

public class Main {

    public static void main(String[] args) {
        new Main().run();
    }

    private void run() {
        Listener listener = new Listener();
        listener.start();
    }
}
