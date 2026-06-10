package com.fabian.xcommands.listeners;

import com.fabian.xcommands.XCommands;
import com.fabian.xcommands.utils.CompatibilityUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServerLoadEvent;

/**
 * Listens for players joining the server to notify about updates
 */
public class UpdateListener implements Listener {

    private final XCommands plugin;

    public UpdateListener(XCommands plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        // Check for updates on server load (after "Done")
        if (plugin.getConfigManager().isCheckUpdates() && plugin.getUpdateChecker() != null) {
            plugin.getUpdateChecker().checkForUpdates();
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Check if player is OP or has permission
        if (!player.isOp() && !player.hasPermission("xcommands.admin.update")) {
            return;
        }

        // Check if an update is available
        if (plugin.getUpdateChecker() == null)
            return;

        if (plugin.getUpdateChecker().isUpdateAvailable()) {
            String latestVersion = plugin.getUpdateChecker().getLatestVersion();
            String currentVersion = CompatibilityUtils.getVersion(plugin);

            CompatibilityUtils.sendMessage(player, 
                    plugin.getLanguageManager().getMessage("update-available", currentVersion, latestVersion));
            CompatibilityUtils.sendMessage(player, plugin.getLanguageManager().getMessage("update-download",
                    plugin.getUpdateChecker().getDownloadUrl()));
        }
    }
}
