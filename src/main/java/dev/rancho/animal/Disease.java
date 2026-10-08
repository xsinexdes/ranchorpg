package dev.rancho.animal;

/** Enfermedades del ganado y el item que las cura. */
public enum Disease {
    NONE("Sano", null),
    PARASITES("Parásitos", "antiparasitic"),
    FLU("Gripe", "antibiotic"),
    INFECTION("Infección", "antibiotic");

    private final String label;
    private final String cure;

    Disease(String label, String cure) {
        this.label = label;
        this.cure = cure;
    }

    public String label() { return label; }

    /** Id del item custom que la cura (null si está sano). */
    public String cure() { return cure; }
}
