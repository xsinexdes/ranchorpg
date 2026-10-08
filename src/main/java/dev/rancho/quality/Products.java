package dev.rancho.quality;

import dev.rancho.RanchoPlugin;
import dev.rancho.util.Keys;
import dev.rancho.util.Text;
import dev.rancho.util.Util;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/** Creación y lectura de productos con nivel de calidad (carne, leche, lana, huevos, cuero, cultivos). */
public final class Products {

    private Products() {}

    /**
     * Crea un producto.
     *
     * @param type    meat, milk, wool, egg, leather o crop
     * @param id      id de la especie o del cultivo
     * @param base    precio base de venta
     */
    public static ItemStack create(String type, String id, String name, Material mat, int quality,
                                   double base, int amount) {
        RanchoPlugin pl = RanchoPlugin.get();
        QualityLevels lv = pl.levels();
        int q = Util.clamp(quality, 1, QualityLevels.MAX);
        ItemStack item = new ItemStack(mat, Math.max(1, Math.min(64, amount)));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        String color = lv.color(q);
        meta.setDisplayName(pl.lang().get("product.name", "color", color, "name", name, "q", q));

        List<String> lore = new ArrayList<>();
        lore.add(pl.lang().get("product.lore.tier", "color", color, "tier", lv.name(q)));
        String l = lv.lore(q);
        if (!l.isBlank()) {
            lore.add(Text.color("&8" + l));
        }
        lore.add(pl.lang().get("product.lore.value", "value", Util.fmt(base * lv.mult(q))));
        if (lv.saturation(q) > 0) {
            lore.add(pl.lang().get("product.lore.sat", "sat", lv.saturation(q)));
        }
        if (lv.effectType(q) != null) {
            lore.add(pl.lang().get("product.lore.buff", "effect", Util.title(lv.effectKey(q)),
                    "secs", lv.effectSeconds(q)));
        }
        meta.setLore(lore);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.values());

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(Keys.PTYPE, PersistentDataType.STRING, type);
        pdc.set(Keys.PID, PersistentDataType.STRING, id);
        pdc.set(Keys.QUALITY, PersistentDataType.INTEGER, q);
        pdc.set(Keys.BASE, PersistentDataType.DOUBLE, base);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isProduct(ItemStack item) {
        return type(item) != null;
    }

    public static String type(ItemStack item) {
        return readString(item, Keys.PTYPE);
    }

    public static String id(ItemStack item) {
        return readString(item, Keys.PID);
    }

    public static int quality(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 0;
        }
        Integer q = item.getItemMeta().getPersistentDataContainer().get(Keys.QUALITY, PersistentDataType.INTEGER);
        return q == null ? 0 : q;
    }

    public static double base(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 0;
        }
        Double b = item.getItemMeta().getPersistentDataContainer().get(Keys.BASE, PersistentDataType.DOUBLE);
        return b == null ? 0 : b;
    }

    /** Valor de venta sin demanda: base × multiplicador del nivel. */
    public static double value(ItemStack item) {
        return base(item) * RanchoPlugin.get().levels().mult(quality(item));
    }

    private static String readString(ItemStack item, org.bukkit.NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }
}
