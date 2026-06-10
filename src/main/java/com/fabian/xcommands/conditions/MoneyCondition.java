package com.fabian.xcommands.conditions;

import com.fabian.xcommands.utils.EconomyUtils;
import com.fabian.xcommands.utils.LoggerUtils;
import org.bukkit.entity.Player;
import java.util.Map;

/**
 * Checks if the player has enough money
 */
public class MoneyCondition implements Condition {
    @Override
    public boolean check(Player player, Map<String, Object> context) {
        if (!EconomyUtils.isEnabled()) {
            LoggerUtils.debug("[IF_MONEY] Economy disabled, passing condition for " + player.getName());
            return true; // Assume true if economy is disabled
        }
        
        String params = (String) context.get("params");
        if (params == null || params.isEmpty()) return false;
        
        try {
            double amount = Double.parseDouble(params.trim());
            return EconomyUtils.has(player, amount);
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
