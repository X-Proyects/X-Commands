package com.fabian.managers.commands;

import com.fabian.XCommands;
import com.fabian.executors.CustomCommandExecutor;
import com.fabian.utils.CompatibilityUtils;
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
        sb.append("§b§lX-Commands List: §f(").append(commands.size()).append(")\n");
        sb.append("§8§m----------------------------------\n");

        int count = 0;
        for (String cmdName : commands.keySet()) {
            sb.append(" §8» §e").append(cmdName);
            count++;
            if (count % 3 == 0) {
                sb.append("\n");
            } else if (count < commands.size()) {
                sb.append("  ");
            }
        }
        
        if (count % 3 != 0) sb.append("\n");
        sb.append("§8§m----------------------------------");

        CompatibilityUtils.sendMessage(sender, sb.toString());
        return true;
    }
}
