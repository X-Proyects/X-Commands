package com.fabian.xcommands.actions;

import com.fabian.xcommands.utils.PlaceholderUtils;
import com.fabian.xcommands.utils.LoggerUtils;
import org.bukkit.entity.Player;
import java.util.Map;

/**
 * Executes a command as console
 * Format: [CONSOLE] command
 */
public class ConsoleAction implements Action {

    @Override
    public void execute(Player player, Map<String, Object> context) {
        String params = (String) context.get("params");
        if (params == null) return;

        String command = PlaceholderUtils.process(params, player);
        if (command == null || command.trim().isEmpty()) return;

        command = sanitizeCommand(command);

        LoggerUtils.debug("[CONSOLE] Dispatching: " + command);
        org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), command);
    }

    /**
     * Sanitizes the command string to prevent injection attacks.
     * Strips newlines, semicolons, and other command separators that could
     * allow execution of unintended commands via placeholder expansion.
     */
    private String sanitizeCommand(String command) {
        // Remove newlines and carriage returns that could chain commands
        command = command.replace("\n", " ").replace("\r", " ");

        // Remove semicolons that could be used as command separators
        command = command.replace(";", " ");

        // Remove null bytes and control characters
        command = command.replace("\0", "");
        command = command.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");

        // Trim to prevent excessively long commands
        if (command.length() > 1024) {
            String original = command;
            command = command.substring(0, 1024);
            LoggerUtils.warn("Console command truncated to 1024 chars. Original length: " + original.length());
        }

        return command.trim();
    }

    @Override
    public String getTag() {
        return "CONSOLE";
    }
}
