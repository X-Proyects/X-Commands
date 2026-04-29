package com.fabian;

import com.fabian.managers.commands.XCCommand;
import com.fabian.managers.*;
import com.fabian.utils.EconomyUtils;
import com.fabian.utils.UpdateChecker;
import com.fabian.listeners.UpdateListener;
import com.fabian.listeners.InventoryListener;
import com.fabian.listeners.CommandHideListener;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;

/**
 * Main plugin class for X-Commands
 */
public class XCommands extends JavaPlugin {

    public static final String INTERNAL_PREFIX = "&8[&bX-Commands&8]";
    private static XCommands instance;

    private ConfigManager configManager;
    private LanguageManager languageManager;
    private ConditionManager conditionManager;
    private ActionManager actionManager;
    private CooldownManager cooldownManager;
    private CommandManager commandManager;
    private InventoryManager inventoryManager;
    private UpdateChecker updateChecker;

    /**
     * Sends an info message to console with custom prefix
     */
    public void logInfo(String message) {
        com.fabian.utils.LoggerUtils.info(message);
    }

    public void logWarning(String message) {
        com.fabian.utils.LoggerUtils.warn(message);
    }

    public void logSevere(String message) {
        com.fabian.utils.LoggerUtils.error(message);
    }

    public void logSevere(String message, Throwable throwable) {
        com.fabian.utils.LoggerUtils.severe(message, throwable);
    }

    @Override
    public void onEnable() {
        try {
            instance = this;

            // Soporte temporal para nombre antiguo
            if (getPluginMeta().getName().equalsIgnoreCase("X-Comands")) {
                logWarning("Deprecated name detected, use X-Commands");
            }

            // Initialize managers
            logInfo("Initializing...");

            // 1. Perform Data Folder Migration (X-Comands -> X-Commands)
            performGlobalMigration();

            // 2. Initialize Core Managers (Required for everything else)
            this.configManager = new ConfigManager(this);
            this.languageManager = new LanguageManager(this);

            // 3. Perform Subfolder Migration (comands -> commands)
            performInternalMigration();

            // 4. Initialize rest of managers
            this.cooldownManager = new CooldownManager(this);
            this.conditionManager = new ConditionManager(this);
            this.actionManager = new ActionManager(this);
            this.commandManager = new CommandManager(this);
            this.inventoryManager = new InventoryManager(this);

            // Register BungeeCord channel
            getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

            // 5. Setup Economy
            if (getServer().getPluginManager().getPlugin("Vault") != null) {
                if (EconomyUtils.setupEconomy()) {
                    logInfo("Vault integration enabled using " + EconomyUtils.getEconomy().getName() + ".");
                } else {
                    logWarning("Vault found, but economy setup failed. Economy actions will be disabled.");
                }
            } else {
                logWarning("Vault not found. Economy actions will be disabled.");
            }

            // Register main command
            var xcCommand = new XCCommand(this);
            var command = getCommand("xc");
            if (command != null) {
                command.setExecutor(xcCommand);
                command.setTabCompleter(xcCommand);
            }

            // Register listeners
            getServer().getPluginManager().registerEvents(new UpdateListener(this), this);
            getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
            getServer().getPluginManager().registerEvents(new CommandHideListener(), this);

            // Load custom commands
            commandManager.loadCommands();

            // Export all available guides
            File guidesFolder = new File(getDataFolder(), "guides");
            if (!guidesFolder.exists() && !guidesFolder.mkdirs()) {
                logWarning("Could not create guides folder!");
            }

            String[] guideLangs = { "EN", "ES" };
            for (String lang : guideLangs) {
                File guideFile = new File(guidesFolder, "guides_" + lang + ".yml");
                if (!guideFile.exists()) {
                    saveResource("guides/guides_" + lang + ".yml", false);
                }
            }

            // Check for updates if enabled
            if (configManager.isCheckUpdates()) {
                updateChecker = new UpdateChecker(this, 132155);
            }

            // Initialize bStats Metrics if enabled
            if (getConfig().getBoolean("metrics", true)) {
                int pluginId = 30996;
                new com.fabian.metrics.Metrics(this, pluginId);
            }

            logInfo("v" + getPluginMeta().getVersion() + " successfully started!");
        } catch (Exception e) {
            com.fabian.utils.LoggerUtils
                    .severe("Failed to enable X-Commands! Please check your configuration and server version.", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        logInfo("successfully disabled!");
    }

    /**
     * Gets the plugin instance
     */
    /**
     * Performs migration of the data folder from the old name to the new one.
     */
    private void performGlobalMigration() {
        File oldFolder = new File(getDataFolder().getParentFile(), "X-Comands");
        File newFolder = getDataFolder();

        if (oldFolder.exists() && !newFolder.exists()) {
            logInfo("Old data folder detected. Migrating X-Comands -> X-Commands...");
            if (oldFolder.renameTo(newFolder)) {
                logInfo("Successfully migrated data folder.");
            } else {
                logWarning("Failed to migrate data folder. Please rename it manually.");
            }
        }
    }

    /**
     * Performs migration of the internal commands folder from the old name to the
     * new one.
     */
    private void performInternalMigration() {
        File legacyCommands = new File(getDataFolder(), "comands");
        File modernCommands = new File(getDataFolder(), "commands");

        if (legacyCommands.exists()) {
            if (!modernCommands.exists()) {
                logInfo("Legacy folder 'comands' detected. Renaming to 'commands'...");
                if (legacyCommands.renameTo(modernCommands)) {
                    logInfo("Successfully renamed folder.");
                } else {
                    logWarning("Could not rename 'comands' folder automatically.");
                }
            } else {
                logInfo("Both 'comands' and 'commands' found. Proceeding with both (compatibility).");
            }
        }
    }

    public static XCommands getInstance() {
        return instance;
    }

    /**
     * Gets the configuration manager
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * Gets the language manager
     */
    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    /**
     * Gets the action manager
     */
    public ConditionManager getConditionManager() {
        return conditionManager;
    }

    public ActionManager getActionManager() {
        return actionManager;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    /**
     * Gets the command manager
     */
    public CommandManager getCommandManager() {
        return commandManager;
    }

    /**
     * Gets the inventory manager
     */
    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }

    /**
     * Gets the update checker instance
     */
    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }
}
