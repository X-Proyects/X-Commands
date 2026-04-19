package com.fabian.conditions;

import org.bukkit.entity.Player;
import java.util.Map;

/**
 * Checks if the player has a specific permission
 */
public class PermissionCondition implements Condition {
    @Override
    public boolean check(Player player, Map<String, Object> context) {
        String permission = (String) context.get("params");
        if (permission == null || permission.isEmpty()) return false;
        return player.hasPermission(permission.trim());
    }
}
