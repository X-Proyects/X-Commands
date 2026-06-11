package com.fabian.xcommands.managers.commands;

import com.fabian.xcommands.XCommands;
import com.fabian.xcommands.executors.CustomCommandExecutor;
import com.fabian.xcommands.utils.CompatibilityUtils;
import com.fabian.xcommands.utils.DebugLogger;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Handles the /xc edit <name> <attribute> [value] command
 */
public class EditCommand {

    private final XCommands plugin;

    public EditCommand(XCommands plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        DebugLogger.debug("Edit command by " + sender.getName() + ": " + java.util.Arrays.toString(args));
        if (!sender.hasPermission("xcommands.admin.edit")) {
            CompatibilityUtils.sendMessage(sender, plugin.getLanguageManager().getMessage("no-permission"));
            return true;
        }

        if (args.length < 3) {
            CompatibilityUtils.sendMessage(sender, plugin.getLanguageManager().getMessageWithPrefix("edit-usage"));
            return true;
        }

        String commandName = args[1].toLowerCase();
        CustomCommandExecutor cmd = plugin.getCommandManager().getCustomCommands().get(commandName);

        if (cmd == null) {
            CompatibilityUtils.sendMessage(sender, plugin.getLanguageManager().getMessageWithPrefix("command-not-found"));
            return true;
        }

        String attribute = args[2].toLowerCase();
        
        // Handle attributes that don't strictly require a 4th argument (e.g. clearing something or boolean toggles)
        
        switch (attribute) {
            case "name":
                if (args.length < 4) return sendUsage(sender, "name <newName>");
                String newName = args[3].toLowerCase();
                if (!newName.matches("[a-zA-Z0-9_]+")) {
                    CompatibilityUtils.sendMessage(sender, plugin.getLanguageManager().getMessage("input-invalid-name"));
                    return true;
                }
                if (plugin.getCommandManager().getCustomCommands().containsKey(newName)) {
                    CompatibilityUtils.sendMessage(sender, plugin.getLanguageManager().getMessage("input-command-exists"));
                    return true;
                }
                
                // Use the proper renameCommand method which handles unregistration/re-registration
                plugin.getCommandManager().updateConfigValue(commandName, "name", newName);
                String renameMsg = plugin.getLanguageManager().getMessageWithPrefix("edit-renamed");
                if (renameMsg.contains("{0}")) renameMsg = renameMsg.replace("{0}", newName);
                CompatibilityUtils.sendMessage(sender, renameMsg);
                break;

            case "permission":
                if (args.length < 4) return sendUsage(sender, "permission <perm/none>");
                String perm = args[3].equalsIgnoreCase("none") ? "" : args[3];
                cmd.setPermission(perm);
                save(cmd, sender, "Permission updated.");
                break;

            case "description":
                if (args.length < 4) return sendUsage(sender, "description <text...>");
                String desc = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                cmd.setDescription(desc);
                save(cmd, sender, "Description updated.");
                break;

            case "cooldown":
                if (args.length < 4) return sendUsage(sender, "cooldown <seconds>");
                try {
                    int cd = Integer.parseInt(args[3]);
                    cmd.setCooldown(Math.max(0, cd));
                    save(cmd, sender, "Cooldown");
                } catch (NumberFormatException e) {
                    CompatibilityUtils.sendMessage(sender, plugin.getLanguageManager().getMessageWithPrefix("edit-invalid-number"));
                }
                break;

            case "interval":
                if (args.length < 4) return sendUsage(sender, "interval <ticks>");
                try {
                    int interval = Integer.parseInt(args[3]);
                    cmd.setInterval(Math.max(0, interval));
                    save(cmd, sender, "Interval");
                } catch (NumberFormatException e) {
                    CompatibilityUtils.sendMessage(sender, plugin.getLanguageManager().getMessageWithPrefix("edit-invalid-number"));
                }
                break;

            case "world":
                if (args.length < 4) return sendUsage(sender, "world <worldName/none>");
                String world = args[3].equalsIgnoreCase("none") ? "" : args[3];
                cmd.setWorld(world);
                save(cmd, sender, "World restriction updated.");
                break;

            case "material":
                if (args.length < 4) return sendUsage(sender, "material <materialName>");
                cmd.setMaterial(args[3].toUpperCase());
                save(cmd, sender, "Material updated.");
                break;

            case "displayname":
                if (args.length < 4) return sendUsage(sender, "displayname <text...>");
                String dName = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                cmd.setDisplayName(dName);
                save(cmd, sender, "Display name updated.");
                break;

            case "register":
                if (args.length < 4) return sendUsage(sender, "register <true/false>");
                boolean reg = Boolean.parseBoolean(args[3]);
                cmd.setRegistered(reg);
                save(cmd, sender, "Registration status updated.");
                break;

            case "alias":
                if (args.length < 5) return sendUsage(sender, "alias <add/remove> <aliasName>");
                String action = args[3].toLowerCase();
                String alias = args[4].toLowerCase();
                List<String> aliases = new ArrayList<>(cmd.getAliases());
                if (action.equals("add")) {
                    if (!aliases.contains(alias)) aliases.add(alias);
                    cmd.setAliases(aliases);
                    save(cmd, sender, "Alias added.");
                } else if (action.equals("remove")) {
                    aliases.remove(alias);
                    cmd.setAliases(aliases);
                    save(cmd, sender, "Alias removed.");
                } else {
                    sendUsage(sender, "alias <add/remove> <aliasName>");
                }
                break;

        case "action":
            if (args.length < 5) return sendUsage(sender, "action <add/remove> ...");
            String actType = args[3].toLowerCase();
            List<String> actions = new ArrayList<>(cmd.getActions());
            
            if (actType.equals("remove")) {
                try {
                    int index = Integer.parseInt(args[4]) - 1; // 1-based index to 0-based
                    if (index >= 0 && index < actions.size()) {
                        actions.remove(index);
                        cmd.getActions().clear();
                        cmd.getActions().addAll(actions);
                        save(cmd, sender, "Action");
                    } else {
                        CompatibilityUtils.sendMessage(sender, plugin.getLanguageManager().getMessageWithPrefix("action-invalid-index"));
                    }
                } catch (NumberFormatException e) {
                    CompatibilityUtils.sendMessage(sender, plugin.getLanguageManager().getMessageWithPrefix("edit-invalid-number"));
                }
            } else if (actType.equals("add")) {
                if (args.length < 6) return sendUsage(sender, "action add <type> <value...>");
                String type = args[4].toUpperCase();
                String value = String.join(" ", Arrays.copyOfRange(args, 5, args.length));
                actions.add("[" + type + "] " + value);
                cmd.getActions().clear();
                cmd.getActions().addAll(actions);
                save(cmd, sender, "Action");
            } else {
                sendUsage(sender, "action <add/remove> ...");
            }
            break;

        default:
            CompatibilityUtils.sendMessage(sender, plugin.getLanguageManager().getMessageWithPrefix("edit-unknown-attribute"));
            break;
        }

        return true;
    }

    private void save(CustomCommandExecutor cmd, CommandSender sender, String attribute) {
        DebugLogger.debug("Saving command edit: " + cmd.getCommandName() + " (" + attribute + ")");
        plugin.getCommandManager().markDirty(cmd.getCommandName());
        plugin.getCommandManager().saveCommand(cmd.getCommandName());
        String msg = plugin.getLanguageManager().getMessageWithPrefix("edit-success");
        if (msg.contains("{0}")) msg = msg.replace("{0}", attribute);
        CompatibilityUtils.sendMessage(sender, msg);
    }

    private boolean sendUsage(CommandSender sender, String usage) {
        String prefix = com.fabian.xcommands.utils.ColorUtils.translate(plugin.getConfigManager().getPrefix());
        CompatibilityUtils.sendMessage(sender, prefix + " §cUsage: /xc edit <command> " + usage);
        return true;
    }
}
