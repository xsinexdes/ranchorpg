package dev.rancho.economy;

import dev.rancho.RanchoPlugin;
import dev.rancho.farmer.Skill;
import dev.rancho.item.CustomItems;
import dev.rancho.quality.Products;
import dev.rancho.season.Season;
import dev.rancho.util.Saver;
import dev.rancho.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Mercado comprador: precio por calidad, demanda por estación y demanda dinámica según lo vendido. */
public final class MarketManager {

    /** Grupos de productos del mercado. */
    public static final List<String> GROUPS = List.of("meat", "milk", "wool", "egg", "leather", "crop");

    private final RanchoPlugin plugin;
    private final File file;
    private final Map<String, Double> dynamic = new LinkedHashMap<>();
    private BukkitTask task;

    public MarketManager(RanchoPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data/market.yml");
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        for (String g : GROUPS) {
            dynamic.put(g, y.getDouble("dynamic." + g, 1.0));
        }
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::recover, 1200L, 1200L);
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
        }
        save();
    }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<String, Double> e : dynamic.entrySet()) {
            y.set("dynamic." + e.getKey(), e.getValue());
        }
        Saver.save(plugin, y, file);
    }

    /** La demanda dinámica se recupera con el tiempo. */
    private void recover() {
        double rec = plugin.getConfig().getDouble("market.dynamic-recover", 0.02);
        for (Map.Entry<String, Double> e : dynamic.entrySet()) {
            e.setValue(Math.min(1.0, e.getValue() + rec));
        }
    }

    public double dynamicFactor(String group) {
        return dynamic.getOrDefault(group, 1.0);
    }

    public double seasonFactor(String group, Season s) {
        return plugin.getConfig().getDouble("market.demand." + group + "." + s.key(), 1.0);
    }

    /** Demanda total (estación × dinámica) de un grupo. */
    public double demand(String group) {
        return seasonFactor(group, plugin.seasons().current()) * dynamicFactor(group);
    }

    /** Precio de venta de UNA unidad del producto para el jugador. */
    public double unitPrice(ItemStack item, Player p) {
        String type = Products.type(item);
        if (type == null) {
            return 0;
        }
        double haggle = 1 + plugin.farmers().skill(p.getUniqueId(), Skill.HAGGLER) * 0.03;
        return Products.value(item) * demand(type) * haggle;
    }

    public boolean sellable(ItemStack item) {
        String t = Products.type(item);
        return t != null && GROUPS.contains(t);
    }

    /** Total que se obtendría vendiendo todo el inventario (sin vender). */
    public double previewAll(Player p) {
        double total = 0;
        for (ItemStack it : p.getInventory().getStorageContents()) {
            if (it != null && sellable(it)) {
                total += unitPrice(it, p) * it.getAmount();
            }
        }
        return total;
    }

    /** Vende todos los productos del inventario. Devuelve lo cobrado. */
    public double sellAll(Player p) {
        double total = 0;
        int units = 0;
        Map<String, Integer> sold = new LinkedHashMap<>();
        ItemStack[] contents = p.getInventory().getStorageContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (it == null || !sellable(it)) {
                continue;
            }
            total += unitPrice(it, p) * it.getAmount();
            units += it.getAmount();
            sold.merge(Products.type(it), it.getAmount(), Integer::sum);
            contents[i] = null;
        }
        if (units == 0) {
            p.sendMessage(plugin.lang().prefixed("market.nothing"));
            return 0;
        }
        p.getInventory().setStorageContents(contents);
        double drop = plugin.getConfig().getDouble("market.dynamic-drop", 0.005);
        double min = plugin.getConfig().getDouble("market.dynamic-min", 0.6);
        for (Map.Entry<String, Integer> e : sold.entrySet()) {
            dynamic.put(e.getKey(), Math.max(min, dynamicFactor(e.getKey()) - drop * e.getValue()));
        }
        plugin.economy().deposit(p, total);
        plugin.farmers().stat(p, "sold", units);
        plugin.farmers().addXp(p, plugin.getConfig().getDouble("farmer.xp.sell", 0.5) * units);
        p.sendMessage(plugin.lang().prefixed("market.sold", "units", units, "total", Util.fmt(total)));
        p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_YES, 1f, 1f);
        return total;
    }

    /** Precio de compra de un item de la tienda. */
    public double shopPrice(String id) {
        CustomItems.Def d = CustomItems.get(id);
        double def = d == null ? 0 : d.price();
        return plugin.getConfig().getDouble("shop." + id, def);
    }

    public double seedPrice(String cropId) {
        var cd = plugin.cropDefs().get(cropId);
        double def = cd == null ? 50 : Math.max(10, cd.price() * 8);
        return plugin.getConfig().getDouble("shop.seed_" + cropId, def);
    }
}
