package com.fabian.xcommands.managers.commands;

import com.fabian.xcommands.XCommands;
import com.fabian.xcommands.utils.CompatibilityUtils;
import com.fabian.xcommands.utils.LoggerUtils;
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
            LoggerUtils.debug("Reload requested by " + sender.getName());
            // Reload all managers
            plugin.getConfigManager().reload();
            plugin.getLanguageManager().reload();
            plugin.getConditionManager().reload();
            plugin.getActionManager().reload();
            plugin.getCooldownManager().reload();
            plugin.getCommandManager().reload();
            plugin.getCommandInterceptorListener().rebuildAliasLookup();
            
            // Re-load inventory manager if needed
            plugin.getInventoryManager().reload();

            CompatibilityUtils.sendMessage(sender, plugin.getLanguageManager().getMessageWithPrefix("reload-success"));
            LoggerUtils.debug("Reload completed successfully");
        } catch (Exception e) {
            plugin.logSevere("Error reloading plugin: " + e.getMessage(), e);
            LoggerUtils.debug("Reload failed: " + e.getMessage());
            CompatibilityUtils.sendMessage(sender, plugin.getLanguageManager().getMessageWithPrefix("reload-error"));
        }

        return true;
    }
}
