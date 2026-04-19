package com.fabian.actions;

import org.bukkit.entity.Player;
import java.util.Map;

/**
 * Feeds the player to full food level
 * Format: [FEED]
 */
public class FeedAction implements Action {

    @Override
    public void execute(Player player, Map<String, Object> context) {
        if (player == null) return;

        player.setFoodLevel(20);
        player.setSaturation(20.0f);
    }

    @Override
    public String getTag() {
        return "FEED";
    }
}
