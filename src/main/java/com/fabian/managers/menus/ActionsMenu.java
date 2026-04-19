package com.fabian.managers.menus;

import com.fabian.XCommands;
import com.fabian.executors.CustomCommandExecutor;
import com.fabian.utils.ColorUtils;
import com.fabian.utils.MenuHolder;
import com.fabian.utils.MenuHolder.MenuType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu showing all actions for a command
 */
public class ActionsMenu extends BaseMenu {

    public ActionsMenu(XCommands plugin) {
        super(plugin);
    }

    public void open(Player player, String commandName) {
        CustomCommandExecutor executor = plugin.getCommandManager().getCustomCommands()
                .get(commandName.toLowerCase());
        if (executor == null)
            return;

        Inventory inv = Bukkit.createInventory(
                new MenuHolder(MenuType.ACTIONS, commandName),
                54,
                lang.getMessage("gui-actions-title", commandName));

        // Fill background
        fillBackground(inv);

        // Add action items
        List<String> actions = executor.getActions();
        for (int i = 0; i < Math.min(actions.size(), 45); i++) {
            inv.setItem(i, createActionItem(i, actions.get(i)));
        }

        // Add Action button
        inv.setItem(49, createItem(Material.NETHER_STAR,
                lang.getMessage("gui-actions-add"),
                lang.getMessage("gui-actions-add-lore").split("\\|")));

        // Back Button
        addBackButton(inv, 45);

        // Reorder Button
        inv.setItem(53, createItem(Material.HOPPER,
                lang.getMessage("gui-actions-reorder"),
                lang.getMessage("gui-actions-reorder-lore")));

        player.openInventory(inv);
    }

    private ItemStack createActionItem(int index, String action) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(lang.getMessage("gui-actions-item-name", (index + 1)));

            String[] lore = lang.getMessage("gui-actions-item-lore", action).split("\\|");
            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(ColorUtils.translate(line));
            }
            meta.setLore(loreList);

            // Store index in PersistentDataContainer
            meta.getPersistentDataContainer().set(keyActionIndex, PersistentDataType.INTEGER, index);

            item.setItemMeta(meta);
        }
        return item;
    }
}
