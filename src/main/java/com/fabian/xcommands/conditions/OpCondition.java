package com.fabian.xcommands.conditions;

import org.bukkit.entity.Player;
import java.util.Map;

/**
 * Checks if the player is an OP
 */
public class OpCondition implements Condition {
    @Override
    public boolean check(Player player, Map<String, Object> context) {
        return player.isOp();
    }
}
