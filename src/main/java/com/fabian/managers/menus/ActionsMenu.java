package com.fabian.managers.menus;

import com.fabian.XCommands;
import com.fabian.executors.CustomCommandExecutor;
import com.fabian.utils.CompatibilityUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu to list and manage actions of a command
 */
public class ActionsMenu extends BaseMenu {

    public ActionsMenu(XCommands plugin) {
        super(plugin);
    }

    public void open(Player player, String commandName) {
        CustomCommandExecutor cmd = plugin.getCommandManager().getCustomCommands().get(commandName);
        if (cmd == null) return;

        Inventory inv = createInventory(new com.fabian.utils.MenuHolder(com.fabian.utils.MenuHolder.MenuType.ACTIONS, commandName, -1, 0), 54, 
                lang.getMessage("gui-actions-title", commandName));

        List<String> actions = cmd.getActions();
        for (int i = 0; i < Math.min(actions.size(), 45); i++) {
            inv.setItem(i, createActionItem(i, actions.get(i)));
        }

        fillBackground(inv);

        // Add Action button
        inv.setItem(49, createItem(Material.NETHER_STAR,
                lang.getMessage("gui-actions-add"),
                lang.getMessage("gui-actions-add-lore").split("\\|")));

        // Back Button
        addBackButton(inv, 45);

        // Reorder Button
        inv.setItem(53, createItem(Material.HOPPER,
                lang.getMessage("gui-actions-reorder"),
                lang.getMessage("gui-actions-reorder-lore").split("\\|")));

        smartOpenInventory(player, inv);
    }

    private ItemStack createActionItem(int index, String action) {
        ItemStack item = new ItemStack(getActionMaterial(action));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            CompatibilityUtils.setDisplayName(meta, lang.getMessage("gui-actions-item-name", (index + 1)));

            String[] loreArr = lang.getMessage("gui-actions-item-lore", action).split("\\|");
            List<String> loreList = new ArrayList<>(java.util.Arrays.asList(loreArr));
            CompatibilityUtils.setLore(meta, loreList);

            // Store index in PersistentDataContainer
            meta.getPersistentDataContainer().set(keyActionIndex, PersistentDataType.INTEGER, index);

            item.setItemMeta(meta);
        }
        return item;
    }
}
