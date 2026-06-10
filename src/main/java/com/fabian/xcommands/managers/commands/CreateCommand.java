package com.fabian.xcommands.managers.commands;

import com.fabian.xcommands.XCommands;
import com.fabian.xcommands.utils.CompatibilityUtils;
import com.fabian.xcommands.utils.LoggerUtils;
import com.fabian.xcommands.utils.SchedulerUtils;
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
            CompatibilityUtils.sendMessage(sender, plugin.getLanguageManager().getMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            CompatibilityUtils.sendMessage(sender, plugin.getLanguageManager().getMessageWithPrefix("create-usage"));
            return true;
        }

        String commandName = args[1].toLowerCase();

        LoggerUtils.debug("Create command requested: " + commandName + " by " + sender.getName());

        if (!commandName.matches("[a-zA-Z0-9_]+")) {
            CompatibilityUtils.sendMessage(sender, plugin.getLanguageManager().getMessageWithPrefix("input-invalid-name"));
            return true;
        }

        if (plugin.getCommandManager().createCommand(commandName)) {
            LoggerUtils.debug("Command created successfully: " + commandName);
            // Check if there is a create-success msg in EN.yml, else fallback
            String successMsg = plugin.getLanguageManager().getMessageWithPrefix("create-success");
            if (successMsg.contains("{0}")) {
                successMsg = successMsg.replace("{0}", commandName);
            }
            CompatibilityUtils.sendMessage(sender, successMsg);

            if (sender instanceof Player) {
                Player player = (Player) sender;
                SchedulerUtils.runForPlayer(plugin, player, () -> {
                    plugin.getInventoryManager().openCommandEditMenu(player, commandName);
                });
            }
        } else {
            LoggerUtils.debug("Command creation failed (already exists): " + commandName);
            String existsMsg = plugin.getLanguageManager().getMessageWithPrefix("create-exists");
            if (existsMsg.contains("{0}")) {
                existsMsg = existsMsg.replace("{0}", commandName);
            }
            CompatibilityUtils.sendMessage(sender, existsMsg);
        }

        return true;
    }
}
