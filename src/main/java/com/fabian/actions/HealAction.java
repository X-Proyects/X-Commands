package com.fabian.actions;

import org.bukkit.entity.Player;
import java.util.Map;

/**
 * Heals the player to full health
 * Format: [HEAL]
 */
public class HealAction implements Action {

    private static final double DEFAULT_MAX_HEALTH = 20.0;

    @Override
    public void execute(Player player, Map<String, Object> context) {
        if (player == null) return;

        String params = (String) context.get("params");
        Double amount = null;

        if (params != null && !params.trim().isEmpty()) {
            try {
                amount = Double.parseDouble(params.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        if (amount != null) {
            if (Double.isNaN(amount) || Double.isInfinite(amount)) {
                com.fabian.utils.LoggerUtils.warn("Invalid heal value (NaN/Infinity). Action skipped.");
                return;
            }
            amount = Math.max(0, amount);
        }

        Object attribute = null;

        // 1. Try modern Registry (1.20.6+) — with safe fallback for pre-1.20.6
        try {
            org.bukkit.NamespacedKey modernKey = org.bukkit.NamespacedKey.minecraft("max_health");
            attribute = org.bukkit.Registry.ATTRIBUTE.get(modernKey);
        } catch (NoSuchFieldError | NoClassDefFoundError e) {
            // Pre-1.20.6: Registry.ATTRIBUTE does not exist
        }

        // 2. Try legacy name if modern name not found (1.18.2 - 1.21.1)
        if (attribute == null) {
            try {
                org.bukkit.NamespacedKey legacyKey = org.bukkit.NamespacedKey.minecraft("generic_max_health");
                attribute = org.bukkit.Registry.ATTRIBUTE.get(legacyKey);
            } catch (NoSuchFieldError | NoClassDefFoundError ignored) {
                // Pre-1.20.6: fall through to deprecated Attribute.valueOf
            }
        }

        // 3. Last resort: deprecated Attribute.valueOf (works on all versions)
        if (attribute == null) {
            try {
                attribute = org.bukkit.attribute.Attribute.valueOf("GENERIC_MAX_HEALTH");
            } catch (Exception ignored) {
            }
        }

        double maxHealth = DEFAULT_MAX_HEALTH;
        if (attribute != null) {
            try {
                org.bukkit.attribute.AttributeInstance instance = player.getAttribute((org.bukkit.attribute.Attribute) attribute);
                if (instance != null) {
                    maxHealth = instance.getValue();
                }
            } catch (ClassCastException ignored) {
            }
        }

        if (amount != null) {
            double newHealth = Math.min(maxHealth, player.getHealth() + amount);
            player.setHealth(newHealth);
        } else {
            player.setHealth(maxHealth);
        }
    }

    @Override
    public String getTag() {
        return "HEAL";
    }
}
