package com.fabian.xcommands.managers;

import com.fabian.xcommands.XCommands;
import com.fabian.xcommands.utils.LoggerUtils;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages per-player and per-command cooldowns
 */
public class CooldownManager {

    // Map<CommandKey, Map<PlayerUUID, ExpiryTimeMillis>>
    private final Map<String, Map<UUID, Long>> cooldowns;

    public CooldownManager(XCommands plugin) {
        this.cooldowns = new ConcurrentHashMap<>();
    }

    /**
     * Sets a cooldown for a player on a specific action/command
     * @param uuid The player UUID
     * @param key The cooldown key (e.g., command name)
     * @param seconds Duration in seconds
     */
    public void setCooldown(UUID uuid, String key, double seconds) {
        long expiry = System.currentTimeMillis() + (long) (seconds * 1000);
        cooldowns.computeIfAbsent(key.toLowerCase(), k -> new ConcurrentHashMap<>()).put(uuid, expiry);
        LoggerUtils.debug("Cooldown set: " + key + " for " + uuid + " (" + seconds + "s)");
    }

    /**
     * Gets the remaining time in seconds for a player's cooldown
     * @return Remaining seconds, or 0 if no active cooldown
     */
    public double getRemaining(UUID uuid, String key) {
        Map<UUID, Long> playerCooldowns = cooldowns.get(key.toLowerCase());
        if (playerCooldowns == null) return 0;

        Long expiry = playerCooldowns.get(uuid);
        if (expiry == null) return 0;

        long remaining = expiry - System.currentTimeMillis();
        if (remaining <= 0) {
            playerCooldowns.remove(uuid);
            return 0;
        }

        return remaining / 1000.0;
    }

    /**
     * Checks if a player is on cooldown
     */
    public boolean isOnCooldown(UUID uuid, String key) {
        boolean onCooldown = getRemaining(uuid, key) > 0;
        if (onCooldown) {
            LoggerUtils.debug("Cooldown active: " + key + " for " + uuid + " (" + getRemaining(uuid, key) + "s remaining)");
        }
        return onCooldown;
    }

    /**
     * Clears all cooldowns for a specific key
     */
    public void clearCooldowns(String key) {
        cooldowns.remove(key.toLowerCase());
        LoggerUtils.debug("Cooldowns cleared for key: " + key);
    }

    /**
     * Clears all cooldowns for a player
     */
    public void clearPlayerCooldowns(UUID uuid) {
        cooldowns.values().forEach(map -> map.remove(uuid));
    }

    /**
     * Reloads the cooldown manager (clears all)
     */
    public void reload() {
        LoggerUtils.debug("Reloading cooldown manager (clearing all cooldowns)...");
        cooldowns.clear();
    }
}
