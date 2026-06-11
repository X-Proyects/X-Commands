package com.fabian.xcommands.managers;

import com.fabian.xcommands.XCommands;
import com.fabian.xcommands.commands.CustomCommandExecutor;
import com.fabian.xcommands.utils.SchedulerUtils;
import com.fabian.xcommands.utils.DebugLogger;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.help.HelpTopic;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages custom command registration and loading
 */
public class CommandManager {

    private final XCommands plugin;
    private final Map<String, CustomCommandExecutor> customCommands;
    private CommandMap commandMap;
    // Cached reflection field
    private Field knownCommandsField;
    private final Map<String, Object> commandTimers = new ConcurrentHashMap<>(); // Object to store BukkitTask or ScheduledTask

    public CommandManager(XCommands plugin) {
        this.plugin = plugin;
        this.customCommands = new ConcurrentHashMap<>();
        initCommandMap();
    }

    public void clearPlayerCooldowns(java.util.UUID uuid) {
        for (CustomCommandExecutor executor : customCommands.values()) {
            executor.clearCooldown(uuid);
        }
    }

    /**
     * Initializes the Bukkit CommandMap using reflection
     */
    private void initCommandMap() {
        try {
            Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            commandMap = (CommandMap) field.get(Bukkit.getServer());

            // Cache knownCommands field if possible
            if (commandMap != null) {
                Class<?> clazz = commandMap.getClass();
                while (clazz != Object.class && knownCommandsField == null) {
                    try {
                        knownCommandsField = clazz.getDeclaredField("knownCommands");
                    } catch (NoSuchFieldException e) {
                        clazz = clazz.getSuperclass();
                    }
                }
                if (knownCommandsField != null) {
                    knownCommandsField.setAccessible(true);
                }
            }
        } catch (Exception e) {
            plugin.logError("Failed to initialize CommandMap: " + e.getMessage());
        }
    }

    /**
     * Loads all custom commands from the commands folder
     */
    public void loadCommands() {
        DebugLogger.debug("Loading custom commands...");
        // Clear existing custom commands
        unregisterAllCommands();

        File oldFolder = new File(plugin.getDataFolder(), "comands");
        File commandsFolder = new File(plugin.getDataFolder(), "commands");

        // Legacy migration support
        if (oldFolder.exists() && !commandsFolder.exists()) {
            plugin.logWarning("Legacy 'comands' folder detected. Renaming it to 'commands'...");
            if (oldFolder.renameTo(commandsFolder)) {
                plugin.logInfo("Successfully migrated legacy folder.");
            } else {
                plugin.logError("Failed to rename 'comands' to 'commands'. Please do it manually.");
            }
        }

        boolean firstRun = !commandsFolder.exists();
        if (firstRun && commandsFolder.mkdirs()) {

            // Save default commands only on very first install
            String[] defaults = { "example.yml", "admin.yml", "welcome.yml", "announce.yml" };
            for (String def : defaults) {
                plugin.saveResource("commands/" + def, false);
            }
        }

        // Load all command files
        File[] files = commandsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                try {
                    loadCommand(file);
                } catch (Exception e) {
                    plugin.logError("Critical error loading command file: " + file.getName());
                    e.printStackTrace();
                }
            }
        }

        plugin.logInfo("Loaded " + customCommands.size() + " custom commands");
        DebugLogger.debug("Finished loading " + customCommands.size() + " custom commands");
    }

    /**
     * Loads a single command from a file
     */
    private void loadCommand(File file) {
        DebugLogger.debug("Loading command from file: " + file.getName());
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);

            String name = config.getString("name");
            boolean register = config.getBoolean("register", true);

            if (name == null || name.isEmpty()) {
                plugin.logWarning("Command file " + file.getName() + " has no name");
                DebugLogger.debug("Skipped command file (no name): " + file.getName());
                return;
            }

            String permission = config.getString("permission", "");
            String world = config.getString("world", "");
            String description = config.getString("description", "");
            List<String> actions = config.getStringList("actions");
            List<String> aliases = config.getStringList("aliases");
            String material = config.getString("item.material", "PAPER");
            String displayName = config.getString("item.display-name", "&b" + name);
            int cooldown = config.getInt("cooldown", 0);
            int interval = config.getInt("interval", 0);

            long creationTime = 0;
            try {
                Path filePath = file.toPath();
                BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
                creationTime = attrs.creationTime().toMillis();
            } catch (IOException e) {
                creationTime = System.currentTimeMillis(); // Fallback
            }

            // Create executor regardless of registration status so it appears in GUI
            CustomCommandExecutor executor = new CustomCommandExecutor(
                    plugin, name, aliases, permission, world, actions, material, displayName, description, register,
                    creationTime, cooldown, interval);

            // Track the original name (filename without extension)
            String fileName = file.getName().replace(".yml", "");
            executor.setOriginalName(fileName);

            customCommands.put(name.toLowerCase(), executor);
            DebugLogger.debug("Registered command: " + name + " (register=" + register + ", actions=" + actions.size() + ")");

            if (register) {
                registerCommand(name, executor);
                startTimer(name, executor);
            }

        } catch (Exception e) {
            plugin.logError("Error loading command from " + file.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Registers a command with Bukkit
     */
    private void registerCommand(String name, CustomCommandExecutor executor) {
        DebugLogger.debug("Registering Bukkit command: " + name);
        if (commandMap == null) {
            plugin.logError("CommandMap is null, cannot register command: " + name);
            return;
        }

        // Register permission dynamically for LuckPerms auto-completion
        String permNode = executor.getPermission();
        if (permNode != null && !permNode.isEmpty()) {
            if (Bukkit.getPluginManager().getPermission(permNode) == null) {
                try {
                    Permission perm = new Permission(permNode, "Custom permission for " + name, PermissionDefault.OP);
                    Bukkit.getPluginManager().addPermission(perm);
                } catch (Exception ignored) {
                }
            }
        }

        Command command = new Command(name.toLowerCase()) {
            @Override
            public boolean execute(org.bukkit.command.CommandSender sender, String label, String[] args) {
                CustomCommandExecutor currentExecutor = customCommands.get(name.toLowerCase());
                if (currentExecutor != null) {
                    return currentExecutor.onCommand(sender, this, label, args);
                }
                return true; // Command was deleted but unregister failed
            }

            @Override
            public java.util.List<String> tabComplete(org.bukkit.command.CommandSender sender, String alias,
                    String[] args) throws IllegalArgumentException {
                if (sender instanceof org.bukkit.entity.Player) {
                    return null; // returning null in Bukkit defaults to online players
                }
                return java.util.Collections.emptyList();
            }
        };

        command.setDescription(executor.getDescription() != null && !executor.getDescription().isEmpty()
                ? executor.getDescription()
                : "Custom command from X-Commands");
        command.setAliases(executor.getAliases());
        
        if (permNode != null && !permNode.isEmpty()) {
            command.setPermission(permNode);
        }
        
        commandMap.register("xcommands", command);

        // Register robust HelpTopic so the command appears in /help and /?
        try {
            final String cmdName = name.toLowerCase();
            final String desc = executor.getDescription() != null && !executor.getDescription().isEmpty()
                    ? executor.getDescription()
                    : "Custom command from X-Commands";
            final String perm = executor.getPermission();
            
            HelpTopic topic = new HelpTopic() {
                {
                    this.name = "/" + cmdName;
                    this.shortText = desc;
                    this.fullText = desc;
                    this.amendedPermission = perm;
                }

                @Override
                public boolean canSee(org.bukkit.command.CommandSender sender) {
                    return amendedPermission == null || amendedPermission.isEmpty() || sender.hasPermission(amendedPermission);
                }
            };
            Bukkit.getHelpMap().addTopic(topic);
        } catch (Exception ignored) {
            // Some server implementations may not support this
        }
    }

    /**
     * Unregisters all custom commands
     */
    private void unregisterAllCommands() {
        if (commandMap == null) {
            return;
        }

        // Create a copy of keys to avoid ConcurrentModificationException if any
        for (String cmdName : new java.util.HashSet<>(customCommands.keySet())) {
            unregisterCommand(cmdName);
        }

        unregisterAllTimers();
        customCommands.clear();
    }

    private void unregisterAllTimers() {
        for (String cmdName : new java.util.HashSet<>(commandTimers.keySet())) {
            stopTimer(cmdName);
        }
    }

    /**
     * Starts a repeating timer for a command if interval > 0
     */
    public void startTimer(String commandName, CustomCommandExecutor executor) {
        stopTimer(commandName);

        if (executor.getInterval() <= 0) {
            return;
        }

        long ticks = executor.getInterval() * 20L;
        // Folia compatible timer
        Object task = SchedulerUtils.runTaskTimer(plugin, () -> {
            plugin.getActionManager().executeActions(null, executor.getActions());
        }, ticks, ticks);
        
        commandTimers.put(commandName.toLowerCase(), task);
    }

    /**
     * Stops a timer for a specific command
     */
    public void stopTimer(String commandName) {
        Object task = commandTimers.remove(commandName.toLowerCase());
        if (task != null) {
            SchedulerUtils.cancelTask(task);
        }
    }

    /**
     * Reloads all custom commands
     */
    public void reload() {
        DebugLogger.debug("Reloading command manager...");
        loadCommands();
        refreshCommands();
    }

    /**
     * Creates a new command file
     * 
     * @param commandName The name of the command to create
     * @return true if the file was created successfully
     */
    public boolean createCommand(String commandName) {
        if (customCommands.containsKey(commandName.toLowerCase())) {
            return false;
        }

        try {
            // Read template as String to preserve comments
            InputStream resourceStream = plugin.getResource("commands/example.yml");
            if (resourceStream == null) {
                // Try legacy check if folder was recently renamed
                resourceStream = plugin.getResource("comands/example.yml"); 
            }
            if (resourceStream == null) {
                plugin.logError("Template 'commands/example.yml' not found in resources.");
                return false;
            }

            String content;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resourceStream, StandardCharsets.UTF_8))) {
                content = reader.lines().collect(Collectors.joining("\n"));
            }

            // Perform replacements to keep comments intact
            // name: ejemplo -> name: NewName
            content = content.replaceAll("(?m)^name:.*", "name: " + java.util.regex.Matcher.quoteReplacement(commandName));
            // permission: xcommands.ejemplo -> permission: xcommands.newname
            content = content.replaceAll("(?m)^permission:.*", "permission: xcommands." + java.util.regex.Matcher.quoteReplacement(commandName.toLowerCase()));
            // display-name: "..." -> display-name: "&bNewName"
            content = content.replaceAll("(?m)^(\\s*)display-name:.*", java.util.regex.Matcher.quoteReplacement("$1display-name: \"&b" + commandName + "\""));

            // Save the file with comments
            File commandsFolder = new File(plugin.getDataFolder(), "commands");
            if (!commandsFolder.exists() && !commandsFolder.mkdirs()) {
                plugin.logWarning("Could not create commands folder!");
            }

            File commandFile = new File(commandsFolder, commandName + ".yml");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(commandFile, StandardCharsets.UTF_8))) {
                writer.write(content);
            }

            // Apply default aliases and actions from config
            if (plugin.getConfigManager().getConfig().contains("default-command")) {
                YamlConfiguration newConfig = YamlConfiguration.loadConfiguration(commandFile);
                
                String defaultMaterial = plugin.getConfigManager().getConfig().getString("default-command.material");
                if (defaultMaterial != null && !defaultMaterial.isEmpty()) {
                    newConfig.set("item.material", defaultMaterial.toUpperCase());
                }
                
                newConfig.set("aliases", plugin.getConfigManager().getConfig().getStringList("default-command.aliases"));
                
                java.util.List<String> defaultActions = plugin.getConfigManager().getConfig().getStringList("default-command.actions");
                // Replace %command% placeholder with the actual command name
                java.util.List<String> parsedActions = new java.util.ArrayList<>();
                for (String action : defaultActions) {
                    parsedActions.add(action.replace("%command%", commandName));
                }
                newConfig.set("actions", parsedActions);
                
                newConfig.save(commandFile);
            }

            // Now load THIS specific file into memory (using the standard loader logic)
            loadCommand(commandFile);

            return true;
        } catch (Exception e) {
            plugin.logError("Error creating command from template: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Deletes a command file and unregisters it
     * 
     * @param commandName The name of the command to delete
     * @return true if the command was deleted successfully
     */
    public boolean deleteCommand(String commandName) {
        DebugLogger.debug("Deleting command: " + commandName);
        CustomCommandExecutor executor = customCommands.get(commandName.toLowerCase());
        if (executor == null)
            return false;

        // Remove from memory immediately
        customCommands.remove(commandName.toLowerCase());
        stopTimer(commandName);

        // Clear cooldowns for this command to prevent memory leak
        plugin.getCooldownManager().clearCooldowns(commandName);

        // Async file deletion
        String originalName = executor.getOriginalName();
        SchedulerUtils.runTaskAsynchronously(plugin, () -> {
            File commandFile = new File(new File(plugin.getDataFolder(), "commands"), (originalName != null ? originalName : commandName) + ".yml");
            if (commandFile.exists()) {
                commandFile.delete();
            }
        });

        // Unregister from Bukkit
        if (commandMap != null) {
            try {
                Command command = commandMap.getCommand(commandName.toLowerCase());
                if (command != null) {
                    command.unregister(commandMap);

                    // Remove from known commands map using cached reflection field
                    try {
                        if (knownCommandsField != null) {
                            @SuppressWarnings("unchecked")
                            Map<String, Command> knownCommands = (Map<String, Command>) knownCommandsField.get(commandMap);
                            knownCommands.remove(commandName.toLowerCase());
                            knownCommands.remove("xcommands:" + commandName.toLowerCase());
                        }
                    } catch (Exception e1) {
                        // Ignore if failed, as it might be version dependent
                    }
                }
            } catch (Exception e) {
                plugin.logWarning("Error unregistering command " + commandName + ": " + e.getMessage());
            }
        }

        return true;
    }

    /**
     * Updates an action for a command and saves to file
     * 
     * @param commandName The command to edit
     * @param index       The index of the action to change
     * @param newContent  The new action content
     */
    public void editAction(String commandName, int index, String newContent) {
        CustomCommandExecutor executor = customCommands.get(commandName.toLowerCase());
        if (executor != null) {
            List<String> actions = executor.getActions();
            if (index >= 0 && index < actions.size()) {
                actions.set(index, newContent);
                markDirty(commandName);
            }
        }
    }

    /**
     * Saves all actions for a command
     */
    public void saveActions(String commandName, List<String> actions) {
        CustomCommandExecutor executor = customCommands.get(commandName.toLowerCase());
        if (executor != null) {
            // Update the live list in memory
            executor.getActions().clear();
            executor.getActions().addAll(actions);
            markDirty(commandName);
        }
    }

    /**
     * Updates a single value in the command config and returns the potentially
     * updated command name
     */
    public String updateConfigValue(String commandName, String path, Object value) {
        CustomCommandExecutor executor = customCommands.get(commandName.toLowerCase());
        if (executor == null)
            return commandName;

        // Handle rename via dedicated method
        if (path.equals("name")) {
            renameCommand(commandName, value.toString());
            return value.toString();
        }

        if (path.equals("description")) {
            executor.setDescription(value.toString());
            markDirty(commandName);
        } else if (path.equals("permission")) {
            executor.setPermission(value.toString());
            markDirty(commandName);
        } else if (path.equals("cooldown")) {
            try {
                int cooldown = Integer.parseInt(value.toString());
                if (cooldown < 0) cooldown = 0;
                executor.setCooldown(cooldown);
                markDirty(commandName);
            } catch (NumberFormatException e) {
                // Ignore invalid numbers
            }
        } else if (path.equals("interval")) {
            try {
                int interval = Integer.parseInt(value.toString());
                if (interval < 0) interval = 0;
                executor.setInterval(interval);
                markDirty(commandName);
            } catch (NumberFormatException e) {
                // Ignore invalid numbers
            }
        } else if (path.equals("aliases")) {
            // value is expected to be a comma-separated string from chat or list
            if (value instanceof String) {
                String[] parts = value.toString().split(",");
                executor.getAliases().clear();
                for (String part : parts) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        executor.getAliases().add(trimmed);
                    }
                }
            } else if (value instanceof List) {
                executor.getAliases().clear();
                @SuppressWarnings("unchecked")
                List<String> listValue = (List<String>) value;
                executor.getAliases().addAll(listValue);
            }
            markDirty(commandName);
        } else if (path.equals("material")) {
            executor.setMaterial(value.toString().toUpperCase());
            markDirty(commandName);
        }

        return commandName;
    }

    private void renameCommand(String oldName, String newName) {
        // This is tricky in memory. We need to remove old, add new.
        CustomCommandExecutor executor = customCommands.remove(oldName.toLowerCase());
        if (executor != null) {
            // Unregister old command from Bukkit to avoid duplication
            unregisterCommand(oldName);
            
            stopTimer(oldName);
            // Update internal name
            executor.setCommandName(newName);
            // Also update display name default to &bName
            executor.setDisplayName("&b" + newName);

            customCommands.put(newName.toLowerCase(), executor);
            
            // Transfer dirty status
            if (dirtyCommands.remove(oldName.toLowerCase())) {
                markDirty(newName);
            } else {
                markDirty(newName); // Renaming itself is a change
            }

            // Re-register Bukkit command
            registerCommand(newName, executor);
            startTimer(newName, executor);
            refreshCommands();
        }
    }

    /**
     * Saves the command from memory to disk asynchronously
     */
    /**
     * Saves the command preserving comments using "Smart Edit"
     */
    public void saveCommand(String commandName) {
        CustomCommandExecutor executor = customCommands.get(commandName.toLowerCase());
        if (executor == null)
            return;

        SchedulerUtils.runTaskAsynchronously(plugin, () -> {
            try {
                File commandsFolder = new File(plugin.getDataFolder(), "commands");
                String currentName = executor.getCommandName();
                String originalName = executor.getOriginalName();

                // 1. Handle Rename
                if (originalName != null && !originalName.equalsIgnoreCase(currentName)) {
                    File oldFile = new File(commandsFolder, originalName.toLowerCase() + ".yml");
                    File newFile = new File(commandsFolder, currentName.toLowerCase() + ".yml");
                    if (oldFile.exists()) {
                        if (!oldFile.renameTo(newFile)) {
                            try {
                                java.nio.file.Files.copy(oldFile.toPath(), newFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                                oldFile.delete();
                            } catch (java.io.IOException ioe) {
                                plugin.logError("Failed to rename command file: " + ioe.getMessage());
                                return;
                            }
                        }
                    }
                }

                File commandFile = new File(commandsFolder, currentName.toLowerCase() + ".yml");
                YamlConfiguration conf = YamlConfiguration.loadConfiguration(commandFile);
                conf.set("name", executor.getCommandName());
                conf.set("register", executor.isRegistered());
                conf.set("permission", executor.getPermission());
                conf.set("aliases", executor.getAliases());
                conf.set("world", executor.getWorld());
                conf.set("description", executor.getDescription());
                conf.set("cooldown", executor.getCooldown());
                conf.set("interval", executor.getInterval());
                conf.set("actions", executor.getActions());
                conf.set("item.material", executor.getMaterial());
                conf.set("item.display-name", executor.getDisplayName());
                conf.save(commandFile);

                // Finalize updates on the Main Thread to ensure thread safety with Bukkit API
                SchedulerUtils.runTask(plugin, () -> {
                    if (!plugin.isEnabled()) return;
                    executor.setOriginalName(currentName);
                    dirtyCommands.remove(currentName.toLowerCase());
                    syncRegistration(currentName);
                    // Refresh commands for all online players so tab-complete updates immediately
                    org.bukkit.Bukkit.getOnlinePlayers().forEach(org.bukkit.entity.Player::updateCommands);
                });
            } catch (Exception e) {
                plugin.logError("Error saving command " + commandName + ": " + e.getMessage());
            }
        });
    }

    /**
     * Refreshes a command's registration in Bukkit's command map.
     * Use this when aliases or registration status change.
     */
    public void syncRegistration(String commandName) {
        CustomCommandExecutor executor = customCommands.get(commandName.toLowerCase());
        if (executor != null) {
            unregisterCommand(commandName);
            if (executor.isRegistered()) {
                registerCommand(commandName, executor);
                startTimer(commandName, executor);
            } else {
                stopTimer(commandName);
            }
        }
    }

    /**
     * Reloads a single command's configuration from disk, discarding any unsaved
     * changes in memory.
     */
    public void reloadCommand(String commandName) {
        CustomCommandExecutor executor = customCommands.get(commandName.toLowerCase());
        String fileName = (executor != null) ? executor.getOriginalName() : commandName;

        File commandsFolder = new File(plugin.getDataFolder(), "commands");
        File commandFile = new File(commandsFolder, fileName + ".yml");

        if (commandFile.exists()) {
            // Unregister and stop timer first to clear live state
            unregisterCommand(commandName);
            stopTimer(commandName);
            
            // If it was renamed in memory, remove the new name from map to avoid ghost duplication
            if (executor != null && !executor.getCommandName().equalsIgnoreCase(fileName)) {
                customCommands.remove(executor.getCommandName().toLowerCase());
                dirtyCommands.remove(executor.getCommandName().toLowerCase());
            }
            
            // Load from file (this replaces the memory entry with stored data)
            loadCommand(commandFile);
            
            // Clear dirty flag for the name loaded from disk
            dirtyCommands.remove(fileName.toLowerCase());
            
            // Refresh commands for all online players
            refreshCommands();
        }
    }

    private final java.util.Set<String> dirtyCommands = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /**
     * Checks if a command has unsaved changes
     */
    public boolean isDirty(String commandName) {
        return dirtyCommands.contains(commandName.toLowerCase());
    }

    /**
     * Marks a command as having unsaved changes
     */
    public void markDirty(String commandName) {
        dirtyCommands.add(commandName.toLowerCase());
    }

    public Map<String, CustomCommandExecutor> getCustomCommands() {
        return java.util.Collections.unmodifiableMap(customCommands);
    }

    /**
     * Toggles the registration status of a command
     */
    public void toggleCommandRegistration(String commandName) {
        CustomCommandExecutor executor = customCommands.get(commandName.toLowerCase());
        if (executor != null) {
            executor.setRegistered(!executor.isRegistered());
            markDirty(commandName);
            syncRegistration(commandName);
            refreshCommands();
        }
    }

    /**
     * Forces all online players to refresh their command lists (tab-complete)
     */
    public void refreshCommands() {
        SchedulerUtils.runTask(plugin, () -> {
            org.bukkit.Bukkit.getOnlinePlayers().forEach(org.bukkit.entity.Player::updateCommands);
        });
    }

    /**
     * Unregisters a command from Bukkit
     */
    private void unregisterCommand(String commandName) {
        DebugLogger.debug("Unregistering command: " + commandName);
        stopTimer(commandName);
        if (commandMap == null) {
            return;
        }

        try {
            Command command = commandMap.getCommand(commandName.toLowerCase());
            if (command != null) {
                // 1. Standard unregister
                command.unregister(commandMap);

                // 2. Reflection removal from knownCommands map
                // Iterate up hierarchy to find 'knownCommands' (it's usually in
                // SimpleCommandMap)
                if (knownCommandsField != null) {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Command> knownCommands = (Map<String, Command>) knownCommandsField.get(commandMap);

                        // Remove all variations
                        knownCommands.remove(commandName.toLowerCase());
                        knownCommands.remove("xcommands:" + commandName.toLowerCase());
                        knownCommands.remove(command.getName().toLowerCase());
                        knownCommands.remove(command.getLabel().toLowerCase());

                        // Also remove aliases
                        for (String alias : command.getAliases()) {
                            knownCommands.remove(alias.toLowerCase());
                            knownCommands.remove("xcommands:" + alias.toLowerCase());
                        }
                    } catch (IllegalAccessException e) {
                        plugin.logWarning("Failed to access knownCommands map: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            plugin.logWarning("Error unregistering command " + commandName + ": " + e.getMessage());
        }
    }
}
