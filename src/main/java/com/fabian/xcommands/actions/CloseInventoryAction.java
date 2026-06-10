package com.fabian.xcommands.actions;

import org.bukkit.entity.Player;
import java.util.Map;

/**
 * Closes the player's open inventory
 * Format: [CLOSE_INVENTORY]
 */
public class CloseInventoryAction implements Action {

    @Override
    public void execute(Player player, Map<String, Object> context) {
        if (player == null) return;
        player.closeInventory();
    }

    @Override
    public String getTag() {
        return "CLOSE_INVENTORY";
    }
}
