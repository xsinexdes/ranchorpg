package dev.rancho.animal;

import dev.rancho.animal.Mutation.Special;
import dev.rancho.util.Util;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TextDisplay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Todos los datos RPG de un animal. Se guardan en el PersistentDataContainer de la entidad. */
public final class AnimalData {

    public String speciesId = "cow";
    public String name = "Animal";
    public boolean female = true;
    /** Edad en días de Minecraft. */
    public double age;
    public double hunger = 80;
    public double thirst = 80;
    public double happiness = 70;
    public double hygiene = 90;
    /** Calidad media de la alimentación reciente (0-100). */
    public double diet = 50;
    /** Media móvil de cuidado (0-100). */
    public double care = 70;
    public int level = 1;
    public double xp;
    public final int[] genes = new int[Gene.values().length];
    public final List<String> mutations = new ArrayList<>();
    public Disease disease = Disease.NONE;
    public double immunity;
    public boolean pregnant;
    public double gestation;
    /** Instantánea del padre: genes/mutaciones/descripción/abuelo1/abuelo2. */
    public String sire = "";
    public boolean stabilized;
    public double breedCooldown;
    /** Carga de producción 0-100 (leche, lana, huevos). */
    public double charge;
    public boolean analyzed;
    public String owner = "";
    /** Id de la raza (breeds.yml) o vacío. */
    public String breed = "";
    /** Linaje: madre, padre, abuela materna, abuelo materno, abuela paterna, abuelo paterno. */
    public final List<String> lineage = new ArrayList<>();
    public double stamina = 100;

    public transient LivingEntity entity;
    public transient TextDisplay holo;
    public transient String holoText = "";
    public transient Stage lastStage;
    public transient boolean tired;
    public transient long lastTick;
    public transient long lastSave;
    public transient double lastX;
    public transient double lastZ;

    public Stage stage(Species sp) {
        if (age < sp.babyDays()) return Stage.BABY;
        if (age < sp.adultDays()) return Stage.YOUNG;
        if (age < sp.oldDays()) return Stage.ADULT;
        return Stage.OLD;
    }

    /** Valor efectivo de un gen: base + bonos de mutaciones, limitado a 0-100. */
    public int eff(Gene g) {
        int v = genes[g.ordinal()];
        for (String id : mutations) {
            Mutation m = Mutations.get(id);
            if (m != null && m.gene() == g) {
                v += m.delta();
            }
        }
        v += dev.rancho.RanchoPlugin.get().breeds().bonus(breed, g);
        return Util.clamp(v, 0, 100);
    }

    public boolean hasSpecial(Special s) {
        for (String id : mutations) {
            Mutation m = Mutations.get(id);
            if (m != null && m.special() == s) {
                return true;
            }
        }
        return false;
    }

    public boolean hasMutation(String id) {
        return mutations.contains(id);
    }

    /** Descripción compacta usada en el linaje. */
    public String describe() {
        return Util.clean(name) + "|" + speciesId + "|" + level + "|" + String.join(",", mutations);
    }

    // ------------------------------------------------------------------ serialización

    private static String r(double v) {
        return String.format(Locale.ROOT, "%.3f", v);
    }

    private static void put(StringBuilder sb, String k, String v) {
        if (sb.length() > 0) {
            sb.append(';');
        }
        sb.append(k).append('=').append(v);
    }

    public String serialize() {
        StringBuilder sb = new StringBuilder();
        put(sb, "sp", speciesId);
        put(sb, "nm", Util.clean(name));
        put(sb, "f", female ? "1" : "0");
        put(sb, "ag", r(age));
        put(sb, "hu", r(hunger));
        put(sb, "th", r(thirst));
        put(sb, "ha", r(happiness));
        put(sb, "hy", r(hygiene));
        put(sb, "di", r(diet));
        put(sb, "ca", r(care));
        put(sb, "lv", String.valueOf(level));
        put(sb, "xp", r(xp));
        StringBuilder g = new StringBuilder();
        for (int i = 0; i < genes.length; i++) {
            if (i > 0) g.append(',');
            g.append(genes[i]);
        }
        put(sb, "g", g.toString());
        put(sb, "m", String.join(",", mutations));
        put(sb, "d", disease.name());
        put(sb, "im", r(immunity));
        put(sb, "pr", pregnant ? "1" : "0");
        put(sb, "ge", r(gestation));
        put(sb, "si", sire);
        put(sb, "st", stabilized ? "1" : "0");
        put(sb, "bc", r(breedCooldown));
        put(sb, "ch", r(charge));
        put(sb, "an", analyzed ? "1" : "0");
        put(sb, "ow", owner);
        put(sb, "br", breed);
        put(sb, "li", String.join("~", lineage));
        put(sb, "sta", r(stamina));
        return sb.toString();
    }

    public static AnimalData parse(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        Map<String, String> m = new HashMap<>();
        for (String part : raw.split(";")) {
            int i = part.indexOf('=');
            if (i > 0) {
                m.put(part.substring(0, i), part.substring(i + 1));
            }
        }
        if (!m.containsKey("sp")) {
            return null;
        }
        AnimalData d = new AnimalData();
        d.speciesId = m.get("sp");
        d.name = m.getOrDefault("nm", "Animal");
        d.female = !"0".equals(m.get("f"));
        d.age = num(m, "ag", 0);
        d.hunger = num(m, "hu", 80);
        d.thirst = num(m, "th", 80);
        d.happiness = num(m, "ha", 70);
        d.hygiene = num(m, "hy", 90);
        d.diet = num(m, "di", 50);
        d.care = num(m, "ca", 70);
        d.level = (int) num(m, "lv", 1);
        d.xp = num(m, "xp", 0);
        String[] gs = m.getOrDefault("g", "").split(",");
        for (int i = 0; i < d.genes.length; i++) {
            try {
                d.genes[i] = i < gs.length ? Integer.parseInt(gs[i].trim()) : 50;
            } catch (NumberFormatException ex) {
                d.genes[i] = 50;
            }
        }
        for (String id : m.getOrDefault("m", "").split(",")) {
            if (!id.isBlank() && Mutations.get(id.trim()) != null) {
                d.mutations.add(id.trim());
            }
        }
        try {
            d.disease = Disease.valueOf(m.getOrDefault("d", "NONE"));
        } catch (IllegalArgumentException ex) {
            d.disease = Disease.NONE;
        }
        d.immunity = num(m, "im", 0);
        d.pregnant = "1".equals(m.get("pr"));
        d.gestation = num(m, "ge", 0);
        d.sire = m.getOrDefault("si", "");
        d.stabilized = "1".equals(m.get("st"));
        d.breedCooldown = num(m, "bc", 0);
        d.charge = num(m, "ch", 0);
        d.analyzed = "1".equals(m.get("an"));
        d.owner = m.getOrDefault("ow", "");
        d.breed = m.getOrDefault("br", "");
        String li = m.getOrDefault("li", "");
        if (!li.isEmpty()) {
            for (String s : li.split("~", -1)) {
                d.lineage.add(s);
            }
        }
        d.stamina = num(m, "sta", 100);
        return d;
    }

    private static double num(Map<String, String> m, String k, double def) {
        try {
            return Double.parseDouble(m.getOrDefault(k, String.valueOf(def)));
        } catch (NumberFormatException ex) {
            return def;
        }
    }
}
