package com.fabian.actions;

import com.fabian.utils.PlaceholderUtils;
import org.bukkit.entity.Player;
import java.util.Map;

/**
 * Executes a command as the player
 * Format: [PLAYER] command
 */
public class PlayerAction implements Action {

    @Override
    public void execute(Player player, Map<String, Object> context) {
        if (player == null) return;
        
        String params = (String) context.get("params");
        if (params == null) return;
        String command = PlaceholderUtils.process(params, player);
        if (command == null || command.trim().isEmpty()) return;
        
        player.performCommand(command);
    }

    @Override
    public String getTag() {
        return "PLAYER";
    }
}
