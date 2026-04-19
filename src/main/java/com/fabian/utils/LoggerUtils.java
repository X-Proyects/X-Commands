package com.fabian.utils;

import com.fabian.XCommands;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;

/**
 * Utility class for professional logging
 */
public class LoggerUtils {

    private static final String PREFIX = "&8[&bX-Commands&8] ";
    private static final ConsoleCommandSender console = Bukkit.getConsoleSender();

    /**
     * Logs an information message
     */
    public static void info(String message) {
        sendMessage("&f" + message);
    }

    /**
     * Logs a warning message
     */
    public static void warn(String message) {
        sendMessage("&e[!] " + message);
    }

    /**
     * Logs a severe error message
     */
    public static void error(String message) {
        sendMessage("&c[ERROR] " + message);
    }

    /**
     * Logs a severe error message with a stack trace
     */
    public static void severe(String message, Throwable throwable) {
        error(message);
        if (throwable != null) {
            System.err.println("[X-Commands ERROR] " + message);
            throwable.printStackTrace();
        }
    }

    /**
     * Logs a debug message (only if debug is enabled in config)
     */
    public static void debug(String message) {
        if (XCommands.getInstance().getConfigManager() != null && 
            XCommands.getInstance().getConfigManager().getConfig().getBoolean("debug", false)) {
            sendMessage("&b[DEBUG] &7" + message);
        }
    }

    private static void sendMessage(String message) {
        if (console != null) {
            console.sendMessage(ColorUtils.translate(PREFIX + message));
        } else {
            Bukkit.getConsoleSender().sendMessage(ColorUtils.translate(PREFIX + message));
        }
    }
}
