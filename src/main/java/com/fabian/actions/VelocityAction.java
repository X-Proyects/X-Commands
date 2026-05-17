package com.fabian.actions;

import com.fabian.XCommands;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import java.util.Map;

/**
 * Sends a player to another server (Velocity/Bungee)
 * Format: [VELOCITY] server
 */
public class VelocityAction implements Action {

    @Override
    public void execute(Player player, Map<String, Object> context) {
        String params = (String) context.get("params");
        if (player == null || params == null || params.isEmpty()) return;

        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(params.trim());
            com.fabian.utils.LoggerUtils.debug("Attempting to send player " + player.getName() + " to server (Velocity): " + params.trim());
            player.sendPluginMessage(XCommands.getInstance(), "BungeeCord", out.toByteArray());
        } catch (Exception e) {
            com.fabian.utils.LoggerUtils.severe("Failed to send player to velocity server: " + params, e);
        }
    }

    @Override
    public String getTag() {
        return "VELOCITY";
    }
}
