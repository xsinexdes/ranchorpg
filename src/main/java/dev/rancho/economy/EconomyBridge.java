package dev.rancho.economy;

import dev.rancho.RanchoPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;

/** Economía: usa Vault (opcional, por reflexión) o, si no está, las monedas internas del plugin. */
public final class EconomyBridge {

    private final RanchoPlugin plugin;
    private Object vault;
    private Method mBalance, mDeposit, mWithdraw;

    public EconomyBridge(RanchoPlugin plugin) {
        this.plugin = plugin;
        hook();
    }

    /** Intenta enlazar con Vault. Se puede llamar de nuevo tras cambiar la config. */
    public void hook() {
        vault = null;
        if (!plugin.getConfig().getBoolean("economy.use-vault", true)
                || Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return;
        }
        try {
            Class<?> eco = Class.forName("net.milkbowl.vault.economy.Economy");
            @SuppressWarnings({"unchecked", "rawtypes"})
            RegisteredServiceProvider<?> rsp = Bukkit.getServicesManager().getRegistration((Class) eco);
            if (rsp == null) {
                return;
            }
            vault = rsp.getProvider();
            mBalance = eco.getMethod("getBalance", OfflinePlayer.class);
            mDeposit = eco.getMethod("depositPlayer", OfflinePlayer.class, double.class);
            mWithdraw = eco.getMethod("withdrawPlayer", OfflinePlayer.class, double.class);
            plugin.getLogger().info("Vault detectado: usando su economía.");
        } catch (Exception ex) {
            vault = null;
        }
    }

    public boolean usingVault() {
        return vault != null;
    }

    public double balance(Player p) {
        if (vault != null) {
            try {
                return (double) mBalance.invoke(vault, p);
            } catch (Exception ignored) {
                // cae a la economía interna
            }
        }
        return plugin.farmers().data(p).coins;
    }

    public void deposit(Player p, double amount) {
        if (amount <= 0) {
            return;
        }
        if (vault != null) {
            try {
                mDeposit.invoke(vault, p, amount);
                return;
            } catch (Exception ignored) {
                // cae a la economía interna
            }
        }
        plugin.farmers().data(p).coins += amount;
    }

    /** Cobra {@code amount}; devuelve false si no hay saldo suficiente. */
    public boolean withdraw(Player p, double amount) {
        if (amount <= 0) {
            return true;
        }
        if (balance(p) < amount) {
            return false;
        }
        if (vault != null) {
            try {
                mWithdraw.invoke(vault, p, amount);
                return true;
            } catch (Exception ignored) {
                // cae a la economía interna
            }
        }
        plugin.farmers().data(p).coins -= amount;
        return true;
    }
}
