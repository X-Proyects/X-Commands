package com.fabian.xcommands.menus;

import com.fabian.xcommands.XCommands;
import com.fabian.xcommands.commands.CustomCommandExecutor;
import com.fabian.xcommands.utils.MenuHolder;
import com.fabian.xcommands.utils.MenuHolder.MenuType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * Menu for configuring GIVE actions
 */
public class GiveMenu extends BaseMenu {

    public GiveMenu(XCommands plugin) {
        super(plugin);
    }

    public void open(Player player, String commandName, int actionIndex) {
        CustomCommandExecutor executor = plugin.getCommandManager().getCustomCommands().get(commandName.toLowerCase());
        if (executor == null || actionIndex < 0 || actionIndex >= executor.getActions().size())
            return;

        String currentAction = executor.getActions().get(actionIndex);

        // Parse: [GIVE] material;amount;customName
        String[] parts = parseGive(currentAction);
        String material = parts[0];
        String amount = parts[1];
        String customName = parts[2];

        Inventory inv = createInventory(
                new MenuHolder(MenuType.GIVE_MENU, commandName, actionIndex),
                27,
                lang.getMessage("gui-give-menu-title", commandName));

        fillBackground(inv);

        // Material (Slot 11)
        inv.setItem(11, createItem(Material.CHEST,
                lang.getMessage("gui-give-material"),
                lang.getMessage("gui-give-material-lore", material).split("\\|")));

        // Amount (Slot 13)
        inv.setItem(13, createItem(Material.GOLD_INGOT,
                lang.getMessage("gui-give-amount"),
                lang.getMessage("gui-give-amount-lore", amount).split("\\|")));

        // Custom Name (Slot 15)
        inv.setItem(15, createItem(Material.NAME_TAG,
                lang.getMessage("gui-give-name"),
                lang.getMessage("gui-give-name-lore", customName).split("\\|")));

        // Confirm (Slot 26)
        inv.setItem(26, createItem(Material.LIME_DYE, lang.getMessage("gui-numeric-confirm")));

        // Back (Slot 18)
        addBackButton(inv, 18);

        smartOpenInventory(player, inv);
    }

    private String[] parseGive(String action) {
        String material = "DIAMOND";
        String amount = "1";
        String customName = "";

        if (action.contains("]")) {
            String content = action.substring(action.indexOf("]") + 1).trim();
            String[] parts = content.split(";");
            if (parts.length > 0)
                material = parts[0];
            if (parts.length > 1)
                amount = parts[1];
            if (parts.length > 2)
                customName = parts[2];
        }

        return new String[] { material, amount, customName };
    }
}
