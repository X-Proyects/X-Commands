package com.fabian.managers.commands;

import com.fabian.XCommands;
import com.fabian.utils.SchedulerUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles the /xc create <name> command
 */
public class CreateCommand {

    private final XCommands plugin;

    public CreateCommand(XCommands plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("xcommands.admin.create")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getLanguageManager().getMessageWithPrefix("create-usage"));
            return true;
        }

        String commandName = args[1].toLowerCase();

        if (!commandName.matches("[a-zA-Z0-9_]+")) {
            sender.sendMessage(plugin.getLanguageManager().getMessageWithPrefix("input-invalid-name"));
            return true;
        }

        if (plugin.getCommandManager().createCommand(commandName)) {
            // Check if there is a create-success msg in EN.yml, else fallback
            String successMsg = plugin.getLanguageManager().getMessageWithPrefix("create-success");
            if (successMsg.contains("{0}")) {
                successMsg = successMsg.replace("{0}", commandName);
            }
            sender.sendMessage(successMsg);

            if (sender instanceof Player) {
                Player player = (Player) sender;
                SchedulerUtils.runForPlayer(plugin, player, () -> {
                    plugin.getInventoryManager().openCommandEditMenu(player, commandName);
                });
            }
        } else {
            String existsMsg = plugin.getLanguageManager().getMessageWithPrefix("create-exists");
            if (existsMsg.contains("{0}")) {
                existsMsg = existsMsg.replace("{0}", commandName);
            }
            sender.sendMessage(existsMsg);
        }

        return true;
    }
}
