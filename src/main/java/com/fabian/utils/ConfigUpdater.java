package com.fabian.utils;

import com.fabian.XCommands;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility to update configuration files while preserving comments
 */
public class ConfigUpdater {

    /**
     * Updates a local YAML file by adding missing keys from a resource file.
     * Preserves comments by appending new keys to the end of the file.
     */
    public static void update(XCommands plugin, String resourcePath, File diskFile) {
        if (!diskFile.exists())
            return;

        try {
            // Load resource config for comparison
            InputStream resourceStream = plugin.getResource(resourcePath);
            if (resourceStream == null)
                return;

            YamlConfiguration resConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(resourceStream, StandardCharsets.UTF_8));

            // Load disk config for comparison
            YamlConfiguration diskConfig = new YamlConfiguration();
            try (InputStreamReader reader = new InputStreamReader(new FileInputStream(diskFile),
                    StandardCharsets.UTF_8)) {
                diskConfig.load(reader);
            } catch (Exception e) {
                // If it fails, we keep an empty config or log it
            }

            // 1. Safe comparison: Find keys present in the internal JAR resource but
            // missing on disk.
            List<String> missingKeys = new ArrayList<>();
            for (String key : resConfig.getKeys(true)) {
                if (!diskConfig.contains(key)) {
                    missingKeys.add(key);
                }
            }

            if (missingKeys.isEmpty())
                return;

            // 2. Hierarchical Appending: Add missing keys to the end of the file while
            // maintaining correct YAML indentation to preserve a valid file structure.
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(diskFile, true), StandardCharsets.UTF_8))) {
                writer.newLine(); // Start with a newline to separate from existing content

                for (String key : missingKeys) {
                    // Only process leaf nodes (keys with actual values).
                    // YamlConfiguration.getKeys(true) includes parents, so we skip them to avoid
                    // duplication.
                    if (resConfig.getConfigurationSection(key) == null) {
                        // Determine the indentation level based on the key depth (dots in path)
                        int depth = key.split("\\.").length - 1;
                        String indent = "";
                        for (int i = 0; i < depth; i++) {
                            indent += "  ";
                        }

                        // Extract only the local key name (e.g., 'material' from 'item.material')
                        String nodeName = key.contains(".") ? key.substring(key.lastIndexOf(".") + 1) : key;
                        Object value = resConfig.get(key);
                        String formattedValue = formatValue(value);

                        // Write the key-value pair with proper indentation and UTF-8 encoding
                        writer.write(indent + nodeName + ": " + formattedValue);
                        writer.newLine();
                    }
                }
            }
        } catch (IOException e) {
            plugin.logWarning("Failed to update " + diskFile.getName() + ": " + e.getMessage());
        }
    }

    private static String formatValue(Object value) {
        if (value == null)
            return "''";
        if (value instanceof String) {
            String s = (String) value;
            if (s.contains("'") || s.contains("&") || s.contains("\"")) {
                return "\"" + s.replace("\"", "\\\"") + "\"";
            }
            return "\"" + s + "\"";
        }
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                sb.append(formatValue(list.get(i)));
                if (i < list.size() - 1)
                    sb.append(", ");
            }
            sb.append("]");
            return sb.toString();
        }
        return value.toString();
    }
}
