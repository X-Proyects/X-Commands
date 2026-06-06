package com.fabian.actions;

import org.bukkit.entity.Player;
import java.util.Map;

/**
 * Delays the next action
 * Format: [DELAY] ticks
 * Note: This action is handled specially by ActionManager, not via execute()
 */
public class DelayAction implements Action {

    @Override
    public void execute(Player player, Map<String, Object> context) {
        // Delay is handled directly by ActionManager.executeActionsLoop()
        // This method is never called for DELAY actions
    }

    @Override
    public String getTag() {
        return "DELAY";
    }
}
