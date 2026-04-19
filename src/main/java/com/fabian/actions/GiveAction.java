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

            Material material = Material.valueOf(materialName);
            ItemStack item = new ItemStack(material, amount);

            if (parts.length > 2) {
                org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(com.fabian.utils.ColorUtils.translate(parts[2].trim()));
                    item.setItemMeta(meta);
                }
            }

            player.getInventory().addItem(item);
        } catch (Exception ignored) {
        }
    }

    @Override
    public String getTag() {
        return "GIVE";
    }
}
