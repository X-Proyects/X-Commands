package com.fabian.xcommands.conditions;

import org.bukkit.entity.Player;
import java.util.Map;

/**
 * Checks if the player is in a specific world
 */
public class WorldCondition implements Condition {
    @Override
    public boolean check(Player player, Map<String, Object> context) {
        String worldName = (String) context.get("params");
        if (worldName == null || worldName.isEmpty()) return false;
        return player.getWorld().getName().equalsIgnoreCase(worldName.trim());
    }
}
