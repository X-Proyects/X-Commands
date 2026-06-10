package com.fabian.xcommands.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import me.clip.placeholderapi.PlaceholderAPI;

/**
 * Utility class for replacing placeholders in messages
 */
public class PlaceholderUtils {

    /**
     * Replaces placeholders in a message
     * Supports PlaceholderAPI and internal placeholders
     * 
     * @param message The message containing placeholders
     * @param player  The player to use for placeholder replacement
     * @return The message with replaced placeholders
     */
    public static String replacePlaceholders(String message, Player player) {
        if (message == null) {
            return "";
        }

        // Hook into PlaceholderAPI if available
        if (player != null && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            message = PlaceholderAPI.setPlaceholders(player, message);
        }

        // Internal replacements (Fallback or additional)
        if (player != null) {
            message = message.replace("%player%", player.getName());
            message = message.replace("%player_display%", CompatibilityUtils.getPlayerDisplayName(player));
            message = message.replace("%player_uuid%", player.getUniqueId().toString());
            message = message.replace("%player_world%", player.getWorld().getName());
            message = message.replace("%player_gamemode%", player.getGameMode().name());
            message = message.replace("%player_health%", String.format("%.1f", player.getHealth()));
            message = message.replace("%player_food%", String.valueOf(player.getFoodLevel()));
            
            if (EconomyUtils.isEnabled()) {
                message = message.replace("%player_money%", String.format("%.2f", EconomyUtils.getBalance(player)));
            } else {
                message = message.replace("%player_money%", "0.00");
            }
        }

        return message;
    }

    /**
     * Replaces argument placeholders in a message
     * 
     * @param message The message containing placeholders
     * @param args    The command arguments
     * @return The message with replaced placeholders
     */
    public static String replaceArgs(String message, String[] args) {
        if (message == null) return "";
        if (args == null || args.length == 0) {
            // Remove placeholders if no args provided
            return message.replace("{args}", "").replaceAll("\\{\\d+\\}", "");
        }

        String fullArgs = String.join(" ", args);
        message = message.replace("{args}", fullArgs);

        for (int i = 0; i < args.length; i++) {
            message = message.replace("{" + i + "}", args[i]);
        }

        // Remove any remaining numeric placeholders that weren't provided
        message = message.replaceAll("\\{\\d+\\}", "");

        return message;
    }

    /**
     * Replaces placeholders and translates colors in a message
     * 
     * @param message The message to process
     * @param player  The player to use for placeholder replacement
     * @return The processed message
     */
    public static String process(String message, Player player) {
        message = replacePlaceholders(message, player);
        return ColorUtils.translate(message);
    }
}
