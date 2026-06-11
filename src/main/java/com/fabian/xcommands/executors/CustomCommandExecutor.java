package com.fabian.xcommands.executors;

import com.fabian.xcommands.XCommands;
import com.fabian.xcommands.utils.CompatibilityUtils;
import com.fabian.xcommands.utils.DebugLogger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Executor for custom commands loaded from YAML files
 */
public class CustomCommandExecutor implements CommandExecutor {

    private final XCommands plugin;
    private String commandName;
    private String permission;
    private String world;
    private final List<String> actions;
    private String material;
    private String displayName;
    private String description;
    private final long creationTime;
    private boolean registered;
    private String originalName;
    private int cooldown;
    private int interval;
    private List<String> aliases;
    private final java.util.Map<java.util.UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public CustomCommandExecutor(XCommands plugin, String commandName, List<String> aliases, String permission,
            String world, List<String> actions, String material, String displayName,
            String description, boolean registered, long creationTime, int cooldown, int interval) {
        this.plugin = plugin;
        this.commandName = commandName;
        this.aliases = aliases != null ? aliases : new java.util.ArrayList<>();
        this.permission = permission;
        this.world = world;
        this.actions = actions;
        this.material = material;
        this.displayName = displayName;
        this.description = description;
        this.registered = registered;
        this.creationTime = creationTime;
        this.cooldown = cooldown;
        this.interval = interval;
    }

    public CustomCommandExecutor(XCommands plugin, String commandName, List<String> aliases, String permission,
            String world, List<String> actions, String material, String displayName,
            String description, long creationTime, int cooldown, int interval) {
        this(plugin, commandName, aliases, permission, world, actions, material, displayName, description, true,
                creationTime,
                cooldown, interval);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        DebugLogger.debug("Custom command executed: /" + commandName + " by " + sender.getName() + " args=" + java.util.Arrays.toString(args));
        // Check if sender is a player (some actions require a player)
        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        }

        // Check permission
        if (!permission.isEmpty() && !sender.hasPermission(permission)) {
            DebugLogger.debug("Permission denied for /" + commandName + ": requires '" + permission + "', " + sender.getName() + " lacks it");
            String message = plugin.getLanguageManager().getMessage("command-no-permission");
            CompatibilityUtils.sendMessage(sender, message);
            return true;
        }

        // Check world restriction
        if (player != null && !world.isEmpty()) {
            if (!player.getWorld().getName().equalsIgnoreCase(world)) {
                String message = plugin.getLanguageManager().getMessage("command-wrong-world");
                CompatibilityUtils.sendMessage(player, message);
                return true;
            }
        }

        // Check cooldown
        if (player != null && cooldown > 0 && !player.hasPermission("xcommands.bypass.cooldown")) {
            long lastUsed = cooldowns.getOrDefault(player.getUniqueId(), 0L);
            long currentTime = System.currentTimeMillis();
            long remaining = (lastUsed + (cooldown * 1000L)) - currentTime;

            if (remaining > 0) {
                DebugLogger.debug("Cooldown active for /" + commandName + ": " + player.getName() + " must wait " + (remaining / 1000 + 1) + "s");
                String message = plugin.getLanguageManager().getMessage("command-cooldown", (remaining / 1000) + 1);
                CompatibilityUtils.sendMessage(player, message);
                return true;
            }
            cooldowns.put(player.getUniqueId(), currentTime);
        }

        // Execute actions
        try {
            plugin.getActionManager().executeActions(player, actions, args);
        } catch (Exception e) {
            String message = plugin.getLanguageManager().getMessage("command-error");
            CompatibilityUtils.sendMessage(sender, message);
            plugin.logError("Error executing command " + commandName + ": " + e.getMessage());
        }

        return true;
    }

    public String getCommandName() {
        return commandName;
    }

    public String getPermission() {
        return permission;
    }

    public String getWorld() {
        return world;
    }

    public void setWorld(String world) {
        this.world = world;
    }

    public List<String> getActions() {
        return actions;
    }

    public String getMaterial() {
        return material;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public boolean isRegistered() {
        return registered;
    }

    public void setRegistered(boolean registered) {
        this.registered = registered;
    }

    public void setCommandName(String commandName) {
        this.commandName = commandName;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public int getCooldown() {
        return cooldown;
    }

    public void setCooldown(int cooldown) {
        this.cooldown = cooldown;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public void setAliases(List<String> aliases) {
        this.aliases = aliases;
    }

    public void clearCooldown(java.util.UUID uuid) {
        cooldowns.remove(uuid);
    }
}
