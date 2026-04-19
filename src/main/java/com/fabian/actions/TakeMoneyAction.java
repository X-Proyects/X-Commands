package com.fabian.actions;

import com.fabian.utils.EconomyUtils;
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
            if (EconomyUtils.has(player, amount)) {
                EconomyUtils.withdraw(player, amount);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public String getTag() {
        return "TAKE_MONEY";
    }
}
