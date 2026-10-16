package dev.rancho.visual;

import dev.rancho.RanchoPlugin;
import dev.rancho.season.Season;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/** Partículas ambientales ligeras según la estación (pétalos, brasas de otoño, nieve...). Solo cliente, por jugador. */
public final class AmbientEffects {

    private final RanchoPlugin plugin;
    private BukkitTask task;

    public AmbientEffects(RanchoPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 60L, 40L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
    }

    private void tick() {
        if (!plugin.getConfig().getBoolean("visuals.season-particles", true)) {
            return;
        }
        Season s = plugin.seasons().current();
        for (Player p : Bukkit.getOnlinePlayers()) {
            World w = p.getWorld();
            if (w.getEnvironment() != World.Environment.NORMAL) {
                continue;
            }
            Location l = p.getLocation();
            if (w.getHighestBlockYAt(l) > l.getBlockY() + 2) {
                continue; // bajo techo
            }
            Location at = l.clone().add(0, 3, 0);
            switch (s) {
                case SPRING -> p.spawnParticle(Particle.CHERRY_LEAVES, at, 4, 6, 1.5, 6, 0.01);
                case SUMMER -> {
                    if (w.getTime() > 13000) {
                        p.spawnParticle(Particle.END_ROD, l.clone().add(0, 1.5, 0), 2, 6, 1.5, 6, 0.005);
                    }
                }
                case AUTUMN -> p.spawnParticle(Particle.CRIMSON_SPORE, at, 5, 6, 1.5, 6, 0.0);
                case WINTER -> p.spawnParticle(Particle.SNOWFLAKE, at, 5, 6, 1.5, 6, 0.01);
            }
        }
    }
}
