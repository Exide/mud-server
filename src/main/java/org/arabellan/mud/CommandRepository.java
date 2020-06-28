package org.arabellan.mud;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.arabellan.utils.PropertiesUtils.loadPropertiesFiles;

class CommandRepository {

    private static final String RELATIVE_PATH = "/commands";

    private Map<Pattern, Command> commands;

    CommandRepository() {
        commands = loadFromPath(RELATIVE_PATH);
    }

    Command get(String input) {
        for (Pattern pattern : commands.keySet()) {
            Matcher matcher = pattern.matcher(input);
            if (matcher.matches()) {
                return commands.get(pattern);
            }
        }

        return null;
    }

    private Map<Pattern, Command> loadFromPath(String path) {
        return loadPropertiesFiles(path).stream()
                .collect(Collectors.toMap(
                        properties -> Pattern.compile(properties.getProperty("pattern")),
                        properties -> new Command(properties)
                ));
    }
}
