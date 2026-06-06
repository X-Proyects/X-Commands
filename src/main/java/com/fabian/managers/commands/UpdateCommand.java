package com.fabian.managers.commands;

import com.fabian.XCommands;
import com.fabian.utils.UpdateChecker;
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
            com.fabian.utils.CompatibilityUtils.sendMessage(sender, plugin.getLanguageManager().getMessage("no-permission"));
            return true;
        }

        UpdateChecker checker = plugin.getUpdateChecker();
        if (checker == null) {
            // Re-initialize with a temporary instance if the main one is null
            // TODO: Add setter for updateChecker in XCommands to persist this
            checker = new UpdateChecker(plugin, 132155);
        }

        com.fabian.utils.CompatibilityUtils.sendMessage(sender, plugin.getLanguageManager().getMessage("update-checking"));
        checker.checkForUpdates(sender);

        return true;
    }
}
