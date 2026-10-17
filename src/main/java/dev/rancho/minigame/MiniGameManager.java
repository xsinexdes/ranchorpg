package dev.rancho.minigame;

import dev.rancho.RanchoPlugin;
import dev.rancho.util.Text;
import dev.rancho.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Minijuego de empuje (para pescar y extraer miel): un objetivo se mueve por una barra en la ActionBar y
 * hay que mantener la zona verde sobre él empujando con click derecho. Si la zona se aleja, el progreso baja.
 */
public final class MiniGameManager implements Listener {

    /** Resultado del minijuego. */
    public interface Callback {
        /** @param perfect true si el progreso nunca cayó por debajo del 25% */
        void win(Player p, boolean perfect);

        void lose(Player p);
    }

    private static final int LEN = 26;

    private static final class Session {
        final Player player;
        final String title;
        final Callback cb;
        final long endAt;
        final double speed;
        final double erratic;
        final int width;
        double fish = LEN / 2.0;
        double fishVel;
        double zone = LEN / 2.0;
        double progress = 30;
        long lastPush;
        boolean perfect = true;

        Session(Player player, String title, Callback cb, long endAt, double speed, double erratic, int width) {
            this.player = player;
            this.title = title;
            this.cb = cb;
            this.endAt = endAt;
            this.speed = speed;
            this.erratic = erratic;
            this.width = width;
        }
    }

    private final RanchoPlugin plugin;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private BukkitTask task;

    public MiniGameManager(RanchoPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 2L, 2L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
        sessions.clear();
    }

    public boolean active(Player p) {
        return sessions.containsKey(p.getUniqueId());
    }

    /**
     * Empieza el minijuego.
     *
     * @param difficulty 0.05 (fácil) a 1.0 (muy difícil)
     */
    public void begin(Player p, String title, double difficulty, int seconds, Callback cb) {
        if (sessions.containsKey(p.getUniqueId())) {
            return;
        }
        double diff = Util.clamp(difficulty, 0.05, 1.0);
        double speed = 0.35 + 0.9 * diff;
        double erratic = 0.05 + 0.25 * diff;
        int width = Math.max(4, 8 - (int) Math.round(diff * 4));
        sessions.put(p.getUniqueId(), new Session(p, title, cb, System.currentTimeMillis() + seconds * 1000L,
                speed, erratic, width));
        p.sendMessage(plugin.lang().prefixed("minigame.start"));
        p.playSound(p.getLocation(), Sound.ENTITY_FISHING_BOBBER_SPLASH, 1f, 1f);
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (Iterator<Session> it = sessions.values().iterator(); it.hasNext(); ) {
            Session s = it.next();
            Player p = s.player;
            if (!p.isOnline() || p.isDead()) {
                it.remove();
                continue;
            }
            if (now >= s.endAt) {
                it.remove();
                finish(s, false);
                continue;
            }
            // el objetivo se mueve de forma errática
            if (Util.chance(s.erratic)) {
                s.fishVel = (Util.rnd() * 2 - 1) * s.speed * 1.6;
            }
            s.fish += s.fishVel;
            if (s.fish < 0) {
                s.fish = 0;
                s.fishVel = Math.abs(s.fishVel);
            }
            if (s.fish > LEN - 1) {
                s.fish = LEN - 1;
                s.fishVel = -Math.abs(s.fishVel);
            }
            // la zona sube si empujas y cae si no
            boolean push = now - s.lastPush < 260;
            s.zone += push ? 1.35 : -0.75;
            double half = s.width / 2.0;
            s.zone = Util.clamp(s.zone, half, LEN - 1 - half);
            boolean in = Math.abs(s.fish - s.zone) <= half;
            s.progress += in ? 2.3 : -1.5;
            if (!in && s.progress < 25) {
                s.perfect = false;
            }
            s.progress = Util.clamp(s.progress, 0, 100);
            render(s);
            if (s.progress >= 100) {
                it.remove();
                finish(s, true);
            } else if (s.progress <= 0) {
                it.remove();
                finish(s, false);
            }
        }
    }

    private void render(Session s) {
        StringBuilder sb = new StringBuilder(s.title).append(" &8[");
        int f = (int) Math.round(s.fish);
        for (int i = 0; i < LEN; i++) {
            boolean zoneCell = Math.abs(i - s.zone) <= s.width / 2.0;
            if (i == f) {
                sb.append(zoneCell ? "&e♦" : "&c♦");
            } else {
                sb.append(zoneCell ? "&a█" : "&8░");
            }
        }
        sb.append("&8] &f").append((int) s.progress).append('%');
        Util.actionBar(s.player, Text.color(sb.toString()));
    }

    private void finish(Session s, boolean won) {
        Player p = s.player;
        if (won) {
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.4f);
            s.cb.win(p, s.perfect);
        } else {
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
            s.cb.lose(p);
        }
    }

    // ------------------------------------------------------------------ entrada

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Session s = sessions.get(e.getPlayer().getUniqueId());
        if (s == null) {
            return;
        }
        s.lastPush = System.currentTimeMillis();
        e.setCancelled(true); // el click empuja; no lanza la caña ni abre nada
        e.setUseItemInHand(Event.Result.DENY);
        e.setUseInteractedBlock(Event.Result.DENY);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFish(PlayerFishEvent e) {
        if (sessions.containsKey(e.getPlayer().getUniqueId())) {
            e.setCancelled(true); // mientras luchas con el pez no se puede lanzar la caña
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        sessions.remove(e.getPlayer().getUniqueId());
    }
}
