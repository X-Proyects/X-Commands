package com.fabian.xcommands.utils;

import com.fabian.xcommands.XCommands;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public final class DebugLogger {

    private static final String PREFIX = "&8[&bDEBUG&8]&r ";
    private static XCommands instance;

    public static void init(XCommands plugin) {
        instance = plugin;
    }

    private static boolean isDebugEnabled() {
        if (instance == null) return false;
        try {
            return instance.getConfigManager() != null &&
                   instance.getConfigManager().getConfig().getBoolean("debug", false);
        } catch (Exception e) {
            return false;
        }
    }

    public static void debug(String message) {
        if (!isDebugEnabled()) return;
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', PREFIX + "&7" + message));
    }

    public static void debug(String category, String message) {
        if (!isDebugEnabled()) return;
        Bukkit.getConsoleSender().sendMessage(
                ChatColor.translateAlternateColorCodes('&', PREFIX + "&f[" + category + "&f]&r &7" + message));
    }

    public static void debug(String category, String message, Throwable throwable) {
        if (!isDebugEnabled()) return;
        Bukkit.getConsoleSender().sendMessage(
                ChatColor.translateAlternateColorCodes('&', PREFIX + "&f[" + category + "&f]&r &7" + message));
        if (throwable != null) {
            throwable.printStackTrace();
        }
    }
}