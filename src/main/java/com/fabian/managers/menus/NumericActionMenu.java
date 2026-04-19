package com.fabian.managers.menus;

import com.fabian.XCommands;
import com.fabian.utils.MenuHolder;
import com.fabian.utils.MenuHolder.MenuType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class NumericActionMenu extends BaseMenu {

        public NumericActionMenu(XCommands plugin) {
                super(plugin);
        }

        public void open(Player player, String commandName, int actionIndex, String actionType, double currentValue) {
                Inventory inv = Bukkit.createInventory(
                                new MenuHolder(MenuType.NUMERIC_ACTION, commandName, actionIndex),
                                54,
                                lang.getMessage("gui-numeric-title", actionType));

                fillBackground(inv);

                // Current Value Display (Center)
                ItemStack center = createItem(Material.PAPER,
                                "&e" + actionType + ": &f" + currentValue,
                                lang.getMessage("gui-numeric-current"));
                // Store current value in PDC
                ItemMeta meta = center.getItemMeta();
                meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "numeric_value"),
                                PersistentDataType.DOUBLE, currentValue);
                meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "action_type_tag"),
                                PersistentDataType.STRING, actionType);
                center.setItemMeta(meta);
                inv.setItem(13, center);

                // Add Buttons
                int[] slots = { 28, 29, 30, 31, 32, 33, 34 };
                double[] values = { 1, 5, 10, 50, 100, 1000, 5000 };

                for (int i = 0; i < slots.length; i++) {
                        inv.setItem(slots[i], createItem(Material.EMERALD, "&a+" + (int) values[i],
                                        lang.getMessage("gui-numeric-add")));
                }

                // Subtract Buttons (Optional? Or maybe just use negative logic or reset)
                // For now, simple additive. Maybe reset button.

                // Reset/Clear (Barrier)
                inv.setItem(49, createItem(Material.BARRIER, lang.getMessage("gui-numeric-reset"),
                                lang.getMessage("gui-numeric-reset-lore")));

                // Confirm (Lime Dye)
                inv.setItem(53, createItem(Material.LIME_DYE, lang.getMessage("gui-numeric-confirm")));

                // Back
                addBackButton(inv, 45);

                player.openInventory(inv);
        }
}
