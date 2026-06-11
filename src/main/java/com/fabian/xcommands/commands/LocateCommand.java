package com.fabian.xcommands.commands;

import com.fabian.xcommands.XCommands;
import com.fabian.xcommands.managers.LanguageManager;
import com.fabian.xcommands.utils.CompatibilityUtils;
import org.bukkit.command.CommandSender;
import java.util.List;

public class LocateCommand {

    private final XCommands plugin;

    public LocateCommand(XCommands plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        LanguageManager langManager = plugin.getLanguageManager();
        
        if (!sender.hasPermission("xcommands.admin.locate")) {
            CompatibilityUtils.sendMessage(sender, langManager.getMessage("no-permission"));
            return true;
        }

        List<String> available = langManager.getAvailableLanguages();
        
        if (args.length == 1) {
            // Show current language and list available
            String current = langManager.getCurrentLanguage();
            CompatibilityUtils.sendMessage(sender, langManager.getMessage("language-current", current));
            CompatibilityUtils.sendMessage(sender, langManager.getMessage("language-list", String.join(", ", available)));
            return true;
        }

        String newLang = args[1].toLowerCase();
        if (available.contains(newLang)) {
            langManager.setLanguage(newLang);
            CompatibilityUtils.sendMessage(sender, langManager.getMessage("language-changed", newLang));
        } else {
            CompatibilityUtils.sendMessage(sender, langManager.getMessage("language-not-found", String.join(", ", available)));
        }

        return true;
    }
}
