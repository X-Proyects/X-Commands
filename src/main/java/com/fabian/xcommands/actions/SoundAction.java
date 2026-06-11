package com.fabian.xcommands.actions;

import com.fabian.xcommands.XCommands;
import org.bukkit.entity.Player;
import java.util.Map;

/**
 * Plays a sound to the player
 * Format: [SOUND] sound;volume;pitch
 */
public class SoundAction implements Action {

    @Override
    public void execute(Player player, Map<String, Object> context) {
        if (player == null) return;

        String params = (String) context.get("params");
        if (params == null) return;

        try {
            String[] parts = params.split(";");
            String soundName = parts[0].trim();
            float volume = parts.length > 1 ? Float.parseFloat(parts[1]) : 1.0f;
            float pitch = parts.length > 2 ? Float.parseFloat(parts[2]) : 1.0f;
            volume = Math.max(0f, Math.min(1f, volume));
            pitch = Math.max(0.1f, Math.min(2f, pitch));

            com.fabian.xcommands.utils.SoundUtils.playSound(player, soundName, volume, pitch);
        } catch (Exception e) {
            XCommands.getInstance().logWarning("Invalid parameters for action [SOUND]: " + params);
        }
    }

    @Override
    public String getTag() {
        return "SOUND";
    }
}
