package dev.rancho.animal;

import dev.rancho.RanchoPlugin;
import dev.rancho.item.CustomItems;
import dev.rancho.util.Saver;
import dev.rancho.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.CauldronLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Comederos (guardan raciones de pienso y alimentan solos a los animales cercanos) y
 * abrevaderos (agua infinita para los animales cercanos).
 */
public final class TroughManager implements Listener {

    private static final double MAX_SERVINGS = 24;

    private static final class Trough {
        final String world;
        final int x, y, z;
        final String type;
        double servings;

        Trough(String world, int x, int y, int z, String type) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.type = type;
        }
    }

    private final RanchoPlugin plugin;
    private final File file;
    private final Map<String, Trough> troughs = new HashMap<>();

    public TroughManager(RanchoPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data/troughs.yml");
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        for (String line : y.getStringList("troughs")) {
            String[] p = line.split("\\|", -1);
            String[] k = p[0].split(";");
            if (p.length < 3 || k.length < 4) continue;
            try {
                Trough t = new Trough(k[0], Integer.parseInt(k[1]), Integer.parseInt(k[2]),
                        Integer.parseInt(k[3]), p[1]);
                t.servings = Double.parseDouble(p[2]);
                troughs.put(p[0], t);
            } catch (NumberFormatException ignored) {
                // línea corrupta: se omite
            }
        }
    }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, Trough> e : troughs.entrySet()) {
            lines.add(e.getKey() + "|" + e.getValue().type + "|" + e.getValue().servings);
        }
        y.set("troughs", lines);
        Saver.save(plugin, y, file);
    }

    private static String keyOf(Block b) {
        return b.getWorld().getName() + ";" + b.getX() + ";" + b.getY() + ";" + b.getZ();
    }

    private Trough at(Block b) {
        String key = keyOf(b);
        Trough t = troughs.get(key);
        if (t == null) {
            return null;
        }
        boolean ok = t.type.equals("feeder") ? b.getType() == Material.BARREL
                : b.getType() == Material.WATER_CAULDRON || b.getType() == Material.CAULDRON;
        if (!ok) {
            troughs.remove(key);
            return null;
        }
        return t;
    }

    /** ¿Hay un abrevadero a menos de 4 bloques? */
    public boolean waterNear(Location loc) {
        String w = loc.getWorld().getName();
        for (Trough t : troughs.values()) {
            if (!t.type.equals("waterer") || !t.world.equals(w)) continue;
            double dx = t.x + 0.5 - loc.getX(), dy = t.y + 0.5 - loc.getY(), dz = t.z + 0.5 - loc.getZ();
            if (dx * dx + dy * dy + dz * dz <= 16) {
                return true;
            }
        }
        return false;
    }

    /** Toma una ración de un comedero a menos de 5 bloques. */
    public boolean takeServing(Location loc) {
        String w = loc.getWorld().getName();
        for (Trough t : troughs.values()) {
            if (!t.type.equals("feeder") || !t.world.equals(w) || t.servings < 1) continue;
            double dx = t.x + 0.5 - loc.getX(), dy = t.y + 0.5 - loc.getY(), dz = t.z + 0.5 - loc.getZ();
            if (dx * dx + dy * dy + dz * dz <= 25) {
                t.servings -= 1;
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ eventos

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        ItemStack item = e.getItemInHand();
        String id = CustomItems.id(item);
        if (id == null || !(id.equals("feeder") || id.equals("waterer"))) {
            return;
        }
        Block b = e.getBlockPlaced();
        if (id.equals("waterer")) {
            b.setType(Material.WATER_CAULDRON);
            if (b.getBlockData() instanceof Levelled lv) {
                lv.setLevel(lv.getMaximumLevel());
                b.setBlockData(lv);
            }
        }
        troughs.put(keyOf(b), new Trough(b.getWorld().getName(), b.getX(), b.getY(), b.getZ(),
                id.equals("feeder") ? "feeder" : "waterer"));
        save();
        e.getPlayer().sendMessage(plugin.lang().prefixed(id.equals("feeder") ? "trough.feeder-placed"
                : "trough.waterer-placed"));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        Trough t = at(b);
        if (t == null) {
            return;
        }
        e.setDropItems(false);
        troughs.remove(keyOf(b));
        save();
        if (e.getPlayer().getGameMode() != GameMode.CREATIVE) {
            b.getWorld().dropItemNaturally(b.getLocation().add(0.5, 0.5, 0.5),
                    CustomItems.create(t.type.equals("feeder") ? "feeder" : "waterer", 1));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCauldron(CauldronLevelChangeEvent e) {
        if (at(e.getBlock()) != null) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null) {
            return;
        }
        Trough t = at(e.getClickedBlock());
        if (t == null) {
            return;
        }
        Player p = e.getPlayer();
        ItemStack item = e.getItem();
        if (p.isSneaking() && item != null && !item.getType().isAir()) {
            return;
        }
        e.setCancelled(true);
        if (t.type.equals("waterer")) {
            Util.actionBar(p, plugin.lang().get("trough.waterer-info"));
            return;
        }
        double add = switch (String.valueOf(CustomItems.id(item))) {
            case "feed_basic" -> 1.0;
            case "feed_premium" -> 2.0;
            case "forage" -> 0.7;
            default -> 0.0;
        };
        if (add > 0) {
            if (t.servings + add > MAX_SERVINGS) {
                p.sendMessage(plugin.lang().prefixed("trough.full"));
                return;
            }
            Util.takeOne(p);
            t.servings += add;
            save();
            p.playSound(p.getLocation(), org.bukkit.Sound.ITEM_BONE_MEAL_USE, 0.8f, 1f);
        }
        Util.actionBar(p, plugin.lang().get("trough.feeder-info", "servings", Util.fmt(t.servings),
                "max", Util.fmt0(MAX_SERVINGS)));
    }
}
