package com.fabian.xcommands.actions;

import org.bukkit.entity.Player;
import java.util.Map;
import java.lang.reflect.Method;

/**
 * Heals the player to full health.
 * Format: [HEAL] or [HEAL] <amount>
 *
 * Compatible with all versions from 1.8.8+ through 1.21+:
 * - 1.21+: Uses Registry.ATTRIBUTE via reflection (max_health)
 * - 1.20.6-1.21.1: Uses Registry.ATTRIBUTE via reflection (generic_max_health)
 * - Pre-1.20.6: Uses deprecated Attribute.valueOf("GENERIC_MAX_HEALTH")
 */
public class HealAction implements Action {

    private static final double DEFAULT_MAX_HEALTH = 20.0;

    // Cached reflection for Registry.ATTRIBUTE (1.20.6+)
    private static volatile Object attributeRegistry = null;
    private static volatile Method registryGetMethod = null;
    private static volatile Boolean registryAvailable = null;

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
                com.fabian.xcommands.utils.LoggerUtils.warn("Invalid heal value (NaN/Infinity). Action skipped.");
                return;
            }
            amount = Math.max(0, amount);
        }

        Object attribute = null;

        // 1. Try modern Registry (1.20.6+) via reflection — safe for all compile targets
        if (attribute == null) {
            attribute = tryRegistryAttribute("max_health");
        }

        // 2. Try legacy key name (1.20.6 - 1.21.1 used "generic.max_health")
        if (attribute == null) {
            attribute = tryRegistryAttribute("generic_max_health");
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

    /**
     * Looks up an attribute from Registry.ATTRIBUTE using reflection.
     * This is safe to call on any version — returns null if Registry.ATTRIBUTE doesn't exist.
     */
    private static Object tryRegistryAttribute(String key) {
        // Lazy-init reflection for Registry.ATTRIBUTE
        if (registryAvailable == null) {
            try {
                attributeRegistry = org.bukkit.Registry.class.getField("ATTRIBUTE").get(null);
                registryGetMethod = attributeRegistry.getClass().getMethod("get", org.bukkit.NamespacedKey.class);
                registryAvailable = true;
            } catch (Exception e) {
                attributeRegistry = null;
                registryGetMethod = null;
                registryAvailable = false;
                com.fabian.xcommands.utils.LoggerUtils.debug("HealAction: Registry.ATTRIBUTE not available (pre-1.20.6), using Attribute.valueOf() fallback");
                return null;
            }
        }

        if (!registryAvailable || attributeRegistry == null || registryGetMethod == null) {
            return null;
        }

        try {
            // Registry keys use underscores when created via NamespacedKey.minecraft()
            return registryGetMethod.invoke(attributeRegistry, org.bukkit.NamespacedKey.minecraft(key));
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    public String getTag() {
        return "HEAL";
    }
}