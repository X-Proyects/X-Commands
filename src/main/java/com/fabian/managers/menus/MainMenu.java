package com.fabian.managers.menus;

import com.fabian.XCommands;
import com.fabian.executors.CustomCommandExecutor;
import com.fabian.utils.ColorUtils;
import com.fabian.utils.MenuHolder;
import com.fabian.utils.MenuHolder.MenuType;
import org.bukkit.Material;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Main menu showing all custom commands
 */
public class MainMenu extends BaseMenu {

    private static final int ITEMS_PER_PAGE = 45;

    public MainMenu(XCommands plugin) {
        super(plugin);
    }

    public void open(Player player, int page) {
        Map<String, CustomCommandExecutor> commands = plugin.getCommandManager().getCustomCommands();
        List<CustomCommandExecutor> sortedCommands = new ArrayList<>(commands.values());
        sortedCommands.sort(Comparator.comparingLong(CustomCommandExecutor::getCreationTime));

        int totalPages = (int) Math.ceil((double) sortedCommands.size() / ITEMS_PER_PAGE);
        if (totalPages == 0)
            totalPages = 1;

        int currentPage = Math.max(0, Math.min(page, totalPages - 1));

        Inventory inv = createInventory(
                new MenuHolder(MenuType.MAIN, currentPage),
                54,
                lang.getMessage("gui-main-title"));

        // Add command items
        int start = currentPage * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, sortedCommands.size());

        for (int i = start; i < end; i++) {
            CustomCommandExecutor executor = sortedCommands.get(i);
            inv.setItem(i - start, createCommandItem(executor));
        }

        // Navigation buttons
        if (currentPage > 0) {
            inv.setItem(45, createItem(Material.ARROW, lang.getMessage("gui-main-prev")));
        }

        if (currentPage < totalPages - 1) {
            inv.setItem(53, createItem(Material.ARROW, lang.getMessage("gui-main-next")));
        }

        // Create command button
        inv.setItem(49, createItem(Material.EMERALD,
                lang.getMessage("gui-main-create"),
                lang.getMessage("gui-main-create-lore").split("\\|")));

        // Fill background
        fillBackground(inv);

        player.openInventory(inv);
    }

    private ItemStack createCommandItem(CustomCommandExecutor executor) {
        Material mat;
        try {
            mat = Material.valueOf(executor.getMaterial().toUpperCase());
        } catch (IllegalArgumentException e) {
            mat = Material.PAPER;
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LEGACY.deserialize(ColorUtils.translate(executor.getDisplayName())));

            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            lore.add(LEGACY.deserialize(ColorUtils.translate(executor.getDescription())));
            lore.add(net.kyori.adventure.text.Component.empty());
            lore.add(LEGACY.deserialize(lang.getMessage("gui-main-click-left", "&7Click Izquierdo: &bEditar")));
            lore.add(LEGACY.deserialize(lang.getMessage("gui-main-click-right", "&7Click Derecho: &eVer acciones")));

            meta.lore(lore);

            // Store command name in PDC
            meta.getPersistentDataContainer().set(keyCommandName, PersistentDataType.STRING, executor.getCommandName());

            item.setItemMeta(meta);
        }
        return item;
    }
}
