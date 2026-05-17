package com.fabian.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for color translation and component management.
 * Supports MiniMessage, Legacy (&, §) and Hex (&#RRGGBB) formats.
 */
public final class ColorUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final LegacyComponentSerializer LEGACY_SECTION =
            LegacyComponentSerializer.builder()
                    .character('§')
                    .hexColors()
                    .useUnusualXRepeatedCharacterHexFormat()
                    .build();

    private static final PlainTextComponentSerializer PLAIN =
            PlainTextComponentSerializer.plainText();

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private ColorUtils() {}

    /**
     * Translates MiniMessage, legacy (&) or hex formats into § formatted text.
     */
    public static String translate(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        return LEGACY_SECTION.serialize(component(input));
    }

    /**
     * Translates a list of strings into § formatted text.
     */
    public static List<String> translate(List<String> input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> translated = new ArrayList<>(input.size());
        for (String line : input) {
            translated.add(translate(line));
        }
        return translated;
    }

    /**
     * Strips all formatting and returns plain text.
     */
    public static String stripColor(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        return PLAIN.serialize(component(input));
    }

    /**
     * Safely converts mixed format text into an Adventure Component.
     */
    public static Component component(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }

        // Optimization: return plain text if no formatting tags are present
        if (!input.contains("<") && !input.contains("&") && !input.contains("§")) {
            return Component.text(input);
        }

        String processed = input;

        // 1. Normalizar formato hexadecimal heredado &#RRGGBB a <#RRGGBB>
        if (processed.contains("&#")) {
            Matcher matcher = HEX_PATTERN.matcher(processed);
            processed = matcher.replaceAll("<#$1>");
        }

        // 2. Convertir codigos legacy a tags MiniMessage si hay tags MiniMessage
        // Esto permite mezclar gradientes con códigos como &l
        if (processed.contains("<")) {
            String miniMessageString = convertLegacyToMiniMessageTags(processed);
            try {
                return MINI_MESSAGE.deserialize(miniMessageString);
            } catch (Exception e) {
                // Fallback a legacy en caso de error de parseo
            }
        }

        // 3. Normalizar ampersand a section para el parser legacy
        if (processed.contains("&")) {
            processed = translateLegacy(processed);
        }

        return LEGACY_SECTION.deserialize(processed);
    }

    /**
     * Convierte codigos legacy (&a, &l, etc) a tags de MiniMessage (<green>, <bold>, etc).
     */
    private static String convertLegacyToMiniMessageTags(String input) {
        char[] b = input.toCharArray();
        StringBuilder sb = new StringBuilder(b.length + 32);
        for (int i = 0; i < b.length; i++) {
            if ((b[i] == '&' || b[i] == '§') && i < b.length - 1) {
                char next = Character.toLowerCase(b[i + 1]);
                String tag = getMiniMessageTag(next);
                if (tag != null) {
                    sb.append(tag);
                    i++; // Saltamos el codigo de color
                    continue;
                }
            }
            sb.append(b[i]);
        }
        return sb.toString();
    }

    private static String getMiniMessageTag(char c) {
        switch (c) {
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
            default: return null;
        }
    }

    /**
     * Converts a list of strings into a list of Adventure Components.
     */
    public static List<Component> component(List<String> input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptyList();
        }
        List<Component> components = new ArrayList<>(input.size());
        for (String line : input) {
            components.add(component(line));
        }
        return components;
    }

    private static String translateLegacy(String input) {
        char[] b = input.toCharArray();
        for (int i = 0; i < b.length - 1; i++) {
            if (b[i] == '&' && "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx".indexOf(b[i + 1]) > -1) {
                b[i] = '§';
                b[i + 1] = Character.toLowerCase(b[i + 1]);
            }
        }
        return new String(b);
    }
}
