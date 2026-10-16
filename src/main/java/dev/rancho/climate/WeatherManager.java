package dev.rancho.climate;

import dev.rancho.RanchoPlugin;
import dev.rancho.season.SeasonEvent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Clima RPG: la sequía impide que llueva, la inundación mantiene la lluvia y, además, si llueve durante
 * mucho tiempo seguido los campos se inundan solos (los cultivos normales se pudren; los submarinos prosperan).
 */
public final class WeatherManager implements Listener {

    private final RanchoPlugin plugin;
    private final Map<String, Double> rain = new HashMap<>();
    private final Set<String> flooded = new HashSet<>();
    private BukkitTask task;

    public WeatherManager(RanchoPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 400L, 400L);
        SeasonEvent ev = plugin.seasons().event();
        if (ev != null) {
            onEventStart(ev);
        }
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
    }

    public boolean drought() {
        return plugin.seasons().eventActive(SeasonEvent.DROUGHT);
    }

    /** ¿Están inundados los campos de este mundo? (evento Inundación o lluvia prolongada) */
    public boolean flooded(World w) {
        return plugin.seasons().eventActive(SeasonEvent.FLOOD) || flooded.contains(w.getName());
    }

    /** Aplica el tiempo que exige un evento al empezar. */
    public void onEventStart(SeasonEvent ev) {
        for (World w : Bukkit.getWorlds()) {
            if (w.getEnvironment() != World.Environment.NORMAL) {
                continue;
            }
            if (ev == SeasonEvent.DROUGHT) {
                w.setStorm(false);
                w.setThundering(false);
            } else if (ev == SeasonEvent.FLOOD) {
                w.setStorm(true);
                w.setWeatherDuration(24000 * Math.max(1, plugin.seasons().eventDays()));
            }
        }
    }

    private void tick() {
        double limit = Math.max(1, plugin.getConfig().getDouble("weather.flood-minutes", 8)) * 60.0;
        for (World w : Bukkit.getWorlds()) {
            if (w.getEnvironment() != World.Environment.NORMAL) {
                continue;
            }
            String name = w.getName();
            double secs = rain.getOrDefault(name, 0.0);
            if (w.hasStorm()) {
                secs += w.isThundering() ? 30 : 20;
            } else {
                secs = Math.max(0, secs - 40);
            }
            rain.put(name, secs);
            boolean was = flooded.contains(name);
            boolean now = was ? secs > limit * 0.5 : secs >= limit;
            if (now && !was) {
                flooded.add(name);
                for (Player p : w.getPlayers()) {
                    p.sendMessage(plugin.lang().prefixed("weather.flood-start"));
                }
            } else if (!now && was) {
                flooded.remove(name);
                for (Player p : w.getPlayers()) {
                    p.sendMessage(plugin.lang().prefixed("weather.flood-end"));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWeather(WeatherChangeEvent e) {
        if (e.getWorld().getEnvironment() != World.Environment.NORMAL) {
            return;
        }
        if (e.toWeatherState() && drought()) {
            e.setCancelled(true); // durante la sequía no llueve
        } else if (!e.toWeatherState() && plugin.seasons().eventActive(SeasonEvent.FLOOD)) {
            e.setCancelled(true); // durante la inundación no deja de llover
        }
    }
}
