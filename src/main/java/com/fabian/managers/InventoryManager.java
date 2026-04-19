package com.fabian.managers;

import com.fabian.XCommands;
import com.fabian.managers.menus.*;
import org.bukkit.entity.Player;

/**
 * Manager responsible for opening all plugin menus by delegating to specific
 * menu classes
 */
public class InventoryManager {

        private final MainMenu mainMenu;
        private final CommandEditMenu commandEditMenu;
        private final ActionsMenu actionsMenu;
        private final ActionEditMenu actionEditMenu;
        private final ActionTypeSelectionMenu actionTypeSelectionMenu;
        private final ConfirmDeleteMenu confirmDeleteMenu;

        public InventoryManager(XCommands plugin) {
                this.mainMenu = new MainMenu(plugin);
                this.commandEditMenu = new CommandEditMenu(plugin);
                this.actionsMenu = new ActionsMenu(plugin);
                this.actionEditMenu = new ActionEditMenu(plugin);
                this.actionTypeSelectionMenu = new ActionTypeSelectionMenu(plugin);
                this.confirmDeleteMenu = new ConfirmDeleteMenu(plugin);
        }

        /**
         * Opens the main menu with all custom commands (default page 0)
         */
        public void openMainMenu(Player player) {
                mainMenu.open(player, 0);
        }

        /**
         * Opens the main menu with all custom commands on a specific page
         */
        public void openMainMenu(Player player, int page) {
                mainMenu.open(player, page);
        }

        /**
         * Opens the edit menu for a specific command
         */
        public void openCommandEditMenu(Player player, String commandName) {
                commandEditMenu.open(player, commandName);
        }

        /**
         * Opens the confirmation menu for deletion
         */
        public void openConfirmationMenu(Player player, String commandName) {
                confirmDeleteMenu.open(player, commandName);
        }

        /**
         * Opens the actions list menu
         */
        public void openActionsMenu(Player player, String commandName) {
                actionsMenu.open(player, commandName);
        }

        /**
         * Opens the action type selection menu
         */
        public void openActionTypeSelectionMenu(Player player, String commandName, int actionIndex) {
                actionTypeSelectionMenu.open(player, commandName, actionIndex);
        }

        /**
         * Opens the specific action edit menu (Type and Value)
         */
        public void openActionEditMenu(Player player, String commandName, int actionIndex) {
                actionEditMenu.open(player, commandName, actionIndex);
        }
}
