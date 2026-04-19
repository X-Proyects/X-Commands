package com.fabian.managers.menus;

import com.fabian.XCommands;
import com.fabian.managers.LanguageManager;
import com.fabian.utils.ColorUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Base class for all menu types providing common utility methods
 */
public abstract class BaseMenu {

    protected final XCommands plugin;
    protected final LanguageManager lang;
    protected final NamespacedKey keyCommandName;
    protected final NamespacedKey keyActionIndex;
    protected final NamespacedKey keyActionType;

    public BaseMenu(XCommands plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
        // Initialize common keys once
        this.keyCommandName = new NamespacedKey(plugin, "command_name");
        this.keyActionIndex = new NamespacedKey(plugin, "action_index");
        this.keyActionType = new NamespacedKey(plugin, "action_type");
    }

    /**
     * Creates an item with name and lore
     */
    protected ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.translate(name));
            if (lore.length > 0) {
                List<String> loreList = Arrays.stream(lore)
                        .map(ColorUtils::translate)
                        .collect(Collectors.toList());
                meta.setLore(loreList);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Fills empty slots with glass pane
     */
    protected void fillBackground(Inventory inv) {
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() == Material.AIR) {
                inv.setItem(i, glass);
            }
        }
    }

    /**
     * Adds a back button at the specified slot
     */
    protected void addBackButton(Inventory inv, int slot) {
        inv.setItem(slot, createItem(Material.ARROW, lang.getMessage("gui-edit-back")));
    }
}
