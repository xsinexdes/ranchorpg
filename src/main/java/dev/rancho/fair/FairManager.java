package dev.rancho.fair;

import dev.rancho.RanchoPlugin;
import dev.rancho.animal.AnimalData;
import dev.rancho.animal.Gene;
import dev.rancho.animal.Mutation;
import dev.rancho.animal.Mutations;
import dev.rancho.animal.Species;
import dev.rancho.animal.Stage;
import dev.rancho.item.CustomItems;
import dev.rancho.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ferias de ganado: durante la feria cada jugador inscribe un animal por especie; al terminar se juzga
 * por nivel, genética, cuidado y mutaciones, y los 3 mejores ganan monedas, XP y un trofeo.
 */
public final class FairManager {

    private record Scored(UUID player, AnimalData animal, double score) {}

    private final RanchoPlugin plugin;
    private final Map<String, Map<UUID, UUID>> entries = new HashMap<>();
    private boolean active;
    private long endsAt;
    private long nextFair;
    private BukkitTask task;

    public FairManager(RanchoPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        nextFair = System.currentTimeMillis() + (long) (plugin.getConfig().getDouble("fair.interval-hours", 3) * 3600000L);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 400L, 400L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
    }

    private void tick() {
        long now = System.currentTimeMillis();
        if (!active && plugin.getConfig().getBoolean("fair.enabled", false) && now >= nextFair) {
            begin();
        } else if (active && now >= endsAt) {
            finish();
        }
    }

    public boolean isActive() {
        return active;
    }

    public long remainingMs() {
        return active ? Math.max(0, endsAt - System.currentTimeMillis()) : 0;
    }

    /** Abre la feria (también desde /rancho feria start). */
    public void begin() {
        if (active) {
            return;
        }
        active = true;
        entries.clear();
        long minutes = Math.max(1, plugin.getConfig().getLong("fair.duration-minutes", 10));
        endsAt = System.currentTimeMillis() + minutes * 60000L;
        Bukkit.broadcastMessage(plugin.lang().prefixed("fair.start", "minutes", minutes));
    }

    public void cancel() {
        if (active) {
            active = false;
            entries.clear();
            Bukkit.broadcastMessage(plugin.lang().prefixed("fair.cancelled"));
            nextFair = System.currentTimeMillis() + (long) (plugin.getConfig().getDouble("fair.interval-hours", 3) * 3600000L);
        }
    }

    public boolean enter(Player p, AnimalData d, Species sp) {
        if (!active) {
            p.sendMessage(plugin.lang().prefixed("fair.inactive"));
            return false;
        }
        if (d.stage(sp) != Stage.ADULT) {
            p.sendMessage(plugin.lang().prefixed("fair.not-adult", "name", d.name));
            return false;
        }
        if (!d.owner.equals(p.getUniqueId().toString())) {
            p.sendMessage(plugin.lang().prefixed("fair.not-owner", "name", d.name));
            return false;
        }
        entries.computeIfAbsent(sp.id(), k -> new HashMap<>()).put(p.getUniqueId(), d.entity.getUniqueId());
        p.sendMessage(plugin.lang().prefixed("fair.entered", "name", d.name, "species", sp.name(),
                "score", Util.fmt0(score(d))));
        return true;
    }

    /** Puntuación del juez. */
    public double score(AnimalData d) {
        double genes = 0;
        for (Gene g : Gene.values()) {
            genes += d.eff(g);
        }
        genes /= Gene.values().length;
        double mut = 0;
        for (String id : d.mutations) {
            Mutation m = Mutations.get(id);
            if (m == null) continue;
            if (m.positive()) {
                mut += switch (m.rarity()) {
                    case COMMON -> 1;
                    case UNCOMMON -> 3;
                    case RARE -> 6;
                    case EPIC -> 10;
                    case LEGENDARY -> 15;
                };
            } else {
                mut -= 4;
            }
        }
        return d.level * 8 + genes * 0.5 + d.care * 0.3 + mut + (d.disease == dev.rancho.animal.Disease.NONE ? 5 : -10);
    }

    private static final String[] TROPHIES = {"fair_gold", "fair_silver", "fair_bronze"};

    /** Juzga la feria y reparte los premios. */
    public void finish() {
        if (!active) {
            return;
        }
        active = false;
        nextFair = System.currentTimeMillis() + (long) (plugin.getConfig().getDouble("fair.interval-hours", 3) * 3600000L);
        boolean any = false;
        for (Map.Entry<String, Map<UUID, UUID>> en : entries.entrySet()) {
            Species sp = plugin.species().get(en.getKey());
            if (sp == null) continue;
            List<Scored> list = new ArrayList<>();
            for (Map.Entry<UUID, UUID> pe : en.getValue().entrySet()) {
                Entity ent = Bukkit.getEntity(pe.getValue());
                AnimalData d = ent == null ? null : plugin.animals().get(ent);
                if (d != null) {
                    list.add(new Scored(pe.getKey(), d, score(d)));
                }
            }
            list.sort((a, b) -> Double.compare(b.score(), a.score()));
            for (int i = 0; i < list.size() && i < 3; i++) {
                Scored s = list.get(i);
                any = true;
                Player pl = Bukkit.getPlayer(s.player());
                String name = pl != null ? pl.getName() : Bukkit.getOfflinePlayer(s.player()).getName();
                Bukkit.broadcastMessage(plugin.lang().prefixed("fair.result", "species", sp.name(), "place", i + 1,
                        "player", name == null ? "?" : name, "animal", s.animal().name,
                        "score", Util.fmt0(s.score())));
                if (pl != null) {
                    plugin.economy().deposit(pl, plugin.getConfig().getDouble("fair.prize." + (i + 1), 100));
                    plugin.farmers().addXp(pl, plugin.getConfig().getDouble("fair.xp." + (i + 1), 30));
                    Util.giveOrDrop(pl, CustomItems.create(TROPHIES[i], 1));
                    if (i == 0) {
                        plugin.farmers().stat(pl, "fair_wins", 1);
                    }
                }
            }
        }
        if (!any) {
            Bukkit.broadcastMessage(plugin.lang().prefixed("fair.empty"));
        }
        entries.clear();
    }
}
