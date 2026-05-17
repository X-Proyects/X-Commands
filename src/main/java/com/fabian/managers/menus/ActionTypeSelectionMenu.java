package com.fabian.managers.menus;

import com.fabian.XCommands;
import com.fabian.utils.CompatibilityUtils;
import com.fabian.utils.MenuHolder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu to select the type of action to add
 */
public class ActionTypeSelectionMenu extends BaseMenu {

    public ActionTypeSelectionMenu(XCommands plugin) {
        super(plugin);
    }

    public void open(Player player, String commandName, int actionIndex) {
        Inventory inv = createInventory(new MenuHolder(MenuHolder.MenuType.ACTION_TYPE_SELECTION, commandName, actionIndex, 0), 54, 
                lang.getMessage("gui-selector-title", (actionIndex + 1)));

        String[] types = {
            "MESSAGE", "BROADCAST", "ACTIONBAR", "TITLE", "SOUND", "EFFECT", "GIVE", "TELEPORT", "DELAY", 
            "HEAL", "FEED", "DAMAGE", "CONSOLE", "PLAYER", "KICK", "CLOSE", "PARTICLE", "BUNGEE", 
            "GIVE_MONEY", "TAKE_MONEY", "VELOCITY", "SENT_TO", "IF_PERMISSION", "IF_CHANCE", "IF_OP", "IF_WORLD", "IF_MONEY"
        };

        for (int i = 0; i < types.length; i++) {
            inv.setItem(i, createTypeItem(types[i]));
        }

        fillBackground(inv);
        addBackButton(inv, 45);

        smartOpenInventory(player, inv);
    }

    private ItemStack createTypeItem(String type) {
        String actionTag = "[" + type + "]";
        ItemStack item = new ItemStack(getActionMaterial(actionTag));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String nameKey = "gui-type-" + type.toLowerCase().replace("_", "-");
            String descKey = nameKey + "-desc";

            CompatibilityUtils.setDisplayName(meta, lang.getMessage(nameKey));

            List<String> lore = new ArrayList<>();
            lore.add(lang.getMessage(descKey));
            lore.add("");
            lore.add(lang.getMessage("gui-type-select"));
            
            CompatibilityUtils.setLore(meta, lore);

            // Store type in PersistentDataContainer
            meta.getPersistentDataContainer().set(keyActionType, PersistentDataType.STRING, type);

            item.setItemMeta(meta);
        }
        return item;
    }
}
