package com.fabian.xcommands.menus;

import com.fabian.xcommands.XCommands;
import com.fabian.xcommands.managers.LanguageManager;
import com.fabian.xcommands.utils.CompatibilityUtils;
import com.fabian.xcommands.utils.MenuHolder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

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
        this.keyCommandName = new NamespacedKey(plugin, "command_name");
        this.keyActionIndex = new NamespacedKey(plugin, "action_index");
        this.keyActionType = new NamespacedKey(plugin, "action_type");
    }

    /**
     * Creates an item with name and lore with cross-version compatibility.
     */
    protected ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            CompatibilityUtils.setDisplayName(meta, name);
            if (lore.length > 0) {
                CompatibilityUtils.setLore(meta, Arrays.asList(lore));
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
     * Creates an inventory with cross-version compatibility.
     */
    protected Inventory createInventory(org.bukkit.inventory.InventoryHolder holder, int size, String title) {
        return CompatibilityUtils.createInventory(holder, size, title);
    }

    /**
     * Gets the display name of an ItemMeta with cross-version compatibility.
     */
    public static String getItemDisplayName(ItemMeta meta) {
        return CompatibilityUtils.getDisplayName(meta);
    }

    /**
     * Gets the appropriate material for an action string based on its tag.
     */
    protected Material getActionMaterial(String action) {
        if (action == null || action.isEmpty()) return Material.PAPER;
        
        String upper = action.toUpperCase();
        
        if (upper.contains("[MESSAGE]")) return Material.PAPER;
        if (upper.contains("[BROADCAST]")) return Material.MAP;
        if (upper.contains("[ACTIONBAR]")) return Material.ITEM_FRAME;
        if (upper.contains("[TITLE]")) return Material.FEATHER;
        if (upper.contains("[SOUND]")) return Material.JUKEBOX;
        if (upper.contains("[EFFECT]")) return Material.POTION;
        if (upper.contains("[GIVE]")) return Material.CHEST;
        if (upper.contains("[TELEPORT]")) return Material.ENDER_PEARL;
        if (upper.contains("[DELAY]")) return Material.CLOCK;
        if (upper.contains("[HEAL]")) return Material.GOLDEN_APPLE;
        if (upper.contains("[FEED]")) return Material.COOKED_BEEF;
        if (upper.contains("[DAMAGE]")) return Material.IRON_SWORD;
        if (upper.contains("[CONSOLE]")) return Material.COMMAND_BLOCK_MINECART;
        if (upper.contains("[PLAYER]")) return Material.OAK_SIGN;
        if (upper.contains("[KICK]")) return Material.BARRIER;
        if (upper.contains("[CLOSE")) return Material.IRON_DOOR;
        if (upper.contains("[PARTICLE]")) return Material.FIREWORK_STAR;
        if (upper.contains("[BUNGEE]")) return Material.ENDER_EYE;
        if (upper.contains("[GIVE_MONEY]")) return Material.GOLD_INGOT;
        if (upper.contains("[TAKE_MONEY]")) return Material.GOLD_NUGGET;
        if (upper.contains("[VELOCITY]")) return Material.BLUE_DYE;
        if (upper.contains("[SEND_TO]") || upper.contains("[SENT_TO]")) return Material.CYAN_DYE;
        
        // Conditionals
        if (upper.contains("[IF_PERMISSION")) return Material.COMMAND_BLOCK_MINECART;
        if (upper.contains("[IF_CHANCE]")) return Material.SUNFLOWER;
        if (upper.contains("[IF_OP]")) return Material.NETHER_STAR;
        if (upper.contains("[IF_WORLD]")) return Material.GLOBE_BANNER_PATTERN;
        if (upper.contains("[IF_MONEY")) return Material.GOLD_BLOCK;
        
        // Multi-line Conditional Block Brackets
        if (action.trim().equals("[") || action.trim().equals("]")) return Material.STRUCTURE_VOID;
        
        return Material.PAPER;
    }

    protected void smartOpenInventory(org.bukkit.entity.Player player, Inventory newInv) {
        Inventory currentInv = player.getOpenInventory().getTopInventory();
        
        if (currentInv.getHolder() instanceof MenuHolder && 
            newInv.getHolder() instanceof MenuHolder &&
            currentInv.getSize() == newInv.getSize()) {
            
            MenuHolder currentHolder = (MenuHolder) currentInv.getHolder();
            MenuHolder newHolder = (MenuHolder) newInv.getHolder();
            
            // Only reuse if it's the same menu type to ensure title updates
            if (currentHolder.getMenuType() == newHolder.getMenuType()) {
                currentHolder.update(newHolder.getMenuType(), newHolder.getCommandName(), newHolder.getActionIndex(), newHolder.getPage());
                currentInv.setContents(newInv.getContents());
                return;
            }
        }

        player.openInventory(newInv);
    }
}
