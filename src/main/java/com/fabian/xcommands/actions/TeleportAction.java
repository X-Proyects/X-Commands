package com.fabian.xcommands.actions;

import com.fabian.xcommands.XCommands;
import com.fabian.xcommands.utils.DebugLogger;
import com.fabian.xcommands.utils.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import java.util.Map;

/**
 * Teleports the player to a location
 * Format: [TELEPORT] world;x;y;z;yaw;pitch
 */
public class TeleportAction implements Action {

    @Override
    public void execute(Player player, Map<String, Object> context) {
        if (player == null) return;

        String params = (String) context.get("params");
        if (params == null) return;

        try {
            String[] parts = params.split(";");
            if (parts.length < 4) return;

            World world = Bukkit.getWorld(parts[0]);
            if (world == null) return;

            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            float yaw = parts.length > 4 ? Float.parseFloat(parts[4]) : 0;
            float pitch = parts.length > 5 ? Float.parseFloat(parts[5]) : 0;

            Location loc = new Location(world, x, y, z, yaw, pitch);
            
            // Safe asynchronous teleport for Folia and better performance
            DebugLogger.debug("[TELEPORT] " + (player != null ? player.getName() : "null") + " -> " + world.getName() + " " + x + "," + y + "," + z);
            SchedulerUtil.teleportAsync(player, loc);
        } catch (Exception e) {
            XCommands.getInstance().logWarning("Invalid parameters for action [TELEPORT]: " + params);
        }
    }

    @Override
    public String getTag() {
        return "TELEPORT";
    }
}
