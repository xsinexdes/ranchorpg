package dev.rancho.food;

import dev.rancho.RanchoPlugin;
import dev.rancho.util.Saver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Cocinas de Rancho colocadas en el mundo y los platos que están cocinando. */
public final class KitchenManager {

    /** Un plato en preparación en una cocina. */
    public static final class Job {
        public final String foodId;
        public final int amount;
        public final int batches;
        public final int quality;
        public final long finishAt;
        public final String owner;
        public final double mult;
        public boolean notified;

        public Job(String foodId, int amount, int batches, int quality, long finishAt, String owner, double mult) {
            this.foodId = foodId;
            this.amount = amount;
            this.batches = batches;
            this.quality = quality;
            this.finishAt = finishAt;
            this.owner = owner;
            this.mult = mult;
        }
    }

    private final RanchoPlugin plugin;
    private final File file;
    private final Map<String, String> stations = new HashMap<>();
    private final Map<String, Job> jobs = new HashMap<>();
    private BukkitTask task;

    public KitchenManager(RanchoPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data/kitchen.yml");
        load();
    }

    private void load() {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        for (String k : y.getStringList("stations")) { // formato antiguo: solo cocinas
            stations.put(k, "kitchen");
        }
        for (String line : y.getStringList("stations2")) {
            String[] sp = line.split("\\|", -1);
            if (sp.length >= 2) {
                stations.put(sp[0], sp[1]);
            }
        }
        for (String line : y.getStringList("jobs")) {
            String[] p = line.split("\\|", -1);
            if (p.length < 8) continue;
            try {
                Job j = new Job(p[1], Integer.parseInt(p[2]), Integer.parseInt(p[3]), Integer.parseInt(p[4]),
                        Long.parseLong(p[5]), p[6], Double.parseDouble(p[7]));
                jobs.put(p[0], j);
            } catch (NumberFormatException ignored) {
                // línea corrupta: se omite
            }
        }
    }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        List<String> st = new ArrayList<>();
        for (Map.Entry<String, String> e : stations.entrySet()) {
            st.add(e.getKey() + "|" + e.getValue());
        }
        y.set("stations2", st);
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, Job> e : jobs.entrySet()) {
            Job j = e.getValue();
            lines.add(e.getKey() + "|" + j.foodId + "|" + j.amount + "|" + j.batches + "|" + j.quality + "|"
                    + j.finishAt + "|" + j.owner + "|" + j.mult);
        }
        y.set("jobs", lines);
        Saver.save(plugin, y, file);
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 40L, 20L);
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
        }
        save();
    }

    private static Block blockOf(String key) {
        String[] k = key.split(";");
        if (k.length < 4) {
            return null;
        }
        World w = Bukkit.getWorld(k[0]);
        if (w == null) {
            return null;
        }
        try {
            return w.getBlockAt(Integer.parseInt(k[1]), Integer.parseInt(k[2]), Integer.parseInt(k[3]));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String keyOf(Block b) {
        return b.getWorld().getName() + ";" + b.getX() + ";" + b.getY() + ";" + b.getZ();
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Job> e : new ArrayList<>(jobs.entrySet())) {
            Job j = e.getValue();
            Block b = blockOf(e.getKey());
            if (b == null || !b.getWorld().isChunkLoaded(b.getX() >> 4, b.getZ() >> 4)) {
                continue;
            }
            if (now < j.finishAt) {
                b.getWorld().spawnParticle(Particle.SMOKE, b.getLocation().add(0.5, 1.1, 0.5), 3, 0.15, 0.05, 0.15, 0.01);
            } else if (!j.notified) {
                j.notified = true;
                b.getWorld().playSound(b.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.4f);
                try {
                    Player owner = Bukkit.getPlayer(UUID.fromString(j.owner));
                    Food f = plugin.foods().get(j.foodId);
                    if (owner != null && f != null) {
                        owner.sendMessage(plugin.lang().prefixed("food.ready", "food", f.name()));
                    }
                } catch (IllegalArgumentException ignored) {
                    // dueño inválido
                }
            }
        }
    }

    /** Material del bloque de cada tipo de estación. */
    public static Material materialOf(String type) {
        return switch (type) {
            case "cask" -> Material.COMPOSTER;
            case "mill" -> Material.GRINDSTONE;
            default -> Material.SMOKER;
        };
    }

    /** Tipo de estación (kitchen, cask, mill) del bloque, o null si no es una. */
    public String typeOf(Block b) {
        String key = keyOf(b);
        String type = stations.get(key);
        if (type == null) {
            return null;
        }
        if (b.getType() != materialOf(type)) { // destruida por otros medios
            stations.remove(key);
            jobs.remove(key);
            return null;
        }
        return type;
    }

    public boolean isStation(Block b) {
        return typeOf(b) != null;
    }

    public void add(Block b, String type) {
        stations.put(keyOf(b), type);
        save();
    }

    public void remove(Block b) {
        stations.remove(keyOf(b));
        jobs.remove(keyOf(b));
        save();
    }

    public Job job(Block b) {
        return jobs.get(keyOf(b));
    }

    public void start(Block b, Job j) {
        jobs.put(keyOf(b), j);
        save();
    }

    public void clear(Block b) {
        jobs.remove(keyOf(b));
        save();
    }
}
