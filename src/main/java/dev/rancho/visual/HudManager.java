package dev.rancho.visual;

import dev.rancho.RanchoPlugin;
import dev.rancho.climate.TemperatureManager;
import dev.rancho.farmer.PlayerData;
import dev.rancho.util.Text;
import dev.rancho.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Información del rancho (estación, día, clima, temperatura, hora, nivel y monedas) en la pantalla del jugador:
 * panel lateral (por defecto), ActionBar, BossBar (la gestiona SeasonManager) o nada. Cada jugador puede
 * ocultarlo con /rancho hud.
 */
public final class HudManager {

    private final RanchoPlugin plugin;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();
    private BukkitTask task;

    public HudManager(RanchoPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        long period = Math.max(20L, (long) (plugin.getConfig().getDouble("hud.update-seconds", 2) * 20));
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 60L, period);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            clear(p);
        }
    }

    private String mode() {
        return plugin.getConfig().getString("hud.mode", "SIDEBAR").toUpperCase();
    }

    private void tick() {
        String mode = mode();
        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerData pd = plugin.farmers().data(p);
            if (pd.hudOff || !(mode.equals("SIDEBAR") || mode.equals("ACTIONBAR"))) {
                clear(p);
                continue;
            }
            try {
                if (mode.equals("SIDEBAR")) {
                    sidebar(p);
                } else {
                    clear(p);
                    Util.actionBar(p, Text.color(compact(p)));
                }
            } catch (RuntimeException ex) {
                plugin.getLogger().warning("Error actualizando el HUD: " + ex);
            }
        }
    }

    /** Quita el panel lateral propio y devuelve al jugador su scoreboard principal. */
    public void clear(Player p) {
        Scoreboard sb = boards.remove(p.getUniqueId());
        if (sb != null && p.getScoreboard() == sb) {
            p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    private String clock(World w) {
        long t = w.getTime();
        int hour = (int) ((t / 1000 + 6) % 24);
        int min = (int) ((t % 1000) * 60 / 1000);
        return String.format("%02d:%02d", hour, min);
    }

    private String weather(World w) {
        if (w.getEnvironment() != World.Environment.NORMAL) {
            return "&7-";
        }
        if (plugin.weather().flooded(w)) {
            return "&c¡Inundación!";
        }
        if (w.isThundering()) {
            return "&8Tormenta";
        }
        if (w.hasStorm()) {
            return "&9Lluvia";
        }
        return plugin.weather().drought() ? "&6Sequía" : "&eDespejado";
    }

    private String compact(Player p) {
        var s = plugin.seasons();
        double t = plugin.temperature().getTemperature(p.getLocation());
        return s.name(s.current()) + " &7día &f" + s.day() + "&7/&f" + s.length() + " &8| &7" + weather(p.getWorld())
                + " &8| " + TemperatureManager.colorFor(t) + Util.fmt(t) + "°C &8| &f" + clock(p.getWorld());
    }

    private List<String> lines(Player p) {
        var s = plugin.seasons();
        PlayerData pd = plugin.farmers().data(p);
        double t = plugin.temperature().getTemperature(p.getLocation());
        List<String> l = new ArrayList<>();
        l.add("&7Estación: " + s.name(s.current()));
        l.add("&7Día: &f" + s.day() + "&7/&f" + s.length());
        if (s.event() != null) {
            l.add("&6" + plugin.lang().get("event." + s.event().key()) + " &7(" + s.eventDays() + "d)");
        }
        l.add("&7Clima: " + weather(p.getWorld()));
        l.add("&7Temp: " + TemperatureManager.colorFor(t) + Util.fmt(t) + "°C");
        l.add("&7Hora: &f" + clock(p.getWorld()));
        l.add("&7Granjero: &aNv " + pd.level);
        l.add("&7Monedas: &6" + Util.fmt0(plugin.economy().balance(p)));
        return l;
    }

    private void sidebar(Player p) {
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        Scoreboard sb = boards.get(p.getUniqueId());
        if (sb == null) {
            if (p.getScoreboard() != main) {
                return; // otro plugin gestiona su scoreboard: no lo tocamos
            }
            sb = Bukkit.getScoreboardManager().getNewScoreboard();
            boards.put(p.getUniqueId(), sb);
            p.setScoreboard(sb);
        } else if (p.getScoreboard() != sb) {
            boards.remove(p.getUniqueId()); // otro plugin tomó el control
            return;
        }
        Objective obj = sb.getObjective("rancho");
        if (obj == null) {
            obj = sb.registerNewObjective("rancho", "dummy", Text.color("&6&lRancho"));
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        }
        List<String> lines = lines(p);
        for (int i = 0; i < 12; i++) {
            String entry = "§" + Integer.toHexString(i) + "§r";
            if (i >= lines.size()) {
                sb.resetScores(entry);
                continue;
            }
            Team t = sb.getTeam("l" + i);
            if (t == null) {
                t = sb.registerNewTeam("l" + i);
                t.addEntry(entry);
            }
            t.setPrefix(Text.color(lines.get(i)));
            obj.getScore(entry).setScore(lines.size() - i);
        }
    }
}
