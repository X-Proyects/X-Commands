package com.fabian.actions;

import com.fabian.utils.PlaceholderUtils;
import org.bukkit.Bukkit;
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

        Bukkit.broadcastMessage(PlaceholderUtils.process(params, player));
    }

    @Override
    public String getTag() {
        return "BROADCAST";
    }
}
