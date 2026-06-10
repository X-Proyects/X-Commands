package com.fabian.xcommands.managers.commands;

import com.fabian.xcommands.XCommands;
import com.fabian.xcommands.managers.LanguageManager;

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
    private final LocateCommand locateCommand;
    private final ForceMessagesCommand forceMessagesCommand;

    public XCCommand(XCommands plugin) {
        this.plugin = plugin;
        this.reloadCommand = new ReloadCommand(plugin);
        this.updateCommand = new UpdateCommand(plugin);
        this.createCommand = new CreateCommand(plugin);
        this.deleteCommand = new DeleteCommand(plugin);
        this.listCommand = new ListCommand(plugin);
        this.infoCommand = new InfoCommand(plugin);
        this.editCommand = new EditCommand(plugin);
        this.locateCommand = new LocateCommand(plugin);
        this.forceMessagesCommand = new ForceMessagesCommand(plugin);
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
                com.fabian.xcommands.utils.CompatibilityUtils.sendMessage(sender, plugin.getLanguageManager().getMessage("no-permission"));
                return true;
            }
            if (!(sender instanceof org.bukkit.entity.Player)) {
                com.fabian.xcommands.utils.CompatibilityUtils.sendMessage(sender, plugin.getLanguageManager().getMessage("player-only"));
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
            
            case "locate":
                return locateCommand.execute(sender, args);

            case "forcemessages":
                return forceMessagesCommand.execute(sender, args);

            case "version":
                return sendVersionInfo(sender);

            default:
                sendHelp(sender);
                return true;
        }
    }


    private void sendHelp(CommandSender sender) {
        LanguageManager lang = plugin.getLanguageManager();

        // Map of permission -> language key
        String[][] entries = {
            { "xcommands.admin.gui",     "help-gui"     },
            { "xcommands.admin.locate",  "help-locate"  },
            { "xcommands.admin.forcemessages", "help-forcemessages" },
            { "xcommands.admin.reload",  "help-reload"  },
            { "xcommands.admin.update",  "help-update"  },
            { "xcommands.admin.list",    "help-list"    },
            { "xcommands.admin.info",    "help-info"    },
            { "xcommands.admin.create",  "help-create"  },
            { "xcommands.admin.delete",  "help-delete"  },
            { "xcommands.admin.edit",    "help-edit"    },
            { "xcommands.admin.version", "help-version" },
        };

        // Collect accessible entries
        java.util.List<String> visible = new java.util.ArrayList<>();
        for (String[] entry : entries) {
            if (sender.hasPermission(entry[0])) {
                visible.add(lang.getMessage(entry[1]));
            }
        }

        if (visible.isEmpty()) {
            com.fabian.xcommands.utils.CompatibilityUtils.sendMessage(sender, lang.getMessage("no-permission"));
            return;
        }

        com.fabian.xcommands.utils.CompatibilityUtils.sendMessage(sender, lang.getMessage("help-header"));
        for (String line : visible) {
            com.fabian.xcommands.utils.CompatibilityUtils.sendMessage(sender, line);
        }
        com.fabian.xcommands.utils.CompatibilityUtils.sendMessage(sender, lang.getMessage("help-footer"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            String[][] allSubs = {
                { "-gui",    "xcommands.admin.gui"     },
                { "help",    "xcommands.admin"         },
                { "reload",  "xcommands.admin.reload"  },
                { "update",  "xcommands.admin.update"  },
                { "create",  "xcommands.admin.create"  },
                { "delete",  "xcommands.admin.delete"  },
                { "list",    "xcommands.admin.list"    },
                { "info",    "xcommands.admin.info"    },
                { "edit",    "xcommands.admin.edit"    },
                { "version", "xcommands.admin.version" },
                { "locate",  "xcommands.admin.locate"  },
                { "forcemessages", "xcommands.admin.forcemessages" },
            };
            for (String[] sub : allSubs) {
                if (sender.hasPermission(sub[1]) && sub[0].startsWith(input)) {
                    completions.add(sub[0]);
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
            } else if (sub.equals("locate")) {
                String input = args[1].toLowerCase();
                for (String lang : plugin.getLanguageManager().getAvailableLanguages()) {
                    if (lang.startsWith(input)) {
                        completions.add(lang);
                    }
                }
            } else if (sub.equals("forcemessages")) {
                String input = args[1].toLowerCase();
                if ("new".startsWith(input)) {
                    completions.add("new");
                }
                if ("keep".startsWith(input)) {
                    completions.add("keep");
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
            } else if (sub.equals("forcemessages")) {
                String input = args[2].toLowerCase();
                if ("all".startsWith(input)) {
                    completions.add("all");
                }
                for (String lang : plugin.getLanguageManager().getAvailableLanguages()) {
                    if (lang.startsWith(input)) {
                        completions.add(lang);
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
    /**
     * Sends the version info to the sender
     */
    private boolean sendVersionInfo(CommandSender sender) {
        if (!sender.hasPermission("xcommands.admin.version")) {
            com.fabian.xcommands.utils.CompatibilityUtils.sendMessage(sender, plugin.getLanguageManager().getMessage("no-permission"));
            return true;
        }

        String prefix = plugin.getLanguageManager().getPrefix();
        String foliaStatus = com.fabian.xcommands.utils.SchedulerUtils.isFolia()
                ? plugin.getLanguageManager().getMessage("version-folia-on")
                : plugin.getLanguageManager().getMessage("version-folia-off");

        com.fabian.xcommands.utils.CompatibilityUtils.sendMessage(sender, plugin.getLanguageManager().getMessage("version-header", prefix));
        com.fabian.xcommands.utils.CompatibilityUtils.sendMessage(sender, plugin.getLanguageManager().getMessage("version-plugin", com.fabian.xcommands.utils.CompatibilityUtils.getVersion(plugin)));
        com.fabian.xcommands.utils.CompatibilityUtils.sendMessage(sender, plugin.getLanguageManager().getMessage("version-server", plugin.getServer().getName() + " " + plugin.getServer().getVersion()));
        com.fabian.xcommands.utils.CompatibilityUtils.sendMessage(sender, plugin.getLanguageManager().getMessage("version-platform", foliaStatus));
        com.fabian.xcommands.utils.CompatibilityUtils.sendMessage(sender, plugin.getLanguageManager().getMessage("version-java", System.getProperty("java.version")));

        return true;
    }
}
