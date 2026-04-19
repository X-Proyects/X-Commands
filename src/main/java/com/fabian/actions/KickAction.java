package com.fabian.actions;

import com.fabian.utils.PlaceholderUtils;
import org.bukkit.entity.Player;
import java.util.Map;

/**
 * Kicks the player from the server
 * Format: [KICK] reason
 */
public class KickAction implements Action {

    @Override
    public void execute(Player player, Map<String, Object> context) {
        if (player == null) return;

        String params = (String) context.get("params");
        if (params == null) return;

        player.kickPlayer(PlaceholderUtils.process(params, player));
    }

    @Override
    public String getTag() {
        return "KICK";
    }
}
