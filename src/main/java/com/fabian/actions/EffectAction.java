package com.fabian.actions;

import org.bukkit.entity.Player;
import java.util.Map;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Applies a potion effect to the player
 * Format: [EFFECT] effect;duration;level
 */
public class EffectAction implements Action {

    @SuppressWarnings("unchecked")
    @Override
    public void execute(Player player, Map<String, Object> context) {
        if (player == null) return;

        String params = (String) context.get("params");
        if (params == null) return;

        try {
            String[] parts = params.split(";");

            String effectName = parts[0].toUpperCase().trim();
            int duration = parts.length > 1 ? Integer.parseInt(parts[1]) : 100;
            int level = parts.length > 2 ? Integer.parseInt(parts[2]) - 1 : 0;

            PotionEffectType effectType = null;
            try {
                java.lang.reflect.Field effectField = org.bukkit.Registry.class.getField("EFFECT");
                org.bukkit.Registry<PotionEffectType> registry = (org.bukkit.Registry<PotionEffectType>) effectField
                        .get(null);
                effectType = registry.get(org.bukkit.NamespacedKey.minecraft(effectName.toLowerCase()));
            } catch (Exception ex) {
                try {
                    java.lang.reflect.Field field = org.bukkit.Registry.class.getField("POTION_EFFECT_TYPE");
                    org.bukkit.Registry<PotionEffectType> registry = (org.bukkit.Registry<PotionEffectType>) field
                            .get(null);
                    effectType = registry.get(org.bukkit.NamespacedKey.minecraft(effectName.toLowerCase()));
                } catch (Exception ex2) {
                    try {
                        java.lang.reflect.Method getByName = PotionEffectType.class.getMethod("getByName",
                                String.class);
                        effectType = (PotionEffectType) getByName.invoke(null, effectName);
                    } catch (Exception reflectiveEx) {
                        effectType = null;
                    }
                }
            }

            if (effectType == null) return;

            player.addPotionEffect(new PotionEffect(effectType, duration, level));
        } catch (Exception ignored) {
        }
    }

    @Override
    public String getTag() {
        return "EFFECT";
    }
}
