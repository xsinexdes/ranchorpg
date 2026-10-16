package dev.rancho.quality;

import dev.rancho.RanchoPlugin;
import dev.rancho.util.Util;

/** Dificultad de los niveles de calidad: curva que hace mucho más difícil llegar a los niveles altos. */
public final class QualityMath {

    private QualityMath() {}

    /**
     * Convierte una puntuación bruta de 0 a 10 en un nivel 1-10 con la curva de dificultad
     * (difficulty.quality-hardness: 1.0 = lineal, 1.8 por defecto, más alto = más difícil).
     */
    public static int harden(double raw10) {
        double h = Math.max(0.5, RanchoPlugin.get().getConfig().getDouble("difficulty.quality-hardness", 1.8));
        double v = 10.0 * Math.pow(Util.clamp(raw10, 0, 10) / 10.0, h);
        return Util.clamp((int) Math.round(v), 1, 10);
    }
}
