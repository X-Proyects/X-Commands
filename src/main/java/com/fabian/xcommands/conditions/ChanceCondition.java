package com.fabian.xcommands.conditions;

import com.fabian.xcommands.XCommands;
import com.fabian.xcommands.utils.DebugLogger;
import org.bukkit.entity.Player;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Checks a probability chance (0-100)
 */
public class ChanceCondition implements Condition {

    @Override
    public boolean check(Player player, Map<String, Object> context) {
        String params = (String) context.get("params");
        if (params == null || params.isEmpty()) return false;
        
        try {
            double chance = Double.parseDouble(params.trim());
            double original = chance;
            chance = Math.max(0, Math.min(100, chance));
            if (original < 0 || original > 100) {
                XCommands.getInstance().logWarning("Chance value " + original + " out of 0-100 range, clamped to " + chance);
            }
            return ThreadLocalRandom.current().nextDouble() * 100 <= chance;
        } catch (NumberFormatException e) {
            DebugLogger.debug("[IF_CHANCE] Invalid number format: " + params);
            return false;
        }
    }
}
