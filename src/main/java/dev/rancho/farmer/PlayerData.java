package dev.rancho.farmer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Datos persistentes de un jugador: nivel de granjero, habilidades, logros y misiones. */
public final class PlayerData {

    public String name = "";
    public double xp;
    public int level = 1;
    public int skillPoints;
    public double coins;
    /** El jugador ocultó el panel de información (/rancho hud). */
    public boolean hudOff;
    /** El jugador forzó ver los modelos 3D (por ejemplo porque instaló el pack en local). */
    public boolean modelsForced;
    public final Map<String, Integer> skills = new HashMap<>();
    public final Map<String, Integer> stats = new HashMap<>();
    public final Set<String> achievements = new HashSet<>();
    /** Misiones activas: id -> progreso. */
    public final Map<String, Integer> quests = new LinkedHashMap<>();

    public int skill(Skill s) {
        return skills.getOrDefault(s.name(), 0);
    }

    public int stat(String key) {
        return stats.getOrDefault(key, 0);
    }
}
