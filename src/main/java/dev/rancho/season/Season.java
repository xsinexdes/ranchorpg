package dev.rancho.season;

import org.bukkit.Material;
import org.bukkit.boss.BarColor;

/** Las cuatro estaciones del año. */
public enum Season {
    SPRING("spring", Material.PINK_TULIP, BarColor.GREEN),
    SUMMER("summer", Material.SUNFLOWER, BarColor.YELLOW),
    AUTUMN("autumn", Material.PUMPKIN, BarColor.RED),
    WINTER("winter", Material.SNOWBALL, BarColor.WHITE);

    private final String key;
    private final Material icon;
    private final BarColor barColor;

    Season(String key, Material icon, BarColor barColor) {
        this.key = key;
        this.icon = icon;
        this.barColor = barColor;
    }

    /** Clave usada en config.yml y lang.yml (spring, summer, autumn, winter). */
    public String key() { return key; }
    public Material icon() { return icon; }
    public BarColor barColor() { return barColor; }

    /** Estación que está {@code steps} posiciones después de esta. */
    public Season plus(long steps) {
        Season[] all = values();
        int idx = (int) ((ordinal() + steps) % all.length);
        return all[idx];
    }

    public Season next() {
        return plus(1);
    }
}
