package com.fabian.managers.commands;

import com.fabian.XCommands;
import com.fabian.utils.CompatibilityUtils;
import org.bukkit.command.CommandSender;

public class ReloadCommand {

    private final XCommands plugin;

    public ReloadCommand(XCommands plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("xcommands.admin.reload")) {
            CompatibilityUtils.sendMessage(sender, plugin.getLanguageManager().getMessage("no-permission"));
            return true;
        }

        try {
            // Reload all managers
            plugin.getConfigManager().reload();
            plugin.getLanguageManager().reload();
            plugin.getConditionManager().reload();
            plugin.getActionManager().reload();
            plugin.getCooldownManager().reload();
            plugin.getCommandManager().reload();
            
            // Re-load inventory manager if needed
            plugin.getInventoryManager().reload();

            CompatibilityUtils.sendMessage(sender, plugin.getLanguageManager().getMessageWithPrefix("reload-success"));
        } catch (Exception e) {
            plugin.getLogger().severe("Error reloading plugin: " + e.getMessage());
            e.printStackTrace();
            CompatibilityUtils.sendMessage(sender, plugin.getLanguageManager().getMessageWithPrefix("reload-error"));
        }

        return true;
    }
}
