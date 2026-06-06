package com.fabian.actions;

import org.bukkit.attribute.Attribute;
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

        Attribute attribute = null;
        
        // 1. Try modern name (1.21.2+)
        org.bukkit.NamespacedKey modernKey = org.bukkit.NamespacedKey.minecraft("max_health");
        attribute = org.bukkit.Registry.ATTRIBUTE.get(modernKey);
        
        // 2. Try legacy name if modern name not found (1.18.2 - 1.21.1)
        if (attribute == null) {
            org.bukkit.NamespacedKey legacyKey = org.bukkit.NamespacedKey.minecraft("generic_max_health");
            attribute = org.bukkit.Registry.ATTRIBUTE.get(legacyKey);
        }

        double maxHealth = DEFAULT_MAX_HEALTH;
        if (attribute != null) {
            org.bukkit.attribute.AttributeInstance instance = player.getAttribute(attribute);
            if (instance != null) {
                maxHealth = instance.getValue();
            }
        }

        if (amount != null) {
            // Heal specific amount
            double newHealth = Math.min(maxHealth, player.getHealth() + amount);
            player.setHealth(newHealth);
        } else {
            // Heal to full
            player.setHealth(maxHealth);
        }
    }

    @Override
    public String getTag() {
        return "HEAL";
    }
}
