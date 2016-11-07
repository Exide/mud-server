package org.arabellan.mud.events;

import lombok.Value;
import org.arabellan.mud.network.Connection;

@Value
public class ConnectionClosed {
    Connection connection;
}
