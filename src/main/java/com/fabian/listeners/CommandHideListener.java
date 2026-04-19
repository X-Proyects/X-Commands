package com.fabian.listeners;

import com.fabian.XCommands;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;

/**
 * Listener that manages which commands are visible to players in their
 * tab-complete list.
 * This helps maintain a clean look and prevents players from seeing internal or
 * namespaced commands.
 */
public class CommandHideListener implements Listener {

    public CommandHideListener() {
    }

    /**
     * Triggered when the server sends the list of available commands to a player.
     * We modify this list to hide specific commands based on the plugin
     * configuration.
     *
     * @param event The command send event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommandSend(PlayerCommandSendEvent event) {
        if (event.getCommands() == null)
            return;

        boolean hideAllColon = XCommands.getInstance().getConfigManager().isHideMinecraftCommands();

        // Use removeIf to aggressively filter commands containing colons
        event.getCommands().removeIf(command -> {
            if (command == null)
                return false;

            String lowerCommand = command.toLowerCase();

            // 1. ALWAYS HIDE: Internal plugin commands that include the namespace
            if (lowerCommand.startsWith("x-commands:") || lowerCommand.startsWith("xcommands:")) {
                return true;
            }

            // 2. OPTIONAL HIDE: All commands containing a colon (e.g., minecraft:tp)
            if (hideAllColon && command.contains(":")) {
                return true;
            }

            return false;
        });
    }
}
