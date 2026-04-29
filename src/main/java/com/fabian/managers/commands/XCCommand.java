package com.fabian.managers.commands;

import com.fabian.XCommands;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Main command handler for /xc
 */
public class XCCommand implements CommandExecutor, TabCompleter {

    private final XCommands plugin;
    private final ReloadCommand reloadCommand;
    private final UpdateCommand updateCommand;
    private final CreateCommand createCommand;
    private final DeleteCommand deleteCommand;
    private final ListCommand listCommand;
    private final InfoCommand infoCommand;
    private final EditCommand editCommand;

    public XCCommand(XCommands plugin) {
        this.plugin = plugin;
        this.reloadCommand = new ReloadCommand(plugin);
        this.updateCommand = new UpdateCommand(plugin);
        this.createCommand = new CreateCommand(plugin);
        this.deleteCommand = new DeleteCommand(plugin);
        this.listCommand = new ListCommand(plugin);
        this.infoCommand = new InfoCommand(plugin);
        this.editCommand = new EditCommand(plugin);
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

            case "create":
                return createCommand.execute(sender, args);

            case "delete":
                return deleteCommand.execute(sender, args);

            case "list":
                return listCommand.execute(sender, args);

            case "info":
                return infoCommand.execute(sender, args);

            case "edit":
                return editCommand.execute(sender, args);

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
        sender.sendMessage("§e/xc list §7- List all custom commands");
        sender.sendMessage("§e/xc info <cmd> §7- Show info of a command");
        sender.sendMessage("§e/xc create <cmd> §7- Create a new command");
        sender.sendMessage("§e/xc delete <cmd> §7- Delete a command");
        sender.sendMessage("§e/xc edit <cmd> ... §7- Edit a command");
        sender.sendMessage(plugin.getLanguageManager().getMessage("help-footer"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("-gui", "help", "reload", "update", "create", "delete", "list", "info", "edit");
            String input = args[0].toLowerCase();
            for (String sub : subCommands) {
                if (sub.startsWith(input)) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (Arrays.asList("delete", "info", "edit").contains(sub)) {
                String input = args[1].toLowerCase();
                for (String cmd : plugin.getCommandManager().getCustomCommands().keySet()) {
                    if (cmd.startsWith(input)) {
                        completions.add(cmd);
                    }
                }
            }
        } else if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("edit")) {
                List<String> attributes = Arrays.asList("name", "permission", "description", "cooldown", "interval", "world", "material", "displayname", "register", "alias", "action");
                String input = args[2].toLowerCase();
                for (String attr : attributes) {
                    if (attr.startsWith(input)) {
                        completions.add(attr);
                    }
                }
            }
        } else if (args.length == 4) {
            String sub = args[0].toLowerCase();
            if (sub.equals("edit")) {
                String attr = args[2].toLowerCase();
                String input = args[3].toLowerCase();
                if (attr.equals("alias")) {
                    for (String opt : Arrays.asList("add", "remove")) {
                        if (opt.startsWith(input)) completions.add(opt);
                    }
                } else if (attr.equals("action")) {
                    for (String opt : Arrays.asList("add", "remove")) {
                        if (opt.startsWith(input)) completions.add(opt);
                    }
                } else if (attr.equals("register")) {
                    for (String opt : Arrays.asList("true", "false")) {
                        if (opt.startsWith(input)) completions.add(opt);
                    }
                }
            }
        }

        return completions;
    }
}
