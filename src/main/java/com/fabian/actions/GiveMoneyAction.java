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
            EconomyUtils.deposit(player, amount);
        } catch (Exception ignored) {
        }
    }

    @Override
    public String getTag() {
        return "GIVE_MONEY";
    }
}
