package com.fabian.utils;

import com.fabian.XCommands;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Utility class for interacting with Vault Economy
 */
public class EconomyUtils {

    private static volatile Economy econ = null;

    /**
     * Setups the economy provider from Vault
     * 
     * @return true if economy was successfully setup
     */
    public static boolean setupEconomy() {
        XCommands plugin = XCommands.getInstance();

        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager()
                .getRegistration(Economy.class);

        if (rsp == null) {
            plugin.logWarning("Vault was found, but no Economy Provider (like EssentialsX) is installed.");
            return false;
        }

        econ = rsp.getProvider();
        return isEnabled();
    }

    /**
     * Gets the Vault economy provider
     * 
     * @return The economy provider
     */
    public static Economy getEconomy() {
        return econ;
    }

    /**
     * Checks if economy is enabled and provider is found
     * 
     * @return true if economy is available
     */
    public static boolean isEnabled() {
        return econ != null;
    }

    /**
     * Checks if a player has a certain amount of money
     */
    public static boolean has(Player player, double amount) {
        return isEnabled() && econ.has(player, amount);
    }

    /**
     * Gets a player's balance
     */
    public static double getBalance(Player player) {
        return isEnabled() ? econ.getBalance(player) : 0.0;
    }

    /**
     * Withdraws money from a player
     */
    public static void withdraw(Player player, double amount) {
        if (isEnabled()) {
            econ.withdrawPlayer(player, amount);
        }
    }

    /**
     * Deposits money to a player
     */
    public static void deposit(Player player, double amount) {
        if (isEnabled()) {
            econ.depositPlayer(player, amount);
        }
    }
}
