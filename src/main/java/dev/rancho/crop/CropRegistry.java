package dev.rancho.crop;

import dev.rancho.RanchoPlugin;
import dev.rancho.util.Saver;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Carga, crea y guarda las definiciones de cultivos (cropdefs.yml). 4 vanilla ajustados + 8 custom. */
public final class CropRegistry {

    /** Ingredientes de la receta de semilla de cada cultivo custom por defecto (da 2 semillas). */
    public static final Map<String, Material[]> SEED_RECIPES = new LinkedHashMap<>();

    static {
        SEED_RECIPES.put("golden_wheat", new Material[]{Material.WHEAT_SEEDS, Material.GOLD_NUGGET, Material.GOLD_NUGGET});
        SEED_RECIPES.put("maize", new Material[]{Material.CARROT, Material.WHEAT_SEEDS, Material.YELLOW_DYE});
        SEED_RECIPES.put("grape", new Material[]{Material.SWEET_BERRIES, Material.PURPLE_DYE, Material.BEETROOT_SEEDS});
        SEED_RECIPES.put("rice", new Material[]{Material.WHEAT_SEEDS, Material.DRIED_KELP, Material.SUGAR});
        SEED_RECIPES.put("lunar_vine", new Material[]{Material.BEETROOT_SEEDS, Material.AMETHYST_SHARD, Material.GLOW_BERRIES});
        SEED_RECIPES.put("fire_fruit", new Material[]{Material.POTATO, Material.BLAZE_POWDER, Material.NETHER_WART});
        SEED_RECIPES.put("frost_root", new Material[]{Material.CARROT, Material.SNOWBALL, Material.PACKED_ICE});
        SEED_RECIPES.put("magic_flower", new Material[]{Material.BEETROOT_SEEDS, Material.LAPIS_LAZULI, Material.AZURE_BLUET});
    }

    private final RanchoPlugin plugin;
    private final File file;
    private YamlConfiguration cfg;
    private final Map<String, CropDef> map = new LinkedHashMap<>();

    public CropRegistry(RanchoPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "cropdefs.yml");
        load();
    }

    public static boolean isCropBlock(Material m) {
        return m == Material.WHEAT || m == Material.CARROTS || m == Material.POTATOES || m == Material.BEETROOTS;
    }

    public void load() {
        cfg = YamlConfiguration.loadConfiguration(file);
        if (!cfg.isConfigurationSection("crops")) {
            writeDefaults();
            save();
        }
        rebuild();
    }

    private void rebuild() {
        map.clear();
        ConfigurationSection sec = cfg.getConfigurationSection("crops");
        if (sec == null) {
            return;
        }
        for (String id : sec.getKeys(false)) {
            map.put(id, new CropDef(id, this));
        }
    }

    YamlConfiguration cfg() { return cfg; }

    /** Acceso público a la configuración (para los menús de edición). */
    public YamlConfiguration cfgPublic() { return cfg; }

    public void save() {
        Saver.save(plugin, cfg, file);
    }

    public CropDef get(String id) { return map.get(id); }
    public List<CropDef> all() { return new ArrayList<>(map.values()); }

    /** Cultivo vanilla que corresponde a una semilla sin etiqueta (null si no es una semilla vanilla). */
    public CropDef forVanillaSeed(Material seed) {
        for (CropDef d : map.values()) {
            if (!d.custom() && d.enabled() && d.seedMaterial() == seed) {
                return d;
            }
        }
        return null;
    }

    /** Cultivo vanilla por defecto de un bloque de cultivo. */
    public CropDef forVanillaBlock(Material block) {
        for (CropDef d : map.values()) {
            if (!d.custom() && d.block() == block) {
                return d;
            }
        }
        return null;
    }

    public CropDef create(String id, String name, String templateId) {
        if (map.containsKey(id)) {
            return map.get(id);
        }
        ConfigurationSection tpl = cfg.getConfigurationSection("crops." + templateId);
        if (tpl != null) {
            for (String k : tpl.getKeys(true)) {
                if (!tpl.isConfigurationSection(k)) {
                    cfg.set("crops." + id + "." + k, tpl.get(k));
                }
            }
        } else {
            template(id);
        }
        cfg.set("crops." + id + ".name", name);
        cfg.set("crops." + id + ".product-name", name);
        cfg.set("crops." + id + ".custom", true);
        cfg.set("crops." + id + ".seed-material", "WHEAT_SEEDS");
        save();
        rebuild();
        return map.get(id);
    }

    public void delete(String id) {
        cfg.set("crops." + id, null);
        save();
        rebuild();
    }

    public boolean isDefault(String id) {
        return List.of("wheat", "carrot", "potato", "beetroot").contains(id) || SEED_RECIPES.containsKey(id);
    }

    // ------------------------------------------------------------------ valores por defecto

    private void set(String id, String k, Object v) {
        cfg.set("crops." + id + "." + k, v);
    }

    private void template(String id) {
        set(id, "enabled", true);
        set(id, "custom", false);
        set(id, "name", id);
        set(id, "block", "WHEAT");
        set(id, "seed-material", "WHEAT_SEEDS");
        set(id, "product-material", "WHEAT");
        set(id, "product-name", id);
        set(id, "growth-days", 3.0);
        set(id, "temp-min", 5.0);
        set(id, "temp-max", 30.0);
        set(id, "light-min", 9);
        set(id, "needs-sun", true);
        set(id, "needs-water", true);
        set(id, "needs-fertilizer", false);
        set(id, "improved-soil", false);
        set(id, "price", 6.0);
        set(id, "harvest-min", 1);
        set(id, "harvest-max", 3);
        for (String s : new String[]{"spring", "summer", "autumn", "winter"}) {
            set(id, "season." + s, !s.equals("winter"));
        }
    }

    private void def(String id, Object... kv) {
        template(id);
        for (int i = 0; i + 1 < kv.length; i += 2) {
            set(id, (String) kv[i], kv[i + 1]);
        }
    }

    private void seasons(String id, boolean sp, boolean su, boolean au, boolean wi) {
        set(id, "season.spring", sp);
        set(id, "season.summer", su);
        set(id, "season.autumn", au);
        set(id, "season.winter", wi);
    }

    private void writeDefaults() {
        // Cultivos vanilla ajustados al sistema RPG
        def("wheat", "name", "Trigo", "block", "WHEAT", "seed-material", "WHEAT_SEEDS", "product-material", "WHEAT",
                "product-name", "Trigo", "growth-days", 2.0, "price", 6.0);
        def("carrot", "name", "Zanahoria", "block", "CARROTS", "seed-material", "CARROT", "product-material", "CARROT",
                "product-name", "Zanahoria", "growth-days", 2.0, "temp-min", 3.0, "temp-max", 28.0, "price", 5.0);
        def("potato", "name", "Papa", "block", "POTATOES", "seed-material", "POTATO", "product-material", "POTATO",
                "product-name", "Papa", "growth-days", 2.5, "temp-min", 2.0, "temp-max", 28.0, "price", 5.0);
        def("beetroot", "name", "Remolacha", "block", "BEETROOTS", "seed-material", "BEETROOT_SEEDS",
                "product-material", "BEETROOT", "product-name", "Remolacha", "growth-days", 2.5,
                "temp-min", 0.0, "temp-max", 26.0, "price", 6.0);

        // 8 cultivos custom
        def("golden_wheat", "custom", true, "name", "Trigo Dorado", "block", "WHEAT", "seed-material", "WHEAT_SEEDS",
                "product-material", "WHEAT", "product-name", "Trigo Dorado", "growth-days", 3.0, "temp-min", 10.0,
                "temp-max", 30.0, "needs-fertilizer", true, "price", 14.0);
        seasons("golden_wheat", false, true, true, false);
        def("maize", "custom", true, "name", "Maíz", "block", "CARROTS", "seed-material", "PUMPKIN_SEEDS",
                "product-material", "BAKED_POTATO", "product-name", "Mazorca de Maíz", "growth-days", 3.5,
                "temp-min", 15.0, "temp-max", 35.0, "price", 12.0);
        seasons("maize", false, true, true, false);
        def("grape", "custom", true, "name", "Uva", "block", "BEETROOTS", "seed-material", "MELON_SEEDS",
                "product-material", "SWEET_BERRIES", "product-name", "Uva", "growth-days", 4.0, "temp-min", 12.0,
                "temp-max", 28.0, "needs-fertilizer", true, "price", 16.0);
        seasons("grape", true, true, true, false);
        def("rice", "custom", true, "name", "Arroz", "block", "WHEAT", "seed-material", "WHEAT_SEEDS",
                "product-material", "DRIED_KELP", "product-name", "Arroz", "growth-days", 3.0, "temp-min", 18.0,
                "temp-max", 35.0, "price", 10.0);
        seasons("rice", true, true, false, false);
        def("lunar_vine", "custom", true, "name", "Vid Lunar", "block", "BEETROOTS", "seed-material", "GLOW_BERRIES",
                "product-material", "GLOW_BERRIES", "product-name", "Fruto Lunar", "growth-days", 5.0,
                "temp-min", -2.0, "temp-max", 18.0, "light-min", 0, "needs-sun", false, "needs-fertilizer", true,
                "improved-soil", true, "price", 40.0);
        seasons("lunar_vine", false, false, true, true);
        def("fire_fruit", "custom", true, "name", "Fruta de Fuego", "block", "POTATOES", "seed-material", "NETHER_WART",
                "product-material", "APPLE", "product-name", "Fruta de Fuego", "growth-days", 4.0, "temp-min", 28.0,
                "temp-max", 50.0, "needs-water", false, "needs-fertilizer", true, "price", 35.0);
        seasons("fire_fruit", false, true, false, false);
        def("frost_root", "custom", true, "name", "Raíz Helada", "block", "CARROTS", "seed-material", "SNOWBALL",
                "product-material", "BEETROOT", "product-name", "Raíz Helada", "growth-days", 4.0, "temp-min", -15.0,
                "temp-max", 6.0, "price", 28.0);
        seasons("frost_root", false, false, true, true);
        def("magic_flower", "custom", true, "name", "Flor Mágica", "block", "BEETROOTS",
                "seed-material", "LILY_OF_THE_VALLEY", "product-material", "AZURE_BLUET", "product-name", "Flor Mágica",
                "growth-days", 6.0, "temp-min", 5.0, "temp-max", 25.0, "needs-fertilizer", true,
                "improved-soil", true, "price", 70.0);
        seasons("magic_flower", true, false, false, false);
    }
}
