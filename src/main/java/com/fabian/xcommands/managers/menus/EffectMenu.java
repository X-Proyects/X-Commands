package com.fabian.xcommands.managers.menus;

import com.fabian.xcommands.XCommands;
import com.fabian.xcommands.commands.CustomCommandExecutor;
import com.fabian.xcommands.utils.MenuHolder;
import com.fabian.xcommands.utils.MenuHolder.MenuType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * Menu for configuring EFFECT actions
 */
public class EffectMenu extends BaseMenu {

    public EffectMenu(XCommands plugin) {
        super(plugin);
    }

    public void open(Player player, String commandName, int actionIndex) {
        CustomCommandExecutor executor = plugin.getCommandManager().getCustomCommands().get(commandName.toLowerCase());
        if (executor == null || actionIndex < 0 || actionIndex >= executor.getActions().size())
            return;

        String currentAction = executor.getActions().get(actionIndex);

        // Parse: [EFFECT] type;duration;amplifier
        String[] parts = parseEffect(currentAction);
        String type = parts[0];
        String duration = parts[1];
        String amplifier = parts[2];

        Inventory inv = createInventory(
                new MenuHolder(MenuType.EFFECT_MENU, commandName, actionIndex),
                27,
                lang.getMessage("gui-effect-menu-title", commandName));

        fillBackground(inv);

        // Effect Type (Slot 11)
        inv.setItem(11, createItem(Material.POTION,
                lang.getMessage("gui-effect-type"),
                lang.getMessage("gui-effect-type-lore", type).split("\\|")));

        // Duration (Slot 13)
        inv.setItem(13, createItem(Material.CLOCK,
                lang.getMessage("gui-effect-duration"),
                lang.getMessage("gui-effect-duration-lore", duration).split("\\|")));

        // Amplifier (Slot 15)
        inv.setItem(15, createItem(Material.REDSTONE,
                lang.getMessage("gui-effect-amplifier"),
                lang.getMessage("gui-effect-amplifier-lore", amplifier).split("\\|")));

        // Confirm (Slot 26)
        inv.setItem(26, createItem(Material.LIME_DYE, lang.getMessage("gui-numeric-confirm")));

        // Back (Slot 18)
        addBackButton(inv, 18);

        smartOpenInventory(player, inv);
    }

    private String[] parseEffect(String action) {
        String type = "SPEED";
        String duration = "60";
        String amplifier = "1";

        if (action.contains("]")) {
            String content = action.substring(action.indexOf("]") + 1).trim();
            String[] parts = content.split(";");
            if (parts.length > 0)
                type = parts[0];
            if (parts.length > 1)
                duration = parts[1];
            if (parts.length > 2)
                amplifier = parts[2];
        }

        return new String[] { type, duration, amplifier };
    }
}
