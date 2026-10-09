package dev.rancho.animal;

import org.bukkit.configuration.file.YamlConfiguration;

/** Una raza de una especie (rasgos propios). Lee en vivo de breeds.yml. */
public final class Breed {

    private final String id;
    private final BreedRegistry reg;

    Breed(String id, BreedRegistry reg) {
        this.id = id;
        this.reg = reg;
    }

    private String p(String k) { return "breeds." + id + "." + k; }
    private YamlConfiguration c() { return reg.cfg(); }

    public String id() { return id; }
    public String name() { return c().getString(p("name"), id); }
    public boolean enabled() { return c().getBoolean(p("enabled"), true); }
    public String speciesId() { return c().getString(p("species"), "cow"); }
    /** Probabilidad relativa de aparecer en animales nuevos. */
    public int weight() { return Math.max(0, c().getInt(p("weight"), 10)); }
    public double prodMult() { return c().getDouble(p("prod-mult"), 1.0); }
    public double meatMult() { return c().getDouble(p("meat-mult"), 1.0); }
    public double growthMult() { return c().getDouble(p("growth-mult"), 1.0); }
    public int bonus(Gene g) { return c().getInt(p("bonus." + g.name()), 0); }
}
