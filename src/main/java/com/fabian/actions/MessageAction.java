package com.fabian.actions;

import com.fabian.utils.PlaceholderUtils;
import org.bukkit.entity.Player;
import java.util.Map;

/**
 * Sends a message to the player
 * Format: [MESSAGE] text
 */
public class MessageAction implements Action {

    @Override
    public void execute(Player player, Map<String, Object> context) {
        if (player == null) return;
        
        String params = (String) context.get("params");
        if (params == null) return;

        player.sendMessage(PlaceholderUtils.process(params, player));
    }

    @Override
    public String getTag() {
        return "MESSAGE";
    }
}
