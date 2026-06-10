package com.fabian.xcommands.actions;

import org.bukkit.entity.Player;
import java.util.Map;

/**
 * Feeds the player to full food level
 * Format: [FEED]
 */
public class FeedAction implements Action {

    private static final int MAX_FOOD_LEVEL = 20;

    @Override
    public void execute(Player player, Map<String, Object> context) {
        if (player == null) return;

        String params = (String) context.get("params");
        Integer amount = null;

        if (params != null && !params.trim().isEmpty()) {
            try {
                amount = Integer.parseInt(params.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        if (amount != null) {
            player.setFoodLevel(Math.min(MAX_FOOD_LEVEL, player.getFoodLevel() + amount));
        } else {
            player.setFoodLevel(MAX_FOOD_LEVEL);
            player.setSaturation(20.0f);
        }
    }

    @Override
    public String getTag() {
        return "FEED";
    }
}
