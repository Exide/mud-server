package org.arabellan.mmud;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Spell {
    int id;
    String fullName;
    String shortName;
    String description;
    int level;
    int difficulty;
    MagicSchool magicSchool;
    int magicLevel;

    public String getMagery() {
        return String.format("%s-%d", magicSchool, magicLevel);
    }

    public String toString() {
        return String.format("<Spell:%s (%s)>", fullName, shortName);
    }
}
