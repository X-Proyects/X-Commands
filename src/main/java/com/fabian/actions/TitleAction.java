package com.fabian.actions;

import com.fabian.utils.CompatibilityUtils;
import com.fabian.utils.PlaceholderUtils;
import org.bukkit.entity.Player;
import java.util.Map;

/**
 * Sends a title and subtitle to the player
 * Format: [TITLE] title;subtitle;fadeIn;stay;fadeOut
 */
public class TitleAction implements Action {

    @Override
    public void execute(Player player, Map<String, Object> context) {
        if (player == null) return;

        String params = (String) context.get("params");
        if (params == null) return;

        String[] parts = params.split(";");

        String title = PlaceholderUtils.process(parts[0], player);
        String subtitle = parts.length > 1 ? PlaceholderUtils.process(parts[1], player) : "";
        
        int fadeIn = parts.length > 2 ? Integer.parseInt(parts[2].trim()) : 10;
        int stay = parts.length > 3 ? Integer.parseInt(parts[3].trim()) : 70;
        int fadeOut = parts.length > 4 ? Integer.parseInt(parts[4].trim()) : 20;

        CompatibilityUtils.sendTitle(player, title, subtitle, fadeIn, stay, fadeOut);
    }

    @Override
    public String getTag() {
        return "TITLE";
    }
}
