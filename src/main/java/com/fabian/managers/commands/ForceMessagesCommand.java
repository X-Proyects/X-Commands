package com.fabian.managers.commands;

import com.fabian.XCommands;
import com.fabian.managers.LanguageManager;
import com.fabian.utils.CompatibilityUtils;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.util.List;

/**
 * /xc forcemessages [language|all]
 *
 * Forces a hot-reload of message files from disk into memory,
 * optionally rewriting the on-disk files with any missing keys
 * from the internal JAR defaults (like locate but for messages).
 *
 * If no argument is given, shows the current language and lists available ones.
 * If a specific language is given, force-updates + reloads only that language file.
 * If "all" is given, force-updates + reloads every language file found on disk.
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

        // /xc forcemessages  ->  show current language + list available
        if (args.length == 1) {
            String current = langManager.getCurrentLanguage();
            CompatibilityUtils.sendMessage(sender, langManager.getMessage("force-messages-current", current));
            CompatibilityUtils.sendMessage(sender, langManager.getMessage("language-list", String.join(", ", langManager.getAvailableLanguages())));
            CompatibilityUtils.sendMessage(sender, langManager.getMessage("force-messages-usage"));
            return true;
        }

        String target = args[1].toLowerCase();
        List<String> available = langManager.getAvailableLanguages();

        // /xc forcemessages all
        if (target.equals("all")) {
            int count = langManager.forceReloadAllMessages();
            CompatibilityUtils.sendMessage(sender, langManager.getMessage("force-messages-all", String.valueOf(count)));
            return true;
        }

        // /xc forcemessages <lang>
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

        return true;
    }
}
