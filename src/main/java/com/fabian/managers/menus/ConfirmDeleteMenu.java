package com.fabian.managers.menus;

import com.fabian.XCommands;
import com.fabian.utils.MenuHolder;
import com.fabian.utils.MenuHolder.MenuType;
import org.bukkit.Bukkit;
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
        Inventory inv = Bukkit.createInventory(
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

        player.openInventory(inv);
    }
}
