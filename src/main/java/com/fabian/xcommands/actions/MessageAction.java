package com.fabian.xcommands.actions;

import com.fabian.xcommands.utils.CompatibilityUtils;
import com.fabian.xcommands.utils.PlaceholderUtils;
import com.fabian.xcommands.utils.LoggerUtils;
import org.bukkit.entity.Player;
import java.util.Map;

/**
 * Sends a private message to the player
 * Format: [MESSAGE] text
 */
public class MessageAction implements Action {

    @Override
    public void execute(Player player, Map<String, Object> context) {
        if (player == null) return;

        String params = (String) context.get("params");
        if (params == null) return;

        String message = PlaceholderUtils.process(params, player);
        LoggerUtils.debug("[MESSAGE] Sending to " + (player != null ? player.getName() : "null") + ": " + message);
        CompatibilityUtils.sendMessage(player, message);
    }

    @Override
    public String getTag() {
        return "MESSAGE";
    }
}
