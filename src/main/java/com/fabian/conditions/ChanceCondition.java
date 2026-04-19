package com.fabian.conditions;

import org.bukkit.entity.Player;
import java.util.Map;
import java.util.Random;

/**
 * Checks a probability chance (0-100)
 */
public class ChanceCondition implements Condition {
    private final Random random = new Random();

    @Override
    public boolean check(Player player, Map<String, Object> context) {
        String params = (String) context.get("params");
        if (params == null || params.isEmpty()) return false;
        
        try {
            double chance = Double.parseDouble(params.trim());
            return random.nextDouble() * 100 <= chance;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
