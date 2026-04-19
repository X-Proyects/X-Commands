package com.fabian.utils;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for safe sound handling with version compatibility
 */
public class SoundUtils {

    private static final Object NULL_MARKER = new Object();
    private static final Map<String, String> LEGACY_MAP = new HashMap<>();
    private static final Map<String, Object> SOUND_CACHE = new ConcurrentHashMap<>();

    static {
        // Map common 1.8 sounds to modern 1.9+ names
        LEGACY_MAP.put("ENDERMAN_TELEPORT", "ENTITY_ENDERMAN_TELEPORT");
        LEGACY_MAP.put("LEVEL_UP", "ENTITY_PLAYER_LEVELUP");
        LEGACY_MAP.put("ORB_PICKUP", "ENTITY_EXPERIENCE_ORB_PICKUP");
        LEGACY_MAP.put("NOTE_PLING", "BLOCK_NOTE_BLOCK_PLING");
        LEGACY_MAP.put("NOTE_PIANO", "BLOCK_NOTE_BLOCK_HARP");
        LEGACY_MAP.put("FIREWORK_LAUNCH", "ENTITY_FIREWORK_ROCKET_LAUNCH");
        LEGACY_MAP.put("FIREWORK_TWINKLE", "ENTITY_FIREWORK_ROCKET_TWINKLE");
        LEGACY_MAP.put("EXPLODE", "ENTITY_GENERIC_EXPLODE");
        LEGACY_MAP.put("WITHER_SPAWN", "ENTITY_WITHER_SPAWN");
        LEGACY_MAP.put("CHICKEN_EGG_POP", "ENTITY_CHICKEN_EGG");
        LEGACY_MAP.put("WOOD_CLICK", "BLOCK_WOODEN_BUTTON_CLICK_ON");
        LEGACY_MAP.put("STEP_GRASS", "BLOCK_GRASS_STEP");
    }

    /**
     * Safely plays a sound to a player
     */
    public static void playSound(Player player, String soundName, float volume, float pitch) {
        if (player == null || soundName == null || soundName.isEmpty()) {
            return;
        }

        Sound sound = resolveSound(soundName);
        if (sound != null) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        } else {
            // Fallback for custom sounds or newer sounds not in Enum
            try {
                // String-based playSound is useful for 1.18+ custom sounds/resource packs
                String cleanName = soundName.toLowerCase(Locale.ROOT).replace(" ", "_");
                player.playSound(player.getLocation(), cleanName, volume, pitch);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Safely plays a sound at a location
     */
    public static void playSound(Location location, String soundName, float volume, float pitch) {
        if (location == null || soundName == null || soundName.isEmpty()) {
            return;
        }

        Sound sound = resolveSound(soundName);
        if (sound != null) {
            location.getWorld().playSound(location, sound, volume, pitch);
        } else {
            try {
                String cleanName = soundName.toLowerCase(Locale.ROOT).replace(" ", "_");
                location.getWorld().playSound(location, cleanName, volume, pitch);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Resolves a sound name by trying the original name and its modern/legacy
     * counterparts. Uses a cache for performance.
     */
    @SuppressWarnings("deprecation")
    public static Sound resolveSound(String soundName) {
        if (soundName == null || soundName.isEmpty())
            return null;

        String key = soundName.toUpperCase(Locale.ROOT).replace(".", "_").replace(" ", "_");

        // Check cache first
        Object cached = SOUND_CACHE.get(key);
        if (cached != null) {
            return cached == NULL_MARKER ? null : (Sound) cached;
        }

        Sound sound = null;

        // 1. Try the name as provided (Modern)
        try {
            sound = Sound.valueOf(key);
        } catch (IllegalArgumentException ignored) {
        }

        // 2. Try the mapped name (Legacy to Modern)
        if (sound == null) {
            String mapped = LEGACY_MAP.get(key);
            if (mapped != null) {
                try {
                    sound = Sound.valueOf(mapped);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        // 3. Try fallback fix (stripping namespaced key prefix if it failed above)
        if (sound == null && key.contains(":")) {
            try {
                String stripped = key.split(":")[1];
                sound = Sound.valueOf(stripped);
            } catch (Exception ignored) {
            }
        }

        // Cache the result (even if null to avoid re-resolution)
        SOUND_CACHE.put(key, sound != null ? sound : NULL_MARKER);

        return sound;
    }
}
