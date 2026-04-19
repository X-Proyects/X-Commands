package com.fabian.managers.commands;

import com.fabian.XCommands;
import org.bukkit.command.CommandSender;

/**
 * Handles the /xc reload command
 */
public class ReloadCommand {

    private final XCommands plugin;

    public ReloadCommand(XCommands plugin) {
        this.plugin = plugin;
    }

    /**
     * Executes the reload command
     */
    public boolean execute(CommandSender sender, String[] args) {
        // Check permission
        if (!sender.hasPermission("xcommands.admin.reload")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no-permission"));
            return true;
        }

        try {
            // Reload configuration
            plugin.getConfigManager().reload();

            // Reload language
            plugin.getLanguageManager().reload();

            // Reload custom commands
            plugin.getCommandManager().reload();

            sender.sendMessage(plugin.getLanguageManager().getMessageWithPrefix("reload-success"));
            plugin.logInfo("Plugin reloaded by " + sender.getName());

        } catch (Exception e) {
            sender.sendMessage(plugin.getLanguageManager().getMessageWithPrefix("reload-error"));
            plugin.logSevere("Error reloading plugin: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }
}
