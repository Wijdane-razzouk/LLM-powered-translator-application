package org.translate.com.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class EnvConfig {

    private static final Map<String, String> FILE_ENV = new ConcurrentHashMap<>();
    private static final AtomicBoolean LOADED = new AtomicBoolean(false);

    private EnvConfig() {
    }

    public static void loadDotEnv() {
        loadDotEnv(".env", "translator_arab/.env");
    }

    public static void loadDotEnv(String... candidates) {
        if (!LOADED.compareAndSet(false, true)) {
            return;
        }
        for (String candidate : candidates) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            loadFile(Paths.get(candidate));
        }
    }

    public static String get(String key) {
        String value = System.getenv(key);
        if (value != null) {
            return value;
        }
        return FILE_ENV.get(key);
    }

    public static String getOrDefault(String key, String fallback) {
        String value = get(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private static void loadFile(Path path) {
        if (!Files.exists(path)) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                if (trimmed.startsWith("export ")) {
                    trimmed = trimmed.substring("export ".length()).trim();
                }
                int equalsIndex = trimmed.indexOf('=');
                if (equalsIndex <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, equalsIndex).trim();
                String rawValue = trimmed.substring(equalsIndex + 1).trim();
                String value = stripQuotes(rawValue);
                if (!key.isEmpty()) {
                    FILE_ENV.putIfAbsent(key, value);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load .env: " + e.getMessage());
        }
    }

    private static String stripQuotes(String value) {
        if (value == null || value.length() < 2) {
            return value;
        }
        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);
        if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
