package org.arabellan.mmud;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;

import static java.util.Arrays.copyOfRange;
import static org.arabellan.utils.ConversionUtils.convertByteArrayToShort;
import static org.arabellan.utils.ConversionUtils.convertByteToInt;

@Slf4j
public class SpellExporter {

    private static final long STARTING_OFFSET = 16392L;

    private static final int ID_START = 0;
    private static final int ID_END = ID_START + 2;
    private static final int FULL_NAME_START = 2;
    private static final int FULL_NAME_END = FULL_NAME_START + 29;
    private static final int SHORT_NAME_START = 250;
    private static final int SHORT_NAME_END = SHORT_NAME_START + 4;
    private static final int DESCRIPTION_ONE_START = 32;
    private static final int DESCRIPTION_ONE_END = DESCRIPTION_ONE_START + 50;
    private static final int DESCRIPTION_TWO_START = 83;
    private static final int DESCRIPTION_TWO_END = DESCRIPTION_TWO_START + 50;
    private static final int LEVEL_START = 190;
    private static final int LEVEL_END = LEVEL_START + 1;
    private static final int DIFFICULTY_START = 201;
    private static final int DIFFICULTY_END = DIFFICULTY_START + 1;
    private static final int MAGERY_LEVEL_START = 244;
    private static final int MAGERY_LEVEL_END = MAGERY_LEVEL_START + 1;

    public static void main(String[] args) {
        if (args.length != 1)
            throw new IllegalArgumentException("missing required path to spell DAT file");

        new SpellExporter().export(args[0]);
    }

    private void export(String filename) {
        try {
            log.info("parsing DAT file: " + filename);
            InputStream inputStream = new FileInputStream(filename);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);

            long bytesSkipped = bufferedInputStream.skip(STARTING_OFFSET);
            if (bytesSkipped != STARTING_OFFSET)
                throw new RuntimeException("starting offset out of bounds");

            while (bufferedInputStream.available() > 0) {
                byte[] buffer = new byte[262];
                int bytesRead = bufferedInputStream.read(buffer, 0, 262);
                Spell spell = parseBlock(buffer);
                log.debug(spell.toString());
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Spell parseBlock(byte[] buffer) {
        return Spell.builder()
                .id(convertByteArrayToShort(copyOfRange(buffer, ID_START, ID_END)))
                .fullName(buildFullName(buffer))
                .shortName(buildShortName(buffer))
                .description(buildDescription(buffer))
                .level(convertByteToInt(copyOfRange(buffer, LEVEL_START, LEVEL_END)))
                .mageryType("magic")
                .mageryLevel(convertByteToInt(copyOfRange(buffer, MAGERY_LEVEL_START, MAGERY_LEVEL_END)))
                .difficulty(convertByteToInt(copyOfRange(buffer, DIFFICULTY_START, DIFFICULTY_END)))
                .build();
    }

    private String buildFullName(byte[] buffer) {
        return new String(copyOfRange(buffer, FULL_NAME_START, FULL_NAME_END)).trim();
    }

    private String buildShortName(byte[] buffer) {
        return new String(copyOfRange(buffer, SHORT_NAME_START, SHORT_NAME_END)).trim();
    }

    private String buildDescription(byte[] buffer) {
        String descriptionOne = new String(copyOfRange(buffer, DESCRIPTION_ONE_START, DESCRIPTION_ONE_END)).trim();
        String descriptionTwo = new String(copyOfRange(buffer, DESCRIPTION_TWO_START, DESCRIPTION_TWO_END)).trim();
        return descriptionOne + " " + descriptionTwo;
    }
}
