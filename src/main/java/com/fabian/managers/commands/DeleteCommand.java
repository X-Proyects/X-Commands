package com.fabian.managers.commands;

import com.fabian.XCommands;
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
            sender.sendMessage(plugin.getLanguageManager().getMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getLanguageManager().getMessageWithPrefix("delete-usage"));
            return true;
        }

        String commandName = args[1].toLowerCase();

        if (plugin.getCommandManager().getCustomCommands().containsKey(commandName)) {
            if (plugin.getCommandManager().deleteCommand(commandName)) {
                String successMsg = plugin.getLanguageManager().getMessageWithPrefix("delete-success");
                if (successMsg.contains("{0}")) {
                    successMsg = successMsg.replace("{0}", commandName);
                }
                sender.sendMessage(successMsg);
            } else {
                sender.sendMessage(plugin.getLanguageManager().getMessageWithPrefix("command-error"));
            }
        } else {
            String notFoundMsg = plugin.getLanguageManager().getMessageWithPrefix("delete-not-found");
            if (notFoundMsg.contains("{0}")) {
                notFoundMsg = notFoundMsg.replace("{0}", commandName);
            }
            sender.sendMessage(notFoundMsg);
        }

        return true;
    }
}
