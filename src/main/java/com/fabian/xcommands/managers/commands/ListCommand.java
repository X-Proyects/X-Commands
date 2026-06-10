package com.fabian.xcommands.managers.commands;

import com.fabian.xcommands.XCommands;
import com.fabian.xcommands.executors.CustomCommandExecutor;
import com.fabian.xcommands.utils.CompatibilityUtils;
import org.bukkit.command.CommandSender;
import java.util.Map;

public class ListCommand {

    private final XCommands plugin;

    public ListCommand(XCommands plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("xcommands.admin.list")) {
            CompatibilityUtils.sendMessage(sender, plugin.getLanguageManager().getMessage("no-permission"));
            return true;
        }

        Map<String, CustomCommandExecutor> commands = plugin.getCommandManager().getCustomCommands();
        if (commands.isEmpty()) {
            CompatibilityUtils.sendMessage(sender, plugin.getLanguageManager().getMessageWithPrefix("list-empty"));
            return true;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(plugin.getLanguageManager().getMessage("list-header", String.valueOf(commands.size()))).append("\n");
        sb.append(plugin.getLanguageManager().getMessage("list-separator")).append("\n");

        int count = 0;
        for (String cmdName : commands.keySet()) {
            sb.append(plugin.getLanguageManager().getMessage("list-item", cmdName));
            count++;
            if (count % 3 == 0) {
                sb.append("\n");
            } else if (count < commands.size()) {
                sb.append("  ");
            }
        }
        
        if (count % 3 != 0) sb.append("\n");
        sb.append(plugin.getLanguageManager().getMessage("list-separator"));

        CompatibilityUtils.sendMessage(sender, sb.toString());
        return true;
    }
}
