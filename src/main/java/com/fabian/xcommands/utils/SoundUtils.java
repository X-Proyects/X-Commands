package com.fabian.xcommands.utils;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for safe sound handling with version compatibility.
 * Supports all server versions from 1.8.8+ through 1.21+.
 */
public class SoundUtils {

    private static final Object NULL_MARKER = new Object();
    private static final Map<String, String> LEGACY_MAP = new HashMap<>();
    private static final Map<String, Object> SOUND_CACHE = new ConcurrentHashMap<>();

    // Cached reflection for Registry.SOUNDS (1.20.5+)
    private static volatile Object soundsRegistry = null;
    private static volatile Method registryGetMethod = null;
    private static volatile Boolean registryAvailable = null;

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
            // Fallback for custom sounds or newer sounds not in Registry
            try {
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
     * Resolves a sound name using the Paper Registry API (1.20.5+) via reflection.
     * Falls back through legacy name mappings and Sound.valueOf().
     * Uses a cache for performance.
     */
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

        // 1. Try Registry lookup with the key as provided (modern, non-deprecated)
        sound = tryRegistryLookup(key);

        // 2. Try mapped legacy name
        if (sound == null) {
            String mapped = LEGACY_MAP.get(key);
            if (mapped != null) {
                sound = tryRegistryLookup(mapped);
            }
        }

        // 3. Try stripping namespace prefix (e.g. "minecraft:entity_player_levelup")
        if (sound == null && key.contains(":")) {
            sound = tryRegistryLookup(key.split(":")[1]);
        }

        // 4. Last resort: try Sound.valueOf() (works on all versions, for enum-style names)
        if (sound == null) {
            try {
                sound = Sound.valueOf(key);
            } catch (IllegalArgumentException ignored) {
            }
        }

        // Cache result
        SOUND_CACHE.put(key, sound != null ? sound : NULL_MARKER);
        return sound;
    }

    /**
     * Looks up a Sound from the Bukkit Registry by name key using reflection.
     * Converts ENUM_STYLE_NAME to minecraft:enum_style_name format.
     * Uses cached reflection for Registry.SOUNDS (1.20.5+) with Sound.valueOf() fallback.
     */
    private static Sound tryRegistryLookup(String enumKey) {
        // Lazy-init reflection for Registry.SOUNDS (1.20.5+)
        if (registryAvailable == null) {
            try {
                soundsRegistry = org.bukkit.Registry.class.getField("SOUNDS").get(null);
                registryGetMethod = soundsRegistry.getClass().getMethod("get", NamespacedKey.class);
                registryAvailable = true;
            } catch (Exception e) {
                soundsRegistry = null;
                registryGetMethod = null;
                registryAvailable = false;
                LoggerUtils.debug("SoundUtils: Registry.SOUNDS not available (pre-1.20.5), using Sound.valueOf() fallback");
                return null;
            }
        }

        if (!registryAvailable || soundsRegistry == null || registryGetMethod == null) {
            return null;
        }

        // Registry keys are lowercase with dots, e.g. "entity.player.levelup"
        String registryKey = enumKey.toLowerCase(Locale.ROOT).replace("_", ".");
        try {
            Sound s = (Sound) registryGetMethod.invoke(soundsRegistry, NamespacedKey.minecraft(registryKey));
            if (s != null) return s;
        } catch (Exception ignored) {
        }

        // Also try with underscores (some keys use them)
        try {
            String underscoreKey = enumKey.toLowerCase(Locale.ROOT);
            return (Sound) registryGetMethod.invoke(soundsRegistry, NamespacedKey.minecraft(underscoreKey));
        } catch (Exception ignored) {
        }

        return null;
    }
}