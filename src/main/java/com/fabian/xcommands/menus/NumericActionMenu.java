package com.fabian.xcommands.menus;

import com.fabian.xcommands.XCommands;
import com.fabian.xcommands.utils.MenuHolder;
import com.fabian.xcommands.utils.MenuHolder.MenuType;
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

        // Action types that use decimal display (others use integer)
        private static final java.util.Set<String> DECIMAL_TYPES = new java.util.HashSet<>(
                java.util.Arrays.asList("HEAL", "FEED", "IF_CHANCE", "IF_MONEY", "SOUND_VOLUME", "SOUND_PITCH"));

        public void open(Player player, String commandName, int actionIndex, String actionType, double currentValue) {
                Inventory inv = createInventory(
                                new MenuHolder(MenuType.NUMERIC_ACTION, commandName, actionIndex),
                                54,
                                lang.getMessage("gui-numeric-title", actionType));

                fillBackground(inv);

                // Format value: integers for DELAY/DAMAGE/etc., decimals for HEAL/FEED/etc.
                boolean isDecimal = DECIMAL_TYPES.contains(actionType) || actionType.startsWith("SOUND_");
                String formattedValue = isDecimal
                                ? String.format("%.1f", currentValue)
                                : String.valueOf((int) currentValue);

                // Current Value Display (Center)
                ItemStack center = createItem(Material.PAPER,
                                "&e" + actionType + ": &f" + formattedValue,
                                lang.getMessage("gui-numeric-current"));
                // Store current value in PDC
                ItemMeta meta = center.getItemMeta();
                meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "numeric_value"),
                                PersistentDataType.DOUBLE, currentValue);
                meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "action_type_tag"),
                                PersistentDataType.STRING, actionType);
                center.setItemMeta(meta);
                inv.setItem(13, center);

                // Add Buttons — row 4 (slots 28-34)
                // DELAY uses small tick-based values; other actions use larger amounts
                int[] addSlots = { 28, 29, 30, 31, 32, 33, 34 };
                int[] addValues = actionType.equals("DELAY")
                        ? new int[]{ 1, 2, 5, 10, 20, 40, 60 }
                        : new int[]{ 1, 5, 10, 50, 100, 1000, 5000 };
                for (int i = 0; i < addSlots.length; i++) {
                        inv.setItem(addSlots[i], createItem(Material.LIME_WOOL, "&a+" + addValues[i],
                                        lang.getMessage("gui-numeric-add")));
                }

                // Subtract Buttons — row 5 (slots 37-43)
                // DELAY uses small tick-based values; other actions use larger amounts
                int[] subSlots  = { 37, 38, 39, 40, 41, 42, 43 };
                int[] subValues = actionType.equals("DELAY")
                        ? new int[]{ 1, 2, 5, 10, 20, 40, 60 }
                        : new int[]{ 1, 5, 10, 50, 100, 1000, 5000 };
                for (int i = 0; i < subSlots.length; i++) {
                        inv.setItem(subSlots[i], createItem(Material.RED_WOOL, "&c-" + subValues[i],
                                        lang.getMessage("gui-numeric-subtract")));
                }

                // Reset to zero (Barrier) — slot 49
                inv.setItem(49, createItem(Material.BARRIER, lang.getMessage("gui-numeric-reset"),
                                lang.getMessage("gui-numeric-reset-lore")));

                // Confirm (Lime Dye) — slot 53
                inv.setItem(53, createItem(Material.LIME_DYE, lang.getMessage("gui-numeric-confirm")));

                // Back — slot 45
                addBackButton(inv, 45);

                smartOpenInventory(player, inv);
        }
}
