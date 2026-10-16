package dev.rancho.climate;

import dev.rancho.RanchoPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Calcula la temperatura (°C) de una ubicación según bioma, altura, hora,
 * clima, estación y bloques cercanos. Solo lee chunks ya cargados y cachea
 * el resultado unos segundos para no generar lag.
 */
public final class TemperatureManager {

    private static final long CACHE_MS = 5000L;
    private static final int CACHE_MAX = 4000;

    private record Cached(double value, long time) {}

    private final RanchoPlugin plugin;
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    private double scale, offset, dayAmp, rain, thunder, lava, fire, torch, ice, water;
    private int radius;

    public TemperatureManager(RanchoPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    /** Relee los valores de config.yml y vacía la caché. */
    public void reload() {
        FileConfiguration c = plugin.getConfig();
        scale = c.getDouble("temperature.biome-scale", 25.0);
        offset = c.getDouble("temperature.biome-offset", -5.0);
        dayAmp = c.getDouble("temperature.day-amplitude", 4.0);
        rain = c.getDouble("temperature.rain-offset", -4.0);
        thunder = c.getDouble("temperature.thunder-offset", -5.0);
        lava = c.getDouble("temperature.lava-bonus", 6.0);
        fire = c.getDouble("temperature.fire-bonus", 4.0);
        torch = c.getDouble("temperature.torch-bonus", 1.0);
        ice = c.getDouble("temperature.ice-bonus", -3.0);
        water = c.getDouble("temperature.water-bonus", -1.0);
        radius = Math.max(0, Math.min(5, c.getInt("temperature.scan-radius", 3)));
        cache.clear();
    }

    /** Temperatura en °C (redondeada a 1 decimal). Si el chunk no está cargado devuelve 15. */
    public double getTemperature(Location loc) {
        World w = loc.getWorld();
        if (w == null || !w.isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
            return 15.0;
        }
        String key = w.getUID() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
        long now = System.currentTimeMillis();
        Cached hit = cache.get(key);
        if (hit != null && now - hit.time() < CACHE_MS) {
            return hit.value();
        }
        double value = Math.round(compute(w, loc) * 10.0) / 10.0;
        if (cache.size() > CACHE_MAX) {
            cache.clear();
        }
        cache.put(key, new Cached(value, now));
        return value;
    }

    private double compute(World w, Location loc) {
        Block block = loc.getBlock();
        double temp;

        switch (w.getEnvironment()) {
            case NETHER -> temp = 40.0;
            case THE_END -> temp = -5.0;
            default -> {
                // Block#getTemperature ya contempla la altura sobre el nivel del mar.
                temp = block.getTemperature() * scale + offset;
                temp += plugin.seasons().temperatureOffset();

                double angle = ((w.getTime() - 6000L) / 24000.0) * 2.0 * Math.PI;
                temp += dayAmp * Math.cos(angle); // máximo al mediodía, mínimo a medianoche

                if (w.hasStorm() && block.getLightFromSky() >= 14) {
                    temp += w.isThundering() ? thunder : rain;
                }
            }
        }
        return temp + nearbyBlocks(w, block);
    }

    /** Suma el mejor aporte de cada categoría de bloque cercano, atenuado por la distancia. */
    private double nearbyBlocks(World w, Block origin) {
        if (radius == 0) {
            return 0.0;
        }
        double bestLava = 0, bestFire = 0, bestTorch = 0, bestIce = 0, bestWater = 0;
        int ox = origin.getX(), oy = origin.getY(), oz = origin.getZ();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (!w.isChunkLoaded((ox + x) >> 4, (oz + z) >> 4)) {
                    continue;
                }
                for (int y = -radius; y <= radius; y++) {
                    Material m = w.getBlockAt(ox + x, oy + y, oz + z).getType();
                    if (m.isAir()) {
                        continue;
                    }
                    double dist = Math.sqrt(x * x + y * y + z * z);
                    double factor = 1.0 - dist / (radius + 1.0);
                    if (factor <= 0) {
                        continue;
                    }
                    switch (m) {
                        case LAVA, MAGMA_BLOCK -> bestLava = Math.max(bestLava, factor);
                        case FIRE, SOUL_FIRE, CAMPFIRE, SOUL_CAMPFIRE -> bestFire = Math.max(bestFire, factor);
                        case TORCH, WALL_TORCH, LANTERN -> bestTorch = Math.max(bestTorch, factor);
                        case ICE, PACKED_ICE, BLUE_ICE, SNOW_BLOCK, POWDER_SNOW -> bestIce = Math.max(bestIce, factor);
                        case WATER -> bestWater = Math.max(bestWater, factor);
                        default -> { }
                    }
                }
            }
        }
        return bestLava * lava + bestFire * fire + bestTorch * torch + bestIce * ice + bestWater * water;
    }

    /** Código de color según la temperatura (para mensajes). */
    public static String colorFor(double temp) {
        if (temp <= 0) return "&9";
        if (temp < 10) return "&b";
        if (temp < 25) return "&a";
        if (temp < 35) return "&e";
        return "&c";
    }
}
