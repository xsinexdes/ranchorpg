package dev.rancho.item;

import dev.rancho.RanchoPlugin;
import dev.rancho.crop.CropDef;
import dev.rancho.crop.CropRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.ArrayList;
import java.util.List;

/** Recetas de crafteo de los items custom y de las semillas custom. */
public final class RecipeRegistry {

    private final RanchoPlugin plugin;
    private final List<NamespacedKey> keys = new ArrayList<>();

    public RecipeRegistry(RanchoPlugin plugin) {
        this.plugin = plugin;
    }

    public List<NamespacedKey> keys() {
        return keys;
    }

    /** Quita las recetas registradas. */
    public void unregister() {
        for (NamespacedKey k : keys) {
            Bukkit.removeRecipe(k);
        }
        keys.clear();
    }

    private void shapeless(String id, int amount, Material... ingredients) {
        NamespacedKey key = new NamespacedKey(plugin, id);
        ShapelessRecipe r = new ShapelessRecipe(key, CustomItems.create(id, amount));
        for (Material m : ingredients) {
            r.addIngredient(m);
        }
        add(key, r);
    }

    private void add(NamespacedKey key, org.bukkit.inventory.Recipe r) {
        Bukkit.removeRecipe(key);
        Bukkit.addRecipe(r);
        keys.add(key);
    }

    /** Registra (o vuelve a registrar) todas las recetas. */
    public void register() {
        unregister();
        shapeless("feed_basic", 3, Material.WHEAT, Material.WHEAT, Material.CARROT);
        shapeless("feed_premium", 2, Material.WHEAT, Material.WHEAT, Material.GOLDEN_CARROT, Material.SUGAR);
        shapeless("forage", 4, Material.HAY_BLOCK);
        shapeless("salt", 2, Material.BONE_MEAL, Material.BONE_MEAL, Material.SUGAR);
        shapeless("vaccine", 1, Material.GLASS_BOTTLE, Material.SPIDER_EYE, Material.GOLDEN_CARROT);
        shapeless("antiparasitic", 1, Material.GLASS_BOTTLE, Material.FERMENTED_SPIDER_EYE, Material.SUGAR);
        shapeless("antibiotic", 1, Material.GLASS_BOTTLE, Material.BROWN_MUSHROOM, Material.SUGAR);
        shapeless("brush", 1, Material.FEATHER, Material.STICK, Material.STRING);
        shapeless("breed_supplement", 1, Material.GOLDEN_CARROT, Material.HAY_BLOCK);
        shapeless("mutation_serum", 1, Material.GLASS_BOTTLE, Material.FERMENTED_SPIDER_EYE,
                Material.GLOWSTONE_DUST, Material.REDSTONE);
        shapeless("stabilizer", 1, Material.GLASS_BOTTLE, Material.AMETHYST_SHARD, Material.GOLD_NUGGET, Material.QUARTZ);
        shapeless("gene_analysis", 1, Material.PAPER, Material.AMETHYST_SHARD, Material.REDSTONE);
        shapeless("notebook", 1, Material.BOOK, Material.FEATHER, Material.INK_SAC);
        shapeless("thermometer", 1, Material.GLASS_PANE, Material.REDSTONE, Material.IRON_NUGGET);
        shapeless("fertilizer_basic", 2, Material.BONE_MEAL, Material.BONE_MEAL, Material.DIRT);
        shapeless("fertilizer_advanced", 2, Material.BONE_MEAL, Material.BONE_MEAL, Material.GLOWSTONE_DUST,
                Material.LAPIS_LAZULI);
        shapeless("improved_soil", 1, Material.DIRT, Material.DIRT, Material.BONE_MEAL, Material.BONE_MEAL);
        shapeless("watering_can", 1, Material.BUCKET, Material.IRON_INGOT, Material.IRON_INGOT);
        shapeless("kitchen", 1, Material.SMOKER, Material.CRAFTING_TABLE, Material.IRON_INGOT);
        shapeless("feeder", 1, Material.BARREL, Material.HAY_BLOCK, Material.IRON_NUGGET);
        shapeless("waterer", 1, Material.CAULDRON, Material.LAPIS_LAZULI, Material.IRON_NUGGET);
        shapeless("cask", 1, Material.BARREL, Material.IRON_INGOT, Material.GLASS_BOTTLE);
        shapeless("mill", 1, Material.GRINDSTONE, Material.IRON_INGOT, Material.STICK);
        shapeless("fridge", 1, Material.BLAST_FURNACE, Material.PACKED_ICE, Material.IRON_INGOT);
        shapeless("sprinkler", 1, Material.END_ROD, Material.BUCKET, Material.IRON_NUGGET);
        shapeless("scarecrow", 1, Material.CARVED_PUMPKIN, Material.HAY_BLOCK, Material.STICK);
        shapeless("hive", 1, Material.BEEHIVE, Material.HONEYCOMB, Material.DANDELION);
        shapeless("aquarium", 1, Material.GLASS, Material.GLASS, Material.KELP);
        shapeless("yeast", 2, Material.WHEAT, Material.SUGAR, Material.BROWN_MUSHROOM);
        shapeless("pesticide", 2, Material.SPIDER_EYE, Material.SUGAR, Material.BONE_MEAL);
        shapeless("pot", 1, Material.FLOWER_POT, Material.GLASS_PANE, Material.BONE_MEAL);

        NamespacedKey gk = new NamespacedKey(plugin, "greenhouse");
        ShapedRecipe gh = new ShapedRecipe(gk, CustomItems.create("greenhouse", 1));
        gh.shape("GGG", "GEG", "GGG");
        gh.setIngredient('G', Material.GLASS);
        gh.setIngredient('E', Material.EMERALD);
        add(gk, gh);

        // Semillas custom por defecto: 3 ingredientes -> 2 semillas
        for (java.util.Map.Entry<String, Material[]> en : CropRegistry.SEED_RECIPES.entrySet()) {
            CropDef def = plugin.cropDefs().get(en.getKey());
            if (def == null) {
                continue;
            }
            NamespacedKey key = new NamespacedKey(plugin, "seed_" + en.getKey());
            ShapelessRecipe r = new ShapelessRecipe(key, CustomItems.createSeed(def, 2));
            for (Material m : en.getValue()) {
                r.addIngredient(m);
            }
            add(key, r);
        }
    }
}
