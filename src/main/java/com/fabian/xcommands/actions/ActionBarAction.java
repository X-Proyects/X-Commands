package com.fabian.xcommands.actions;

import com.fabian.xcommands.utils.CompatibilityUtils;
import com.fabian.xcommands.utils.PlaceholderUtils;
import org.bukkit.entity.Player;
import java.util.Map;

/**
 * Sends an action bar message to the player
 * Format: [ACTIONBAR] text
 */
public class ActionBarAction implements Action {

    @Override
    public void execute(Player player, Map<String, Object> context) {
        if (player == null) return;

        String params = (String) context.get("params");
        if (params == null) return;

        String message = PlaceholderUtils.process(params, player);
        CompatibilityUtils.sendActionBar(player, message);
    }

    @Override
    public String getTag() {
        return "ACTIONBAR";
    }
}
