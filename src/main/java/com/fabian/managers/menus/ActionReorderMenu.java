package com.fabian.managers.menus;

import com.fabian.XCommands;
import com.fabian.executors.CustomCommandExecutor;
import com.fabian.utils.MenuHolder;
import com.fabian.utils.MenuHolder.MenuType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * Menu for reordering actions via Drag & Drop
 */
public class ActionReorderMenu extends BaseMenu {

    public ActionReorderMenu(XCommands plugin) {
        super(plugin);
    }

    public void open(Player player, String commandName) {
        CustomCommandExecutor executor = plugin.getCommandManager().getCustomCommands().get(commandName.toLowerCase());
        if (executor == null)
            return;

        Inventory inv = createInventory(
                new MenuHolder(MenuType.ACTION_REORDER, commandName, -1),
                54,
                lang.getMessage("gui-reorder-title", commandName));

        // Fill background for non-action slots (bottom rows)
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 36; i < 54; i++) {
            inv.setItem(i, glass);
        }

        // Add actions
        List<String> actions = executor.getActions();
        for (int i = 0; i < actions.size() && i < 36; i++) {
            String action = actions.get(i);
            Material mat = Material.PAPER;

            // Try to guess material based on type
            if (action.contains("[GIVE]"))
                mat = Material.CHEST;
            else if (action.contains("[MESSAGE]"))
                mat = Material.PAPER;
            else if (action.contains("[SOUND]"))
                mat = Material.JUKEBOX;
            else if (action.contains("[TITLE]"))
                mat = Material.FEATHER;
            else if (action.contains("[TELEPORT]"))
                mat = Material.ENDER_PEARL;
            else if (action.contains("[GIVE_MONEY]"))
                mat = Material.GOLD_INGOT;

            ItemStack item = createItem(mat, "&e" + action,
                    lang.getMessage("gui-reorder-lore").split("\\|"));

            // Store original index in PDC? actually not strictly needed if we just
            // reconstruct list
            // But we might want to store the full action string in PDC if lore truncation
            // is an issue
            // For now, let's just rely on the item display name or store it in PDC
            ItemMeta meta = item.getItemMeta();
            meta.getPersistentDataContainer().set(
                    new org.bukkit.NamespacedKey(plugin, "action_content"),
                    PersistentDataType.STRING,
                    action);
            item.setItemMeta(meta);

            inv.setItem(i, item);
        }

        // Trash Can
        inv.setItem(49, createItem(Material.BARRIER, lang.getMessage("gui-reorder-trash"),
                lang.getMessage("gui-reorder-trash-lore").split("\\|")));

        // Back/Save Button
        inv.setItem(45, createItem(Material.ARROW, lang.getMessage("gui-back"), lang.getMessage("gui-back-lore")));

        player.openInventory(inv);
    }
}
