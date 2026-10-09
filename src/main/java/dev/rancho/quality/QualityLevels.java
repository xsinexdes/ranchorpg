package dev.rancho.quality;

import dev.rancho.RanchoPlugin;
import dev.rancho.util.Saver;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.potion.PotionEffectType;

import java.io.File;

/** Los 10 niveles de calidad de productos (nombre, color, lore, valor y efectos). Editables por GUI. */
public final class QualityLevels {

    public static final int MAX = 10;

    private static final String[] NAMES = {"Pobre", "Común", "Aceptable", "Buena", "Fina", "Selecta",
            "Excelente", "Superior", "Maestra", "Legendaria"};
    private static final String[] COLORS = {"&7", "&f", "&a", "&2", "&b", "&9", "&d", "&5", "&6", "&c&l"};
    private static final String[] LORES = {
            "Un producto de baja calidad.", "Corriente, pero sirve.", "Cumple sin destacar.",
            "Se nota el buen cuidado.", "Fina y bien cuidada.", "Seleccionada para paladares exigentes.",
            "Digna de un banquete real.", "Superior a todo lo conocido.", "Obra maestra del rancho.",
            "Una leyenda entre los ganaderos."};
    private static final double[] MULT = {0.5, 0.8, 1.0, 1.3, 1.7, 2.2, 3.0, 4.0, 5.5, 8.0};
    private static final int[] SAT = {0, 0, 0, 1, 1, 2, 3, 4, 5, 6};
    private static final String[] EFFECT = {"none", "none", "none", "speed", "speed", "regeneration",
            "jump_boost", "strength", "resistance", "absorption"};
    private static final int[] SECS = {0, 0, 0, 20, 30, 6, 30, 30, 30, 60};
    private static final int[] AMP = {0, 0, 0, 0, 0, 0, 0, 0, 0, 1};

    private final RanchoPlugin plugin;
    private final File file;
    private YamlConfiguration cfg;

    public QualityLevels(RanchoPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "levels.yml");
        load();
    }

    public void load() {
        cfg = YamlConfiguration.loadConfiguration(file);
        boolean changed = false;
        for (int i = 1; i <= MAX; i++) {
            String p = "levels." + i + ".";
            if (!cfg.contains(p + "name")) {
                cfg.set(p + "name", NAMES[i - 1]);
                cfg.set(p + "color", COLORS[i - 1]);
                cfg.set(p + "lore", LORES[i - 1]);
                cfg.set(p + "value-mult", MULT[i - 1]);
                cfg.set(p + "saturation", SAT[i - 1]);
                cfg.set(p + "effect", EFFECT[i - 1]);
                cfg.set(p + "effect-seconds", SECS[i - 1]);
                cfg.set(p + "effect-amp", AMP[i - 1]);
                changed = true;
            }
        }
        if (changed) {
            save();
        }
    }

    public void save() {
        Saver.save(plugin, cfg, file);
    }

    public YamlConfiguration cfg() {
        return cfg;
    }

    private String p(int q, String k) {
        return "levels." + Math.max(1, Math.min(MAX, q)) + "." + k;
    }

    public String name(int q) { return cfg.getString(p(q, "name"), "Nivel " + q); }
    public String color(int q) { return cfg.getString(p(q, "color"), "&f"); }
    public String lore(int q) { return cfg.getString(p(q, "lore"), ""); }
    public double mult(int q) { return cfg.getDouble(p(q, "value-mult"), 1.0); }
    public int saturation(int q) { return cfg.getInt(p(q, "saturation"), 0); }
    public String effectKey(int q) { return cfg.getString(p(q, "effect"), "none"); }
    public int effectSeconds(int q) { return cfg.getInt(p(q, "effect-seconds"), 0); }
    public int effectAmp(int q) { return cfg.getInt(p(q, "effect-amp"), 0); }

    /** Efecto de poción del nivel, o null si no tiene. */
    public PotionEffectType effectType(int q) {
        String key = effectKey(q);
        if (key == null || key.equalsIgnoreCase("none") || effectSeconds(q) <= 0) {
            return null;
        }
        return PotionEffectType.getByKey(NamespacedKey.minecraft(key.toLowerCase()));
    }
}
