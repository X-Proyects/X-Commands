package com.fabian.xcommands.managers.menus;

import com.fabian.xcommands.XCommands;
import com.fabian.xcommands.executors.CustomCommandExecutor;
import com.fabian.xcommands.utils.MenuHolder;
import com.fabian.xcommands.utils.MenuHolder.MenuType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * Menu for editing a specific command
 */
public class CommandEditMenu extends BaseMenu {

        public CommandEditMenu(XCommands plugin) {
                super(plugin);
        }

        public void open(Player player, String commandName) {
                CustomCommandExecutor executor = plugin.getCommandManager().getCustomCommands()
                                .get(commandName.toLowerCase());
                if (executor == null)
                        return;

                Inventory inv = createInventory(
                                new MenuHolder(MenuType.EDIT, commandName),
                                45,
                                lang.getMessage("gui-edit-title", commandName));

                // Fill background
                fillBackground(inv);

                // Change Name
                inv.setItem(10, createItem(Material.NAME_TAG,
                                lang.getMessage("gui-edit-name"),
                                lang.getMessage("gui-edit-name-lore", executor.getCommandName()).split("\\|")));

                // Change Description
                inv.setItem(11, createItem(Material.BOOK,
                                lang.getMessage("gui-edit-desc"),
                                lang.getMessage("gui-edit-desc-lore", executor.getDescription()).split("\\|")));

                // Change Permission
                String permString = executor.getPermission().isEmpty() ? lang.getMessage("gui-none")
                                : executor.getPermission();
                inv.setItem(12, createItem(Material.BARRIER,
                                lang.getMessage("gui-edit-perm"),
                                lang.getMessage("gui-edit-perm-lore", permString).split("\\|")));

                // Toggle Registration
                Material toggleMat = executor.isRegistered() ? Material.REPEATER : Material.LEVER;
                String toggleStatus = executor.isRegistered()
                                ? lang.getMessage("gui-edit-reg-on")
                                : lang.getMessage("gui-edit-reg-off");

                inv.setItem(13, createItem(toggleMat,
                                lang.getMessage("gui-edit-reg"),
                                lang.getMessage("gui-edit-reg-lore", toggleStatus).split("\\|")));

                // Edit Actions
                inv.setItem(15, createItem(Material.KNOWLEDGE_BOOK,
                                lang.getMessage("gui-edit-actions"),
                                lang.getMessage("gui-actions-add-lore").split("\\|")));

                // Reorder Actions (Coming soon)
                inv.setItem(16, createItem(Material.COMPARATOR,
                                lang.getMessage("gui-edit-reorder"),
                                lang.getMessage("gui-edit-reorder-lore").split("\\|")));

                // Change Cooldown (Under Name)
                inv.setItem(19, createItem(Material.CLOCK,
                                lang.getMessage("gui-edit-cooldown"),
                                lang.getMessage("gui-edit-cooldown-lore", String.valueOf(executor.getCooldown()))
                                                .split("\\|")));

                // Change Aliases (Under Description)
                String aliasesString = executor.getAliases().isEmpty() ? lang.getMessage("gui-none")
                                : String.join(", ", executor.getAliases());
                inv.setItem(20, createItem(Material.PAPER,
                                lang.getMessage("gui-edit-alias"),
                                lang.getMessage("gui-edit-alias-lore", aliasesString).split("\\|")));

                // Change Interval (Under Permission)
                inv.setItem(21, createItem(Material.COMPASS,
                                lang.getMessage("gui-edit-interval"),
                                lang.getMessage("gui-edit-interval-lore", String.valueOf(executor.getInterval()))
                                                .split("\\|")));

                // Change Material (Under Toggle Registration, next to Compass)
                Material cmdMat;
                try {
                    cmdMat = Material.valueOf(executor.getMaterial().toUpperCase());
                } catch (Exception e) {
                    cmdMat = Material.PAPER;
                }
                inv.setItem(22, createItem(cmdMat,
                                lang.getMessage("gui-edit-material"),
                                lang.getMessage("gui-edit-material-lore", cmdMat.name()).split("\\|")));

                // Save Changes
                inv.setItem(40, createItem(Material.LIME_DYE,
                                lang.getMessage("gui-edit-save"),
                                lang.getMessage("gui-edit-save-lore").split("\\|")));

                // Delete Command
                inv.setItem(44, createItem(Material.REDSTONE_BLOCK,
                                lang.getMessage("gui-edit-delete"),
                                lang.getMessage("gui-edit-delete-lore").split("\\|")));

                // Back Button
                addBackButton(inv, 36);

                smartOpenInventory(player, inv);
        }
}
