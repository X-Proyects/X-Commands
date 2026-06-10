package com.fabian.xcommands.conditions;

import com.fabian.xcommands.utils.LoggerUtils;
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
        
        permission = permission.trim();

        LoggerUtils.debug("[IF_PERMISSION] Checking '" + permission + "' for " + player.getName());

        // Handle wildcard permission check (ends with %)
        if (permission.endsWith("%")) {
            String prefix = permission.substring(0, permission.length() - 1).toLowerCase();
            for (org.bukkit.permissions.PermissionAttachmentInfo pai : player.getEffectivePermissions()) {
                if (pai.getValue() && pai.getPermission().toLowerCase().startsWith(prefix)) {
                    return true;
                }
            }
            return false;
        }
        
        return player.hasPermission(permission);
    }
}
