package com.fabian.xcommands;

import com.fabian.xcommands.managers.DependencyManager;
import com.fabian.xcommands.commands.XCCommand;
import com.fabian.xcommands.managers.*;
import com.fabian.xcommands.utils.DebugLogger;
import com.fabian.xcommands.utils.EconomyUtils;
import com.fabian.xcommands.utils.UpdateChecker;
import com.fabian.xcommands.hooks.XCommandsExpansion;
import com.fabian.xcommands.listeners.UpdateListener;
import com.fabian.xcommands.listeners.InventoryListener;
import com.fabian.xcommands.listeners.CommandHideListener;
import com.fabian.xcommands.listeners.CommandInterceptorListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;

/**
 * Main plugin class for X-Commands
 */
public class XCommands extends JavaPlugin {

    private static final int BSTATS_ID = 30996;
    private static final int UPDATE_CHECKER_ID = 132155;
    private static XCommands instance;

    private ConfigManager configManager;
    private LanguageManager languageManager;
    private ConditionManager conditionManager;
    private ActionManager actionManager;
    private CooldownManager cooldownManager;
    private CommandManager commandManager;
    private GUIManager guiManager;
    private CommandInterceptorListener commandInterceptorListener;
    private UpdateChecker updateChecker;
    private com.fabian.xcommands.metrics.Metrics metrics;

    public void logInfo(String message) {
        getLogger().info(message);
    }

    public void logWarning(String message) {
        getLogger().warning(message);
    }

    public void logError(String message) {
        getLogger().severe(message);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onEnable() {
        instance = this;

        try {
            // Initialize config managers first
            this.configManager = new ConfigManager(this);
            DebugLogger.debug("Config", "ConfigManager initialized");
            this.languageManager = new LanguageManager(this);
            DebugLogger.debug("Config", "LanguageManager initialized");
        } catch (Exception e) {
            DebugLogger.debug("Config", "Failed to initialize config managers", e);
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Load libraries before anything else
        DebugLogger.debug("Dependency", "Initializing DependencyManager...");
        new DependencyManager(this).loadDependencies();

        // Initialize remaining managers
        try {
            DebugLogger.init(this);

            // Support for old plugin name
            if (getDescription().getName().equalsIgnoreCase("X-Comands")) {
                logWarning("Deprecated name detected, use X-Commands");
            }

            // Perform Data Folder Migration (X-Comands -> X-Commands)
            performGlobalMigration();

            // Perform Subfolder Migration (comands -> commands)
            performInternalMigration();

            DebugLogger.debug("Init", "Initializing remaining managers...");
            this.cooldownManager = new CooldownManager(this);
            this.conditionManager = new ConditionManager(this);
            this.actionManager = new ActionManager(this);
            this.commandManager = new CommandManager(this);
            this.guiManager = new GUIManager(this);

            // Register BungeeCord channel
            getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

            // Setup Economy
            if (getServer().getPluginManager().getPlugin("Vault") != null) {
                if (EconomyUtils.setupEconomy()) {
                    DebugLogger.debug("Vault", "Vault integration enabled using " + EconomyUtils.getEconomy().getName());
                } else {
                    logWarning("Vault found, but economy setup failed. Economy actions will be disabled.");
                }
            } else {
                DebugLogger.debug("Vault", "Vault not found, economy actions will be disabled");
            }

            // Register main command
            var xcCommand = new XCCommand(this);
            try {
                var command = getCommand("xc");
                if (command != null) {
                    command.setExecutor(xcCommand);
                    command.setTabCompleter(xcCommand);
                    getServer().getHelpMap().addTopic(new org.bukkit.help.GenericCommandHelpTopic(command));
                } else {
                    logWarning("Failed to register /xc command! It might not be defined in plugin.yml.");
                }
            } catch (UnsupportedOperationException e) {
                logWarning("Paper detected: Legacy command registration skipped.");
            }
            DebugLogger.debug("Command", "Commands registered");

            // Register listeners
            getServer().getPluginManager().registerEvents(new UpdateListener(this), this);
            getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
            getServer().getPluginManager().registerEvents(new CommandHideListener(), this);
            commandInterceptorListener = new CommandInterceptorListener(this);
            getServer().getPluginManager().registerEvents(commandInterceptorListener, this);

            // Load custom commands
            commandManager.loadCommands();
            commandInterceptorListener.rebuildAliasLookup();
            DebugLogger.debug("Init", "Custom commands loaded and listeners registered");

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

            // PlaceholderAPI Integration
            if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
                DebugLogger.debug("PAPI", "PlaceholderAPI found, registering expansion");
                new XCommandsExpansion(this).register();
            } else {
                DebugLogger.debug("PAPI", "PlaceholderAPI not found, skipping expansion");
            }

        } catch (Exception e) {
            DebugLogger.debug("Init", "Failed to initialize managers", e);
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Check for updates
        if (configManager.isCheckUpdates()) {
            DebugLogger.debug("Update", "Update checker enabled");
            updateChecker = new UpdateChecker(this, UPDATE_CHECKER_ID);
        }

        // Initialize bStats Metrics
        setupMetrics();

        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&8[&bX-Commands&8] &7----------------------------------------------"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&8[&bX-Commands&8]   &aEnabled v" + getDescription().getVersion() + "! Commands are now custom."));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&8[&bX-Commands&8]   &fLanguage: &e" + getConfig().getString("language", "en").toUpperCase()));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&8[&bX-Commands&8] &7----------------------------------------------"));
    }

    @Override
    public void onDisable() {
        DebugLogger.debug("Init", "Plugin disabling...");
        if (metrics != null) {
            metrics.shutdown();
        }
        if (instance != null) {
            com.fabian.xcommands.utils.EconomyUtils.teardown();
        }

        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&8[&bX-Commands&8] &7----------------------------------------------"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&8[&bX-Commands&8]   &cDisabled v" + getDescription().getVersion() + "! Out."));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&8[&bX-Commands&8] &7----------------------------------------------"));
    }

    private void setupMetrics() {
        if (getConfig().getBoolean("metrics", true)) {
            try {
                metrics = new com.fabian.xcommands.metrics.Metrics(this, BSTATS_ID);
            } catch (Exception e) {
                logWarning("Could not start bStats Metrics: " + e.getMessage());
            }
        }
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
     * Gets the GUI manager
     */
    public GUIManager getGUIManager() {
        return guiManager;
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
