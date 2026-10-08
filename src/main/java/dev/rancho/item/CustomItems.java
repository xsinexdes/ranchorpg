package dev.rancho.item;

import dev.rancho.RanchoPlugin;
import dev.rancho.crop.CropDef;
import dev.rancho.util.Keys;
import dev.rancho.util.Text;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Registro de items custom del plugin (pienso, medicinas, fertilizantes, herramientas...). */
public final class CustomItems {

    /** Definición base de un item custom. */
    public record Def(String id, Material mat, String name, String lore, boolean glow, double price) {
        public boolean buyable() {
            return price > 0;
        }
    }

    private static final Map<String, Def> DEFS = new LinkedHashMap<>();

    private static void add(String id, Material m, String name, String lore, boolean glow, double price) {
        DEFS.put(id, new Def(id, m, name, lore, glow, price));
    }

    static {
        add("feed_basic", Material.WHEAT, "&ePienso Básico", "&7Alimento básico para ganado.|&8Restaura hambre.", false, 10);
        add("feed_premium", Material.HONEYCOMB, "&6Pienso Premium", "&7Alimento enriquecido.|&8Mucha hambre y mejor calidad.", true, 40);
        add("forage", Material.HAY_BLOCK, "&eForraje", "&7Heno seco para el ganado.", false, 6);
        add("salt", Material.SUGAR, "&fSal de Ganado", "&7Mejora la felicidad y la salud.", false, 8);
        add("vaccine", Material.PRISMARINE_SHARD, "&bVacuna", "&7Inmuniza al animal contra enfermedades.", true, 60);
        add("antiparasitic", Material.PRISMARINE_CRYSTALS, "&aAntiparasitario", "&7Cura los parásitos.", false, 40);
        add("antibiotic", Material.GHAST_TEAR, "&2Antibiótico", "&7Cura la gripe y las infecciones.", false, 50);
        add("brush", Material.BRUSH, "&eCepillo de Establo", "&7Click derecho a un animal para limpiarlo.", false, 30);
        add("breed_supplement", Material.GOLDEN_CARROT, "&dSuplemento de Cría", "&7Necesario para la reproducción.|&8Úsalo en un adulto con pareja cerca.", true, 80);
        add("mutation_serum", Material.FERMENTED_SPIDER_EYE, "&5Suero de Mutación", "&7Provoca una mutación al azar.|&8Puede ser buena o mala.", true, 400);
        add("stabilizer", Material.AMETHYST_SHARD, "&3Estabilizador Genético", "&7Elimina una mutación negativa o|&7protege la próxima cría.", true, 300);
        add("gene_analysis", Material.PAPER, "&9Análisis Genético", "&7Revela la genética completa y el|&7árbol genealógico de un animal.", true, 150);
        add("notebook", Material.BOOK, "&6Libreta de Rancho", "&7Click derecho a animales y cultivos|&7para ver su información.", false, 20);
        add("thermometer", Material.COMPASS, "&cTermómetro", "&7Click derecho para ver la temperatura.", false, 25);
        add("fertilizer_basic", Material.BROWN_DYE, "&eFertilizante Básico", "&7Aumenta la fertilidad de la tierra.", false, 15);
        add("fertilizer_advanced", Material.GREEN_DYE, "&aFertilizante Avanzado", "&7Mucha fertilidad para la tierra.", true, 50);
        add("manure", Material.COCOA_BEANS, "&6Estiércol", "&7Fertilizante natural.|&8Mantén limpio el corral.", false, 0);
        add("improved_soil", Material.COARSE_DIRT, "&6Tierra Mejorada", "&7Click derecho a tierra de labranza.|&8Necesaria para cultivos exóticos.", true, 40);
        add("watering_can", Material.BUCKET, "&bRegadera", "&7Click derecho a la tierra para regar.|&8Se rellena en agua.", false, 100);
        add("greenhouse", Material.SEA_LANTERN, "&aNúcleo de Invernadero", "&7Anula clima y estación en un área grande.|&8Colócalo cerca de tus cultivos.", true, 500);
        add("pot", Material.DECORATED_POT, "&6Maceta de Rancho", "&7Anula clima y estación en un área pequeña.", true, 120);
    }

    private CustomItems() {}

    public static Collection<Def> all() { return DEFS.values(); }
    public static Def get(String id) { return DEFS.get(id); }
    public static Set<String> ids() { return DEFS.keySet(); }

    /** Registra las claves de Lang de todos los items. */
    public static void defineLang() {
        for (Def d : DEFS.values()) {
            dev.rancho.config.Lang.define("item." + d.id() + ".name", d.name());
            dev.rancho.config.Lang.define("item." + d.id() + ".lore", d.lore());
        }
        dev.rancho.config.Lang.define("item.seed.name", "&aSemilla de {crop}");
        dev.rancho.config.Lang.define("item.seed.lore", "&7Semilla especial.|&8Plántala en tierra de labranza.");
    }

    public static ItemStack create(String id, int amount) {
        Def d = DEFS.get(id);
        if (d == null) {
            return new ItemStack(Material.AIR);
        }
        RanchoPlugin pl = RanchoPlugin.get();
        ItemStack item = new ItemStack(d.mat(), Math.max(1, Math.min(64, amount)));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(pl.lang().get("item." + id + ".name"));
        meta.setLore(loreOf(pl.lang().raw("item." + id + ".lore")));
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.values());
        if (d.glow()) {
            meta.setEnchantmentGlintOverride(true);
        }
        meta.getPersistentDataContainer().set(Keys.ITEM, PersistentDataType.STRING, id);
        if (id.equals("watering_can")) {
            meta.getPersistentDataContainer().set(Keys.USES, PersistentDataType.INTEGER,
                    pl.getConfig().getInt("crops.can-uses", 20));
        }
        item.setItemMeta(meta);
        if (id.equals("watering_can")) {
            refreshCan(item);
        }
        return item;
    }

    /** Semilla custom de un cultivo. */
    public static ItemStack createSeed(CropDef def, int amount) {
        RanchoPlugin pl = RanchoPlugin.get();
        ItemStack item = new ItemStack(def.seedMaterial(), Math.max(1, Math.min(64, amount)));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(pl.lang().get("item.seed.name", "crop", def.name()));
        meta.setLore(loreOf(pl.lang().raw("item.seed.lore")));
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.values());
        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(Keys.ITEM, PersistentDataType.STRING, "seed");
        meta.getPersistentDataContainer().set(Keys.SEED, PersistentDataType.STRING, def.id());
        item.setItemMeta(meta);
        return item;
    }

    private static List<String> loreOf(String raw) {
        List<String> lore = new ArrayList<>();
        for (String line : raw.split("\\|")) {
            lore.add(Text.color(line));
        }
        return lore;
    }

    /** Id del item custom, o null si no lo es. */
    public static String id(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(Keys.ITEM, PersistentDataType.STRING);
    }

    public static boolean is(ItemStack item, String id) {
        return id.equals(id(item));
    }

    /** Id del cultivo de una semilla custom, o null. */
    public static String seedCrop(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(Keys.SEED, PersistentDataType.STRING);
    }

    public static int uses(ItemStack can) {
        if (can == null || !can.hasItemMeta()) {
            return 0;
        }
        Integer u = can.getItemMeta().getPersistentDataContainer().get(Keys.USES, PersistentDataType.INTEGER);
        return u == null ? 0 : u;
    }

    public static void setUses(ItemStack can, int uses) {
        ItemMeta meta = can.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.getPersistentDataContainer().set(Keys.USES, PersistentDataType.INTEGER, Math.max(0, uses));
        can.setItemMeta(meta);
        refreshCan(can);
    }

    /** Actualiza la línea de usos en el lore de la regadera. */
    public static void refreshCan(ItemStack can) {
        ItemMeta meta = can.getItemMeta();
        if (meta == null) {
            return;
        }
        RanchoPlugin pl = RanchoPlugin.get();
        int max = pl.getConfig().getInt("crops.can-uses", 20);
        List<String> lore = loreOf(pl.lang().raw("item.watering_can.lore"));
        Integer u = meta.getPersistentDataContainer().get(Keys.USES, PersistentDataType.INTEGER);
        lore.add(pl.lang().get("item.can.uses", "uses", u == null ? 0 : u, "max", max));
        meta.setLore(lore);
        can.setItemMeta(meta);
    }
}
