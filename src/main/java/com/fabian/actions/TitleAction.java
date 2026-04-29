package com.fabian.actions;

import com.fabian.utils.ColorUtils;
import com.fabian.utils.PlaceholderUtils;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.Ticks;
import org.bukkit.entity.Player;
import java.util.Map;

/**
 * Sends a title to the player
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

            String titleStr    = parts.length > 0 ? ColorUtils.translate(PlaceholderUtils.process(parts[0], player)) : "";
            String subtitleStr = parts.length > 1 ? ColorUtils.translate(PlaceholderUtils.process(parts[1], player)) : "";
            int fadeIn  = parts.length > 2 ? Integer.parseInt(parts[2].trim()) : 10;
            int stay    = parts.length > 3 ? Integer.parseInt(parts[3].trim()) : 70;
            int fadeOut = parts.length > 4 ? Integer.parseInt(parts[4].trim()) : 20;

            LegacyComponentSerializer ser = LegacyComponentSerializer.legacySection();
            Title title = Title.title(
                    ser.deserialize(titleStr),
                    ser.deserialize(subtitleStr),
                    Title.Times.times(
                            Ticks.duration(fadeIn),
                            Ticks.duration(stay),
                            Ticks.duration(fadeOut)
                    )
            );
            player.showTitle(title);
        } catch (Exception ignored) {
        }
    }

    @Override
    public String getTag() {
        return "TITLE";
    }
}
