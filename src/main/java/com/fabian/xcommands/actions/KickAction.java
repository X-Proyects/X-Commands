package com.fabian.xcommands.actions;

import com.fabian.xcommands.utils.ColorUtils;
import com.fabian.xcommands.utils.PlaceholderUtils;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import java.util.Map;

/**
 * Kicks the player from the server
 * Format: [KICK] reason
 */
public class KickAction implements Action {

    @Override
    public void execute(Player player, Map<String, Object> context) {
        if (player == null) return;

        String params = (String) context.get("params");
        if (params == null) return;

        String reason = PlaceholderUtils.process(params, player);
        player.kick(LegacyComponentSerializer.legacySection().deserialize(ColorUtils.stripColor(reason)));
    }

    @Override
    public String getTag() {
        return "KICK";
    }
}
