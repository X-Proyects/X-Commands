package com.fabian.xcommands.conditions;

import org.bukkit.entity.Player;
import java.util.Map;

/**
 * Represents a requirement that must be met for actions to execute
 */
public interface Condition {
    /**
     * Checks if the condition is met
     * @param player The player to check against
     * @param context Dynamic context for the check
     * @return true if met, false otherwise
     */
    boolean check(Player player, Map<String, Object> context);
}
