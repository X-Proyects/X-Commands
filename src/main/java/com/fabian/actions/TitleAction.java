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

        try {
            String[] parts = params.split(";");

            String title = parts[0].trim();
            if (!title.isEmpty()) {
                title = PlaceholderUtils.process(title, player);
            }
            String subtitle = parts.length > 1 ? PlaceholderUtils.process(parts[1].trim(), player) : "";

            int fadeIn = parts.length > 2 ? Integer.parseInt(parts[2].trim()) : 10;
            int stay = parts.length > 3 ? Integer.parseInt(parts[3].trim()) : 70;
            int fadeOut = parts.length > 4 ? Integer.parseInt(parts[4].trim()) : 20;

            // Clamp to non-negative values to prevent unexpected API behavior
            fadeIn = Math.max(0, fadeIn);
            stay = Math.max(0, stay);
            fadeOut = Math.max(0, fadeOut);

            if (title != null) {
                CompatibilityUtils.sendTitle(player, title, subtitle, fadeIn, stay, fadeOut);
            }
        } catch (NumberFormatException e) {
            com.fabian.utils.LoggerUtils.warn("Invalid number format in action [TITLE]: " + params);
        } catch (Exception e) {
            com.fabian.utils.LoggerUtils.warn("Error executing action [TITLE]: " + e.getMessage());
        }
    }

    @Override
    public String getTag() {
        return "TITLE";
    }
}
