package com.fabian.xcommands.menus;

import com.fabian.xcommands.XCommands;
import com.fabian.xcommands.commands.CustomCommandExecutor;
import com.fabian.xcommands.utils.MenuHolder;
import com.fabian.xcommands.utils.MenuHolder.MenuType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * Menu for configuring TITLE actions
 */
public class TitleMenu extends BaseMenu {

    public TitleMenu(XCommands plugin) {
        super(plugin);
    }

    public void open(Player player, String commandName, int actionIndex) {
        CustomCommandExecutor executor = plugin.getCommandManager().getCustomCommands().get(commandName.toLowerCase());
        if (executor == null || actionIndex < 0 || actionIndex >= executor.getActions().size())
            return;

        String currentAction = executor.getActions().get(actionIndex);

        // Parse current title action: [TITLE] title;subtitle;fadeIn;stay;fadeOut
        String[] parts = parseTitle(currentAction);
        String title = parts[0];
        String subtitle = parts[1];
        String fadeIn = parts[2];
        String stay = parts[3];
        String fadeOut = parts[4];

        Inventory inv = createInventory(
                new MenuHolder(MenuType.TITLE_MENU, commandName, actionIndex),
                27,
                lang.getMessage("gui-title-menu-title", commandName));

        fillBackground(inv);

        // Main Title (Slot 10)
        inv.setItem(10, createItem(Material.NAME_TAG,
                lang.getMessage("gui-title-main"),
                lang.getMessage("gui-title-main-lore", title).split("\\|")));

        // Subtitle (Slot 11)
        inv.setItem(11, createItem(Material.NAME_TAG,
                lang.getMessage("gui-title-sub"),
                lang.getMessage("gui-title-sub-lore", subtitle).split("\\|")));

        // Fade In (Slot 13)
        inv.setItem(13, createItem(Material.CLOCK,
                lang.getMessage("gui-title-fadein"),
                lang.getMessage("gui-title-fadein-lore", fadeIn).split("\\|")));

        // Stay (Slot 14)
        inv.setItem(14, createItem(Material.CLOCK,
                lang.getMessage("gui-title-stay"),
                lang.getMessage("gui-title-stay-lore", stay).split("\\|")));

        // Fade Out (Slot 15)
        inv.setItem(15, createItem(Material.CLOCK,
                lang.getMessage("gui-title-fadeout"),
                lang.getMessage("gui-title-fadeout-lore", fadeOut).split("\\|")));

        // Confirm (Slot 26)
        inv.setItem(26, createItem(Material.LIME_DYE, lang.getMessage("gui-numeric-confirm")));

        // Back (Slot 18)
        addBackButton(inv, 18);

        smartOpenInventory(player, inv);
    }

    private String[] parseTitle(String action) {
        // Default values
        String title = "";
        String subtitle = "";
        String fadeIn = "10";
        String stay = "70";
        String fadeOut = "20";

        if (action.contains("]")) {
            String content = action.substring(action.indexOf("]") + 1).trim();
            String[] parts = content.split(";");
            if (parts.length > 0)
                title = parts[0];
            if (parts.length > 1)
                subtitle = parts[1];
            if (parts.length > 2)
                fadeIn = parts[2];
            if (parts.length > 3)
                stay = parts[3];
            if (parts.length > 4)
                fadeOut = parts[4];
        }

        return new String[] { title, subtitle, fadeIn, stay, fadeOut };
    }
}
