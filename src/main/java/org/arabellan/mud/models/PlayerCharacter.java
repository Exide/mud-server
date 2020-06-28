package org.arabellan.mud.models;

public class PlayerCharacter {

    private static final String STATELINE_FORMAT = "[HP=%d/MA=%d]:";

    private int id;
    private int hitpoints;
    private int mana;

    public PlayerCharacter(int id) {
        this.id = id;
    }

    public String getCurrentStatLine() {
        return String.format(STATELINE_FORMAT, hitpoints, mana);
    }
}
