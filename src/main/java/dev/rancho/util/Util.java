package dev.rancho.util;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/** Utilidades generales. */
public final class Util {

    private Util() {}

    public static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    public static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    public static double rnd() {
        return ThreadLocalRandom.current().nextDouble();
    }

    /** Entero aleatorio en [lo, hi] (ambos incluidos). */
    public static int rndInt(int lo, int hi) {
        if (hi <= lo) {
            return lo;
        }
        return ThreadLocalRandom.current().nextInt(lo, hi + 1);
    }

    public static boolean chance(double probability) {
        return rnd() < probability;
    }

    public static String fmt(double d) {
        return String.format(Locale.ROOT, "%.1f", d);
    }

    public static String fmt0(double d) {
        return String.format(Locale.ROOT, "%.0f", d);
    }

    /** Lista separada por comas, sin vacíos. */
    public static List<String> csv(String s) {
        List<String> out = new ArrayList<>();
        if (s == null || s.isBlank()) {
            return out;
        }
        for (String p : s.split(",")) {
            String t = p.trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }

    public static Material mat(String name, Material def) {
        if (name == null) {
            return def;
        }
        Material m = Material.matchMaterial(name.trim());
        return m == null ? def : m;
    }

    /** Barra de progreso coloreada con códigos &amp;. */
    public static String bar(double pct, int len) {
        double p = clamp(pct, 0, 100);
        int filled = (int) Math.round(p / 100.0 * len);
        String color = p > 60 ? "&a" : p > 30 ? "&e" : "&c";
        return color + "█".repeat(filled) + "&7" + "█".repeat(len - filled);
    }

    /** Rango S/A/B/C/D/F para un valor 0-100. */
    public static String rank(double v) {
        if (v >= 90) return "&6S";
        if (v >= 75) return "&aA";
        if (v >= 60) return "&2B";
        if (v >= 45) return "&eC";
        if (v >= 30) return "&6D";
        return "&cF";
    }

    /** Quita los caracteres usados como separadores en datos serializados. */
    public static String clean(String s) {
        return s == null ? "" : s.replaceAll("[;=~|/,§]", "").trim();
    }

    /** Consume un item de la mano principal (no en creativo). */
    public static void takeOne(Player p) {
        if (p.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            return;
        }
        ItemStack it = p.getInventory().getItemInMainHand();
        if (it.getAmount() <= 1) {
            p.getInventory().setItemInMainHand(null);
        } else {
            it.setAmount(it.getAmount() - 1);
        }
    }

    public static void giveOrDrop(Player p, ItemStack item) {
        for (ItemStack left : p.getInventory().addItem(item).values()) {
            p.getWorld().dropItemNaturally(p.getLocation(), left);
        }
    }

    /** Muestra un mensaje en la ActionBar (API de Spigot). */
    public static void actionBar(Player p, String text) {
        p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                new net.md_5.bungee.api.chat.TextComponent(text));
    }

    public static String title(String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        String s = key.replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
