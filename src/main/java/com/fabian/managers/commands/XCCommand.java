package com.fabian.managers.commands;

import com.fabian.XCommands;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

/**
 * Main command handler for /xc
 */
public class XCCommand implements CommandExecutor, TabCompleter {

    private final XCommands plugin;
    private final ReloadCommand reloadCommand;
    private final UpdateCommand updateCommand;

    public XCCommand(XCommands plugin) {
        this.plugin = plugin;
        this.reloadCommand = new ReloadCommand(plugin);
        this.updateCommand = new UpdateCommand(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("-gui")) {
            if (!sender.hasPermission("xcommands.admin.gui")) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("no-permission"));
                return true;
            }
            if (!(sender instanceof org.bukkit.entity.Player)) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("player-only"));
                return true;
            }
            plugin.getInventoryManager().openMainMenu((org.bukkit.entity.Player) sender);
            return true;
        }

        switch (subCommand) {
            case "help":
                sendHelp(sender);
                return true;

            case "reload":
                return reloadCommand.execute(sender, args);

            case "update":
                return updateCommand.execute(sender, args);

            default:
                sendHelp(sender);
                return true;
        }
    }

    /**
     * Sends the help menu to the sender
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.getLanguageManager().getMessage("help-header"));
        sender.sendMessage(plugin.getLanguageManager().getMessage("help-gui"));
        sender.sendMessage(plugin.getLanguageManager().getMessage("help-reload"));
        sender.sendMessage(plugin.getLanguageManager().getMessage("help-update"));
        sender.sendMessage(plugin.getLanguageManager().getMessage("help-footer"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>();
            subCommands.add("-gui");
            subCommands.add("help");
            subCommands.add("reload");
            subCommands.add("update");

            String input = args[0].toLowerCase();
            for (String sub : subCommands) {
                if (sub.startsWith(input)) {
                    completions.add(sub);
                }
            }
        }

        return completions;
    }
}
