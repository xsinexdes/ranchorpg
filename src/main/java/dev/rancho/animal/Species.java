package dev.rancho.animal;

import dev.rancho.season.Season;
import dev.rancho.util.Util;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import java.util.List;

/** Una especie de ganado. Lee sus valores en vivo de species.yml, por lo que la GUI los cambia al instante. */
public final class Species {

    private final String id;
    private final SpeciesRegistry reg;

    Species(String id, SpeciesRegistry reg) {
        this.id = id;
        this.reg = reg;
    }

    private String p(String k) { return "species." + id + "." + k; }
    private YamlConfiguration c() { return reg.cfg(); }
    private double num(String k, double def) { return c().getDouble(p(k), def); }
    private boolean bool(String k, boolean def) { return c().getBoolean(p(k), def); }
    private String str(String k, String def) { return c().getString(p(k), def); }

    public String id() { return id; }
    public String name() { return str("name", id); }
    public boolean enabled() { return bool("enabled", true); }

    public EntityType entity() {
        try {
            return EntityType.valueOf(str("entity", "COW").toUpperCase());
        } catch (IllegalArgumentException ex) {
            return EntityType.COW;
        }
    }

    public double baseHealth() { return Math.max(2, num("base-health", 10)); }
    public double babyDays() { return num("baby-days", 1); }
    public double adultDays() { return Math.max(babyDays(), num("adult-days", 3)); }
    public double oldDays() { return Math.max(adultDays(), num("old-days", 25)); }
    public double lifespan() { return Math.max(oldDays(), num("lifespan-days", 35)); }
    public double gestationDays() { return Math.max(0.05, num("gestation-days", 2)); }
    public int litterMin() { return (int) num("litter-min", 1); }
    public int litterMax() { return Math.max(litterMin(), (int) num("litter-max", 1)); }
    public int maxDensity() { return Math.max(1, (int) num("max-density", 4)); }
    public double hungerRate() { return num("hunger-rate", 50); }
    public double thirstRate() { return num("thirst-rate", 70); }
    public double hygieneRate() { return num("hygiene-rate", 15); }
    public double coldLimit() { return num("cold-limit", 0); }
    public double heatLimit() { return num("heat-limit", 35); }
    public boolean grazes() { return bool("grazes", true); }

    public List<String> feeds() { return Util.csv(str("feed", "WHEAT,feed_basic,feed_premium,forage")); }

    /** Indica si el token (id de item custom o nombre de Material) es comida de esta especie. */
    public boolean acceptsFood(String token) {
        for (String f : feeds()) {
            if (f.equalsIgnoreCase(token)) {
                return true;
            }
        }
        return false;
    }

    public String meatName() { return str("meat-name", "Carne"); }
    public Material meatMaterial() { return Util.mat(str("meat-material", "COOKED_BEEF"), Material.COOKED_BEEF); }
    public int meatMin() { return (int) num("meat-min", 1); }
    public int meatMax() { return Math.max(meatMin(), (int) num("meat-max", 3)); }
    public double meatPrice() { return num("meat-price", 20); }
    public String leatherName() { return str("leather-name", "Cuero"); }
    public int leatherMin() { return (int) num("leather-min", 0); }
    public int leatherMax() { return Math.max(leatherMin(), (int) num("leather-max", 2)); }
    public double leatherPrice() { return num("leather-price", 12); }

    /** NONE, MILK, WOOL o EGGS. */
    public String produce() { return str("produce", "NONE").toUpperCase(); }
    public String produceName() { return str("produce-name", "Producto"); }
    public Material produceMaterial() { return Util.mat(str("produce-material", "WHEAT"), Material.WHEAT); }
    public double produceDays() { return Math.max(0.05, num("produce-days", 1)); }
    public double producePrice() { return num("produce-price", 15); }

    /** Tipo de producto para el mercado: milk, wool, egg. */
    public String produceType() {
        return switch (produce()) {
            case "MILK" -> "milk";
            case "WOOL" -> "wool";
            case "EGGS" -> "egg";
            default -> "none";
        };
    }

    public boolean producesIn(Season s) { return bool("produce." + s.key(), true); }
    public boolean breedsIn(Season s) { return bool("breed." + s.key(), true); }
    public boolean suffersIn(Season s) { return bool("suffer." + s.key(), false); }
}
