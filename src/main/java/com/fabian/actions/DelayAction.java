package com.fabian.actions;

import org.bukkit.entity.Player;
import java.util.Map;

/**
 * Delays the next action
 * Format: [DELAY] ticks
 * Note: This action is handled specially by ActionManager
 */
public class DelayAction implements Action {

    private int delayTicks;

    @Override
    public void execute(Player player, Map<String, Object> context) {
        String params = (String) context.get("params");
        if (params == null) return;

        try {
            delayTicks = Integer.parseInt(params.trim());
        } catch (Exception e) {
            com.fabian.utils.LoggerUtils.warn("Invalid parameters for action [DELAY]: " + params);
        }
    }

    public int getDelayTicks() {
        return delayTicks;
    }

    @Override
    public String getTag() {
        return "DELAY";
    }
}
