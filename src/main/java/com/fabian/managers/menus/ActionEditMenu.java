package com.fabian.managers.menus;

import com.fabian.XCommands;
import com.fabian.executors.CustomCommandExecutor;
import com.fabian.utils.MenuHolder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * Menu for editing a specific action (type and value)
 */
public class ActionEditMenu extends BaseMenu {

    public ActionEditMenu(XCommands plugin) {
        super(plugin);
    }

    public void open(Player player, String commandName, int actionIndex) {
        CustomCommandExecutor executor = plugin.getCommandManager().getCustomCommands()
                .get(commandName.toLowerCase());
        if (executor == null || actionIndex < 0 || actionIndex >= executor.getActions().size()) {
            plugin.logWarning("Failed to open ActionEditMenu: Invalid command or index. Cmd: " + commandName + ", Idx: "
                    + actionIndex);
            return;
        }

        String action = executor.getActions().get(actionIndex);
        String type = "MESSAGE";
        String value = "";

        if (action.startsWith("[")) {
            int end = action.indexOf("]");
            if (end != -1) {
                type = action.substring(1, end);
                value = action.substring(end + 1).trim();
            }
        } else {
            value = action;
        }

        Inventory inv = createInventory(
                new MenuHolder(MenuHolder.MenuType.ACTION_EDIT, commandName, actionIndex),
                27,
                lang.getMessage("gui-action-edit-title", (actionIndex + 1)));

        // Fill background
        fillBackground(inv);

        // Edit Type
        inv.setItem(11, createItem(Material.COMPARATOR,
                lang.getMessage("gui-action-edit-type"),
                lang.getMessage("gui-action-edit-type-lore", type).split("\\|")));

        // Edit Value
        inv.setItem(15, createItem(Material.PAPER,
                lang.getMessage("gui-action-edit-value"),
                lang.getMessage("gui-action-edit-value-lore", (value.isEmpty() ? lang.getMessage("gui-empty") : value))
                        .split("\\|")));

        // Back Button
        addBackButton(inv, 18);

        smartOpenInventory(player, inv);
    }
}
