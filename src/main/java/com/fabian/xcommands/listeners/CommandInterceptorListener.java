package com.fabian.xcommands.listeners;

import com.fabian.xcommands.XCommands;
import com.fabian.xcommands.commands.CustomCommandExecutor;
import com.fabian.xcommands.utils.DebugLogger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Intercepts commands that are not registered in Bukkit but exist in X-Commands.
 * This allows "register: false" commands to work without being listed.
 */
public class CommandInterceptorListener implements Listener {

    private final XCommands plugin;
    private final Map<String, String> aliasLookup = new HashMap<>();

    public CommandInterceptorListener(XCommands plugin) {
        this.plugin = plugin;
    }

    /**
     * Rebuilds the alias lookup map. Should be called after commands are loaded/reloaded.
     */
    public void rebuildAliasLookup() {
        DebugLogger.debug("Rebuilding alias lookup map...");
        aliasLookup.clear();
        for (CustomCommandExecutor executor : plugin.getCommandManager().getCustomCommands().values()) {
            String commandName = executor.getCommandName().toLowerCase();
            for (String alias : executor.getAliases()) {
                aliasLookup.put(alias.toLowerCase(), commandName);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (event.isCancelled()) return;

        String message = event.getMessage().substring(1); // Remove /
        if (handleCommand(event.getPlayer(), message)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onServerCommand(ServerCommandEvent event) {
        if (handleCommand(event.getSender(), event.getCommand())) {
            event.setCancelled(true);
        }
    }

    private boolean handleCommand(org.bukkit.command.CommandSender sender, String fullCommand) {
        String[] parts = fullCommand.split(" ");
        if (parts.length == 0) return false;

        String cmdLabel = parts[0].toLowerCase();
        
        // Find if this is one of our commands
        CustomCommandExecutor executor = plugin.getCommandManager().getCustomCommands().get(cmdLabel);
        
        // Check aliases using O(1) lookup map
        if (executor == null) {
            String resolvedName = aliasLookup.get(cmdLabel);
            if (resolvedName != null) {
                executor = plugin.getCommandManager().getCustomCommands().get(resolvedName);
            }
        }

        // If we found a command and it's NOT registered in Bukkit, we handle it manually
        if (executor != null && !executor.isRegistered()) {
            String[] args = parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : new String[0];
            DebugLogger.debug("Intercepted unregistered command: " + cmdLabel + " by " + sender.getName());
            executor.onCommand(sender, null, cmdLabel, args);
            return true;
        }

        return false;
    }
}
