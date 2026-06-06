package com.fabian.managers.commands;

import com.fabian.XCommands;
import com.fabian.executors.CustomCommandExecutor;
import com.fabian.managers.LanguageManager;
import com.fabian.utils.CompatibilityUtils;
import org.bukkit.command.CommandSender;

public class InfoCommand {

    private final XCommands plugin;

    public InfoCommand(XCommands plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        LanguageManager lang = plugin.getLanguageManager();
        
        if (!sender.hasPermission("xcommands.admin.info")) {
            CompatibilityUtils.sendMessage(sender, lang.getMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            CompatibilityUtils.sendMessage(sender, lang.getMessageWithPrefix("info-usage"));
            return true;
        }

        String cmdName = args[1].toLowerCase();
        CustomCommandExecutor cmd = plugin.getCommandManager().getCustomCommands().get(cmdName);

        if (cmd == null) {
            CompatibilityUtils.sendMessage(sender, lang.getMessageWithPrefix("command-not-found"));
            return true;
        }

        String none = lang.getMessage("gui-none");

        CompatibilityUtils.sendMessage(sender, lang.getMessage("info-header"));
        CompatibilityUtils.sendMessage(sender, lang.getMessage("info-title", cmd.getCommandName()));
        CompatibilityUtils.sendMessage(sender, lang.getMessage("info-perm", (cmd.getPermission().isEmpty() ? none : cmd.getPermission())));
        CompatibilityUtils.sendMessage(sender, lang.getMessage("info-cooldown", cmd.getCooldown()));
        CompatibilityUtils.sendMessage(sender, lang.getMessage("info-actions", cmd.getActions().size()));
        CompatibilityUtils.sendMessage(sender, lang.getMessage("info-desc", (cmd.getDescription().isEmpty() ? none : cmd.getDescription())));
        CompatibilityUtils.sendMessage(sender, lang.getMessage("info-footer"));

        return true;
    }
}
