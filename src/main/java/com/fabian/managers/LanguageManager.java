package com.fabian.managers;

import com.fabian.XCommands;
import com.fabian.utils.ColorUtils;
import com.fabian.utils.ConfigUpdater;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.io.File;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages language files and message retrieval
 */
public class LanguageManager {

    private final XCommands plugin;
    private FileConfiguration languageConfig;
    private final Map<String, String> messageCache = new HashMap<>();

    public LanguageManager(XCommands plugin) {
        this.plugin = plugin;
        loadLanguage();
    }

    /**
     * Loads the language file based on config settings
     */
    public void loadLanguage() {
        messageCache.clear();
        String lang = plugin.getConfigManager().getLanguage();

        String fileName = lang.endsWith(".yml") ? lang : lang + ".yml";
        String langBase = lang.endsWith(".yml") ? lang.substring(0, lang.length() - 4) : lang;

        File languageFolder = new File(plugin.getDataFolder(), "languages");
        File languageFile = new File(languageFolder, fileName);

        // Case-insensitive fallback for filenames (useful for Linux)
        if (!languageFile.exists()) {
            File[] files = languageFolder.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.getName().equalsIgnoreCase(fileName)) {
                        languageFile = f;
                        fileName = f.getName();
                        break;
                    }
                }
            }
        }

        // Create languages folder if it doesn't exist
        if (!languageFolder.exists() && languageFolder.mkdirs()) {
            // Optional: log success or do nothing, but result is consumed
        }

        // Save default language files only if they don't exist
        // Optimized: Check known defaults once
        saveDefaultLanguageFiles();

        // Update physical files on disk first (adds public keys only)
        updateLanguageFile(fileName);
        if (!langBase.equalsIgnoreCase("EN") && !langBase.equalsIgnoreCase("CUSTOM")) {
            updateLanguageFile("EN.yml");
        }

        // Load the selected language file
        languageConfig = new YamlConfiguration();
        if (languageFile.exists()) {
            try (InputStreamReader reader = new InputStreamReader(
                    java.nio.file.Files.newInputStream(languageFile.toPath()), StandardCharsets.UTF_8)) {
                languageConfig.load(reader);
            } catch (Exception e) {
                plugin.logSevere("Could not read language file " + fileName + ": " + e.getMessage());
            }

            // Load internal fallbacks into memory ONLY
            loadInternalFallbacks(langBase);

            // Fallback for missing keys in memory
            if (!langBase.equalsIgnoreCase("EN") && !langBase.equalsIgnoreCase("CUSTOM")) {
                loadFallbacks("EN.yml");
                loadInternalFallbacks("EN");
            }
        } else {
            plugin.logWarning("Target language file not found: " + fileName);
            plugin.logInfo("Attempting to fallback to EN.yml...");
            languageFile = new File(languageFolder, "EN.yml");
            if (languageFile.exists()) {
                languageConfig = YamlConfiguration.loadConfiguration(languageFile);
                loadInternalFallbacks("EN");
                plugin.logInfo("Successfully fell back to EN.yml");
            } else {
                plugin.logSevere("CRITICAL: EN.yml not found! Creating an empty configuration.");
                loadInternalFallbacks("EN");
            }
        }
    }

    private void saveDefaultLanguageFiles() {
        String[] defaults = { "EN.yml", "ES.yml", "JA.yml", "PT.yml", "RU.yml", "CUSTOM.yml" };
        File languageFolder = new File(plugin.getDataFolder(), "languages");

        for (String def : defaults) {
            File file = new File(languageFolder, def);
            if (!file.exists()) {
                saveResource("languages/" + def, file);
            }
        }
    }

    /**
     * Saves a resource from the JAR to the disk
     */
    private void saveResource(String resourcePath, File destination) {
        try {
            InputStream inputStream = plugin.getResource(resourcePath);
            if (inputStream != null) {
                plugin.saveResource(resourcePath, false);
            }
        } catch (Exception e) {
            plugin.logWarning("Could not save default language file: " + resourcePath);
        }
    }

    /**
     * Loads fallback messages from a specified file (usually EN.yml)
     */
    private void loadFallbacks(String fallbackFileName) {
        File fallbackFile = new File(new File(plugin.getDataFolder(), "languages"), fallbackFileName);
        if (!fallbackFile.exists())
            return;

        try (InputStreamReader reader = new InputStreamReader(
                java.nio.file.Files.newInputStream(fallbackFile.toPath()), StandardCharsets.UTF_8)) {
            FileConfiguration fallbackConfig = new YamlConfiguration();
            fallbackConfig.load(reader);

            for (String key : fallbackConfig.getKeys(true)) {
                if (!languageConfig.contains(key)) {
                    languageConfig.set(key, fallbackConfig.get(key));
                }
            }
        } catch (Exception e) {
            plugin.logWarning("Could not load fallbacks from " + fallbackFileName);
        }
    }

    /**
     * Updates an existing language file with missing keys from the internal
     * resource
     */
    private void updateLanguageFile(String fileName) {
        File languageFolder = new File(plugin.getDataFolder(), "languages");
        File diskFile = new File(languageFolder, fileName);
        if (!diskFile.exists())
            return;

        ConfigUpdater.update(plugin, "languages/" + fileName, diskFile);
    }

    /**
     * Reloads the language file
     */
    public void reload() {
        loadLanguage();
    }

    /**
     * Gets a message from the language file
     * 
     * @param key The message key
     * @return The message with colors translated
     */
    public String getMessage(String key) {
        String cached = messageCache.get(key);
        if (cached != null) {
            return cached;
        }

        String message = languageConfig.getString(key);

        if (message == null) {
            // Use internal fallback strictly if not found in any config
            // Log warning only once per key per reload to avoid spam? No, spam is good for
            // visibility.
            plugin.logWarning("Missing language key: " + key);
            String missing = ColorUtils.translate("&cMissing message: " + key);
            messageCache.put(key, missing);
            return missing;
        }

        String translated = ColorUtils.translate(message);
        messageCache.put(key, translated);
        return translated;
    }

    /**
     * Gets a message with arguments
     * 
     * @param key  The message key
     * @param args Arguments to format into the message
     * @return The formatted message with colors translated
     */
    public String getMessage(String key, Object... args) {
        String message = getMessage(key);

        if (args.length == 0)
            return message;

        // Escape single quotes for MessageFormat to prevent placeholder breakage
        // Optimization: Only check if quote exists
        if (message.indexOf('\'') != -1 && !message.contains("''")) {
            message = message.replace("'", "''");
        }
        return MessageFormat.format(message, args);
    }

    /**
     * Gets a message with prefix
     * 
     * @param key The message key
     * @return The message with prefix and colors translated
     */
    public String getMessageWithPrefix(String key) {
        return getMessage("prefix") + " " + getMessage(key);
    }

    /**
     * Gets a message with prefix and arguments
     * 
     * @param key  The message key
     * @param args Arguments to format into the message
     * @return The formatted message with prefix and colors translated
     */
    public String getMessageWithPrefix(String key, Object... args) {
        return getMessage("prefix") + " " + getMessage(key, args);
    }

    /**
     * Loads internal language strings from the JAR resources
     */
    private void loadInternalFallbacks(String langCode) {
        try (InputStream resourceStream = plugin.getResource("languages/internal/" + langCode + ".yml")) {
            if (resourceStream == null)
                return;

            YamlConfiguration internalConfig = YamlConfiguration
                    .loadConfiguration(new InputStreamReader(resourceStream, StandardCharsets.UTF_8));

            for (String key : internalConfig.getKeys(true)) {
                if (!languageConfig.contains(key)) {
                    languageConfig.set(key, internalConfig.get(key));
                }
            }
        } catch (Exception e) {
            // Silently fail if resource doesn't exist (e.g. for CUSTOM)
        }
    }
}
