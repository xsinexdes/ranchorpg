package dev.rancho.item;

import dev.rancho.RanchoPlugin;
import dev.rancho.animal.Species;
import dev.rancho.crop.CropDef;
import dev.rancho.food.Food;
import dev.rancho.quality.Products;
import dev.rancho.util.Saver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * CustomModelData (resource pack) de cada item del plugin. Se guarda en models.yml.
 * Claves: item:id, food:id, seed:cultivo, crop:cultivo, meat:especie, leather:especie, produce:especie.
 */
public final class ModelService {

    private final RanchoPlugin plugin;
    private final File file;
    private YamlConfiguration cfg;

    public ModelService(RanchoPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "models.yml");
        load();
    }

    public void load() {
        cfg = YamlConfiguration.loadConfiguration(file);
    }

    private static String path(String key) {
        int i = key.indexOf(':');
        return i < 0 ? "models." + key : "models." + key.substring(0, i) + "." + key.substring(i + 1);
    }

    /** CustomModelData configurado para la clave (0 = ninguno). */
    public int get(String key) {
        return Math.max(0, cfg.getInt(path(key), 0));
    }

    public void set(String key, int value) {
        cfg.set(path(key), value <= 0 ? null : value);
        Saver.save(plugin, cfg, file);
    }

    /** Clave de modelo de un producto según su tipo (la leche, lana y huevos comparten 'produce'). */
    public static String productKey(String type, String id) {
        return switch (type) {
            case "milk", "wool", "egg" -> "produce:" + id;
            default -> type + ":" + id;
        };
    }

    /** Clave de modelo de un item concreto, o null si no es un item del plugin. */
    public String keyOf(ItemStack it) {
        if (it == null || it.getType().isAir()) {
            return null;
        }
        String food = plugin.food().foodId(it);
        if (food != null) {
            return food.equals("rotten") ? null : "food:" + food;
        }
        String seed = CustomItems.seedCrop(it);
        if (seed != null) {
            return "seed:" + seed;
        }
        String cid = CustomItems.id(it);
        if (cid != null) {
            return "item:" + cid;
        }
        String type = Products.type(it);
        String id = Products.id(it);
        if (type != null && id != null) {
            return productKey(type, id);
        }
        return null;
    }

    /** Todas las claves que se pueden configurar, ordenadas por categoría. */
    public List<String> allKeys() {
        List<String> keys = new ArrayList<>();
        for (String id : CustomItems.ids()) keys.add("item:" + id);
        for (Food f : plugin.foods().all()) keys.add("food:" + f.id());
        for (CropDef c : plugin.cropDefs().all()) {
            if (c.custom()) keys.add("seed:" + c.id());
        }
        for (CropDef c : plugin.cropDefs().all()) keys.add("crop:" + c.id());
        for (Species s : plugin.species().all()) {
            keys.add("meat:" + s.id());
            if (s.leatherMax() > 0) keys.add("leather:" + s.id());
            if (!s.produce().equals("NONE")) keys.add("produce:" + s.id());
        }
        return keys;
    }

    public boolean isValidKey(String key) {
        return allKeys().contains(key);
    }
}
