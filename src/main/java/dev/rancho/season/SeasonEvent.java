package dev.rancho.season;

/** Eventos de estación configurables. */
public enum SeasonEvent {
    HARVEST("harvest", Season.AUTUMN),
    FROST("frost", Season.WINTER),
    HEATWAVE("heatwave", Season.SUMMER),
    DROUGHT("drought", Season.SUMMER);

    private final String key;
    private final Season season;

    SeasonEvent(String key, Season season) {
        this.key = key;
        this.season = season;
    }

    public String key() { return key; }
    public Season season() { return season; }
}
