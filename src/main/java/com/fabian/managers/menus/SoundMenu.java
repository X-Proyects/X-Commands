package com.fabian.managers.menus;

import com.fabian.XCommands;
import com.fabian.utils.MenuHolder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * Specialized menu to edit [SOUND] action parameters
 */
public class SoundMenu extends BaseMenu {

    public SoundMenu(XCommands plugin) {
        super(plugin);
    }

    public void open(Player player, String commandName, int actionIndex) {
        com.fabian.executors.CustomCommandExecutor cmd = plugin.getCommandManager().getCustomCommands().get(commandName.toLowerCase());
        if (cmd == null || actionIndex < 0 || actionIndex >= cmd.getActions().size()) return;

        String action = cmd.getActions().get(actionIndex);
        String sound = "BLOCK_NOTE_BLOCK_PLING";
        float volume = 1.0f;
        float pitch = 1.0f;

        // Parse format: [SOUND] sound;volume;pitch
        if (action.startsWith("[SOUND]")) {
            String params = action.substring(7).trim();
            if (!params.isEmpty()) {
                String[] parts = params.split(";");
                if (parts.length > 0) sound = parts[0].trim();
                if (parts.length > 1) {
                    try { volume = Float.parseFloat(parts[1]); } catch (Exception ignored) {}
                }
                if (parts.length > 2) {
                    try { pitch = Float.parseFloat(parts[2]); } catch (Exception ignored) {}
                }
            }
        }

        Inventory inv = createInventory(new MenuHolder(MenuHolder.MenuType.SOUND_MENU, commandName, actionIndex), 27,
                lang.getMessage("gui-sound-title", (actionIndex + 1)));

        fillBackground(inv);

        // Sound Selection
        inv.setItem(11, createItem(Material.JUKEBOX,
                lang.getMessage("gui-sound-select"),
                lang.getMessage("gui-sound-select-lore", sound).split("\\|")));

        // Volume
        inv.setItem(13, createItem(Material.BELL,
                lang.getMessage("gui-sound-volume"),
                lang.getMessage("gui-sound-volume-lore", String.valueOf(volume)).split("\\|")));

        // Pitch
        inv.setItem(15, createItem(Material.NOTE_BLOCK,
                lang.getMessage("gui-sound-pitch"),
                lang.getMessage("gui-sound-pitch-lore", String.valueOf(pitch)).split("\\|")));

        // Back Button
        addBackButton(inv, 18);

        smartOpenInventory(player, inv);
    }
}
