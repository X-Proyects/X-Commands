package com.fabian.xcommands.actions;

import com.fabian.xcommands.XCommands;
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
            if (Double.isNaN(damage) || Double.isInfinite(damage) || damage < 0) {
                XCommands.getInstance().logWarning("Invalid damage value for [DAMAGE]: " + damage + " (must be a positive finite number). Action skipped.");
                return;
            }
            player.damage(damage);
        } catch (Exception e) {
            XCommands.getInstance().logWarning("Invalid parameters for action [DAMAGE]: " + params);
        }
    }

    @Override
    public String getTag() {
        return "DAMAGE";
    }
}
