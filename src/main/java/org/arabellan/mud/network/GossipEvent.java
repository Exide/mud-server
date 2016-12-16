package org.arabellan.mud.network;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

@Slf4j
class GossipEvent {

    public static final Pattern GOSSIP_PATTERN = Pattern.compile("goss?i?p? (.*)");
    private static final String GOSSIP_FORMAT = "%s gossips: %s";

    @Getter
    private String message;

    GossipEvent(int id, String message) {
        this.message = String.format(GOSSIP_FORMAT, id, message);
    }
}
