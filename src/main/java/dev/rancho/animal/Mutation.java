package dev.rancho.animal;

/** Una mutación del catálogo. {@code gene} puede ser null en las mutaciones especiales. */
public record Mutation(String id, String name, Gene gene, int delta, Rarity rarity, boolean positive, Special special) {

    /** Efectos especiales que no son un simple bono a un gen. */
    public enum Special { NONE, FAST_GROWTH, SICKLY, INFERTILE }

    public String colored() {
        return (positive ? "&a" : "&c") + rarity.color() + name;
    }

    /** Nombre con el color de su rareza. */
    public String display() {
        return rarity.color() + name + (positive ? " &a+" : " &c-");
    }
}
