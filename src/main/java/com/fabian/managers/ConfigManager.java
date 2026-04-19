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
    private boolean cachedCheckUpdates;
    private boolean cachedHideMinecraftCommands;

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
        this.cachedLanguage = config.getString("language", "en").trim().toUpperCase();
        this.cachedCheckUpdates = config.getBoolean("check-updates", true);
        this.cachedHideMinecraftCommands = config.getBoolean("hide-minecraft-commands", false);

        validateConfig();

        // Check for updates
        checkUpdate();

        plugin.logInfo("Configuration loaded (" + cachedLanguage + ")!");
    }

    private void validateConfig() {
        boolean changed = false;

        if (!config.contains("language")) {
            config.set("language", "en");
            changed = true;
        }

        if (!config.contains("check-updates")) {
            config.set("check-updates", true);
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

        YamlConfiguration resConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(resourceStream, StandardCharsets.UTF_8));

        int currentCode = resConfig.getInt("code", 1);
        int diskCode = config.getInt("code", 0);

        if (diskCode < currentCode) {
            plugin.logInfo("Updating configuration files...");

            // Use ConfigUpdater to add missing keys without wiping comments
            ConfigUpdater.update(plugin, "config.yml", new File(plugin.getDataFolder(), "config.yml"));

            // Update code in config
            config.set("code", currentCode);
            plugin.saveConfig();
        }
    }

    /**
     * Reloads the configuration from disk
     */
    public void reload() {
        loadConfig();
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
     * Gets the configuration object
     * 
     * @return The FileConfiguration object
     */
    public FileConfiguration getConfig() {
        return config;
    }
}
