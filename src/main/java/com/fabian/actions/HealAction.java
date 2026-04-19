package com.fabian.actions;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import java.util.Map;

/**
 * Heals the player to full health
 * Format: [HEAL]
 */
public class HealAction implements Action {

    @Override
    public void execute(Player player, Map<String, Object> context) {
        if (player == null) return;

        org.bukkit.attribute.AttributeInstance healthAttr = null;
        try {
            java.lang.reflect.Method valueOf = Attribute.class.getMethod("valueOf", String.class);
            healthAttr = player.getAttribute((Attribute) valueOf.invoke(null, "GENERIC_MAX_HEALTH"));
        } catch (Exception e) {
            try {
                java.lang.reflect.Method valueOf = Attribute.class.getMethod("valueOf", String.class);
                healthAttr = player.getAttribute((Attribute) valueOf.invoke(null, "MAX_HEALTH"));
            } catch (Exception e2) {
                return;
            }
        }

        if (healthAttr == null) return;

        double maxHealth = healthAttr.getValue();
        player.setHealth(maxHealth);
    }

    @Override
    public String getTag() {
        return "HEAL";
    }
}
