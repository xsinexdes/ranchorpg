package dev.rancho.farmer;

import dev.rancho.RanchoPlugin;
import dev.rancho.util.Saver;
import dev.rancho.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Nivel de Granjero, habilidades, logros y misiones simples de rancho. */
public final class FarmerManager implements Listener {

    /** Logro: se consigue al alcanzar {@code target} en la estadística {@code stat}. */
    public record Achievement(String id, String name, String desc, String stat, int target, double reward) {}

    /** Misión: completar {@code target} acciones de la estadística {@code stat}. */
    public record QuestDef(String id, String desc, String stat, int target, double coins, int xp) {}

    public static final List<Achievement> ACHIEVEMENTS = List.of(
            new Achievement("first_harvest", "Primera cosecha", "Cosecha tu primer cultivo", "harvest", 1, 20),
            new Achievement("farmer", "Agricultor", "Cosecha 100 cultivos", "harvest", 100, 300),
            new Achievement("master_farmer", "Maestro agricultor", "Cosecha 1000 cultivos", "harvest", 1000, 2000),
            new Achievement("first_birth", "Primera cría", "Consigue una cría", "born", 1, 50),
            new Achievement("breeder", "Criador", "Consigue 25 crías", "born", 25, 600),
            new Achievement("healer", "Veterinario", "Cura a 10 animales", "cured", 10, 250),
            new Achievement("cook", "Cocinero", "Cocina 25 platos", "cooked", 25, 250),
            new Achievement("chef", "Chef del rancho", "Cocina 250 platos", "cooked", 250, 1500),
            new Achievement("geneticist", "Genetista", "Provoca o consigue 5 mutaciones", "mutations", 5, 500),
            new Achievement("angler", "Pescador", "Pesca 25 peces", "fished", 25, 250),
            new Achievement("beekeeper", "Apicultor", "Recoge 20 botellas de miel", "honey", 20, 250),
            new Achievement("champion", "Campeón de la feria", "Gana una feria de ganado", "fair_wins", 1, 600),
            new Achievement("producer", "Productor", "Obtén 50 productos de animales", "produced", 50, 300),
            new Achievement("level_5", "Granjero experto", "Alcanza el nivel 5 de granjero", "level", 5, 300),
            new Achievement("level_10", "Granjero legendario", "Alcanza el nivel 10 de granjero", "level", 10, 1000));

    public static final List<QuestDef> QUESTS = List.of(
            new QuestDef("q_harvest", "Cosecha 20 cultivos", "harvest", 20, 120, 40),
            new QuestDef("q_harvest2", "Cosecha 50 cultivos", "harvest", 50, 300, 90),
            new QuestDef("q_cook", "Cocina 12 platos", "cooked", 12, 150, 50),
            new QuestDef("q_feed", "Alimenta a tus animales 15 veces", "fed", 15, 90, 30),
            new QuestDef("q_born", "Consigue 2 crías", "born", 2, 200, 70),
            new QuestDef("q_cure", "Cura a 2 animales enfermos", "cured", 2, 150, 60),
            new QuestDef("q_produce", "Obtén 10 productos de animales", "produced", 10, 120, 40),
            new QuestDef("q_fish", "Pesca 8 peces", "fished", 8, 130, 45),
            new QuestDef("q_honey", "Recoge 5 botellas de miel", "honey", 5, 140, 45),
            new QuestDef("q_brush", "Limpia a tus animales 10 veces", "brushed", 10, 80, 25),
            new QuestDef("q_water", "Riega la tierra 15 veces", "watered", 15, 90, 30),
            new QuestDef("q_plant", "Planta 20 cultivos", "planted", 20, 100, 35));

    private static final int ACTIVE_QUESTS = 3;

    private final RanchoPlugin plugin;
    private final File file;
    private final Map<UUID, PlayerData> players = new HashMap<>();
    private BukkitTask saveTask;

    public FarmerManager(RanchoPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data/players.yml");
        load();
    }

    // ------------------------------------------------------------------ persistencia

    private void load() {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = y.getConfigurationSection("players");
        if (sec == null) {
            return;
        }
        for (String key : sec.getKeys(false)) {
            UUID id;
            try {
                id = UUID.fromString(key);
            } catch (IllegalArgumentException ex) {
                continue;
            }
            ConfigurationSection s = sec.getConfigurationSection(key);
            if (s == null) {
                continue;
            }
            PlayerData d = new PlayerData();
            d.name = s.getString("name", "");
            d.xp = s.getDouble("xp");
            d.level = Math.max(1, s.getInt("level", 1));
            d.skillPoints = s.getInt("skill-points");
            d.coins = s.getDouble("coins");
            readMap(s.getConfigurationSection("skills"), d.skills);
            readMap(s.getConfigurationSection("stats"), d.stats);
            readMap(s.getConfigurationSection("quests"), d.quests);
            d.achievements.addAll(s.getStringList("achievements"));
            players.put(id, d);
        }
    }

    private void readMap(ConfigurationSection s, Map<String, Integer> out) {
        if (s == null) {
            return;
        }
        for (String k : s.getKeys(false)) {
            out.put(k, s.getInt(k));
        }
    }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerData> e : players.entrySet()) {
            PlayerData d = e.getValue();
            String p = "players." + e.getKey() + ".";
            y.set(p + "name", d.name);
            y.set(p + "xp", d.xp);
            y.set(p + "level", d.level);
            y.set(p + "skill-points", d.skillPoints);
            y.set(p + "coins", d.coins);
            for (Map.Entry<String, Integer> s : d.skills.entrySet()) y.set(p + "skills." + s.getKey(), s.getValue());
            for (Map.Entry<String, Integer> s : d.stats.entrySet()) y.set(p + "stats." + s.getKey(), s.getValue());
            for (Map.Entry<String, Integer> s : d.quests.entrySet()) y.set(p + "quests." + s.getKey(), s.getValue());
            y.set(p + "achievements", new ArrayList<>(d.achievements));
        }
        Saver.save(plugin, y, file);
    }

    public void start() {
        saveTask = Bukkit.getScheduler().runTaskTimer(plugin, this::save, 6000L, 6000L);
    }

    public void shutdown() {
        if (saveTask != null) {
            saveTask.cancel();
        }
        save();
    }

    // ------------------------------------------------------------------ acceso

    public PlayerData data(UUID id) {
        return players.computeIfAbsent(id, k -> new PlayerData());
    }

    public PlayerData data(Player p) {
        PlayerData d = data(p.getUniqueId());
        d.name = p.getName();
        return d;
    }

    public int level(UUID id) {
        return data(id).level;
    }

    public int skill(UUID id, Skill s) {
        return data(id).skill(s);
    }

    /** XP necesaria para pasar del nivel dado al siguiente. */
    public double xpNeeded(int level) {
        return Math.round(100.0 * Math.pow(level, 1.5));
    }

    public void addXp(Player p, double amount) {
        PlayerData d = data(p);
        double boost = plugin.food() != null && plugin.food().has(p.getUniqueId(), "farmer_xp") ? 1.5 : 1.0;
        d.xp += amount * plugin.getConfig().getDouble("farmer.xp-multiplier", 1.0) * boost;
        boolean leveled = false;
        while (d.xp >= xpNeeded(d.level)) {
            d.xp -= xpNeeded(d.level);
            d.level++;
            d.skillPoints++;
            leveled = true;
            p.sendMessage(plugin.lang().prefixed("farmer.levelup", "level", d.level));
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        }
        if (leveled) {
            setStat(p, "level", d.level);
        }
    }

    public boolean upgrade(Player p, Skill s) {
        PlayerData d = data(p);
        int cur = d.skill(s);
        if (cur >= Skill.MAX_LEVEL) {
            p.sendMessage(plugin.lang().prefixed("farmer.skill.max"));
            return false;
        }
        if (d.skillPoints <= 0) {
            p.sendMessage(plugin.lang().prefixed("farmer.skill.nopoints"));
            return false;
        }
        d.skillPoints--;
        d.skills.put(s.name(), cur + 1);
        p.sendMessage(plugin.lang().prefixed("farmer.skill.up", "skill", s.label(), "level", cur + 1));
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.6f);
        return true;
    }

    // ------------------------------------------------------------------ estadísticas, misiones y logros

    public void stat(Player p, String key, int amount) {
        PlayerData d = data(p);
        d.stats.merge(key, amount, Integer::sum);
        progressQuests(p, d, key, amount);
        checkAchievements(p, d);
    }

    private void setStat(Player p, String key, int value) {
        PlayerData d = data(p);
        d.stats.put(key, value);
        checkAchievements(p, d);
    }

    public void ensureQuests(Player p) {
        PlayerData d = data(p);
        while (d.quests.size() < ACTIVE_QUESTS) {
            List<QuestDef> pool = new ArrayList<>();
            for (QuestDef q : QUESTS) {
                if (!d.quests.containsKey(q.id())) {
                    pool.add(q);
                }
            }
            if (pool.isEmpty()) {
                return;
            }
            d.quests.put(pool.get(Util.rndInt(0, pool.size() - 1)).id(), 0);
        }
    }

    public QuestDef quest(String id) {
        for (QuestDef q : QUESTS) {
            if (q.id().equals(id)) {
                return q;
            }
        }
        return null;
    }

    private void progressQuests(Player p, PlayerData d, String key, int amount) {
        for (String id : new ArrayList<>(d.quests.keySet())) {
            QuestDef q = quest(id);
            if (q == null) {
                d.quests.remove(id);
                continue;
            }
            if (!q.stat().equals(key)) {
                continue;
            }
            int prog = d.quests.get(id) + amount;
            if (prog >= q.target()) {
                d.quests.remove(id);
                plugin.economy().deposit(p, q.coins());
                addXp(p, q.xp());
                p.sendMessage(plugin.lang().prefixed("quest.complete", "quest", q.desc(), "coins", Util.fmt0(q.coins())));
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                ensureQuests(p);
            } else {
                d.quests.put(id, prog);
            }
        }
    }

    private void checkAchievements(Player p, PlayerData d) {
        for (Achievement a : ACHIEVEMENTS) {
            if (!d.achievements.contains(a.id()) && d.stat(a.stat()) >= a.target()) {
                d.achievements.add(a.id());
                plugin.economy().deposit(p, a.reward());
                p.sendMessage(plugin.lang().prefixed("achievement.unlocked", "name", a.name(),
                        "coins", Util.fmt0(a.reward())));
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f);
            }
        }
    }

    // ------------------------------------------------------------------ eventos

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        ensureQuests(e.getPlayer());
        data(e.getPlayer());
        for (org.bukkit.NamespacedKey k : plugin.recipeKeys()) {
            e.getPlayer().discoverRecipe(k);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        save();
    }
}
