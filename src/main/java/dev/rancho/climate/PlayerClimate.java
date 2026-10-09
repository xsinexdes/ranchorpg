package dev.rancho.climate;

import dev.rancho.RanchoPlugin;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

/** Efectos opcionales de la temperatura sobre los jugadores (activable en config). */
public final class PlayerClimate {

    private final RanchoPlugin plugin;
    private BukkitTask task;

    public PlayerClimate(RanchoPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 200L, 200L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        if (!plugin.getConfig().getBoolean("temperature.players.enabled", false)) {
            return;
        }
        double cold = plugin.getConfig().getDouble("temperature.players.cold-threshold", -2);
        double heat = plugin.getConfig().getDouble("temperature.players.heat-threshold", 40);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }
            double t = plugin.temperature().getTemperature(p.getLocation());
            double warmth = leatherPieces(p) * 3.0;
            if (t + warmth <= cold && !plugin.food().has(p.getUniqueId(), "warm")) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 260, 0, true, false, true));
                if (t + warmth <= cold - 8) {
                    p.damage(1.0);
                }
                dev.rancho.util.Util.actionBar(p, plugin.lang().get("climate.cold"));
            } else if (t >= heat && !plugin.food().has(p.getUniqueId(), "cool")) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 260, 0, true, false, true));
                p.setExhaustion(p.getExhaustion() + 1.0f);
                dev.rancho.util.Util.actionBar(p, plugin.lang().get("climate.hot"));
            }
        }
    }

    private int leatherPieces(Player p) {
        int n = 0;
        for (ItemStack a : p.getInventory().getArmorContents()) {
            if (a != null && a.getType().name().startsWith("LEATHER_")) {
                n++;
            }
        }
        return n;
    }
}
