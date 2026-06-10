package com.fabian.xcommands.actions;

import org.bukkit.Particle;
import org.bukkit.entity.Player;
import java.util.Map;

/**
 * Spawns particles around the player
 * Format: [PARTICLE] flame 0.5 0.5 0.5 10
 */
public class ParticleAction implements Action {

    private static final int MAX_PARTICLE_COUNT = 1000;

    @Override
    public void execute(Player player, Map<String, Object> context) {
        if (player == null) return;

        String params = (String) context.get("params");
        if (params == null) return;

        String[] args = params.split(" ");
        if (args.length < 1) return;

        try {
            Particle particle = Particle.valueOf(args[0].toUpperCase());
            double offsetX = args.length > 1 ? Double.parseDouble(args[1]) : 0;
            double offsetY = args.length > 2 ? Double.parseDouble(args[2]) : 0;
            double offsetZ = args.length > 3 ? Double.parseDouble(args[3]) : 0;
            int count = args.length > 4 ? Integer.parseInt(args[4]) : 1;
            count = Math.min(count, MAX_PARTICLE_COUNT);

            // Validate offsets against NaN and Infinity
            if (Double.isNaN(offsetX) || Double.isInfinite(offsetX)) offsetX = 0;
            if (Double.isNaN(offsetY) || Double.isInfinite(offsetY)) offsetY = 0;
            if (Double.isNaN(offsetZ) || Double.isInfinite(offsetZ)) offsetZ = 0;

            player.getWorld().spawnParticle(particle, player.getLocation(), count, offsetX, offsetY, offsetZ, 0.1);
        } catch (Exception e) {
            com.fabian.xcommands.utils.LoggerUtils.warn("Invalid parameters for action [PARTICLE]: " + params);
        }
    }

    @Override
    public String getTag() {
        return "PARTICLE";
    }
}
