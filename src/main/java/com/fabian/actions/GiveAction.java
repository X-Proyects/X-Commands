package com.fabian.actions;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import java.util.Map;
import org.bukkit.inventory.ItemStack;

/**
 * Gives an item to the player
 * Format: [GIVE] material;amount
 */
public class GiveAction implements Action {

    @Override
    public void execute(Player player, Map<String, Object> context) {
        if (player == null) return;

        String params = (String) context.get("params");
        if (params == null) return;

        try {
            String[] parts = params.split(";");

            String materialName = parts[0].toUpperCase().trim();
            int amount = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 1;
            if (amount <= 0) amount = 1;

            Material material = Material.matchMaterial(materialName);
            if (material == null) {
                com.fabian.utils.LoggerUtils.warn("Invalid material in [GIVE] action: " + materialName);
                return;
            }

            ItemStack item = new ItemStack(material, amount);

            if (parts.length > 2) {
                org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    com.fabian.utils.CompatibilityUtils.setDisplayName(meta, parts[2].trim());
                    item.setItemMeta(meta);
                }
            }

            // Add to inventory and drop if full
            java.util.Map<Integer, ItemStack> remaining = player.getInventory().addItem(item);
            if (!remaining.isEmpty()) {
                remaining.values().forEach(remainingItem -> 
                    player.getWorld().dropItemNaturally(player.getLocation(), remainingItem)
                );
            }
        } catch (Exception e) {
            com.fabian.utils.LoggerUtils.severe("Error executing [GIVE] action with params: " + params, e);
        }
    }

    @Override
    public String getTag() {
        return "GIVE";
    }
}
