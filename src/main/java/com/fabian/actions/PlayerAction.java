package com.fabian.actions;

import com.fabian.utils.PlaceholderUtils;
import com.fabian.utils.LoggerUtils;
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

        command = sanitizeCommand(command);

        player.performCommand(command);
    }

    /**
     * Sanitizes the command string to prevent injection attacks.
     * Strips newlines and semicolons that could allow command chaining
     * through crafted placeholder values.
     */
    private String sanitizeCommand(String command) {
        // Remove newlines and carriage returns that could chain commands
        command = command.replace("\n", " ").replace("\r", " ");

        // Remove semicolons that could be used as command separators
        command = command.replace(";", " ");

        // Trim to prevent excessively long commands
        if (command.length() > 1024) {
            String original = command;
            command = command.substring(0, 1024);
            LoggerUtils.warn("Player command truncated to 1024 chars. Original length: " + original.length());
        }

        return command.trim();
    }

    @Override
    public String getTag() {
        return "PLAYER";
    }
}
