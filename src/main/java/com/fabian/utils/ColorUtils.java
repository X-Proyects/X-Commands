package com.fabian.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for handling color codes in messages.
 * Supports:
 *   - Legacy codes:  &amp;a, &amp;b, &amp;l, etc.
 *   - Hex RGB:       &amp;#RRGGBB  (e.g. &amp;#FF5500)
 *   - MiniMessage:   &lt;red&gt;, &lt;#FF5500&gt;, &lt;gradient:red:blue&gt;, etc.
 */
public class ColorUtils {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    // Detects likely MiniMessage tags to avoid the heavier parsing path for plain messages.
    private static final Pattern MINI_MESSAGE_PATTERN =
            Pattern.compile("<[a-zA-Z#!/][^>]*>");

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final LegacyComponentSerializer LEGACY_SERIALIZER =
            LegacyComponentSerializer.builder()
                    .character('§')
                    .hexColors()
                    .useUnusualXRepeatedCharacterHexFormat()
                    .build();

    /**
     * Translates color codes in a message.
     * Supports legacy codes (&amp;a, &amp;b…), hex (&amp;#RRGGBB), and MiniMessage tags.
     *
     * @param message The message to translate
     * @return The translated message with color codes applied
     */
    public static String translate(String message) {
        if (message == null) {
            return "";
        }

        // Fast path: if there are no '<' characters, use the lightweight legacy pipeline.
        if (!MINI_MESSAGE_PATTERN.matcher(message).find()) {
            return translateLegacy(message);
        }

        // Slow path: MiniMessage detected.
        // First convert &#RRGGBB → <#RRGGBB> so MiniMessage understands them,
        // and convert legacy &x codes to <color_name> equivalents.
        // The simplest bridge: convert &<legacy> to their MiniMessage tag form.
        String preprocessed = convertLegacyToMiniMessage(message);

        Component component = MINI_MESSAGE.deserialize(preprocessed);
        return LEGACY_SERIALIZER.serialize(component);
    }

    /**
     * Translates legacy &amp; codes and &amp;#RRGGBB hex only (no MiniMessage parsing).
     */
    @SuppressWarnings("deprecation")
    private static String translateLegacy(String message) {
        message = translateHexColors(message);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Converts &amp;#RRGGBB hex codes and &amp;[0-9a-fk-or] legacy codes
     * into their MiniMessage equivalents so they can be mixed with MiniMessage tags.
     */
    private static String convertLegacyToMiniMessage(String message) {
        // Convert &#RRGGBB → <#RRGGBB>
        Matcher hexMatcher = HEX_PATTERN.matcher(message);
        StringBuffer hexBuffer = new StringBuffer();
        while (hexMatcher.find()) {
            hexMatcher.appendReplacement(hexBuffer, "<#" + hexMatcher.group(1) + ">");
        }
        hexMatcher.appendTail(hexBuffer);
        message = hexBuffer.toString();

        // Convert &<code> legacy codes → MiniMessage equivalents
        StringBuilder sb = new StringBuilder(message.length() + 16);
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if ((c == '&') && i + 1 < message.length()) {
                char next = message.charAt(i + 1);
                String tag = legacyCodeToMiniMessage(next);
                if (tag != null) {
                    sb.append(tag);
                    i++; // skip the code character
                    continue;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Maps a single legacy color code character to its MiniMessage tag.
     * Returns null if not a known code.
     */
    private static String legacyCodeToMiniMessage(char code) {
        switch (Character.toLowerCase(code)) {
            case '0': return "<black>";
            case '1': return "<dark_blue>";
            case '2': return "<dark_green>";
            case '3': return "<dark_aqua>";
            case '4': return "<dark_red>";
            case '5': return "<dark_purple>";
            case '6': return "<gold>";
            case '7': return "<gray>";
            case '8': return "<dark_gray>";
            case '9': return "<blue>";
            case 'a': return "<green>";
            case 'b': return "<aqua>";
            case 'c': return "<red>";
            case 'd': return "<light_purple>";
            case 'e': return "<yellow>";
            case 'f': return "<white>";
            case 'k': return "<obfuscated>";
            case 'l': return "<bold>";
            case 'm': return "<strikethrough>";
            case 'n': return "<underlined>";
            case 'o': return "<italic>";
            case 'r': return "<reset>";
            default:  return null;
        }
    }

    /**
     * Translates hex color codes (&amp;#RRGGBB) to Minecraft legacy format.
     * Used in the legacy-only fast path.
     */
    private static String translateHexColors(String message) {
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder buffer = new StringBuilder(message.length() + 32);
        while (matcher.find()) {
            String hexCode = matcher.group(1);
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
     * Strips all color codes from a message.
     *
     * @param message The message to strip
     * @return The message without color codes
     */
    @SuppressWarnings("deprecation")
    public static String stripColor(String message) {
        if (message == null) {
            return "";
        }
        return ChatColor.stripColor(translate(message));
    }
}
