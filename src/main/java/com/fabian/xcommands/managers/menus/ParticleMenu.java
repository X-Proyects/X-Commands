package com.fabian.xcommands.managers.menus;

import com.fabian.xcommands.XCommands;
import com.fabian.xcommands.executors.CustomCommandExecutor;
import com.fabian.xcommands.utils.MenuHolder;
import com.fabian.xcommands.utils.MenuHolder.MenuType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * Menu for configuring PARTICLE actions
 */
public class ParticleMenu extends BaseMenu {

    public ParticleMenu(XCommands plugin) {
        super(plugin);
    }

    public void open(Player player, String commandName, int actionIndex) {
        CustomCommandExecutor executor = plugin.getCommandManager().getCustomCommands().get(commandName.toLowerCase());
        if (executor == null || actionIndex < 0 || actionIndex >= executor.getActions().size())
            return;

        String currentAction = executor.getActions().get(actionIndex);

        // Parse: [PARTICLE] type;amount
        String[] parts = parseParticle(currentAction);
        String type = parts[0];
        String amount = parts[1];

        Inventory inv = createInventory(
                new MenuHolder(MenuType.PARTICLE_MENU, commandName, actionIndex),
                27,
                lang.getMessage("gui-particle-menu-title", commandName));

        fillBackground(inv);

        // Particle Type (Slot 11)
        inv.setItem(11, createItem(Material.BLAZE_POWDER,
                lang.getMessage("gui-particle-type"),
                lang.getMessage("gui-particle-type-lore", type).split("\\|")));

        // Amount (Slot 15)
        inv.setItem(15, createItem(Material.GOLD_NUGGET,
                lang.getMessage("gui-particle-amount"),
                lang.getMessage("gui-particle-amount-lore", amount).split("\\|")));

        // Confirm (Slot 26)
        inv.setItem(26, createItem(Material.LIME_DYE, lang.getMessage("gui-numeric-confirm")));

        // Back (Slot 18)
        addBackButton(inv, 18);

        smartOpenInventory(player, inv);
    }

    private String[] parseParticle(String action) {
        String type = "FLAME";
        String amount = "10";

        if (action.contains("]")) {
            String content = action.substring(action.indexOf("]") + 1).trim();
            String[] parts = content.split(";");
            if (parts.length > 0)
                type = parts[0];
            if (parts.length > 1)
                amount = parts[1];
        }

        return new String[] { type, amount };
    }
}
