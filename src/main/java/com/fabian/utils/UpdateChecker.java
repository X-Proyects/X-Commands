package com.fabian.utils;

import com.fabian.XCommands;
import com.fabian.managers.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker {

    private final XCommands plugin;
    private final int resourceId;
    private String latestVersion;
    private boolean updateAvailable;

    public UpdateChecker(XCommands plugin, int resourceId) {
        this.plugin = plugin;
        this.resourceId = resourceId;
        this.updateAvailable = false;
    }

    public void checkForUpdates() {
        checkForUpdates(null);
    }

    public void checkForUpdates(CommandSender sender) {
        SchedulerUtils.runTaskAsynchronously(plugin, () -> {
            try {
                String currentVersion = plugin.getPluginMeta().getVersion();

                // Spigot API for resource versions
                URL url = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + resourceId);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "Fabian/X-Commands/" + currentVersion);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String version = reader.readLine();
                reader.close();

                this.latestVersion = version;
                LanguageManager lang = plugin.getLanguageManager();

                if (latestVersion != null && isNewerVersion(currentVersion, latestVersion)) {
                    this.updateAvailable = true;

                    if (sender != null) {
                        sender.sendMessage(
                                lang.getMessageWithPrefix("update-available", currentVersion, latestVersion));
                        sender.sendMessage(lang.getMessageWithPrefix("update-download", getDownloadUrl()));
                    } else {
                        Bukkit.getConsoleSender()
                                .sendMessage(
                                        lang.getMessageWithPrefix("update-available", currentVersion, latestVersion));
                        Bukkit.getConsoleSender()
                                .sendMessage(lang.getMessageWithPrefix("update-download", getDownloadUrl()));
                    }
                } else {
                    if (sender != null) {
                        sender.sendMessage(lang.getMessageWithPrefix("update-current"));
                    } else {
                        Bukkit.getConsoleSender().sendMessage(lang.getMessageWithPrefix("update-current"));
                    }
                }

            } catch (Exception e) {
                if (sender != null) {
                    sender.sendMessage(plugin.getLanguageManager().getMessageWithPrefix("update-error"));
                } else {
                    Bukkit.getConsoleSender()
                            .sendMessage(plugin.getLanguageManager().getMessageWithPrefix("update-error"));
                }
            }
        });
    }

    private boolean isNewerVersion(String current, String remote) {
        if (remote == null || remote.isEmpty())
            return false;

        // Remove 'v' or 'V' if present
        String v1 = current.toLowerCase().replace("v", "");
        String v2 = remote.toLowerCase().replace("v", "");

        String[] currentParts = v1.split("\\.");
        String[] remoteParts = v2.split("\\.");

        int length = Math.max(currentParts.length, remoteParts.length);
        for (int i = 0; i < length; i++) {
            int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i].replaceAll("[^0-9]", "")) : 0;
            int remotePart = i < remoteParts.length ? Integer.parseInt(remoteParts[i].replaceAll("[^0-9]", "")) : 0;

            if (remotePart > currentPart)
                return true;
            if (remotePart < currentPart)
                return false;
        }

        return false;
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public String getDownloadUrl() {
        return "https://www.spigotmc.org/resources/" + resourceId + "/";
    }
}
