package com.fabian.xcommands.utils;

import com.fabian.xcommands.XCommands;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI expansion for X-Commands.
 * <p>
 * Provides the following placeholders:
 * <ul>
 *   <li>{@code %xcommands_version%} — Plugin version</li>
 *   <li>{@code %xcommands_total_commands%} — Total number of registered custom commands</li>
 *   <li>{@code %xcommands_language%} — Configured language code</li>
 *   <li>{@code %xcommands_cooldown_remaining_<command>%} — Remaining cooldown seconds for an online player (e.g. %xcommands_cooldown_remaining_kit%)</li>
 *   <li>{@code %xcommands_on_cooldown_<command>%} — Whether an online player is on cooldown (true/false)</li>
 *   <li>{@code %xcommands_player_cooldown_<command>%} — Alias for cooldown_remaining</li>
 * </ul>
 */
public class XCommandsExpansion extends PlaceholderExpansion {

    private final XCommands plugin;

    public XCommandsExpansion(XCommands plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "xcommands";
    }

    @Override
    public @NotNull String getAuthor() {
        return "fabianfamr";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        if (params == null || params.isEmpty()) {
            return null;
        }

        String lower = params.toLowerCase();

        // --- Server-wide placeholders (no player required) ---

        // %xcommands_version%
        if (lower.equals("version")) {
            return plugin.getDescription().getVersion();
        }

        // %xcommands_total_commands%
        if (lower.equals("total_commands")) {
            return String.valueOf(plugin.getCommandManager().getCustomCommands().size());
        }

        // %xcommands_language%
        if (lower.equals("language")) {
            return plugin.getConfigManager().getLanguage();
        }

        // --- Player-specific placeholders (require online player) ---

        if (offlinePlayer == null || !offlinePlayer.isOnline()) {
            return null;
        }

        Player player = offlinePlayer.getPlayer();
        if (player == null) {
            return null;
        }

        // %xcommands_cooldown_remaining_<command>%
        if (lower.startsWith("cooldown_remaining_")) {
            String commandName = lower.substring("cooldown_remaining_".length());
            double remaining = plugin.getCooldownManager().getRemaining(player.getUniqueId(), commandName);
            return formatCooldown(remaining);
        }

        // %xcommands_player_cooldown_<command>%  (alias)
        if (lower.startsWith("player_cooldown_")) {
            String commandName = lower.substring("player_cooldown_".length());
            double remaining = plugin.getCooldownManager().getRemaining(player.getUniqueId(), commandName);
            return formatCooldown(remaining);
        }

        // %xcommands_on_cooldown_<command>%
        if (lower.startsWith("on_cooldown_")) {
            String commandName = lower.substring("on_cooldown_".length());
            return String.valueOf(plugin.getCooldownManager().isOnCooldown(player.getUniqueId(), commandName));
        }

        return null;
    }

    /**
     * Formats a cooldown value for display.
     * Whole seconds are shown without decimals (e.g. "5"),
     * fractional values show one decimal place (e.g. "4.3").
     * Zero returns "0".
     */
    private String formatCooldown(double seconds) {
        if (seconds <= 0) {
            return "0";
        }
        if (seconds == Math.floor(seconds)) {
            return String.valueOf((long) seconds);
        }
        return String.format("%.1f", seconds);
    }
}