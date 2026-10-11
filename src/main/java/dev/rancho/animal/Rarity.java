package dev.rancho.animal;

/** Rareza de una mutación (define el color y la probabilidad). */
public enum Rarity {
    COMMON("&7", "Común", 40),
    UNCOMMON("&a", "Poco común", 30),
    RARE("&9", "Rara", 18),
    EPIC("&5", "Épica", 9),
    LEGENDARY("&6", "Legendaria", 3);

    private final String color;
    private final String label;
    private final int weight;

    Rarity(String color, String label, int weight) {
        this.color = color;
        this.label = label;
        this.weight = weight;
    }

    public String color() { return color; }
    public String label() { return label; }
    public int weight() { return weight; }
}
