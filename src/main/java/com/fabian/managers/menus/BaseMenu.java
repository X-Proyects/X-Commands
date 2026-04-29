package com.fabian.managers.menus;

import com.fabian.XCommands;
import com.fabian.managers.LanguageManager;
import com.fabian.utils.ColorUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
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

    public static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    public BaseMenu(XCommands plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
        this.keyCommandName = new NamespacedKey(plugin, "command_name");
        this.keyActionIndex = new NamespacedKey(plugin, "action_index");
        this.keyActionType = new NamespacedKey(plugin, "action_type");
    }

    /**
     * Creates an item with name and lore using Adventure Components (non-deprecated).
     */
    protected ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LEGACY.deserialize(ColorUtils.translate(name)));
            if (lore.length > 0) {
                List<Component> loreList = Arrays.stream(lore)
                        .map(l -> (Component) LEGACY.deserialize(ColorUtils.translate(l)))
                        .collect(Collectors.toList());
                meta.lore(loreList);
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

    /**
     * Creates an inventory using the non-deprecated Component title overload.
     * Drop-in replacement for Bukkit.createInventory(holder, size, String).
     */
    protected Inventory createInventory(org.bukkit.inventory.InventoryHolder holder, int size, String title) {
        return Bukkit.createInventory(holder, size, LEGACY.deserialize(title));
    }

    /**
     * Serializes a Component display name from ItemMeta back to a legacy String,
     * to be compatible with string-based logic (e.g. action type detection).
     */
    public static String getItemDisplayName(ItemMeta meta) {
        if (meta == null || meta.displayName() == null) return "";
        return LEGACY.serialize(meta.displayName());
    }
}
