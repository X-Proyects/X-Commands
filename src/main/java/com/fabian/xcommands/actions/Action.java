package com.fabian.xcommands.actions;

import org.bukkit.entity.Player;
import java.util.Map;

/**
 * Represents an action that can be executed by a command
 */
public interface Action {

    /**
     * Executes the action
     * 
     * @param player  The player executing the command (null if console)
     * @param context The dynamic context for the action
     */
    void execute(Player player, Map<String, Object> context);

    /**
     * Gets the action tag (e.g., "MESSAGE", "BROADCAST")
     * 
     * @return The action tag
     */
    String getTag();
}
