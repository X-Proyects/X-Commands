package com.fabian.xcommands.commands;

import com.fabian.xcommands.XCommands;
import com.fabian.xcommands.managers.LanguageManager;
import com.fabian.xcommands.utils.CompatibilityUtils;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * /xc forcemessages [new|keep] [all|language]
 *
 * Forces a hot-reload of message files from disk into memory.
 *
 * Modes:
 *   new   - Regenerates files from JAR defaults (overwrites customizations)
 *   keep  - Adds missing keys only, preserves existing values
 *
 * If no argument is given, shows the current language and usage.
 * "all" applies to every language file; a specific language code applies to that file only.
 */
public class ForceMessagesCommand {

    private final XCommands plugin;

    public ForceMessagesCommand(XCommands plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        LanguageManager langManager = plugin.getLanguageManager();

        if (!sender.hasPermission("xcommands.admin.forcemessages")) {
            CompatibilityUtils.sendMessage(sender, langManager.getMessage("no-permission"));
            return true;
        }

        // /xc forcemessages  ->  show current language + usage
        if (args.length == 1) {
            String current = langManager.getCurrentLanguage();
            CompatibilityUtils.sendMessage(sender, langManager.getMessage("force-messages-current", current));
            CompatibilityUtils.sendMessage(sender, langManager.getMessage("language-list", String.join(", ", langManager.getAvailableLanguages())));
            CompatibilityUtils.sendMessage(sender, langManager.getMessage("force-messages-usage"));
            return true;
        }

        String mode = args[1].toLowerCase();

        // /xc forcemessages <mode> (no target) -> show usage
        if (args.length == 2) {
            CompatibilityUtils.sendMessage(sender, langManager.getMessage("force-messages-usage"));
            return true;
        }

        String target = args[2].toLowerCase();
        List<String> available = langManager.getAvailableLanguages();

        // ---- NEW mode (overwrite from JAR) ----
        if (mode.equals("new")) {
            if (target.equals("all")) {
                int count = langManager.forceResetAllMessages();
                CompatibilityUtils.sendMessage(sender, langManager.getMessage("force-messages-reset-all", String.valueOf(count)));
            } else {
                if (!available.contains(target)) {
                    CompatibilityUtils.sendMessage(sender, langManager.getMessage("language-not-found", String.join(", ", available)));
                    return true;
                }
                boolean updated = langManager.forceResetMessages(target);
                if (updated) {
                    CompatibilityUtils.sendMessage(sender, langManager.getMessage("force-messages-reset-success", target));
                } else {
                    CompatibilityUtils.sendMessage(sender, langManager.getMessage("force-messages-reset-no-active", target));
                }
            }
            return true;
        }

        // ---- KEEP mode (add missing keys only) ----
        if (mode.equals("keep")) {
            if (target.equals("all")) {
                int count = langManager.forceReloadAllMessages();
                CompatibilityUtils.sendMessage(sender, langManager.getMessage("force-messages-all", String.valueOf(count)));
            } else {
                if (!available.contains(target)) {
                    CompatibilityUtils.sendMessage(sender, langManager.getMessage("language-not-found", String.join(", ", available)));
                    return true;
                }
                boolean updated = langManager.forceReloadMessages(target);
                if (updated) {
                    CompatibilityUtils.sendMessage(sender, langManager.getMessage("force-messages-success", target));
                } else {
                    CompatibilityUtils.sendMessage(sender, langManager.getMessage("force-messages-no-changes", target));
                }
            }
            return true;
        }

        // Invalid mode
        CompatibilityUtils.sendMessage(sender, langManager.getMessage("force-messages-invalid-mode"));
        CompatibilityUtils.sendMessage(sender, langManager.getMessage("force-messages-usage"));
        return true;
    }
}
