package dev.rancho.util;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Guarda YAML en disco de forma asíncrona (la serialización se hace en el hilo principal). */
public final class Saver {

    private static final Object LOCK = new Object();

    private Saver() {}

    public static void save(Plugin plugin, YamlConfiguration yaml, File file) {
        final String data = yaml.saveToString();
        Runnable job = () -> write(plugin, file, data);
        if (plugin.isEnabled()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, job);
        } else {
            job.run();
        }
    }

    private static void write(Plugin plugin, File file, String data) {
        synchronized (LOCK) {
            try {
                File parent = file.getParentFile();
                if (parent != null) {
                    parent.mkdirs();
                }
                Path tmp = file.toPath().resolveSibling(file.getName() + ".tmp");
                Files.writeString(tmp, data, StandardCharsets.UTF_8);
                Files.move(tmp, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                plugin.getLogger().warning("No se pudo guardar " + file.getName() + ": " + ex.getMessage());
            }
        }
    }
}
