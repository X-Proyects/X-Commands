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
            org.bukkit.NamespacedKey key = org.bukkit.NamespacedKey.minecraft(effectName.toLowerCase());

            // 1. Try modern Registry (1.21.2+)
            try {
                @SuppressWarnings("unchecked")
                org.bukkit.Registry<PotionEffectType> registry = (org.bukkit.Registry<PotionEffectType>) org.bukkit.Registry.class.getField("EFFECT").get(null);
                effectType = registry.get(key);
            } catch (Exception ignored) {}

            // 2. Try legacy Registry (1.18.2 - 1.21.1)
            if (effectType == null) {
                try {
                    effectType = org.bukkit.Registry.POTION_EFFECT_TYPE.get(key);
                } catch (Exception ignored) {}
            }

            // 3. Last resort fallback
            if (effectType == null) {
                @SuppressWarnings("deprecation")
                PotionEffectType legacy = PotionEffectType.getByName(effectName);
                effectType = legacy;
            }

            if (effectType == null) {
                com.fabian.utils.LoggerUtils.warn("Unknown effect type: " + effectName);
                return;
            }

            player.addPotionEffect(new PotionEffect(effectType, duration, level));
        } catch (Exception e) {
            com.fabian.utils.LoggerUtils.warn("Invalid parameters for action [EFFECT]: " + params);
        }
    }

    @Override
    public String getTag() {
        return "EFFECT";
    }
}
