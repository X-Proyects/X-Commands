package com.fabian.xcommands.actions;

import com.fabian.xcommands.XCommands;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import java.util.Map;

/**
 * Sends a player to another server (Proxy)
 * Format: [SEND_TO] server
 */
public class SendToAction implements Action {

    @Override
    public void execute(Player player, Map<String, Object> context) {
        String params = (String) context.get("params");
        if (player == null || params == null || params.isEmpty()) return;

        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(params.trim());
            com.fabian.xcommands.utils.LoggerUtils.debug("Attempting to send player " + player.getName() + " to server (SEND_TO): " + params.trim());
            player.sendPluginMessage(XCommands.getInstance(), "BungeeCord", out.toByteArray());
        } catch (Exception e) {
            com.fabian.xcommands.utils.LoggerUtils.severe("Failed to send player to proxy server: " + params, e);
        }
    }

    @Override
    public String getTag() {
        return "SEND_TO";
    }
}
