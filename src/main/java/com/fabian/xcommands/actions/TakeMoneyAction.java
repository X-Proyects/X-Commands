package com.fabian.xcommands.actions;

import com.fabian.xcommands.utils.EconomyUtils;
import org.bukkit.entity.Player;
import java.util.Map;

/**
 * Takes money from the player using Vault
 * Format: [TAKE_MONEY] 500
 */
public class TakeMoneyAction implements Action {

    @Override
    public void execute(Player player, Map<String, Object> context) {
        if (player == null || !EconomyUtils.isEnabled()) return;

        String params = (String) context.get("params");
        if (params == null) return;

        try {
            double amount = Double.parseDouble(params.trim());

            // Prevent negative amounts that would duplicate money instead of taking it
            if (amount <= 0) {
                com.fabian.xcommands.utils.LoggerUtils.warn("Invalid amount for action [TAKE_MONEY]: " + amount + " (must be positive). Action skipped.");
                return;
            }

            if (EconomyUtils.has(player, amount)) {
                EconomyUtils.withdraw(player, amount);
            }
        } catch (Exception e) {
            com.fabian.xcommands.utils.LoggerUtils.warn("Invalid parameters for action [TAKE_MONEY]: " + params);
        }
    }

    @Override
    public String getTag() {
        return "TAKE_MONEY";
    }
}
