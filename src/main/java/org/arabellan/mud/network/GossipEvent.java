package org.arabellan.mud.network;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
class GossipEvent {

    private static final String GOSSIP_PATTERN = "goss?i?p? (.*)";
    private static final String GOSSIP_FORMAT = "{0} gossips: {1}";

    @Getter
    private String message;

    GossipEvent(int id, String message) {
        String strippedMessage = Pattern.matcher(GOSSIP_PATTERN, message);
        this.message = String.format(GOSSIP_FORMAT, id, message);
    }

    static boolean match(String message) {
        return Pattern.matches(GOSSIP_PATTERN, message);
    }
}
