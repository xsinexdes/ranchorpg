package dev.rancho.animal;

import org.bukkit.Material;

/** Genes de un animal (estilo ARK). Cada gen vale 0-100. */
public enum Gene {
    HEALTH("Vida", Material.GOLDEN_APPLE),
    SPEED("Velocidad", Material.SUGAR),
    PRODUCTION("Producción", Material.WHEAT),
    RESISTANCE("Resistencia", Material.IRON_CHESTPLATE),
    FERTILITY("Fertilidad", Material.EGG),
    SIZE("Tamaño", Material.SLIME_BALL),
    QUALITY("Calidad", Material.DIAMOND),
    JUMP("Salto", Material.RABBIT_FOOT);

    private final String label;
    private final Material icon;

    Gene(String label, Material icon) {
        this.label = label;
        this.icon = icon;
    }

    public String label() { return label; }
    public Material icon() { return icon; }
}
