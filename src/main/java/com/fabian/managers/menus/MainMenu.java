package com.fabian.managers.menus;

import com.fabian.XCommands;
import com.fabian.executors.CustomCommandExecutor;
import com.fabian.utils.CompatibilityUtils;
import com.fabian.utils.MenuHolder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Main menu of the plugin GUI
 */
public class MainMenu extends BaseMenu {

    public MainMenu(XCommands plugin) {
        super(plugin);
    }

    public void open(Player player, int page) {
        Inventory inv = createInventory(new MenuHolder(MenuHolder.MenuType.MAIN, null, -1, page), 54, lang.getMessage("gui-main-title"));

        Map<String, CustomCommandExecutor> commands = plugin.getCommandManager().getCustomCommands();
        List<CustomCommandExecutor> executorList = new ArrayList<>(commands.values());
        
        int start = page * 45;
        for (int i = 0; i < 45 && (start + i) < executorList.size(); i++) {
            inv.setItem(i, createCommandItem(executorList.get(start + i)));
        }

        fillBackground(inv);

        // Add Command button
        inv.setItem(49, createItem(Material.NETHER_STAR,
                lang.getMessage("gui-main-create"),
                lang.getMessage("gui-main-create-lore").split("\\|")));

        // Help Button
        inv.setItem(53, createItem(Material.BOOK,
                lang.getMessage("gui-main-help"),
                lang.getMessage("gui-main-help-lore").split("\\|")));

        smartOpenInventory(player, inv);
    }

    private ItemStack createCommandItem(CustomCommandExecutor executor) {
        Material mat = Material.matchMaterial(executor.getMaterial());
        if (mat == null) mat = Material.COMMAND_BLOCK;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            CompatibilityUtils.setDisplayName(meta, executor.getDisplayName());

            List<String> lore = new ArrayList<>();
            lore.add("§7" + executor.getDescription());
            lore.add("");
            lore.add(lang.getMessage("gui-main-click-left"));
            lore.add(lang.getMessage("gui-main-click-right"));
            
            CompatibilityUtils.setLore(meta, lore);

            // Store command name in PersistentDataContainer
            meta.getPersistentDataContainer().set(keyCommandName, PersistentDataType.STRING, executor.getCommandName());

            item.setItemMeta(meta);
        }
        return item;
    }
}
