package dev.rancho.animal;

import dev.rancho.RanchoPlugin;
import dev.rancho.util.Saver;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Razas de cada especie (breeds.yml). Cada raza modifica genes, producción, carne y crecimiento. */
public final class BreedRegistry {

    private final RanchoPlugin plugin;
    private final File file;
    private YamlConfiguration cfg;
    private boolean added;
    private final Map<String, Breed> map = new LinkedHashMap<>();

    public BreedRegistry(RanchoPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "breeds.yml");
        load();
    }

    public void load() {
        cfg = YamlConfiguration.loadConfiguration(file);
        added = false;
        writeDefaults();
        if (added) {
            save();
        }
        map.clear();
        ConfigurationSection sec = cfg.getConfigurationSection("breeds");
        if (sec != null) {
            for (String id : sec.getKeys(false)) {
                map.put(id, new Breed(id, this));
            }
        }
    }

    YamlConfiguration cfg() { return cfg; }

    public YamlConfiguration cfgPublic() { return cfg; }

    public void save() {
        Saver.save(plugin, cfg, file);
    }

    public Breed get(String id) {
        return id == null || id.isEmpty() ? null : map.get(id);
    }

    public List<Breed> all() { return new ArrayList<>(map.values()); }

    public int bonus(String breedId, Gene g) {
        Breed b = get(breedId);
        return b == null || !b.enabled() ? 0 : b.bonus(g);
    }

    public double prodMult(String breedId) {
        Breed b = get(breedId);
        return b == null || !b.enabled() ? 1.0 : b.prodMult();
    }

    public double meatMult(String breedId) {
        Breed b = get(breedId);
        return b == null || !b.enabled() ? 1.0 : b.meatMult();
    }

    public double growthMult(String breedId) {
        Breed b = get(breedId);
        return b == null || !b.enabled() ? 1.0 : b.growthMult();
    }

    /** Elige una raza al azar (por peso) para una especie; "" si no tiene razas. */
    public String pick(String speciesId) {
        List<Breed> pool = new ArrayList<>();
        int total = 0;
        for (Breed b : map.values()) {
            if (b.enabled() && b.speciesId().equals(speciesId) && b.weight() > 0) {
                pool.add(b);
                total += b.weight();
            }
        }
        if (pool.isEmpty()) {
            return "";
        }
        int roll = dev.rancho.util.Util.rndInt(1, total);
        for (Breed b : pool) {
            roll -= b.weight();
            if (roll <= 0) {
                return b.id();
            }
        }
        return pool.get(0).id();
    }

    public Breed create(String id, String name, String templateId) {
        if (map.containsKey(id)) {
            return map.get(id);
        }
        ConfigurationSection tpl = cfg.getConfigurationSection("breeds." + templateId);
        if (tpl != null) {
            for (String k : tpl.getKeys(true)) {
                if (!tpl.isConfigurationSection(k)) {
                    cfg.set("breeds." + id + "." + k, tpl.get(k));
                }
            }
        }
        cfg.set("breeds." + id + ".name", name);
        cfg.set("breeds." + id + ".default", false);
        save();
        load();
        return map.get(id);
    }

    public void delete(String id) {
        cfg.set("breeds." + id, null);
        save();
        load();
    }

    public boolean isDefault(String id) {
        return cfg.getBoolean("breeds." + id + ".default", false);
    }

    // ------------------------------------------------------------------ valores por defecto

    private void def(String id, String species, String name, int weight, double prod, double meat, double growth,
                     Object... bonuses) {
        if (cfg.contains("breeds." + id)) {
            return;
        }
        added = true;
        String p = "breeds." + id + ".";
        cfg.set(p + "default", true);
        cfg.set(p + "name", name);
        cfg.set(p + "enabled", true);
        cfg.set(p + "species", species);
        cfg.set(p + "weight", weight);
        cfg.set(p + "prod-mult", prod);
        cfg.set(p + "meat-mult", meat);
        cfg.set(p + "growth-mult", growth);
        for (Gene g : Gene.values()) {
            cfg.set(p + "bonus." + g.name(), 0);
        }
        for (int i = 0; i + 1 < bonuses.length; i += 2) {
            cfg.set(p + "bonus." + bonuses[i], bonuses[i + 1]);
        }
    }

    private void writeDefaults() {
        def("holstein", "cow", "Holstein", 40, 1.3, 0.9, 1.0, "HEALTH", -3);
        def("angus", "cow", "Angus", 35, 0.8, 1.35, 1.0, "HEALTH", 5);
        def("jersey", "cow", "Jersey", 25, 1.1, 1.0, 1.05, "QUALITY", 8, "SIZE", -8);
        def("merino", "sheep", "Merino", 50, 1.35, 0.9, 1.0, "QUALITY", 6);
        def("suffolk", "sheep", "Suffolk", 50, 0.85, 1.3, 1.0, "SIZE", 6);
        def("iberico", "pig", "Ibérico", 40, 1.0, 1.2, 0.9, "QUALITY", 10);
        def("landrace", "pig", "Landrace", 60, 1.0, 1.0, 1.1, "FERTILITY", 12);
        def("leghorn", "chicken", "Leghorn", 55, 1.4, 0.8, 1.0, "FERTILITY", 5);
        def("plymouth", "chicken", "Plymouth Rock", 45, 1.0, 1.1, 1.0, "HEALTH", 8, "RESISTANCE", 10);
        def("arabian", "horse", "Árabe", 30, 1.0, 1.0, 1.0, "SPEED", 15, "RESISTANCE", 12, "HEALTH", -5);
        def("clydesdale", "horse", "Clydesdale", 35, 1.0, 1.0, 1.0, "HEALTH", 15, "SPEED", -8, "JUMP", -5);
        def("mustang", "horse", "Mustang", 35, 1.0, 1.0, 1.0, "JUMP", 10, "RESISTANCE", 8);
        def("giant_rabbit", "rabbit", "Conejo Gigante", 50, 1.0, 1.3, 1.0, "SIZE", 15);
        def("angora", "rabbit", "Angora", 50, 1.0, 1.4, 1.0, "QUALITY", 10);
        def("alpine", "goat", "Alpina", 55, 1.3, 0.9, 1.0, "PRODUCTION", 6);
        def("boer", "goat", "Boer", 45, 0.9, 1.3, 1.0, "HEALTH", 4);
    }
}
