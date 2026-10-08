package dev.rancho.crop;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;

/** Un cultivo plantado y gestionado por el plugin. */
public final class CropData {

    public final String key;
    public String cropId;
    /** Progreso de crecimiento 0-1. */
    public double progress;
    public double qualitySum;
    public int samples;
    /** Acumulado de estrés (0-1); al llegar a 1 el cultivo se marchita. */
    public double wither;
    public boolean withered;
    public String owner = "";

    public CropData(String key, String cropId) {
        this.key = key;
        this.cropId = cropId;
    }

    public static String key(Block b) {
        return b.getWorld().getName() + ";" + b.getX() + ";" + b.getY() + ";" + b.getZ();
    }

    public static String key(Location l) {
        return l.getWorld().getName() + ";" + l.getBlockX() + ";" + l.getBlockY() + ";" + l.getBlockZ();
    }

    public World world() {
        return Bukkit.getWorld(key.split(";")[0]);
    }

    public int x() { return Integer.parseInt(key.split(";")[1]); }
    public int y() { return Integer.parseInt(key.split(";")[2]); }
    public int z() { return Integer.parseInt(key.split(";")[3]); }

    public Block block() {
        World w = world();
        return w == null ? null : w.getBlockAt(x(), y(), z());
    }

    public boolean mature() {
        return progress >= 1.0 && !withered;
    }

    /** Calidad media (0-1) de las condiciones durante el crecimiento. */
    public double avgScore() {
        return samples == 0 ? 0.5 : qualitySum / samples;
    }
}
