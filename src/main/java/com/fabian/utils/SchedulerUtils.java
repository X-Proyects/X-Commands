package com.fabian.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.util.function.Consumer;

/**
 * Utility class to handle scheduling tasks on both standard Bukkit/Spigot
 * and Folia's region-based threading model.
 */
public class SchedulerUtils {

    private static Boolean isFolia = null;
    private static java.lang.reflect.Method teleportAsyncMethod = null;
    private static Boolean teleportAsyncAvailable = null;

    /**
     * Checks if the server is running Folia.
     */
    public static boolean isFolia() {
        if (isFolia == null) {
            try {
                // Only RegionizedServer is a reliable indicator of a running Folia server.
                // RegionScheduler might be present in standard Paper API stubs, causing false positives on Paper/Spigot.
                Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
                isFolia = true;
            } catch (ClassNotFoundException e) {
                isFolia = false;
            }
        }
        return isFolia;
    }

    /**
     * Runs a task on the next tick (synchronously on Spigot, or on the global region on Folia).
     */
    public static void runTask(Plugin plugin, Runnable runnable) {
        if (isFolia()) {
            try {
                Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                scheduler.getClass().getMethod("run", Plugin.class, Consumer.class)
                         .invoke(scheduler, plugin, (Consumer<Object>) t -> runnable.run());
            } catch (Throwable e) {
                LoggerUtils.debug("Folia GlobalRegionScheduler 'run' failed: " + e.getMessage());
                // Fallback to async if global fails or other errors
                runTaskAsynchronously(plugin, runnable);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    /**
     * Runs a task after a delay.
     */
    public static void runTaskLater(Plugin plugin, Runnable runnable, long delayTicks) {
        if (isFolia()) {
            try {
                Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                scheduler.getClass().getMethod("runDelayed", Plugin.class, Consumer.class, long.class)
                         .invoke(scheduler, plugin, (Consumer<Object>) t -> runnable.run(), Math.max(1L, delayTicks));
            } catch (Throwable e) {
                LoggerUtils.debug("Folia GlobalRegionScheduler 'runDelayed' failed: " + e.getMessage());
                // Cannot use Bukkit.getScheduler() on Folia, use async as fallback
                runTaskAsynchronously(plugin, runnable);
            }
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
        }
    }

    /**
     * Runs a task asynchronously.
     */
    public static void runTaskAsynchronously(Plugin plugin, Runnable runnable) {
        if (isFolia()) {
            try {
                Object scheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
                scheduler.getClass().getMethod("runNow", Plugin.class, Consumer.class)
                         .invoke(scheduler, plugin, (Consumer<Object>) t -> runnable.run());
            } catch (Throwable e) {
                LoggerUtils.debug("Folia AsyncScheduler 'runNow' failed: " + e.getMessage());
                // Last resort fallback
                try {
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
                } catch (UnsupportedOperationException ignored) {}
            }
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
        }
    }

    /**
     * Runs a repeating task.
     * Returns the task object (BukkitTask or ScheduledTask) or null if failed.
     */
    public static Object runTaskTimer(Plugin plugin, Runnable runnable, long delayTicks, long periodTicks) {
        if (isFolia()) {
            try {
                Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                return scheduler.getClass().getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class)
                         .invoke(scheduler, plugin, (Consumer<Object>) t -> runnable.run(), Math.max(1L, delayTicks), Math.max(1L, periodTicks));
            } catch (Throwable e) {
                LoggerUtils.debug("Folia GlobalRegionScheduler 'runAtFixedRate' failed: " + e.getMessage());
                // Cannot use Bukkit.getScheduler() on Folia, log and return null
                return null;
            }
        } else {
            return Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks);
        }
    }

    /**
     * Cancels a task (BukkitTask or ScheduledTask).
     */
    public static void cancelTask(Object task) {
        if (task == null) return;
        try {
            task.getClass().getMethod("cancel").invoke(task);
        } catch (Throwable e) {
            LoggerUtils.debug("Task cancellation failed: " + e.getMessage());
        }
    }

    /**
     * Executes a task on a player's thread (region-aware on Folia).
     */
    public static void runForPlayer(Plugin plugin, HumanEntity player, Runnable runnable) {
        if (player == null) {
            runTask(plugin, runnable);
            return;
        }
        
        if (isFolia()) {
            try {
                // Folia: player.getScheduler().run(...)
                Object scheduler = player.getClass().getMethod("getScheduler").invoke(player);
                scheduler.getClass().getMethod("run", Plugin.class, Consumer.class, Runnable.class)
                         .invoke(scheduler, plugin, (Consumer<Object>) t -> runnable.run(), null);
            } catch (Throwable e) {
                LoggerUtils.debug("Folia EntityScheduler 'run' failed for " + player.getName() + ": " + e.getMessage());
                runTask(plugin, runnable);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    /**
     * Runs a delayed task for a specific player
     */
    public static void runTaskLaterForPlayer(Plugin plugin, HumanEntity player, Runnable runnable, long delayTicks) {
        if (player == null) {
            runTaskLater(plugin, runnable, delayTicks);
            return;
        }

        if (isFolia()) {
            try {
                // Folia: player.getScheduler().runDelayed(...)
                Object scheduler = player.getClass().getMethod("getScheduler").invoke(player);
                scheduler.getClass().getMethod("runDelayed", Plugin.class, Consumer.class, Runnable.class, long.class)
                         .invoke(scheduler, plugin, (Consumer<Object>) t -> runnable.run(), null, delayTicks);
            } catch (Throwable e) {
                LoggerUtils.debug("Folia EntityScheduler 'runDelayed' failed for " + player.getName() + ": " + e.getMessage());
                runTaskLater(plugin, runnable, delayTicks);
            }
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
        }
    }


    /**
     * Teleports a player asynchronously if on Folia/Paper, or synchronously on Spigot.
     */
    public static void teleportAsync(Player player, org.bukkit.Location location) {
        if (player == null || location == null) return;
        
        try {
            // Check cached method first
            if (teleportAsyncAvailable == null) {
                try {
                    teleportAsyncMethod = player.getClass().getMethod("teleportAsync", org.bukkit.Location.class);
                    teleportAsyncAvailable = true;
                } catch (NoSuchMethodException e) {
                    teleportAsyncAvailable = false;
                }
            }
            
            if (teleportAsyncAvailable && teleportAsyncMethod != null) {
                teleportAsyncMethod.invoke(player, location);
            } else {
                player.teleport(location);
            }
        } catch (Throwable e) {
            // Fallback to standard synchronous teleport
            player.teleport(location);
        }
    }

}
