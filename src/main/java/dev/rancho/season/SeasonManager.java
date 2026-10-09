package dev.rancho.season;

import dev.rancho.RanchoPlugin;
import dev.rancho.util.Saver;
import dev.rancho.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;

/**
 * Gestiona la estación actual, el día y los eventos de estación (cosecha, helada, ola de calor, sequía).
 * El estado se guarda en data/season.yml; los días transcurridos con el servidor apagado se aplican al arrancar.
 */
public final class SeasonManager implements Listener {

    private final RanchoPlugin plugin;
    private final File file;

    private Season current = Season.SPRING;
    private int day = 1;
    private long lastDayIndex = -1;
    private SeasonEvent event;
    private int eventDays;

    private BossBar bar;
    private BukkitTask task;

    public SeasonManager(RanchoPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data/season.yml");
        load();
    }

    // ------------------------------------------------------------------ persistencia

    private void load() {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        try {
            current = Season.valueOf(y.getString("season", "SPRING"));
        } catch (IllegalArgumentException ex) {
            current = Season.SPRING;
        }
        day = Math.max(1, y.getInt("day", 1));
        lastDayIndex = y.getLong("last-day-index", -1);
        eventDays = y.getInt("event-days", 0);
        try {
            event = y.contains("event") ? SeasonEvent.valueOf(y.getString("event", "")) : null;
        } catch (IllegalArgumentException ex) {
            event = null;
        }
        if (event == null) {
            eventDays = 0;
        }
    }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("season", current.name());
        y.set("day", day);
        y.set("last-day-index", lastDayIndex);
        if (event != null) {
            y.set("event", event.name());
            y.set("event-days", eventDays);
        }
        Saver.save(plugin, y, file);
    }

    // ------------------------------------------------------------------ ciclo de vida

    public void start() {
        if (lastDayIndex < 0) {
            lastDayIndex = dayIndex();
        }
        reload();
        tick();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 100L, 100L);
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        removeBar();
        save();
    }

    public void reload() {
        try {
            if (config().getBoolean("seasons.bossbar", true)) {
                if (bar == null) {
                    bar = Bukkit.createBossBar("", current.barColor(), BarStyle.SEGMENTED_10);
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        bar.addPlayer(p);
                    }
                }
                updateBar();
            } else {
                removeBar();
            }
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("No se pudo crear la BossBar de estación: " + ex);
            bar = null;
        }
    }

    private void removeBar() {
        if (bar != null) {
            bar.removeAll();
            bar = null;
        }
    }

    // ------------------------------------------------------------------ tiempo

    private FileConfiguration config() {
        return plugin.getConfig();
    }

    private long dayIndex() {
        if (config().getBoolean("seasons.use-real-time", false)) {
            return System.currentTimeMillis() / 86_400_000L;
        }
        World w = Bukkit.getWorld(config().getString("seasons.world", "world"));
        if (w == null && !Bukkit.getWorlds().isEmpty()) {
            w = Bukkit.getWorlds().get(0);
        }
        return w == null ? lastDayIndex : w.getFullTime() / 24000L;
    }

    private void clampDay() {
        if (day > length()) {
            day = length();
        }
    }

    public int length() {
        return Math.max(1, config().getInt("seasons.length-days", 7));
    }

    private void tick() {
        clampDay();
        long idx = dayIndex();
        if (idx < lastDayIndex) {
            lastDayIndex = idx;
            save();
            return;
        }
        long diff = idx - lastDayIndex;
        if (diff > 0) {
            lastDayIndex = idx;
            advance(diff);
        }
        updateBar();
    }

    /** Avanza {@code days} días, cambiando de estación y lanzando eventos si corresponde. */
    public void advance(long days) {
        if (days <= 0) {
            return;
        }
        Season before = current;
        long length = length();
        long total = (day - 1) + days;
        current = current.plus(total / length);
        day = (int) (total % length) + 1;

        if (current != before) {
            if (event != null && event.season() != current) {
                endEvent();
            }
            announce();
        }
        long rolls = Math.min(days, 30);
        for (long i = 0; i < rolls; i++) {
            newDay();
        }
        updateBar();
        save();
    }

    public void setSeason(Season season) {
        current = season;
        day = 1;
        if (event != null && event.season() != current) {
            endEvent();
        }
        announce();
        updateBar();
        save();
    }

    // ------------------------------------------------------------------ eventos

    private void newDay() {
        if (event != null) {
            eventDays--;
            if (eventDays <= 0) {
                endEvent();
            }
            return;
        }
        for (SeasonEvent ev : SeasonEvent.values()) {
            if (ev.season() != current) {
                continue;
            }
            String p = "events." + ev.key() + ".";
            if (config().getBoolean(p + "enabled", true) && Util.rnd() * 100.0 < config().getDouble(p + "chance", 10)) {
                startEvent(ev, Math.max(1, config().getInt(p + "duration-days", 2)));
                return;
            }
        }
    }

    public void startEvent(SeasonEvent ev, int days) {
        event = ev;
        eventDays = days;
        String name = plugin.lang().get("event." + ev.key());
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(plugin.lang().prefixed("event.start", "event", name, "days", days));
            p.sendTitle(name, plugin.lang().get("event.subtitle"), 10, 60, 20);
            p.playSound(p.getLocation(), Sound.EVENT_RAID_HORN, 0.6f, 1.0f);
        }
        save();
    }

    public void endEvent() {
        if (event == null) {
            return;
        }
        String name = plugin.lang().get("event." + event.key());
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(plugin.lang().prefixed("event.end", "event", name));
        }
        event = null;
        eventDays = 0;
        save();
    }

    public SeasonEvent event() { return event; }
    public int eventDays() { return eventDays; }

    public boolean eventActive(SeasonEvent ev) {
        return event == ev;
    }

    // ------------------------------------------------------------------ avisos

    private void announce() {
        String name = name(current);
        boolean title = config().getBoolean("seasons.title", true);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(plugin.lang().prefixed("season.change.chat", "season", name));
            if (title) {
                p.sendTitle(plugin.lang().get("season.change.title", "season", name),
                        plugin.lang().get("season.change.subtitle", "season", name), 10, 60, 20);
            }
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.2f);
        }
    }

    private void updateBar() {
        if (bar == null) {
            return;
        }
        try {
            String title = plugin.lang().get("season.bossbar", "season", name(current), "day", day,
                    "length", length());
            if (event != null) {
                title += plugin.lang().get("season.bossbar.event", "event",
                        plugin.lang().get("event." + event.key()));
            }
            if (title.length() > 250) {
                title = title.substring(0, 250);
            }
            bar.setTitle(title);
            bar.setColor(current.barColor());
            double progress = day / (double) Math.max(1, length());
            if (Double.isNaN(progress) || Double.isInfinite(progress)) {
                progress = 0.0;
            }
            bar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Error actualizando la BossBar de estación: " + ex);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (bar != null) {
            bar.addPlayer(e.getPlayer());
        }
    }

    // ------------------------------------------------------------------ consulta

    public Season current() { return current; }
    public int day() { return day; }

    public String name(Season season) {
        return plugin.lang().get("season." + season.key());
    }

    /** Ajuste de temperatura (°C) de la estación actual más el del evento activo. */
    public double temperatureOffset() {
        double off = config().getDouble("seasons.offset." + current.key(), 0.0);
        if (event == SeasonEvent.FROST) {
            off += config().getDouble("events.frost.temp-offset", -10);
        } else if (event == SeasonEvent.HEATWAVE) {
            off += config().getDouble("events.heatwave.temp-offset", 10);
        }
        return off;
    }
}
