package com.fabian.actions;

import com.fabian.utils.PlaceholderUtils;
import org.bukkit.entity.Player;
import java.util.Map;

/**
 * Broadcasts a message to all online players
 * Format: [BROADCAST] text
 */
public class BroadcastAction implements Action {

    @Override
    public void execute(Player player, Map<String, Object> context) {
        if (player == null) return;

        String params = (String) context.get("params");
        if (params == null) return;

        String processed = PlaceholderUtils.process(params, player);
        com.fabian.utils.CompatibilityUtils.broadcast(processed);
    }

    @Override
    public String getTag() {
        return "BROADCAST";
    }
}
