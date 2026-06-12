package com.fabian.xcommands.menus;

import com.fabian.xcommands.XCommands;
import com.fabian.xcommands.utils.MenuHolder;
import com.fabian.xcommands.utils.MenuHolder.MenuType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * Confirmation menu for deleting a command
 */
public class ConfirmDeleteMenu extends BaseMenu {

    public ConfirmDeleteMenu(XCommands plugin) {
        super(plugin);
    }

    public void open(Player player, String commandName) {
        Inventory inv = createInventory(
                new MenuHolder(MenuType.CONFIRM_DELETE, commandName),
                27,
                lang.getMessage("gui-confirm-delete-title", commandName));

        // Fill background
        fillBackground(inv);

        // Confirm button
        inv.setItem(11, createItem(Material.LIME_CONCRETE,
                lang.getMessage("gui-confirm-yes"),
                lang.getMessage("gui-confirm-yes-lore").split("\\|")));

        // Cancel button
        inv.setItem(15, createItem(Material.RED_CONCRETE,
                lang.getMessage("gui-confirm-no"),
                lang.getMessage("gui-confirm-no-lore").split("\\|")));

        smartOpenInventory(player, inv);
    }
}
