package com.fabian.actions;

import com.fabian.utils.ColorUtils;
import com.fabian.utils.PlaceholderUtils;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import java.util.Map;

/**
 * Sends an action bar message to the player
 * Format: [ACTIONBAR] text
 */
public class ActionBarAction implements Action {

    @Override
    public void execute(Player player, Map<String, Object> context) {
        if (player == null) return;

        String params = (String) context.get("params");
        if (params == null) return;

        String message = ColorUtils.translate(PlaceholderUtils.process(params, player));
        player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(message));
    }

    @Override
    public String getTag() {
        return "ACTIONBAR";
    }
}
