package com.fabian.managers.commands;

import com.fabian.XCommands;
import com.fabian.executors.CustomCommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Map;

/**
 * Handles the /xc list command
 */
public class ListCommand {

    private final XCommands plugin;

    public ListCommand(XCommands plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("xcommands.admin.gui")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no-permission"));
            return true;
        }

        Map<String, CustomCommandExecutor> commands = plugin.getCommandManager().getCustomCommands();
        if (commands.isEmpty()) {
            sender.sendMessage(plugin.getLanguageManager().getMessageWithPrefix("list-empty"));
            return true;
        }

        String header = plugin.getLanguageManager().getMessageWithPrefix("list-header");
        if (header.contains("{0}")) header = header.replace("{0}", String.valueOf(commands.size()));
        
        StringBuilder sb = new StringBuilder();
        sb.append(header);
        
        boolean first = true;
        for (String name : commands.keySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(name);
            first = false;
        }

        sender.sendMessage(sb.toString());
        return true;
    }
}
