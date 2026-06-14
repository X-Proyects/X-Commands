package com.fabian.xcommands.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.lang.reflect.Method;

/**
 * Utility class to handle cross-version compatibility between Spigot and Paper.
 * Handles differences in Inventory creation, Title sending, and ActionBar.
 */
@SuppressWarnings({"unchecked", "deprecation"})
public class CompatibilityUtils {

    private static volatile Boolean isPaper = null;

    /**
     * Checks if the server is running Paper (for Adventure API support).
     */
    public static boolean isPaper() {
        if (isPaper == null) {
            DebugLogger.debug("Detecting server platform (Paper/Spigot)...");
            try {
                // Check for a class that is definitely Paper and likely to stay
                Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
                isPaper = true;
                DebugLogger.debug("Detected Paper (Folia RegionizedServer)");
                return true;
            } catch (ClassNotFoundException ignored) {}
            
            try {
                Class.forName("io.papermc.paper.plugin.configuration.PluginMeta");
                isPaper = true;
            } catch (ClassNotFoundException e) {
                isPaper = false;
                DebugLogger.debug("Detected Spigot (not Paper)");
            }
        }
        return isPaper;
    }

    /**
     * Creates an inventory with compatibility for both Spigot (String) and Paper
     * (Component).
     */
    public static Inventory createInventory(InventoryHolder holder, int size, String title) {
        String translatedTitle = ColorUtils.translate(title);

        if (isPaper()) {
            try {
                // net.kyori.adventure.text.Component component =
                // ColorUtils.miniMessage(translatedTitle);
                // We use reflection to call Bukkit.createInventory(InventoryHolder, int,
                // Component)
                Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
                Method deserializeMethod = Class
                        .forName("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer")
                        .getMethod("legacySection");
                Object serializer = deserializeMethod.invoke(null);
                Object component = serializer.getClass().getMethod("deserialize", String.class).invoke(serializer,
                        translatedTitle);

                Method createInv = Bukkit.class.getMethod("createInventory", InventoryHolder.class, int.class,
                        componentClass);
                return (Inventory) createInv.invoke(null, holder, size, component);
            } catch (Exception e) {
                // Fallback to String version if something fails
            }
        }

        // Default Spigot/Legacy version
        return Bukkit.createInventory(holder, size, translatedTitle);
    }

    /**
     * Sends an ActionBar message with compatibility.
     */
    public static void sendActionBar(Player player, String message) {
        if (player == null)
            return;
        String translated = ColorUtils.translate(message);

        if (isPaper()) {
            try {
                Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
                Method deserializeMethod = Class
                        .forName("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer")
                        .getMethod("legacySection");
                Object serializer = deserializeMethod.invoke(null);
                Object component = serializer.getClass().getMethod("deserialize", String.class).invoke(serializer,
                        translated);

                Method sendActionBar = player.getClass().getMethod("sendActionBar", componentClass);
                sendActionBar.invoke(player, component);
                return;
            } catch (Exception ignored) {
            }
        }

        // Spigot fallback
        try {
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(translated));
        } catch (Exception ignored) {
        }
    }

    /**
     * Sends a Title message with compatibility.
     */
    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (player == null)
            return;

        String t = ColorUtils.translate(title);
        String s = ColorUtils.translate(subtitle);

        // Standard Bukkit method (works on almost everything, though deprecated on
        // Paper)
        player.sendTitle(t, s, fadeIn, stay, fadeOut);
    }

    /**
     * Sets the display name of an ItemMeta with compatibility.
     */
    public static void setDisplayName(org.bukkit.inventory.meta.ItemMeta meta, String name) {
        if (meta == null)
            return;
        String translated = ColorUtils.translate(name);

        if (isPaper()) {
            try {
                Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
                Class<?> decorationClass = Class.forName("net.kyori.adventure.text.format.TextDecoration");
                Class<?> stateClass = Class.forName("net.kyori.adventure.text.format.TextDecoration$State");
                
                Object italicDecoration = decorationClass.getField("ITALIC").get(null);
                Object stateFalse = stateClass.getField("FALSE").get(null);

                Method deserializeMethod = Class
                        .forName("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer")
                        .getMethod("legacySection");
                Object serializer = deserializeMethod.invoke(null);
                Method deserialize = serializer.getClass().getMethod("deserialize", String.class);
                Method decoration = componentClass.getMethod("decoration", decorationClass, stateClass);

                Object component = deserialize.invoke(serializer, translated);
                // Force italics to false
                component = decoration.invoke(component, italicDecoration, stateFalse);

                Method setDisplayName = meta.getClass().getMethod("displayName", componentClass);
                setDisplayName.invoke(meta, component);
                return;
            } catch (Exception ignored) {
            }
        }

        // Spigot fallback
        meta.setDisplayName(translated.startsWith("§") ? translated : "§f" + translated);
    }

    /**
     * Sets the lore of an ItemMeta with compatibility.
     */
    public static void setLore(org.bukkit.inventory.meta.ItemMeta meta, java.util.List<String> lore) {
        if (meta == null || lore == null)
            return;
        java.util.List<String> translatedLore = new java.util.ArrayList<>();
        for (String line : lore) {
            translatedLore.add(ColorUtils.translate(line));
        }

        if (isPaper()) {
            try {
                Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
                Class<?> decorationClass = Class.forName("net.kyori.adventure.text.format.TextDecoration");
                Class<?> stateClass = Class.forName("net.kyori.adventure.text.format.TextDecoration$State");
                
                Object italicDecoration = decorationClass.getField("ITALIC").get(null);
                Object stateFalse = stateClass.getField("FALSE").get(null);

                Method deserializeMethod = Class
                        .forName("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer")
                        .getMethod("legacySection");
                Object serializer = deserializeMethod.invoke(null);
                Method deserialize = serializer.getClass().getMethod("deserialize", String.class);
                Method decoration = componentClass.getMethod("decoration", decorationClass, stateClass);

                java.util.List<Object> components = new java.util.ArrayList<>();
                for (String line : translatedLore) {
                    // Handle empty or space-only lines to ensure they aren't purple
                    String finalLine = (line == null || line.trim().isEmpty()) ? "§7 " : line;
                    Object comp = deserialize.invoke(serializer, finalLine);
                    // Force italics to false to prevent purple lore effect
                    comp = decoration.invoke(comp, italicDecoration, stateFalse);
                    components.add(comp);
                }

                Method setLore = meta.getClass().getMethod("lore", java.util.List.class);
                setLore.invoke(meta, components);
                return;
            } catch (Exception ignored) {
            }
        }

        // Spigot fallback - prepend reset and gray color if it's likely to be purple
        java.util.List<String> spigotLore = new java.util.ArrayList<>();
        for (String line : translatedLore) {
            if (!line.startsWith("§")) {
                spigotLore.add("§7" + line);
            } else {
                spigotLore.add(line);
            }
        }
        meta.setLore(spigotLore);
    }

    /**
     * Gets the display name of an ItemMeta as a legacy String.
     */
    public static String getDisplayName(org.bukkit.inventory.meta.ItemMeta meta) {
        if (meta == null)
            return "";

        if (isPaper()) {
            try {
                Method getDisplayName = meta.getClass().getMethod("displayName");
                Object component = getDisplayName.invoke(meta);
                if (component == null)
                    return "";

                Method serializeMethod = Class
                        .forName("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer")
                        .getMethod("legacySection");
                Object serializer = serializeMethod.invoke(null);
                Method serialize = serializer.getClass().getMethod("serialize",
                        Class.forName("net.kyori.adventure.text.Component"));

                return (String) serialize.invoke(serializer, component);
            } catch (Exception ignored) {
            }
        }

        // Spigot fallback
        return meta.hasDisplayName() ? meta.getDisplayName() : "";
    }

    /**
     * Sends a message to a CommandSender with compatibility.
     */
    public static void sendMessage(org.bukkit.command.CommandSender sender, String message) {
        if (sender == null || message == null || message.isEmpty()) return;
        String translated = ColorUtils.translate(message);
        if (translated.isEmpty()) return;

        if (isPaper()) {
            try {
                Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
                Method deserializeMethod = Class.forName("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer")
                        .getMethod("legacySection");
                Object serializer = deserializeMethod.invoke(null);
                Object component = serializer.getClass().getMethod("deserialize", String.class).invoke(serializer, translated);
                if (component == null) return;

                Method sendMessage = sender.getClass().getMethod("sendMessage", componentClass);
                sendMessage.invoke(sender, component);
                return;
            } catch (Exception ignored) {}
        }

        // Spigot fallback - Using Spigot API if possible for better formatting support
        if (sender instanceof Player) {
            try {
                ((Player) sender).spigot().sendMessage(net.md_5.bungee.api.chat.TextComponent.fromLegacyText(translated));
                return;
            } catch (NoSuchMethodError | Exception ignored) {}
        }

        // Console or fallback — strip § codes to avoid garbled output on Windows CP437/CP850
        sender.sendMessage(org.bukkit.ChatColor.stripColor(translated));
    }

    /**
     * Broadcasts a message to all players with compatibility.
     */
    public static void broadcast(String message) {
        if (message == null) return;
        String translated = ColorUtils.translate(message);

        if (isPaper()) {
            try {
                Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
                Method deserializeMethod = Class.forName("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer")
                        .getMethod("legacySection");
                Object serializer = deserializeMethod.invoke(null);
                Object component = serializer.getClass().getMethod("deserialize", String.class).invoke(serializer, translated);

                Method broadcast = Bukkit.class.getMethod("broadcast", componentClass);
                broadcast.invoke(null, component);
                return;
            } catch (Exception ignored) {}
        }

        // Spigot fallback
        Bukkit.broadcastMessage(translated);
    }

    /**
     * Gets the display name of a player with compatibility.
     */
    public static String getPlayerDisplayName(Player player) {
        if (player == null) return "";

        if (isPaper()) {
            try {
                Method displayName = player.getClass().getMethod("displayName");
                Object component = displayName.invoke(player);
                if (component == null) return player.getName();

                Method serializeMethod = Class
                        .forName("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer")
                        .getMethod("legacySection");
                Object serializer = serializeMethod.invoke(null);
                Method serialize = serializer.getClass().getMethod("serialize",
                        Class.forName("net.kyori.adventure.text.Component"));

                return (String) serialize.invoke(serializer, component);
            } catch (Exception ignored) {
            }
        }

        // Spigot fallback
        return player.getDisplayName();
    }

    /**
     * Gets the plugin version with compatibility.
     */
    @SuppressWarnings("deprecation")
    public static String getVersion(org.bukkit.plugin.Plugin plugin) {
        if (isPaper()) {
            try {
                Method getPluginMeta = plugin.getClass().getMethod("getPluginMeta");
                Object meta = getPluginMeta.invoke(plugin);
                return (String) meta.getClass().getMethod("getVersion").invoke(meta);
            } catch (Exception ignored) {}
        }
        return plugin.getDescription().getVersion();
    }
}
