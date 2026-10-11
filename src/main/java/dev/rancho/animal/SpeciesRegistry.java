package dev.rancho.animal;

import dev.rancho.RanchoPlugin;
import dev.rancho.util.Saver;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Carga, crea y guarda las especies (species.yml). Incluye las 7 especies por defecto. */
public final class SpeciesRegistry {

    private final RanchoPlugin plugin;
    private final File file;
    private YamlConfiguration cfg;
    private final Map<String, Species> map = new LinkedHashMap<>();

    public SpeciesRegistry(RanchoPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "species.yml");
        load();
    }

    public void load() {
        cfg = YamlConfiguration.loadConfiguration(file);
        if (!cfg.isConfigurationSection("species")) {
            writeDefaults();
            save();
        }
        rebuild();
    }

    private void rebuild() {
        map.clear();
        ConfigurationSection sec = cfg.getConfigurationSection("species");
        if (sec == null) {
            return;
        }
        for (String id : sec.getKeys(false)) {
            map.put(id, new Species(id, this));
        }
    }

    YamlConfiguration cfg() { return cfg; }

    /** Acceso público a la configuración (para los menús de edición). */
    public YamlConfiguration cfgPublic() { return cfg; }

    public void save() {
        Saver.save(plugin, cfg, file);
    }

    public Species get(String id) { return map.get(id); }
    public List<Species> all() { return new ArrayList<>(map.values()); }
    public Set<String> ids() { return map.keySet(); }

    /** Primera especie habilitada cuyo tipo de entidad coincide (la de por defecto tiene prioridad). */
    public Species forEntity(EntityType type) {
        for (Species s : map.values()) {
            if (s.enabled() && s.entity() == type) {
                return s;
            }
        }
        return null;
    }

    /** Crea una especie copiando la plantilla indicada. */
    public Species create(String id, String name, String templateId) {
        if (map.containsKey(id)) {
            return map.get(id);
        }
        ConfigurationSection tpl = cfg.getConfigurationSection("species." + templateId);
        if (tpl != null) {
            for (String k : tpl.getKeys(true)) {
                if (!tpl.isConfigurationSection(k)) {
                    cfg.set("species." + id + "." + k, tpl.get(k));
                }
            }
        } else {
            template(id);
        }
        cfg.set("species." + id + ".name", name);
        save();
        rebuild();
        return map.get(id);
    }

    public void delete(String id) {
        cfg.set("species." + id, null);
        save();
        rebuild();
    }

    public boolean isDefault(String id) {
        return List.of("cow", "sheep", "pig", "chicken", "horse", "rabbit", "goat").contains(id);
    }

    // ------------------------------------------------------------------ valores por defecto

    private void set(String id, String k, Object v) {
        cfg.set("species." + id + "." + k, v);
    }

    private void template(String id) {
        set(id, "enabled", true);
        set(id, "name", id);
        set(id, "entity", "COW");
        set(id, "base-health", 10.0);
        set(id, "baby-days", 1.0);
        set(id, "adult-days", 3.0);
        set(id, "old-days", 25.0);
        set(id, "lifespan-days", 35.0);
        set(id, "gestation-days", 2.0);
        set(id, "litter-min", 1);
        set(id, "litter-max", 1);
        set(id, "max-density", 4);
        set(id, "hunger-rate", 50.0);
        set(id, "thirst-rate", 70.0);
        set(id, "hygiene-rate", 15.0);
        set(id, "cold-limit", 0.0);
        set(id, "heat-limit", 35.0);
        set(id, "grazes", true);
        set(id, "feed", "WHEAT,feed_basic,feed_premium,forage");
        set(id, "meat-name", "Carne");
        set(id, "meat-material", "COOKED_BEEF");
        set(id, "meat-min", 1);
        set(id, "meat-max", 3);
        set(id, "meat-price", 20.0);
        set(id, "leather-name", "Cuero");
        set(id, "leather-min", 0);
        set(id, "leather-max", 2);
        set(id, "leather-price", 12.0);
        set(id, "produce", "NONE");
        set(id, "produce-name", "Producto");
        set(id, "produce-material", "WHEAT");
        set(id, "produce-days", 1.0);
        set(id, "produce-price", 15.0);
        for (String s : new String[]{"spring", "summer", "autumn", "winter"}) {
            set(id, "produce." + s, true);
            set(id, "breed." + s, !s.equals("winter"));
            set(id, "suffer." + s, false);
        }
    }

    private void def(String id, Object... kv) {
        template(id);
        for (int i = 0; i + 1 < kv.length; i += 2) {
            set(id, (String) kv[i], kv[i + 1]);
        }
    }

    private void writeDefaults() {
        def("cow", "name", "Vaca", "entity", "COW", "base-health", 10.0, "produce", "MILK",
                "produce-name", "Leche de Vaca", "produce-material", "MILK_BUCKET", "produce-days", 0.5,
                "produce-price", 18.0, "meat-name", "Carne de Res", "meat-material", "COOKED_BEEF",
                "meat-min", 2, "meat-max", 4, "meat-price", 24.0, "leather-name", "Cuero de Vaca",
                "leather-min", 1, "leather-max", 2, "cold-limit", -5.0, "heat-limit", 33.0);
        def("sheep", "name", "Oveja", "entity", "SHEEP", "base-health", 8.0, "produce", "WOOL",
                "produce-name", "Lana de Oveja", "produce-material", "WHITE_WOOL", "produce-days", 1.0,
                "produce-price", 14.0, "meat-name", "Carne de Oveja", "meat-material", "COOKED_MUTTON",
                "meat-min", 1, "meat-max", 3, "meat-price", 20.0, "leather-min", 0, "leather-max", 0,
                "cold-limit", -10.0, "heat-limit", 30.0, "suffer.summer", true);
        def("pig", "name", "Cerdo", "entity", "PIG", "base-health", 10.0, "gestation-days", 1.5,
                "litter-min", 2, "litter-max", 4, "feed", "CARROT,POTATO,BEETROOT,feed_basic,feed_premium,forage",
                "meat-name", "Carne de Cerdo", "meat-material", "COOKED_PORKCHOP", "meat-min", 2, "meat-max", 4,
                "meat-price", 22.0, "leather-min", 0, "leather-max", 0, "cold-limit", 0.0, "heat-limit", 32.0);
        def("chicken", "name", "Gallina", "entity", "CHICKEN", "base-health", 4.0, "baby-days", 0.5,
                "adult-days", 1.5, "old-days", 12.0, "lifespan-days", 18.0, "gestation-days", 0.7,
                "litter-min", 2, "litter-max", 3, "max-density", 6, "produce", "EGGS",
                "produce-name", "Huevo de Gallina", "produce-material", "EGG", "produce-days", 0.5,
                "produce-price", 8.0, "feed", "WHEAT_SEEDS,BEETROOT_SEEDS,feed_basic,feed_premium,forage",
                "meat-name", "Pollo", "meat-material", "COOKED_CHICKEN", "meat-min", 1, "meat-max", 1,
                "meat-price", 12.0, "leather-min", 0, "leather-max", 0, "cold-limit", 0.0, "heat-limit", 34.0);
        def("horse", "name", "Caballo", "entity", "HORSE", "base-health", 22.0, "baby-days", 2.0,
                "adult-days", 6.0, "old-days", 40.0, "lifespan-days", 55.0, "gestation-days", 4.0,
                "max-density", 3, "feed", "APPLE,CARROT,GOLDEN_CARROT,WHEAT,feed_basic,feed_premium,forage",
                "meat-name", "Carne de Caballo", "meat-material", "COOKED_BEEF", "meat-min", 2, "meat-max", 3,
                "meat-price", 30.0, "leather-name", "Cuero de Caballo", "leather-min", 1, "leather-max", 3,
                "leather-price", 18.0, "cold-limit", -8.0, "heat-limit", 34.0);
        def("rabbit", "name", "Conejo", "entity", "RABBIT", "base-health", 3.0, "baby-days", 0.5,
                "adult-days", 1.2, "old-days", 10.0, "lifespan-days", 14.0, "gestation-days", 0.6,
                "litter-min", 2, "litter-max", 4, "max-density", 6,
                "feed", "CARROT,DANDELION,feed_basic,feed_premium,forage", "meat-name", "Carne de Conejo",
                "meat-material", "COOKED_RABBIT", "meat-min", 1, "meat-max", 2, "meat-price", 10.0,
                "leather-name", "Piel de Conejo", "leather-min", 0, "leather-max", 1, "leather-price", 6.0,
                "cold-limit", -3.0, "heat-limit", 30.0);
        def("goat", "name", "Cabra", "entity", "GOAT", "base-health", 10.0, "produce", "MILK",
                "produce-name", "Leche de Cabra", "produce-material", "MILK_BUCKET", "produce-days", 0.7,
                "produce-price", 20.0, "meat-name", "Carne de Cabra", "meat-material", "COOKED_MUTTON",
                "meat-min", 1, "meat-max", 3, "meat-price", 22.0, "leather-name", "Cuero de Cabra",
                "leather-min", 0, "leather-max", 1, "cold-limit", -10.0, "heat-limit", 30.0);
    }
}
