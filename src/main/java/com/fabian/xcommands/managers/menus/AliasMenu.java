package com.fabian.xcommands.managers.menus;

import com.fabian.xcommands.XCommands;
import com.fabian.xcommands.commands.CustomCommandExecutor;
import com.fabian.xcommands.utils.MenuHolder;
import com.fabian.xcommands.utils.MenuHolder.MenuType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

/**
 * Menu for managing command aliases
 */
public class AliasMenu extends BaseMenu {

    public static final int[] ALIAS_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25
    };

    public AliasMenu(XCommands plugin) {
        super(plugin);
    }

    public void open(Player player, String commandName) {
        CustomCommandExecutor executor = plugin.getCommandManager().getCustomCommands()
                .get(commandName.toLowerCase());
        if (executor == null)
            return;

        Inventory inv = createInventory(
                new MenuHolder(MenuType.ALIAS_MENU, commandName),
                36,
                lang.getMessage("gui-alias-title", commandName));

        // Fill background
        fillBackground(inv);

        // Alias list
        List<String> aliases = executor.getAliases();

        for (int i = 0; i < Math.min(aliases.size(), ALIAS_SLOTS.length); i++) {
            String alias = aliases.get(i);
            inv.setItem(ALIAS_SLOTS[i], createItem(Material.PAPER,
                    "&e" + alias,
                    lang.getMessage("gui-alias-edit-lore", alias).split("\\|")));
        }

        // Add Alias button
        inv.setItem(31, createItem(Material.EMERALD,
                lang.getMessage("gui-alias-add"),
                lang.getMessage("gui-alias-add-lore").split("\\|")));

        // Back Button
        addBackButton(inv, 27);

        smartOpenInventory(player, inv);
    }
}
