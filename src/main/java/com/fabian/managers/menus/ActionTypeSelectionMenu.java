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

import java.util.Arrays;
import java.util.List;

/**
 * Menu for selecting action type
 */
public class ActionTypeSelectionMenu extends BaseMenu {

        public ActionTypeSelectionMenu(XCommands plugin) {
                super(plugin);
        }

        public void open(Player player, String commandName, int actionIndex) {
                Inventory inv = Bukkit.createInventory(
                                new MenuHolder(MenuType.ACTION_TYPE_SELECTION, commandName, actionIndex),
                                27,
                                lang.getMessage("gui-type-title", (actionIndex + 1)));

                // Add all action types
                addActionType(inv, 0, Material.PAPER, "MESSAGE", "action-name-message", "action-desc-message");
                addActionType(inv, 1, Material.MAP, "BROADCAST", "action-name-broadcast", "action-desc-broadcast");
                addActionType(inv, 2, Material.ITEM_FRAME, "ACTIONBAR", "action-name-actionbar",
                                "action-desc-actionbar");
                addActionType(inv, 3, Material.FEATHER, "TITLE", "action-name-title", "action-desc-title");
                addActionType(inv, 4, Material.JUKEBOX, "SOUND", "action-name-sound", "action-desc-sound");
                addActionType(inv, 5, Material.POTION, "EFFECT", "action-name-effect", "action-desc-effect");
                addActionType(inv, 6, Material.CHEST, "GIVE", "action-name-give", "action-desc-give");
                addActionType(inv, 7, Material.ENDER_PEARL, "TELEPORT", "action-name-teleport", "action-desc-teleport");
                addActionType(inv, 8, Material.CLOCK, "DELAY", "action-name-delay", "action-desc-delay");
                addActionType(inv, 9, Material.GOLDEN_APPLE, "HEAL", "action-name-heal", "action-desc-heal");
                addActionType(inv, 10, Material.COOKED_BEEF, "FEED", "action-name-feed", "action-desc-feed");
                addActionType(inv, 11, Material.IRON_SWORD, "DAMAGE", "action-name-damage", "action-desc-damage");
                addActionType(inv, 12, Material.COMMAND_BLOCK, "CONSOLE", "action-name-console", "action-desc-console");
                addActionType(inv, 13, Material.OAK_SIGN, "PLAYER", "action-name-player", "action-desc-player");
                addActionType(inv, 14, Material.BARRIER, "KICK", "action-name-kick", "action-desc-kick");
                addActionType(inv, 15, Material.IRON_DOOR, "CLOSE_INVENTORY", "action-name-close", "action-desc-close");
                addActionType(inv, 16, Material.FIREWORK_STAR, "PARTICLE", "action-name-particle",
                                "action-desc-particle");
                addActionType(inv, 17, Material.ENDER_EYE, "BUNGEE", "action-name-bungee", "action-desc-bungee");
                addActionType(inv, 18, Material.GOLD_INGOT, "GIVE_MONEY", "action-name-give-money",
                                "action-desc-give-money");
                addActionType(inv, 19, Material.GOLD_NUGGET, "TAKE_MONEY", "action-name-take-money",
                                "action-desc-take-money");

                // Back Button
                addBackButton(inv, 26);

                player.openInventory(inv);
        }

        private void addActionType(Inventory inv, int slot, Material mat, String actionTag, String nameKey,
                        String descKey) {
                ItemStack item = new ItemStack(mat);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                        meta.setDisplayName(lang.getMessage(nameKey));
                        List<String> lore = Arrays.asList(
                                        lang.getMessage(descKey),
                                        "",
                                        lang.getMessage("gui-type-select"));
                        meta.setLore(lore);

                        // Store action type tag
                        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "action_type");
                        meta.getPersistentDataContainer().set(key, org.bukkit.persistence.PersistentDataType.STRING,
                                        actionTag);

                        item.setItemMeta(meta);
                }
                inv.setItem(slot, item);
        }
}
