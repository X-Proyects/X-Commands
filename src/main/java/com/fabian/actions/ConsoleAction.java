package com.fabian.actions;

import com.fabian.utils.PlaceholderUtils;
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
        org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), command);
    }

    @Override
    public String getTag() {
        return "CONSOLE";
    }
}
