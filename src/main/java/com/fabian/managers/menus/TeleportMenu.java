package com.fabian.managers.menus;

import com.fabian.XCommands;
import com.fabian.executors.CustomCommandExecutor;
import com.fabian.utils.MenuHolder;
import com.fabian.utils.MenuHolder.MenuType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * Menu for configuring TELEPORT actions
 */
public class TeleportMenu extends BaseMenu {

    public TeleportMenu(XCommands plugin) {
        super(plugin);
    }

    public void open(Player player, String commandName, int actionIndex) {
        CustomCommandExecutor executor = plugin.getCommandManager().getCustomCommands().get(commandName.toLowerCase());
        if (executor == null || actionIndex < 0 || actionIndex >= executor.getActions().size())
            return;

        String currentAction = executor.getActions().get(actionIndex);

        // Parse: [TELEPORT] world;x;y;z
        String[] parts = parseTeleport(currentAction);
        String world = parts[0];
        String x = parts[1];
        String y = parts[2];
        String z = parts[3];

        Inventory inv = createInventory(
                new MenuHolder(MenuType.TELEPORT_MENU, commandName, actionIndex),
                27,
                lang.getMessage("gui-teleport-menu-title", commandName));

        fillBackground(inv);

        // Current Location Button (Slot 11)
        inv.setItem(11, createItem(Material.COMPASS,
                lang.getMessage("gui-teleport-current"),
                lang.getMessage("gui-teleport-current-lore")));

        // Manual Coordinates (Slot 13)
        inv.setItem(13, createItem(Material.PAPER,
                lang.getMessage("gui-teleport-coords"),
                lang.getMessage("gui-teleport-coords-lore", x, y, z).split("\\|")));

        // World (Slot 15)
        inv.setItem(15, createItem(Material.GRASS_BLOCK,
                lang.getMessage("gui-teleport-world"),
                lang.getMessage("gui-teleport-world-lore", world).split("\\|")));

        // Confirm (Slot 26)
        inv.setItem(26, createItem(Material.LIME_DYE, lang.getMessage("gui-numeric-confirm")));

        // Back (Slot 18)
        addBackButton(inv, 18);

        smartOpenInventory(player, inv);
    }

    private String[] parseTeleport(String action) {
        String world = "world";
        String x = "0";
        String y = "64";
        String z = "0";

        if (action.contains("]")) {
            String content = action.substring(action.indexOf("]") + 1).trim();
            String[] parts = content.split(";");
            if (parts.length > 0)
                world = parts[0];
            if (parts.length > 1)
                x = parts[1];
            if (parts.length > 2)
                y = parts[2];
            if (parts.length > 3)
                z = parts[3];
        }

        return new String[] { world, x, y, z };
    }
}
