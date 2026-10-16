package dev.rancho.food;

import dev.rancho.util.Util;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.List;

/** Definición de una comida o ingrediente de la cocina. Lee en vivo de foods.yml. */
public final class Food {

    private final String id;
    private final FoodRegistry reg;

    Food(String id, FoodRegistry reg) {
        this.id = id;
        this.reg = reg;
    }

    private String p(String k) { return "foods." + id + "." + k; }
    private YamlConfiguration c() { return reg.cfg(); }

    public String id() { return id; }
    public String name() { return c().getString(p("name"), id); }
    public boolean enabled() { return c().getBoolean(p("enabled"), true); }
    public Material material() { return Util.mat(c().getString(p("material"), "BREAD"), Material.BREAD); }
    /** Hambre extra que restaura al comerla (se suma a la vanilla). */
    public int foodPoints() { return c().getInt(p("food-points"), 0); }
    public double saturation() { return c().getDouble(p("saturation"), 0); }
    /** Minutos REALES hasta que se pudre. */
    public double spoilMinutes() { return Math.max(1, c().getDouble(p("spoil-minutes"), 180)); }
    public int outAmount() { return Math.max(1, c().getInt(p("out-amount"), 1)); }
    public List<String> ingredients() { return Util.csv(c().getString(p("ingredients"), "")); }
    /** Segundos REALES que tarda en cocinarse en la Cocina (0 = al instante). */
    public int cookSeconds() { return Math.max(0, c().getInt(p("cook-seconds"), 0)); }
    /** Un efecto de la comida: clave (vanilla o custom), segundos y potencia. */
    public record Eff(String key, int seconds, int amp) {}

    /** Efectos al comerla. Formato: clave:segundos:potencia separados por comas (ej. speed:30:0,farmer_xp:300:0). */
    public List<Eff> effects() {
        List<Eff> out = new java.util.ArrayList<>();
        for (String tok : Util.csv(c().getString(p("effects"), ""))) {
            String[] parts = tok.split(":");
            String key = parts[0].trim().toLowerCase();
            if (key.isEmpty() || key.equals("none")) {
                continue;
            }
            int secs = 30;
            int amp = 0;
            try {
                if (parts.length > 1) secs = Integer.parseInt(parts[1].trim());
                if (parts.length > 2) amp = Integer.parseInt(parts[2].trim());
            } catch (NumberFormatException ignored) {
                // se usan los valores por defecto
            }
            out.add(new Eff(key, secs, amp));
        }
        return out;
    }

    /** Estaciones donde se prepara (kitchen, cask, mill), separadas por comas. */
    public List<String> stations() { return Util.csv(c().getString(p("station"), "kitchen")); }

    public boolean inStation(String type) {
        for (String s : stations()) {
            if (s.equalsIgnoreCase(type)) {
                return true;
            }
        }
        return false;
    }

    /** Minutos reales extra en la barrica para ganar +1 nivel de calidad (0 = no mejora). */
    public double ageMinutes() { return c().getDouble(p("age-minutes"), 0); }
    public int ageMaxBonus() { return c().getInt(p("age-max-bonus"), 3); }
    /** Grupo (ej. fish) para recetas con "group:fish:2". */
    public String group() { return c().getString(p("group"), ""); }
    /** Peso al pescar (0 = no es un pez). */
    public int fishWeight() { return c().getInt(p("fish-weight"), 0); }
    public boolean fishIn(String season) {
        for (String s : Util.csv(c().getString(p("fish-seasons"), "spring,summer,autumn,winter"))) {
            if (s.equalsIgnoreCase(season)) {
                return true;
            }
        }
        return false;
    }
    /** ANY, FRESH (agua dulce) o SALT (mar). */
    public String fishEnv() { return c().getString(p("fish-env"), "ANY").toUpperCase(); }
    /** ANY, DAY o NIGHT. */
    public String fishTime() { return c().getString(p("fish-time"), "ANY").toUpperCase(); }

    /** Tiene receta en la cocina. */
    public boolean cookable() { return !ingredients().isEmpty(); }
}
