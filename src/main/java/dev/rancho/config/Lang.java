package dev.rancho.config;

import dev.rancho.RanchoPlugin;
import dev.rancho.util.Saver;
import dev.rancho.util.Text;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Mensajes traducibles. Se guardan en lang.yml y se editan desde /rancho admin. */
public final class Lang {

    private static final Map<String, String> DEFAULTS = new LinkedHashMap<>();

    /** Registra un mensaje con su valor por defecto (llamar antes de crear la instancia). */
    public static void define(String key, String value) {
        DEFAULTS.putIfAbsent(key, value);
    }

    private final RanchoPlugin plugin;
    private File file;
    private YamlConfiguration yaml;

    public Lang(RanchoPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    /** Carga lang.yml y añade las claves que falten. */
    public void load() {
        file = new File(plugin.getDataFolder(), "lang.yml");
        yaml = YamlConfiguration.loadConfiguration(file);
        boolean changed = !file.exists();
        for (Map.Entry<String, String> e : DEFAULTS.entrySet()) {
            if (!yaml.contains(e.getKey())) {
                yaml.set(e.getKey(), e.getValue());
                changed = true;
            }
        }
        if (changed) {
            Saver.save(plugin, yaml, file);
        }
    }

    /** Texto sin colorear, tal cual está guardado. */
    public String raw(String key) {
        return yaml.getString(key, DEFAULTS.getOrDefault(key, key));
    }

    /** Texto coloreado con los pares clave/valor reemplazados: get("k", "day", 3). */
    public String get(String key, Object... pairs) {
        String text = raw(key);
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            text = text.replace("{" + pairs[i] + "}", String.valueOf(pairs[i + 1]));
        }
        return Text.color(text);
    }

    /** Igual que {@link #get} pero con el prefijo del plugin delante. */
    public String prefixed(String key, Object... pairs) {
        return Text.color(raw("prefix")) + get(key, pairs);
    }

    public void set(String key, String value) {
        yaml.set(key, value);
        Saver.save(plugin, yaml, file);
    }

    public List<String> keys() {
        return new ArrayList<>(DEFAULTS.keySet());
    }
}
