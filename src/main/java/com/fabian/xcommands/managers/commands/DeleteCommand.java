package com.fabian.xcommands.managers.commands;

import com.fabian.xcommands.XCommands;
import com.fabian.xcommands.utils.CompatibilityUtils;
import org.bukkit.command.CommandSender;

/**
 * Handles the /xc delete <name> command
 */
public class DeleteCommand {

    private final XCommands plugin;

    public DeleteCommand(XCommands plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("xcommands.admin.delete")) {
            CompatibilityUtils.sendMessage(sender, plugin.getLanguageManager().getMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            CompatibilityUtils.sendMessage(sender, plugin.getLanguageManager().getMessageWithPrefix("delete-usage"));
            return true;
        }

        String commandName = args[1].toLowerCase();

        if (plugin.getCommandManager().getCustomCommands().containsKey(commandName)) {
            if (plugin.getCommandManager().deleteCommand(commandName)) {
                String successMsg = plugin.getLanguageManager().getMessageWithPrefix("delete-success");
                if (successMsg.contains("{0}")) {
                    successMsg = successMsg.replace("{0}", commandName);
                }
                CompatibilityUtils.sendMessage(sender, successMsg);
            } else {
                CompatibilityUtils.sendMessage(sender, plugin.getLanguageManager().getMessageWithPrefix("command-error"));
            }
        } else {
            String notFoundMsg = plugin.getLanguageManager().getMessageWithPrefix("delete-not-found");
            if (notFoundMsg.contains("{0}")) {
                notFoundMsg = notFoundMsg.replace("{0}", commandName);
            }
            CompatibilityUtils.sendMessage(sender, notFoundMsg);
        }

        return true;
    }
}
