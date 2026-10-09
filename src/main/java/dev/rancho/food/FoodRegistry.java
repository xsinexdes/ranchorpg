package dev.rancho.food;

import dev.rancho.RanchoPlugin;
import dev.rancho.util.Saver;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Carga, crea y guarda las comidas y recetas de cocina (foods.yml). */
public final class FoodRegistry {

    private final RanchoPlugin plugin;
    private final File file;
    private YamlConfiguration cfg;
    private boolean added;
    private final Map<String, Food> map = new LinkedHashMap<>();

    public FoodRegistry(RanchoPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "foods.yml");
        load();
    }

    public void load() {
        cfg = YamlConfiguration.loadConfiguration(file);
        added = false;
        writeDefaults(); // solo añade las recetas por defecto que falten (respeta las que ya editaste)
        if (added) {
            save();
        }
        rebuild();
    }

    private void rebuild() {
        map.clear();
        ConfigurationSection sec = cfg.getConfigurationSection("foods");
        if (sec == null) {
            return;
        }
        for (String id : sec.getKeys(false)) {
            map.put(id, new Food(id, this));
        }
    }

    YamlConfiguration cfg() { return cfg; }

    public YamlConfiguration cfgPublic() { return cfg; }

    public void save() {
        Saver.save(plugin, cfg, file);
    }

    public Food get(String id) { return map.get(id); }
    public List<Food> all() { return new ArrayList<>(map.values()); }

    public Food create(String id, String name, String templateId) {
        if (map.containsKey(id)) {
            return map.get(id);
        }
        ConfigurationSection tpl = cfg.getConfigurationSection("foods." + templateId);
        if (tpl != null) {
            for (String k : tpl.getKeys(true)) {
                if (!tpl.isConfigurationSection(k)) {
                    cfg.set("foods." + id + "." + k, tpl.get(k));
                }
            }
        } else {
            template(id);
        }
        cfg.set("foods." + id + ".name", name);
        cfg.set("foods." + id + ".default", false);
        save();
        rebuild();
        return map.get(id);
    }

    public void delete(String id) {
        cfg.set("foods." + id, null);
        save();
        rebuild();
    }

    public boolean isDefault(String id) {
        return cfg.getBoolean("foods." + id + ".default", false);
    }

    // ------------------------------------------------------------------ valores por defecto

    private void set(String id, String k, Object v) {
        cfg.set("foods." + id + "." + k, v);
    }

    private void template(String id) {
        set(id, "enabled", true);
        set(id, "name", id);
        set(id, "material", "BREAD");
        set(id, "food-points", 0);
        set(id, "saturation", 0.0);
        set(id, "spoil-minutes", 180.0);
        set(id, "out-amount", 1);
        set(id, "ingredients", "");
        set(id, "cook-seconds", 0);
        set(id, "station", "kitchen");
        set(id, "age-minutes", 0.0);
        set(id, "age-max-bonus", 3);
        set(id, "group", "");
        set(id, "fish-weight", 0);
        set(id, "fish-seasons", "spring,summer,autumn,winter");
        set(id, "fish-env", "ANY");
        set(id, "fish-time", "ANY");
        set(id, "effects", "");
    }

    private void def(String id, Object... kv) {
        if (cfg.contains("foods." + id)) {
            return;
        }
        added = true;
        template(id);
        set(id, "default", true);
        for (int i = 0; i + 1 < kv.length; i += 2) {
            set(id, (String) kv[i], kv[i + 1]);
        }
    }

    /** Ingredientes: MATERIAL:cantidad, food:id:cantidad (otra comida) o item:id:cantidad (item custom). */
    private void writeDefaults() {
        def("flour", "cook-seconds", 8, "name", "Harina de Trigo", "material", "SUGAR", "ingredients", "WHEAT:3",
                "out-amount", 2, "spoil-minutes", 240.0);
        def("dough", "cook-seconds", 10, "name", "Masa de Pan", "material", "CLAY_BALL", "ingredients", "food:flour:2,WATER_BUCKET:1",
                "out-amount", 2, "spoil-minutes", 120.0);
        def("bread_rustic", "cook-seconds", 30, "name", "Pan Rústico", "material", "BREAD", "ingredients", "food:dough:1",
                "out-amount", 2, "food-points", 3, "saturation", 4.0, "spoil-minutes", 180.0);
        def("butter", "cook-seconds", 12, "name", "Mantequilla", "material", "GOLD_NUGGET", "ingredients", "MILK_BUCKET:1",
                "out-amount", 2, "spoil-minutes", 180.0);
        def("cheese", "cook-seconds", 45, "name", "Queso Curado", "material", "HONEYCOMB", "ingredients", "MILK_BUCKET:2,item:salt:1",
                "out-amount", 2, "spoil-minutes", 480.0);
        def("sandwich", "cook-seconds", 5, "name", "Sándwich de Queso", "material", "BAKED_POTATO",
                "ingredients", "food:bread_rustic:1,food:cheese:1", "food-points", 5, "saturation", 6.0,
                "spoil-minutes", 120.0);
        def("cake", "name", "Pastel de Granja", "material", "PUMPKIN_PIE",
                "ingredients", "food:flour:2,EGG:1,SUGAR:1,MILK_BUCKET:1", "food-points", 8, "saturation", 8.0,
                "spoil-minutes", 240.0, "cook-seconds", 60, "effects", "regeneration:6:0");
        def("pumpkin_pie", "cook-seconds", 45, "name", "Tarta de Calabaza", "material", "PUMPKIN_PIE",
                "ingredients", "PUMPKIN:1,SUGAR:1,EGG:1,food:dough:1", "out-amount", 2, "food-points", 6,
                "saturation", 6.0, "spoil-minutes", 300.0);
        def("cookies", "cook-seconds", 25, "name", "Galletas de Mantequilla", "material", "COOKIE",
                "ingredients", "food:flour:1,food:butter:1,SUGAR:1", "out-amount", 4, "food-points", 2,
                "saturation", 2.0, "spoil-minutes", 600.0);
        def("pasta", "cook-seconds", 20, "name", "Pasta Fresca", "material", "STRING", "ingredients", "food:flour:2,EGG:1",
                "out-amount", 3, "spoil-minutes", 480.0);
        def("pasta_dish", "name", "Pasta con Queso", "material", "MUSHROOM_STEW",
                "ingredients", "food:pasta:1,food:cheese:1,BOWL:1", "food-points", 8, "saturation", 9.0,
                "spoil-minutes", 150.0, "cook-seconds", 30, "effects", "speed:30:0");
        def("tortilla", "cook-seconds", 15, "name", "Tortilla de Maíz", "material", "BREAD",
                "ingredients", "food:maize_flour:2,WATER_BUCKET:1", "out-amount", 4, "food-points", 3,
                "saturation", 3.0, "spoil-minutes", 150.0);
        def("rice_dish", "cook-seconds", 30, "name", "Arroz Cocido", "material", "BEETROOT_SOUP",
                "ingredients", "DRIED_KELP:3,BOWL:1,WATER_BUCKET:1", "food-points", 7, "saturation", 7.0,
                "spoil-minutes", 150.0);
        def("grape_jam", "cook-seconds", 40, "name", "Mermelada de Uva", "material", "HONEY_BOTTLE",
                "ingredients", "SWEET_BERRIES:3,SUGAR:2,GLASS_BOTTLE:1", "food-points", 4, "saturation", 5.0,
                "spoil-minutes", 1440.0);
        def("meat_stew", "name", "Estofado de Res", "material", "RABBIT_STEW",
                "ingredients", "COOKED_BEEF:2,POTATO:1,CARROT:1,BOWL:1", "food-points", 10, "saturation", 10.0,
                "spoil-minutes", 180.0, "cook-seconds", 50, "effects", "resistance:30:0,heal:0:2");
        def("fruit_salad", "cook-seconds", 3, "name", "Ensalada de Frutas", "material", "MELON_SLICE",
                "ingredients", "APPLE:1,SWEET_BERRIES:2,MELON_SLICE:1,BOWL:1", "food-points", 5, "saturation", 5.0,
                "spoil-minutes", 90.0);
        def("omelette", "cook-seconds", 20, "name", "Tortilla de Huevo", "material", "COOKED_COD",
                "ingredients", "EGG:2,food:butter:1", "food-points", 6, "saturation", 7.0, "spoil-minutes", 120.0);

        def("warm_soup", "name", "Sopa Reconfortante", "material", "MUSHROOM_STEW",
                "ingredients", "POTATO:2,CARROT:1,WATER_BUCKET:1,BOWL:1,item:salt:1", "food-points", 8,
                "saturation", 8.0, "spoil-minutes", 150.0, "cook-seconds", 45,
                "effects", "warm:900:0,regeneration:6:0");
        def("grape_juice", "name", "Jugo de Uva", "material", "HONEY_BOTTLE",
                "ingredients", "SWEET_BERRIES:4,SUGAR:1,GLASS_BOTTLE:1,WATER_BUCKET:1", "food-points", 3,
                "saturation", 3.0, "spoil-minutes", 120.0, "cook-seconds", 15, "effects", "cool:900:0,speed:45:0");
        def("steak", "name", "Filete a la Parrilla", "material", "COOKED_BEEF",
                "ingredients", "COOKED_BEEF:2,item:salt:1,food:butter:1", "food-points", 8, "saturation", 10.0,
                "spoil-minutes", 150.0, "cook-seconds", 40, "effects", "strength:60:0,resistance:60:0");
        def("harvest_cake", "name", "Pastel del Cosechador", "material", "PUMPKIN_PIE",
                "ingredients", "food:cake:1,SWEET_BERRIES:2,GOLDEN_CARROT:1", "food-points", 10, "saturation", 10.0,
                "spoil-minutes", 300.0, "cook-seconds", 60,
                "effects", "lucky_harvest:600:0,farmer_xp:600:0,regeneration:8:0");
        def("golden_bread", "name", "Pan Dorado", "material", "BREAD",
                "ingredients", "food:bread_rustic:2,GOLD_NUGGET:3,food:butter:1", "out-amount", 2, "food-points", 6,
                "saturation", 8.0, "spoil-minutes", 360.0, "cook-seconds", 50, "effects", "absorption:120:1,haste:120:0");
        def("farmers_breakfast", "name", "Desayuno del Granjero", "material", "COOKED_CHICKEN",
                "ingredients", "food:omelette:1,food:bread_rustic:1", "food-points", 9, "saturation", 9.0,
                "spoil-minutes", 120.0, "cook-seconds", 30, "effects", "farmer_xp:900:0,speed:60:0");
        def("lunar_tea", "name", "Té Lunar", "material", "HONEY_BOTTLE",
                "ingredients", "GLOW_BERRIES:2,WATER_BUCKET:1,SUGAR:1,GLASS_BOTTLE:1", "food-points", 2,
                "saturation", 2.0, "spoil-minutes", 200.0, "cook-seconds", 25,
                "effects", "night_vision:300:0,slow_falling:120:0");
        def("fire_stew", "name", "Guiso de Fuego", "material", "RABBIT_STEW",
                "ingredients", "APPLE:2,COOKED_BEEF:1,BOWL:1", "food-points", 9, "saturation", 9.0,
                "spoil-minutes", 150.0, "cook-seconds", 45, "effects", "fire_resistance:300:0,strength:60:1");
        def("frost_salad", "name", "Ensalada Helada", "material", "BEETROOT_SOUP",
                "ingredients", "BEETROOT:2,SNOWBALL:2,BOWL:1", "food-points", 5, "saturation", 5.0,
                "spoil-minutes", 90.0, "cook-seconds", 10, "effects", "cool:900:0,water_breathing:120:0");
        def("magic_cookie", "name", "Galleta Mágica", "material", "COOKIE",
                "ingredients", "food:cookies:1,AZURE_BLUET:1,AMETHYST_SHARD:1", "out-amount", 2, "food-points", 3,
                "saturation", 3.0, "spoil-minutes", 720.0, "cook-seconds", 30,
                "effects", "luck:300:0,farmer_xp:300:0,heal:0:3");

        // ---- molino / prensa
        def("maize_flour", "name", "Harina de Maíz", "material", "SUGAR", "station", "mill",
                "ingredients", "BAKED_POTATO:3", "out-amount", 2, "cook-seconds", 8, "spoil-minutes", 300.0);
        def("grape_must", "name", "Mosto de Uva", "material", "GLASS_BOTTLE", "station", "mill",
                "ingredients", "SWEET_BERRIES:6,GLASS_BOTTLE:2", "out-amount", 2, "cook-seconds", 20,
                "spoil-minutes", 240.0);
        def("lunar_must", "name", "Mosto Lunar", "material", "GLASS_BOTTLE", "station", "mill",
                "ingredients", "GLOW_BERRIES:6,GLASS_BOTTLE:2", "out-amount", 2, "cook-seconds", 20,
                "spoil-minutes", 240.0);
        def("fire_must", "name", "Mosto de Fuego", "material", "GLASS_BOTTLE", "station", "mill",
                "ingredients", "APPLE:6,GLASS_BOTTLE:2", "out-amount", 2, "cook-seconds", 20,
                "spoil-minutes", 240.0);

        // ---- barrica: vino, queso añejo y jamón (mejoran con el tiempo)
        def("wine_young", "name", "Vino de Uva", "material", "HONEY_BOTTLE", "station", "cask",
                "ingredients", "food:grape_must:2,item:yeast:1", "out-amount", 2, "cook-seconds", 900,
                "age-minutes", 20.0, "age-max-bonus", 3, "spoil-minutes", 20000.0, "food-points", 3,
                "saturation", 2.0, "effects", "regeneration:10:0,resistance:60:0");
        def("lunar_wine", "name", "Vino Lunar", "material", "HONEY_BOTTLE", "station", "cask",
                "ingredients", "food:lunar_must:2,item:yeast:1", "out-amount", 2, "cook-seconds", 900,
                "age-minutes", 20.0, "age-max-bonus", 3, "spoil-minutes", 20000.0, "food-points", 3,
                "saturation", 2.0, "effects", "night_vision:300:0,absorption:60:0");
        def("fire_wine", "name", "Vino de Fuego", "material", "HONEY_BOTTLE", "station", "cask",
                "ingredients", "food:fire_must:2,item:yeast:1", "out-amount", 2, "cook-seconds", 900,
                "age-minutes", 20.0, "age-max-bonus", 3, "spoil-minutes", 20000.0, "food-points", 3,
                "saturation", 2.0, "effects", "fire_resistance:300:0,strength:60:0");
        def("aged_cheese", "name", "Queso Añejo", "material", "GOLDEN_CARROT", "station", "cask",
                "ingredients", "food:cheese:2", "out-amount", 2, "cook-seconds", 600, "age-minutes", 30.0,
                "age-max-bonus", 2, "spoil-minutes", 4000.0, "food-points", 6, "saturation", 6.0,
                "effects", "regeneration:6:0");
        def("cured_ham", "name", "Jamón Curado", "material", "COOKED_PORKCHOP", "station", "cask",
                "ingredients", "COOKED_PORKCHOP:3,item:salt:2", "out-amount", 2, "cook-seconds", 720,
                "age-minutes", 30.0, "age-max-bonus", 3, "spoil-minutes", 6000.0, "food-points", 8,
                "saturation", 9.0, "effects", "strength:30:0");

        // ---- miel (colmenas)
        def("honey", "name", "Miel de Rancho", "material", "HONEY_BOTTLE", "spoil-minutes", 200000.0,
                "food-points", 3, "saturation", 3.0, "effects", "regeneration:5:0");
        def("royal_honey", "name", "Miel Real", "material", "HONEY_BOTTLE", "spoil-minutes", 200000.0,
                "food-points", 5, "saturation", 5.0, "effects", "absorption:120:1,regeneration:10:0,farmer_xp:300:0");
        def("honey_bread", "name", "Pan con Miel", "material", "BREAD",
                "ingredients", "food:bread_rustic:1,food:honey:1", "food-points", 6, "saturation", 7.0,
                "spoil-minutes", 300.0, "cook-seconds", 15, "effects", "regeneration:6:0,speed:45:0");

        // ---- peces (se obtienen pescando; no tienen receta)
        def("trout", "name", "Trucha", "material", "COD", "group", "fish", "fish-weight", 40, "fish-env", "FRESH",
                "food-points", 1, "saturation", 1.0, "spoil-minutes", 90.0);
        def("carp", "name", "Carpa", "material", "COD", "group", "fish", "fish-weight", 30, "fish-env", "FRESH",
                "fish-seasons", "spring,summer,autumn", "food-points", 1, "saturation", 1.0, "spoil-minutes", 90.0);
        def("salmon_r", "name", "Salmón", "material", "SALMON", "group", "fish", "fish-weight", 25,
                "fish-seasons", "spring,autumn,winter", "food-points", 2, "saturation", 2.0, "spoil-minutes", 90.0);
        def("tropical_r", "name", "Pez Tropical", "material", "TROPICAL_FISH", "group", "fish", "fish-weight", 20,
                "fish-env", "SALT", "fish-seasons", "spring,summer", "food-points", 1, "saturation", 1.0,
                "spoil-minutes", 90.0);
        def("catfish", "name", "Siluro", "material", "COD", "group", "fish", "fish-weight", 15, "fish-env", "FRESH",
                "fish-time", "NIGHT", "food-points", 2, "saturation", 2.0, "spoil-minutes", 90.0);
        def("golden_carp", "name", "Carpa Dorada", "material", "SALMON", "group", "fish", "fish-weight", 4,
                "fish-env", "FRESH", "food-points", 3, "saturation", 3.0, "spoil-minutes", 120.0,
                "effects", "luck:120:0");
        def("moon_fish", "name", "Pez Lunar", "material", "COD", "group", "fish", "fish-weight", 3,
                "fish-time", "NIGHT", "food-points", 3, "saturation", 3.0, "spoil-minutes", 120.0,
                "effects", "night_vision:180:0,slow_falling:60:0");

        // ---- platos de pescado
        def("grilled_fish", "name", "Pescado a la Parrilla", "material", "COOKED_SALMON",
                "ingredients", "group:fish:1,item:salt:1,food:butter:1", "food-points", 7, "saturation", 8.0,
                "spoil-minutes", 150.0, "cook-seconds", 25, "effects", "water_breathing:120:0,speed:30:0");
        def("fish_stew", "name", "Sopa de Pescado", "material", "MUSHROOM_STEW",
                "ingredients", "group:fish:2,POTATO:1,BOWL:1,item:salt:1", "food-points", 9, "saturation", 9.0,
                "spoil-minutes", 150.0, "cook-seconds", 40, "effects", "water_breathing:300:0,regeneration:5:0");
        def("sushi", "name", "Sushi", "material", "COOKED_COD",
                "ingredients", "group:fish:1,DRIED_KELP:2", "out-amount", 2, "food-points", 5, "saturation", 6.0,
                "spoil-minutes", 90.0, "cook-seconds", 20, "effects", "dolphins_grace:60:0");
    }
}
