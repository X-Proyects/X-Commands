package com.fabian.xcommands.managers.commands;

import com.fabian.xcommands.XCommands;
import com.fabian.xcommands.utils.UpdateChecker;
import org.bukkit.command.CommandSender;

/**
 * Handles the /xc update subcommand
 */
public class UpdateCommand {

    private final XCommands plugin;

    public UpdateCommand(XCommands plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("xcommands.admin.update")) {
            com.fabian.xcommands.utils.CompatibilityUtils.sendMessage(sender, plugin.getLanguageManager().getMessage("no-permission"));
            return true;
        }

        UpdateChecker checker = plugin.getUpdateChecker();
        if (checker == null) {
            checker = new UpdateChecker(plugin, 132155);
            plugin.setUpdateChecker(checker);
        }

        com.fabian.xcommands.utils.CompatibilityUtils.sendMessage(sender, plugin.getLanguageManager().getMessage("update-checking"));
        checker.checkForUpdates(sender);

        return true;
    }
}
