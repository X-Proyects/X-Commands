package com.fabian.managers;

import com.fabian.XCommands;
import com.fabian.utils.ConfigUpdater;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Manages the main configuration file
 */
public class ConfigManager {

    private final XCommands plugin;
    private FileConfiguration config;

    private String cachedLanguage;
    private String cachedPrefix;
    private boolean cachedCheckUpdates;
    private boolean cachedHideMinecraftCommands;
    private boolean cachedHidePluginCommands;

    public ConfigManager(XCommands plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    /**
     * Loads or reloads the configuration file
     */
    public void loadConfig() {
        // Save default config if it doesn't exist
        plugin.saveDefaultConfig();

        // Reload config from disk
        plugin.reloadConfig();
        config = plugin.getConfig();

        // Cache values
        this.cachedLanguage = config.getString("language", "en").trim().toLowerCase();
        this.cachedPrefix = config.getString("prefix", "&8[&bX-Commands&8]&r");
        this.cachedCheckUpdates = config.getBoolean("check-updates", true);
        this.cachedHideMinecraftCommands = config.getBoolean("hide-namespaced-commands.hide-minecraft", false);
        this.cachedHidePluginCommands = config.getBoolean("hide-namespaced-commands.hide-plugins", false);

        validateConfig();

        // Also check if config needs auto-migration from an older version
        checkUpdate();

        plugin.logInfo("Configuration loaded (" + cachedLanguage + ")!");
    }

    private void validateConfig() {
        boolean changed = false;

        if (!config.contains("language")) {
            config.set("language", "en");
            changed = true;
        }

        if (!config.contains("prefix")) {
            config.set("prefix", "&8[&bX-Commands&8]&r");
            changed = true;
        }

        if (!config.contains("check-updates")) {
            config.set("check-updates", true);
            changed = true;
        }

        if (!config.contains("hide-namespaced-commands")) {
            config.set("hide-namespaced-commands.hide-minecraft", false);
            config.set("hide-namespaced-commands.hide-plugins", false);
            changed = true;
        }

        if (changed) {
            plugin.saveConfig();
        }
    }

    private void checkUpdate() {
        // Load resource config for comparison
        InputStream resourceStream = plugin.getResource("config.yml");
        if (resourceStream == null)
            return;

        try {
            YamlConfiguration resConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(resourceStream, StandardCharsets.UTF_8));

            int currentCode = resConfig.getInt("code", 1);
            int diskCode = config.getInt("code", 0);

            if (diskCode < currentCode) {
                plugin.logInfo("Updating configuration files...");

                // Use ConfigUpdater to add missing keys without wiping comments
                ConfigUpdater.update(plugin, "config.yml", new File(plugin.getDataFolder(), "config.yml"));
                
                // Also update all language files
                String[] langFiles = { "en.yml", "es.yml", "ja.yml", "pt.yml", "ru.yml", "custom.yml" };
                File messagesFolder = new File(plugin.getDataFolder(), "messages");
                for (String langFile : langFiles) {
                    File diskLangFile = new File(messagesFolder, langFile);
                    if (diskLangFile.exists()) {
                        ConfigUpdater.update(plugin, "messages/" + langFile, diskLangFile);
                    }
                }

                // Update code in config
                config.set("code", currentCode);
                plugin.saveConfig();
            }
        } finally {
            try {
                resourceStream.close();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Reloads the configuration from disk
     */
    public void reload() {
        loadConfig();
    }

    /**
     * Gets the configured plugin prefix
     *
     * @return The prefix string with color codes
     */
    public String getPrefix() {
        return cachedPrefix;
    }

    /**
     * Gets the configured language
     * 
     * @return The language code (EN, ES, or CUSTOM)
     */
    public String getLanguage() {
        return cachedLanguage;
    }

    /**
     * Sets and persists the language setting
     */
    public void setLanguage(String lang) {
        this.cachedLanguage = lang.toLowerCase();
        config.set("language", this.cachedLanguage);
        plugin.saveConfig();
        plugin.getLanguageManager().reload();
    }

    /**
     * Checks if update checking is enabled
     * 
     * @return true if update checking is enabled
     */
    public boolean isCheckUpdates() {
        return cachedCheckUpdates;
    }

    /**
     * Checks if hiding Minecraft namespaced commands is enabled
     * 
     * @return true if Minecraft commands should be hidden
     */
    public boolean isHideMinecraftCommands() {
        return cachedHideMinecraftCommands;
    }

    /**
     * Checks if hiding Plugin namespaced commands is enabled
     * 
     * @return true if Plugin commands should be hidden
     */
    public boolean isHidePluginCommands() {
        return cachedHidePluginCommands;
    }

    /**
     * Gets the configuration object
     * 
     * @return The FileConfiguration object
     */
    public FileConfiguration getConfig() {
        return config;
    }
}
