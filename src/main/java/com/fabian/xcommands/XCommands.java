package com.fabian.xcommands;

import com.fabian.xcommands.managers.DependencyManager;
import com.fabian.xcommands.managers.commands.XCCommand;
import com.fabian.xcommands.managers.*;
import com.fabian.xcommands.utils.ColorUtils;
import com.fabian.xcommands.utils.EconomyUtils;
import com.fabian.xcommands.utils.LoggerUtils;
import com.fabian.xcommands.utils.UpdateChecker;
import com.fabian.xcommands.utils.XCommandsExpansion;
import com.fabian.xcommands.listeners.UpdateListener;
import com.fabian.xcommands.listeners.InventoryListener;
import com.fabian.xcommands.listeners.CommandHideListener;
import com.fabian.xcommands.listeners.CommandInterceptorListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;

/**
 * Main plugin class for X-Commands
 */
public class XCommands extends JavaPlugin {

    public static final String INTERNAL_PREFIX = "&8[&bX-Commands&8]";
    private static final int BSTATS_ID = 30996;
    private static final int UPDATE_CHECKER_ID = 132155;
    private static XCommands instance;

    private ConfigManager configManager;
    private LanguageManager languageManager;
    private ConditionManager conditionManager;
    private ActionManager actionManager;
    private CooldownManager cooldownManager;
    private CommandManager commandManager;
    private InventoryManager inventoryManager;
    private CommandInterceptorListener commandInterceptorListener;
    private UpdateChecker updateChecker;
    private com.fabian.xcommands.metrics.Metrics metrics;

    /**
     * Sends an info message to console with custom prefix
     */
    public void logInfo(String message) {
        com.fabian.xcommands.utils.LoggerUtils.info(message);
    }

    public void logWarning(String message) {
        com.fabian.xcommands.utils.LoggerUtils.warn(message);
    }

    public void logSevere(String message) {
        com.fabian.xcommands.utils.LoggerUtils.error(message);
    }

    public void logSevere(String message, Throwable throwable) {
        com.fabian.xcommands.utils.LoggerUtils.severe(message, throwable);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onEnable() {
        try {
            instance = this;

            // Load libraries before anything else
            new DependencyManager(this).loadDependencies();

            // Soporte temporal para nombre antiguo
            if (getDescription().getName().equalsIgnoreCase("X-Comands")) {
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

            // Register PlaceholderAPI expansion
            if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
                try {
                    new XCommandsExpansion(this).register();
                    logInfo("PlaceholderAPI expansion registered!");
                    LoggerUtils.debug("PAPI: PlaceholderAPI expansion registered");
                } catch (Exception e) {
                    logWarning("Could not register PlaceholderAPI expansion: " + e.getMessage());
                }
            }

            // Register main command
            var xcCommand = new XCCommand(this);
            try {
                var command = getCommand("xc");
                if (command != null) {
                    command.setExecutor(xcCommand);
                    command.setTabCompleter(xcCommand);
                    // Register HelpTopic so /xc appears in /help and /?
                    getServer().getHelpMap().addTopic(new org.bukkit.help.GenericCommandHelpTopic(command));
                } else {
                    logWarning("Failed to register /xc command! It might not be defined in plugin.yml.");
                }
            } catch (UnsupportedOperationException e) {
                // This happens on modern Paper plugins that handle commands differently.
                // Since we rely on CommandInterceptorListener as a fallback for custom commands,
                // we only log this if it's truly unexpected. 
                // For now, we ignore it to allow the plugin to enable.
                logWarning("Paper detected: Legacy command registration skipped. /xc might not work if not registered via Paper API.");
            }

            // Register listeners
            getServer().getPluginManager().registerEvents(new UpdateListener(this), this);
            getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
            getServer().getPluginManager().registerEvents(new CommandHideListener(), this);
            commandInterceptorListener = new CommandInterceptorListener(this);
            getServer().getPluginManager().registerEvents(commandInterceptorListener, this);

            // Load custom commands
            commandManager.loadCommands();
            commandInterceptorListener.rebuildAliasLookup();

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
                updateChecker = new UpdateChecker(this, UPDATE_CHECKER_ID);
            }

            // Initialize bStats Metrics if enabled
            if (getConfig().getBoolean("metrics", true)) {
                metrics = new com.fabian.xcommands.metrics.Metrics(this, BSTATS_ID);
            }

            logInfo("v" + getDescription().getVersion() + " successfully started!");
        } catch (Exception e) {
            logSevere("Failed to enable X-Commands! Please check your configuration and server version.", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (metrics != null) {
            metrics.shutdown();
        }
        if (instance != null) {
            com.fabian.xcommands.utils.EconomyUtils.teardown();
        }

        String version = getDescription().getVersion();
        Bukkit.getConsoleSender().sendMessage(ColorUtils.translate(
                "&8[&bX-Commands&8] &8--------------------------------------------------"));
        Bukkit.getConsoleSender().sendMessage(ColorUtils.translate(
                "&8[&bX-Commands&8] &c  Disabled v" + version + "! Goodbye."));
        Bukkit.getConsoleSender().sendMessage(ColorUtils.translate(
                "&8[&bX-Commands&8] &8--------------------------------------------------"));
    }

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



    /**
     * Gets the plugin instance
     */
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
     * Gets the condition manager
     */
    public ConditionManager getConditionManager() {
        return conditionManager;
    }

    /**
     * Gets the action manager
     */
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
     * Gets the command interceptor listener
     */
    public CommandInterceptorListener getCommandInterceptorListener() {
        return commandInterceptorListener;
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

    public void setUpdateChecker(UpdateChecker checker) {
        this.updateChecker = checker;
    }
}
