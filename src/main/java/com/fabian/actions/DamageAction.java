package com.fabian.actions;

import org.bukkit.entity.Player;
import java.util.Map;

/**
 * Damages the player
 * Format: [DAMAGE] amount
 */
public class DamageAction implements Action {

    @Override
    public void execute(Player player, Map<String, Object> context) {
        if (player == null) return;

        String params = (String) context.get("params");
        if (params == null) return;

        try {
            double damage = Double.parseDouble(params.trim());
            player.damage(damage);
        } catch (Exception ignored) {
        }
    }

    @Override
    public String getTag() {
        return "DAMAGE";
    }
}
