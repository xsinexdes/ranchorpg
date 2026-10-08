package dev.rancho.farmer;

import org.bukkit.Material;

/** Habilidades desbloqueables del Granjero (máx. nivel 5 cada una). */
public enum Skill {
    PRODUCTION("Producción", "+5% de producción de los animales por nivel", Material.WHEAT),
    FRUGAL("Frugal", "-8% de consumo de comida y agua por nivel", Material.BREAD),
    HEALER("Curandero", "+10% de curación de salud por nivel", Material.GLISTERING_MELON_SLICE),
    GREEN_THUMB("Mano Verde", "+5% de crecimiento de cultivos por nivel", Material.BONE_MEAL),
    HAGGLER("Regateo", "+3% de precio de venta por nivel", Material.EMERALD),
    BREEDER("Criador", "-4% de tiempo de gestación por nivel", Material.EGG);

    public static final int MAX_LEVEL = 5;

    private final String label;
    private final String description;
    private final Material icon;

    Skill(String label, String description, Material icon) {
        this.label = label;
        this.description = description;
        this.icon = icon;
    }

    public String label() { return label; }
    public String description() { return description; }
    public Material icon() { return icon; }
}
