package com.fabian.utils;

import org.bukkit.ChatColor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for handling color codes in messages
 */
public class ColorUtils {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    /**
     * Translates color codes in a message
     * Supports both legacy codes (&a, &b, etc.) and hex colors (&#RRGGBB)
     * 
     * @param message The message to translate
     * @return The translated message with color codes
     */
    public static String translate(String message) {
        if (message == null) {
            return "";
        }

        // Translate hex colors for 1.16+
        message = translateHexColors(message);

        // Translate legacy color codes
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Translates hex color codes (&#RRGGBB) to Minecraft format
     * 
     * @param message The message containing hex colors
     * @return The message with translated hex colors
     */
    private static String translateHexColors(String message) {
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder buffer = new StringBuilder(message.length() + 32);

        while (matcher.find()) {
            String hexCode = matcher.group(1);
            // Optimized replacement avoiding inner StringBuilder
            matcher.appendReplacement(buffer, "");
            buffer.append("§x");
            for (int i = 0; i < 6; i++) {
                buffer.append('§').append(hexCode.charAt(i));
            }
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }

    /**
     * Strips all color codes from a message
     * 
     * @param message The message to strip
     * @return The message without color codes
     */
    public static String stripColor(String message) {
        if (message == null) {
            return "";
        }
        return ChatColor.stripColor(translate(message));
    }
}
