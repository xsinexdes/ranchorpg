package dev.rancho.crop;

import dev.rancho.season.Season;
import dev.rancho.util.Util;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

/** Definición de un cultivo. Lee sus valores en vivo de cropdefs.yml. */
public final class CropDef {

    private final String id;
    private final CropRegistry reg;

    CropDef(String id, CropRegistry reg) {
        this.id = id;
        this.reg = reg;
    }

    private String p(String k) { return "crops." + id + "." + k; }
    private YamlConfiguration c() { return reg.cfg(); }
    private double num(String k, double def) { return c().getDouble(p(k), def); }
    private boolean bool(String k, boolean def) { return c().getBoolean(p(k), def); }
    private String str(String k, String def) { return c().getString(p(k), def); }

    public String id() { return id; }
    public String name() { return str("name", id); }
    public boolean enabled() { return bool("enabled", true); }
    /** Los cultivos custom exigen semilla con etiqueta; los vanilla usan la semilla normal. */
    public boolean custom() { return bool("custom", false); }

    /** Bloque de cultivo (WHEAT, CARROTS, POTATOES o BEETROOTS). */
    public Material block() {
        Material m = Util.mat(str("block", "WHEAT"), Material.WHEAT);
        return CropRegistry.isCropBlock(m) ? m : Material.WHEAT;
    }

    public Material seedMaterial() { return Util.mat(str("seed-material", "WHEAT_SEEDS"), Material.WHEAT_SEEDS); }
    public Material productMaterial() { return Util.mat(str("product-material", "WHEAT"), Material.WHEAT); }
    public String productName() { return str("product-name", name()); }
    public double growthDays() { return Math.max(0.1, num("growth-days", 3)); }
    public double tempMin() { return num("temp-min", 5); }
    public double tempMax() { return Math.max(tempMin(), num("temp-max", 30)); }
    public int lightMin() { return (int) num("light-min", 9); }
    public boolean needsSun() { return bool("needs-sun", true); }
    public boolean needsWater() { return bool("needs-water", true); }
    public boolean needsFertilizer() { return bool("needs-fertilizer", false); }
    public boolean improvedSoil() { return bool("improved-soil", false); }
    public boolean growsIn(Season s) { return bool("season." + s.key(), true); }
    public double price() { return num("price", 6); }
    public int harvestMin() { return (int) num("harvest-min", 1); }
    public int harvestMax() { return Math.max(harvestMin(), (int) num("harvest-max", 3)); }
}
