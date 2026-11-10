package com.example.student;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class DatabaseConfig {
    private static final Properties DOTENV = loadDotenv();

    private DatabaseConfig() {
    }

    public static String get(String key, String defaultValue) {
        String environmentValue = System.getenv(key);
        if (environmentValue != null && !environmentValue.isEmpty()) {
            return environmentValue;
        }
        return DOTENV.getProperty(key, defaultValue);
    }

    private static Properties loadDotenv() {
        Properties properties = new Properties();
        Path dotenvPath = Path.of(".env");
        if (!Files.exists(dotenvPath)) {
            return properties;
        }

        try (Reader reader = Files.newBufferedReader(dotenvPath)) {
            properties.load(reader);
            return properties;
        } catch (IOException e) {
            throw new IllegalStateException("Could not read database configuration from .env", e);
        }
    }
}
