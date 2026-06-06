package com.fabian.actions;

import com.fabian.utils.EconomyUtils;
import org.bukkit.entity.Player;
import java.util.Map;

/**
 * Gives money to the player using Vault
 * Format: [GIVE_MONEY] 100
 */
public class GiveMoneyAction implements Action {

    @Override
    public void execute(Player player, Map<String, Object> context) {
        if (player == null || !EconomyUtils.isEnabled()) return;

        String params = (String) context.get("params");
        if (params == null) return;

        try {
            double amount = Double.parseDouble(params.trim());

            // Prevent negative amounts that could withdraw money instead of depositing
            if (amount <= 0) {
                com.fabian.utils.LoggerUtils.warn("Invalid amount for action [GIVE_MONEY]: " + amount + " (must be positive). Action skipped.");
                return;
            }

            EconomyUtils.deposit(player, amount);
        } catch (Exception e) {
            com.fabian.utils.LoggerUtils.warn("Invalid parameters for action [GIVE_MONEY]: " + params);
        }
    }

    @Override
    public String getTag() {
        return "GIVE_MONEY";
    }
}
