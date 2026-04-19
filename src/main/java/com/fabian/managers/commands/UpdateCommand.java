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
            sender.sendMessage(plugin.getLanguageManager().getMessage("no-permission"));
            return true;
        }

        UpdateChecker checker = plugin.getUpdateChecker();
        if (checker == null) {
            // Re-initialize if for some reason it's null, making sure we persist it if
            // possible or just use a temp one
            // The main plugin class usually holds one instance.
            checker = new UpdateChecker(plugin, 132155);
            // We should probably set it back to plugin if possible, but there's no setter.
            // But usually it's initialized in onEnable.
        }

        sender.sendMessage(plugin.getLanguageManager().getMessage("update-checking"));
        checker.checkForUpdates(sender);

        return true;
    }
}
