package com.fabian.xcommands.actions;

import com.fabian.xcommands.utils.PlaceholderUtils;
import org.bukkit.entity.Player;
import java.util.Map;

/**
 * Broadcasts a message to all online players
 * Format: [BROADCAST] text
 */
public class BroadcastAction implements Action {

    @Override
    public void execute(Player player, Map<String, Object> context) {
        String params = (String) context.get("params");
        if (params == null) return;

        String processed = (player != null) ? PlaceholderUtils.process(params, player) : params;
        com.fabian.xcommands.utils.CompatibilityUtils.broadcast(processed);
    }

    @Override
    public String getTag() {
        return "BROADCAST";
    }
}
