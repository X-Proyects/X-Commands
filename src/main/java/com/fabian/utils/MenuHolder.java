package com.fabian.utils;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class MenuHolder implements InventoryHolder {

    private MenuType type;
    private String commandName;
    private int actionIndex;
    private int page;

    public enum MenuType {
        MAIN,
        EDIT,
        CONFIRM_DELETE,
        ACTIONS,
        ACTION_TYPE_SELECTION,
        ACTION_EDIT,
        ACTION_REORDER,
        NUMERIC_ACTION,
        TITLE_MENU,
        TELEPORT_MENU,
        EFFECT_MENU,
        GIVE_MENU,
        PARTICLE_MENU,
        ALIAS_MENU,
        SOUND_MENU
    }

    public MenuHolder(MenuType type) {
        this(type, null, -1, 0);
    }

    public MenuHolder(MenuType type, int page) {
        this(type, null, -1, page);
    }

    public MenuHolder(MenuType type, String commandName) {
        this(type, commandName, -1, 0);
    }

    public MenuHolder(MenuType type, String commandName, int actionIndex) {
        this(type, commandName, actionIndex, 0);
    }

    public MenuHolder(MenuType type, String commandName, int actionIndex, int page) {
        this.type = type;
        this.commandName = commandName;
        this.actionIndex = actionIndex;
        this.page = page;
    }

    public void update(MenuType type, String commandName, int actionIndex, int page) {
        this.type = type;
        this.commandName = commandName;
        this.actionIndex = actionIndex;
        this.page = page;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }

    public MenuType getMenuType() {
        return type;
    }

    public String getCommandName() {
        return commandName;
    }

    public int getActionIndex() {
        return actionIndex;
    }

    public int getPage() {
        return page;
    }
}
