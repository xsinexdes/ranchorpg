package dev.rancho.animal;

/** Etapa de vida de un animal. */
public enum Stage {
    BABY("Cría", "&b"),
    YOUNG("Joven", "&e"),
    ADULT("Adulto", "&a"),
    OLD("Viejo", "&7");

    private final String label;
    private final String color;

    Stage(String label, String color) {
        this.label = label;
        this.color = color;
    }

    public String label() { return label; }
    public String color() { return color; }
}
