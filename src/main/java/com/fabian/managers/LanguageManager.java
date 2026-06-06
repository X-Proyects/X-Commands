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

        File oldFolder = new File(plugin.getDataFolder(), "languages");
        File messagesFolder = new File(plugin.getDataFolder(), "messages");
        
        // Migration: languages -> messages
        if (oldFolder.exists() && !messagesFolder.exists()) {
            plugin.logInfo("Migrating 'languages' folder to 'messages'...");
            if (oldFolder.renameTo(messagesFolder)) {
                plugin.logInfo("Migration successful.");
            } else {
                plugin.logSevere("Failed to migrate 'languages' folder to 'messages'. Please rename it manually.");
            }
        }

        File languageFile = new File(messagesFolder, fileName);

        // Case-insensitive fallback for filenames (useful for Linux)
        if (!languageFile.exists()) {
            File[] files = messagesFolder.listFiles();
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

        // Create messages folder if it doesn't exist
        if (!messagesFolder.exists() && messagesFolder.mkdirs()) {
            // Optional: log success or do nothing
        }

        // Save default language files only if they don't exist
        // Optimized: Check known defaults once
        saveDefaultLanguageFiles();

        // Update physical files on disk first (adds missing keys)
        updateLanguageFile(fileName);
        if (!langBase.equalsIgnoreCase("en") && !langBase.equalsIgnoreCase("custom")) {
            updateLanguageFile("en.yml");
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
            if (!langBase.equalsIgnoreCase("en") && !langBase.equalsIgnoreCase("custom")) {
                loadFallbacks("en.yml");
                loadInternalFallbacks("en");
            }
        } else {
            plugin.logWarning("Target language file not found: " + fileName);
            plugin.logInfo("Attempting to fallback to en.yml...");
            languageFile = new File(messagesFolder, "en.yml");
            if (languageFile.exists()) {
                languageConfig = YamlConfiguration.loadConfiguration(languageFile);
                loadInternalFallbacks("en");
                plugin.logInfo("Successfully fell back to en.yml");
            } else {
                plugin.logSevere("CRITICAL: en.yml not found! Creating an empty configuration.");
                loadInternalFallbacks("en");
            }
        }
    }

    private void saveDefaultLanguageFiles() {
        String[] defaults = { "en.yml", "es.yml", "ja.yml", "pt.yml", "ru.yml", "custom.yml" };
        File messagesFolder = new File(plugin.getDataFolder(), "messages");

        for (String def : defaults) {
            File file = new File(messagesFolder, def);
            if (!file.exists()) {
                saveResource("messages/" + def, file);
            }
        }
    }

    /**
     * Saves a resource from the JAR to the disk
     */
    private void saveResource(String resourcePath, File destination) {
        try {
            if (plugin.getResource(resourcePath) != null) {
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
        File fallbackFile = new File(new File(plugin.getDataFolder(), "messages"), fallbackFileName);
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
        File messagesFolder = new File(plugin.getDataFolder(), "messages");
        File diskFile = new File(messagesFolder, fileName);
        if (!diskFile.exists())
            return;

        ConfigUpdater.update(plugin, "messages/" + fileName, diskFile);
    }

    /**
     * Reloads the language file
     */
    public void reload() {
        loadLanguage();
    }

    public String getCurrentLanguage() {
        return plugin.getConfigManager().getLanguage();
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
            plugin.logWarning("Missing language key: " + key);
            String missing = ColorUtils.translate("&cMissing message: " + key);
            messageCache.put(key, missing);
            return missing;
        }

        String translatedMessage = ColorUtils.translate(message);
        messageCache.put(key, translatedMessage);
        return translatedMessage;
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
        return getPrefix() + " " + getMessage(key);
    }

    /**
     * Gets a message with prefix and arguments
     * 
     * @param key  The message key
     * @param args Arguments to format into the message
     * @return The formatted message with prefix and colors translated
     */
    public String getMessageWithPrefix(String key, Object... args) {
        return getPrefix() + " " + getMessage(key, args);
    }

    /**
     * Loads internal language strings from the JAR resources
     */
    private void loadInternalFallbacks(String langCode) {
        try (InputStream resourceStream = plugin.getResource("messages/" + langCode + ".yml")) {
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
    /**
     * Gets the universal prefix translated with colors
     * 
     * @return The translated prefix
     */
    public String getPrefix() {
        return ColorUtils.translate(plugin.getConfigManager().getPrefix());
    }

    /**
     * Returns a list of available languages on disk
     */
    public java.util.List<String> getAvailableLanguages() {
        java.util.List<String> languages = new java.util.ArrayList<>();
        File messagesFolder = new File(plugin.getDataFolder(), "messages");
        File[] files = messagesFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        
        if (files != null) {
            for (File file : files) {
                languages.add(file.getName().substring(0, file.getName().length() - 4));
            }
        }
        return languages;
    }
    
    public void setLanguage(String lang) {
        plugin.getConfigManager().setLanguage(lang);
    }

    /**
     * Force-updates a specific language file from JAR defaults, then reloads
     * into memory if it is the currently active language.
     *
     * @param langCode language code (e.g. "en", "es")
     * @return true if the active language was reloaded
     */
    public boolean forceReloadMessages(String langCode) {
        String fileName = langCode.endsWith(".yml") ? langCode : langCode + ".yml";
        File messagesFolder = new File(plugin.getDataFolder(), "messages");
        File diskFile = new File(messagesFolder, fileName);

        if (!diskFile.exists()) {
            saveResource("messages/" + fileName, diskFile);
        }

        ConfigUpdater.update(plugin, "messages/" + fileName, diskFile);

        String currentLang = plugin.getConfigManager().getLanguage();
        String currentBase = currentLang.endsWith(".yml") ? currentLang.substring(0, currentLang.length() - 4) : currentLang;
        if (currentBase.equalsIgnoreCase(langCode)) {
            reload();
            return true;
        }

        return false;
    }

    /**
     * Force-updates ALL language files found on disk, then reloads the
     * active language into memory.
     *
     * @return the number of language files that were processed
     */
    public int forceReloadAllMessages() {
        java.util.List<String> available = getAvailableLanguages();
        for (String lang : available) {
            String fileName = lang.endsWith(".yml") ? lang : lang + ".yml";
            File messagesFolder = new File(plugin.getDataFolder(), "messages");
            File diskFile = new File(messagesFolder, fileName);

            if (diskFile.exists()) {
                ConfigUpdater.update(plugin, "messages/" + fileName, diskFile);
            } else {
                saveResource("messages/" + fileName, diskFile);
            }
        }

        reload();
        return available.size();
    }
}
