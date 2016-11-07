package org.arabellan.mud.server.events;

import lombok.Value;
import org.arabellan.mud.server.network.Connection;

@Value
public class ConnectionClosed {
    Connection connection;
}
