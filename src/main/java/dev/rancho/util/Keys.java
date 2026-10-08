package dev.rancho.util;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/** Claves de PersistentDataContainer usadas por el plugin. */
public final class Keys {

    public static NamespacedKey ITEM, USES, PTYPE, PID, QUALITY, BASE, SEED, ANIMAL, SPECIES, HOLO, NPC;

    private Keys() {}

    public static void init(Plugin p) {
        ITEM = new NamespacedKey(p, "item");
        USES = new NamespacedKey(p, "uses");
        PTYPE = new NamespacedKey(p, "ptype");
        PID = new NamespacedKey(p, "pid");
        QUALITY = new NamespacedKey(p, "quality");
        BASE = new NamespacedKey(p, "base");
        SEED = new NamespacedKey(p, "seed");
        ANIMAL = new NamespacedKey(p, "animal");
        SPECIES = new NamespacedKey(p, "species");
        HOLO = new NamespacedKey(p, "holo");
        NPC = new NamespacedKey(p, "npc");
    }
}
