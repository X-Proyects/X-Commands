package com.fabian.managers.commands;

import com.fabian.XCommands;
import com.fabian.executors.CustomCommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Handles the /xc info <name> command
 */
public class InfoCommand {

    private final XCommands plugin;

    public InfoCommand(XCommands plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("xcommands.admin.gui")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getLanguageManager().getMessageWithPrefix("info-usage"));
            return true;
        }

        String commandName = args[1].toLowerCase();
        CustomCommandExecutor cmd = plugin.getCommandManager().getCustomCommands().get(commandName);

        if (cmd == null) {
            sender.sendMessage(plugin.getLanguageManager().getMessageWithPrefix("command-not-found"));
            return true;
        }

        sender.sendMessage("§8§m----------------------------------");
        sender.sendMessage("§bX-Commands Info: §e" + cmd.getCommandName());
        sender.sendMessage("§7Description: §f" + (cmd.getDescription().isEmpty() ? "None" : cmd.getDescription()));
        sender.sendMessage("§7Permission: §f" + (cmd.getPermission().isEmpty() ? "None" : cmd.getPermission()));
        sender.sendMessage("§7World: §f" + (cmd.getWorld().isEmpty() ? "All" : cmd.getWorld()));
        sender.sendMessage("§7Cooldown: §f" + cmd.getCooldown() + "s");
        sender.sendMessage("§7Interval: §f" + cmd.getInterval() + " ticks");
        sender.sendMessage("§7Registered: §f" + (cmd.isRegistered() ? "§aYes" : "§cNo"));
        
        List<String> aliases = cmd.getAliases();
        sender.sendMessage("§7Aliases: §f" + (aliases.isEmpty() ? "None" : String.join(", ", aliases)));
        
        List<String> actions = cmd.getActions();
        sender.sendMessage("§7Actions (" + actions.size() + "):");
        for (int i = 0; i < actions.size(); i++) {
            sender.sendMessage("  §8" + (i + 1) + ". §e" + actions.get(i));
        }
        sender.sendMessage("§8§m----------------------------------");

        return true;
    }
}
