package dev.rancho.animal;

import dev.rancho.animal.Mutation.Special;
import dev.rancho.util.Util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Catálogo de mutaciones positivas y negativas. */
public final class Mutations {

    private static final Map<String, Mutation> ALL = new LinkedHashMap<>();

    private static void add(String id, String name, Gene g, int delta, Rarity r, boolean pos, Special s) {
        ALL.put(id, new Mutation(id, name, g, delta, r, pos, s));
    }

    static {
        add("vitality", "Vitalidad", Gene.HEALTH, 15, Rarity.UNCOMMON, true, Special.NONE);
        add("titan", "Titán", Gene.HEALTH, 30, Rarity.EPIC, true, Special.NONE);
        add("swift", "Veloz", Gene.SPEED, 15, Rarity.UNCOMMON, true, Special.NONE);
        add("lightning", "Relámpago", Gene.SPEED, 30, Rarity.EPIC, true, Special.NONE);
        add("prolific", "Productor", Gene.PRODUCTION, 20, Rarity.UNCOMMON, true, Special.NONE);
        add("hardy", "Resistente al clima", Gene.RESISTANCE, 20, Rarity.RARE, true, Special.NONE);
        add("fertile", "Fértil", Gene.FERTILITY, 20, Rarity.UNCOMMON, true, Special.NONE);
        add("giant", "Gigante", Gene.SIZE, 20, Rarity.RARE, true, Special.NONE);
        add("fine", "Fino", Gene.QUALITY, 15, Rarity.RARE, true, Special.NONE);
        add("golden", "Dorado", Gene.QUALITY, 35, Rarity.LEGENDARY, true, Special.NONE);
        add("leaper", "Saltarín", Gene.JUMP, 20, Rarity.RARE, true, Special.NONE);
        add("fast_growth", "Crecimiento rápido", null, 0, Rarity.EPIC, true, Special.FAST_GROWTH);

        add("fragile", "Frágil", Gene.HEALTH, -15, Rarity.COMMON, false, Special.NONE);
        add("slow", "Lento", Gene.SPEED, -15, Rarity.COMMON, false, Special.NONE);
        add("unproductive", "Improductivo", Gene.PRODUCTION, -20, Rarity.COMMON, false, Special.NONE);
        add("weak", "Débil al clima", Gene.RESISTANCE, -20, Rarity.UNCOMMON, false, Special.NONE);
        add("tiny", "Diminuto", Gene.SIZE, -15, Rarity.COMMON, false, Special.NONE);
        add("coarse", "Tosco", Gene.QUALITY, -15, Rarity.UNCOMMON, false, Special.NONE);
        add("sickly", "Enfermizo", null, 0, Rarity.UNCOMMON, false, Special.SICKLY);
        add("infertile", "Infértil", Gene.FERTILITY, -35, Rarity.RARE, false, Special.INFERTILE);
    }

    private Mutations() {}

    public static Mutation get(String id) {
        return ALL.get(id);
    }

    public static List<Mutation> all() {
        return new ArrayList<>(ALL.values());
    }

    /** Elige una mutación al azar ponderada por rareza. */
    public static Mutation random(boolean positive) {
        List<Mutation> pool = new ArrayList<>();
        int total = 0;
        for (Mutation m : ALL.values()) {
            if (m.positive() == positive) {
                pool.add(m);
                total += m.rarity().weight();
            }
        }
        int roll = Util.rndInt(1, Math.max(1, total));
        for (Mutation m : pool) {
            roll -= m.rarity().weight();
            if (roll <= 0) {
                return m;
            }
        }
        return pool.get(0);
    }
}
