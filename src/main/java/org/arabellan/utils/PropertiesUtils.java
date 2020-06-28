package org.arabellan.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

@Slf4j
public class PropertiesUtils {

    public static Properties loadPropertiesFile(InputStream inputStream) {
        try {
            Properties properties = new Properties();
            properties.load(inputStream);
            return properties;
        } catch (IOException e) {
            throw new RuntimeException("cannot load properties file", e);
        }
    }

    public static Properties loadPropertiesFile(Path path) {
        try {
            InputStream inputStream = new FileInputStream(path.toString());
            return loadPropertiesFile(inputStream);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("properties file not found: " + path, e);
        }
    }

    public static Properties loadPropertiesFile(String filename) {
        InputStream inputStream = PropertiesUtils.class.getResourceAsStream(filename);
        return loadPropertiesFile(inputStream);
    }

    public static List<Properties> loadPropertiesFiles(String path) {
        try {
            URI uri = PropertiesUtils.class.getResource(path).toURI();
            return Files.list(Paths.get(uri))
                    .map(PropertiesUtils::loadPropertiesFile)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("cannot load properties files at: " + path, e);
        }
    }
}
