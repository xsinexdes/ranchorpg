package dev.rancho.api;

import dev.rancho.RanchoPlugin;
import dev.rancho.animal.AnimalData;
import dev.rancho.animal.Gene;
import dev.rancho.season.Season;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/** API pública para que otros plugins lean estación, temperatura, niveles y genética. */
public final class RanchoAPI {

    private RanchoAPI() {}

    public static Season getSeason() {
        return RanchoPlugin.get().seasons().current();
    }

    public static int getSeasonDay() {
        return RanchoPlugin.get().seasons().day();
    }

    public static int getSeasonLength() {
        return RanchoPlugin.get().seasons().length();
    }

    /** Temperatura en °C de una ubicación (chunk cargado). */
    public static double getTemperature(Location location) {
        return RanchoPlugin.get().temperature().getTemperature(location);
    }

    /** Nivel de granjero del jugador. */
    public static int getFarmerLevel(Player player) {
        return RanchoPlugin.get().farmers().level(player.getUniqueId());
    }

    /** Nivel (1-10) de un animal, o 0 si la entidad no es un animal gestionado. */
    public static int getAnimalLevel(Entity entity) {
        AnimalData d = RanchoPlugin.get().animals().get(entity);
        return d == null ? 0 : d.level;
    }

    /** Valor efectivo (0-100) de un gen, o -1 si la entidad no es un animal gestionado. */
    public static int getGene(Entity entity, Gene gene) {
        AnimalData d = RanchoPlugin.get().animals().get(entity);
        return d == null ? -1 : d.eff(gene);
    }
}
