package dev.rancho.machine;

import dev.rancho.RanchoPlugin;
import dev.rancho.crop.SoilData;
import dev.rancho.food.Food;
import dev.rancho.item.CustomItems;
import dev.rancho.quality.Products;
import dev.rancho.season.Season;
import dev.rancho.util.Saver;
import dev.rancho.util.Text;
import dev.rancho.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Máquinas del rancho: aspersor (riego automático), espantapájaros (anti-plagas), colmena (miel),
 * nevera (comida se pudre mucho más lento) y acuario (peces que no se pudren).
 */
public final class MachineManager implements Listener {

    public static final Set<String> TYPES = Set.of("sprinkler", "scarecrow", "hive", "fridge", "aquarium");

    private static final class Machine {
        final String key;
        final String type;
        final String world;
        final int x, y, z;
        double value;
        long last;
        double flowers;
        long lastScan;
        final Map<Integer, ItemStack> items = new HashMap<>();

        Machine(String key, String type, String world, int x, int y, int z) {
            this.key = key;
            this.type = type;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    /** Dueño del inventario virtual de neveras y acuarios. */
    public static final class StorageHolder implements InventoryHolder {
        final String key;
        final String type;
        Inventory inv;

        StorageHolder(String key, String type) {
            this.key = key;
            this.type = type;
        }

        @Override
        public Inventory getInventory() {
            return inv;
        }
    }

    private final RanchoPlugin plugin;
    private final File file;
    private final Map<String, Machine> machines = new HashMap<>();
    private BukkitTask task;
    private boolean dirty;

    public MachineManager(RanchoPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data/machines.yml");
        load();
    }

    // ------------------------------------------------------------------ persistencia

    private void load() {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = y.getConfigurationSection("machines");
        if (sec == null) {
            return;
        }
        for (String idx : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(idx);
            if (s == null) continue;
            String key = s.getString("key", "");
            String[] k = key.split(";");
            if (k.length < 4) continue;
            try {
                Machine m = new Machine(key, s.getString("type", ""), k[0], Integer.parseInt(k[1]),
                        Integer.parseInt(k[2]), Integer.parseInt(k[3]));
                m.value = s.getDouble("value");
                m.last = s.getLong("last", System.currentTimeMillis());
                ConfigurationSection items = s.getConfigurationSection("items");
                if (items != null) {
                    for (String slot : items.getKeys(false)) {
                        ItemStack it = items.getItemStack(slot);
                        if (it != null) {
                            m.items.put(Integer.parseInt(slot), it);
                        }
                    }
                }
                machines.put(key, m);
            } catch (NumberFormatException ignored) {
                // entrada corrupta: se omite
            }
        }
    }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        int i = 0;
        for (Machine m : machines.values()) {
            String base = "machines." + (i++);
            y.set(base + ".key", m.key);
            y.set(base + ".type", m.type);
            y.set(base + ".value", m.value);
            y.set(base + ".last", m.last);
            for (Map.Entry<Integer, ItemStack> e : m.items.entrySet()) {
                y.set(base + ".items." + e.getKey(), e.getValue());
            }
        }
        Saver.save(plugin, y, file);
        dirty = false;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 100L, 100L);
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
        }
        save();
    }

    // ------------------------------------------------------------------ utilidades

    private static String keyOf(Block b) {
        return b.getWorld().getName() + ";" + b.getX() + ";" + b.getY() + ";" + b.getZ();
    }

    private static Material materialOf(String type) {
        return switch (type) {
            case "sprinkler" -> Material.END_ROD;
            case "scarecrow" -> Material.CARVED_PUMPKIN;
            case "hive" -> Material.BEEHIVE;
            case "fridge" -> Material.BLAST_FURNACE;
            default -> Material.TINTED_GLASS;
        };
    }

    private Machine at(Block b) {
        Machine m = machines.get(keyOf(b));
        if (m == null) {
            return null;
        }
        if (b.getType() != materialOf(m.type)) {
            machines.remove(m.key);
            dirty = true;
            return null;
        }
        return m;
    }

    private Block blockOf(Machine m) {
        org.bukkit.World w = Bukkit.getWorld(m.world);
        return w == null ? null : w.getBlockAt(m.x, m.y, m.z);
    }

    /** ¿Hay un espantapájaros cerca de la ubicación? */
    public boolean scarecrowNear(org.bukkit.Location loc) {
        double r = plugin.getConfig().getDouble("crops.scarecrow-radius", 8);
        double r2 = r * r;
        String w = loc.getWorld().getName();
        for (Machine m : machines.values()) {
            if (!m.type.equals("scarecrow") || !m.world.equals(w)) continue;
            double dx = m.x + 0.5 - loc.getX(), dy = m.y + 0.5 - loc.getY(), dz = m.z + 0.5 - loc.getZ();
            if (dx * dx + dy * dy + dz * dz <= r2) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ simulación

    private void tick() {
        FileConfiguration cfg = plugin.getConfig();
        long now = System.currentTimeMillis();
        long interval = cfg.getLong("crops.sprinkler-interval", 90) * 1000L;
        int radius = cfg.getInt("crops.sprinkler-radius", 4);
        double hours = Math.max(0.05, cfg.getDouble("machines.hive-hours", 0.5));
        Season season = plugin.seasons().current();

        for (Machine m : machines.values()) {
            Block b = blockOf(m);
            if (b == null || !b.getWorld().isChunkLoaded(m.x >> 4, m.z >> 4)) {
                continue;
            }
            if (m.type.equals("sprinkler")) {
                if (now - m.last < interval) continue;
                m.last = now;
                if (m.value < 1) continue;
                boolean used = false;
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        for (int dy = -2; dy <= 1; dy++) {
                            Block f = b.getRelative(dx, dy, dz);
                            if (f.getType() != Material.FARMLAND) continue;
                            SoilData s = plugin.crops().soilAt(f);
                            if (s.water < 80) {
                                s.water = 100;
                                used = true;
                                f.getWorld().spawnParticle(Particle.SPLASH, f.getLocation().add(0.5, 1.1, 0.5),
                                        5, 0.3, 0.1, 0.3, 0.01);
                            }
                        }
                    }
                }
                if (used) {
                    m.value -= 1;
                    dirty = true;
                }
            } else if (m.type.equals("hive")) {
                long elapsed = Math.min(30 * 60000L, Math.max(0, now - m.last));
                m.last = now;
                if (m.lastScan == 0 || now - m.lastScan > 300000) {
                    m.flowers = scanFlowers(b);
                    m.lastScan = now;
                }
                double seasonF = switch (season) {
                    case SPRING -> 1.3;
                    case SUMMER -> 1.2;
                    case AUTUMN -> 0.8;
                    case WINTER -> 0.2;
                };
                double flowerF = Math.min(1.7, 0.2 + 0.25 * m.flowers);
                double temp = plugin.temperature().getTemperature(b.getLocation());
                double tempF = (temp < 5 || temp > 38) ? 0.3 : 1.0;
                double rainF = b.getWorld().hasStorm() ? 0.6 : 1.0;
                m.value = Math.min(100, m.value + 100.0 * elapsed / (hours * 3600000.0) * seasonF * flowerF * tempF * rainF);
                if (m.value >= 25) {
                    b.getWorld().spawnParticle(Particle.FALLING_HONEY, b.getLocation().add(0.5, 0.2, 0.5), 1, 0.2, 0, 0.2, 0);
                }
                dirty = true;
            }
        }
        if (dirty) {
            save();
        }
    }

    private int scanFlowers(Block b) {
        Set<Material> found = new HashSet<>();
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                if (!b.getWorld().isChunkLoaded((b.getX() + dx) >> 4, (b.getZ() + dz) >> 4)) continue;
                for (int dy = -2; dy <= 3; dy++) {
                    Material t = b.getRelative(dx, dy, dz).getType();
                    if (Tag.FLOWERS.isTagged(t)) {
                        found.add(t);
                    }
                }
            }
        }
        return Math.min(8, found.size());
    }

    // ------------------------------------------------------------------ colocar y romper

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        String id = CustomItems.id(e.getItemInHand());
        if (id == null || !TYPES.contains(id)) {
            return;
        }
        Block b = e.getBlockPlaced();
        Machine m = new Machine(keyOf(b), id, b.getWorld().getName(), b.getX(), b.getY(), b.getZ());
        m.last = System.currentTimeMillis();
        machines.put(m.key, m);
        save();
        e.getPlayer().sendMessage(plugin.lang().prefixed("machine." + id + ".placed"));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        Machine m = at(b);
        if (m == null) {
            return;
        }
        e.setDropItems(false);
        machines.remove(m.key);
        save();
        if (e.getPlayer().getGameMode() == GameMode.CREATIVE) {
            return;
        }
        org.bukkit.Location loc = b.getLocation().add(0.5, 0.5, 0.5);
        b.getWorld().dropItemNaturally(loc, CustomItems.create(m.type, 1));
        for (ItemStack it : m.items.values()) {
            b.getWorld().dropItemNaturally(loc, it);
        }
    }

    // ------------------------------------------------------------------ interacción

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || e.getAction() != Action.RIGHT_CLICK_BLOCK
                || e.getClickedBlock() == null) {
            return;
        }
        Machine m = at(e.getClickedBlock());
        if (m == null) {
            return;
        }
        Player p = e.getPlayer();
        ItemStack item = e.getItem();
        if (p.isSneaking() && item != null && !item.getType().isAir()) {
            return;
        }
        e.setCancelled(true);
        switch (m.type) {
            case "sprinkler" -> {
                if (item != null && item.getType() == Material.WATER_BUCKET) {
                    if (m.value + 8 > 32) {
                        p.sendMessage(plugin.lang().prefixed("machine.sprinkler.full"));
                    } else {
                        m.value += 8;
                        p.getInventory().setItemInMainHand(new ItemStack(Material.BUCKET));
                        p.playSound(p.getLocation(), Sound.ITEM_BUCKET_EMPTY, 0.8f, 1f);
                        save();
                    }
                }
                Util.actionBar(p, plugin.lang().get("machine.sprinkler.info", "charges", Util.fmt0(m.value), "max", 32));
            }
            case "scarecrow" -> Util.actionBar(p, plugin.lang().get("machine.scarecrow.info",
                    "radius", plugin.getConfig().getInt("crops.scarecrow-radius", 8)));
            case "hive" -> {
                if (item != null && item.getType() == Material.GLASS_BOTTLE && CustomItems.id(item) == null) {
                    harvestHoney(p, m);
                } else {
                    Util.actionBar(p, plugin.lang().get("machine.hive.info", "pct", Util.fmt0(m.value),
                            "flowers", (int) m.flowers));
                }
            }
            default -> openStorage(p, m);
        }
    }

    private void harvestHoney(Player p, Machine m) {
        if (m.value < 25) {
            Util.actionBar(p, plugin.lang().get("machine.hive.notready", "pct", Util.fmt0(m.value)));
            return;
        }
        Season s = plugin.seasons().current();
        boolean goodSeason = s == Season.SPRING || s == Season.SUMMER;
        int q = Util.clamp((int) Math.round(2 + m.flowers * 1.1 + (goodSeason ? 1 : 0) + (m.value >= 90 ? 1 : 0)), 1, 10);
        Food f = plugin.foods().get(q >= 8 ? "royal_honey" : "honey");
        if (f == null) {
            return;
        }
        Util.takeOne(p);
        m.value -= 25;
        Util.giveOrDrop(p, plugin.food().create(f, q, 1, System.currentTimeMillis(),
                plugin.food().spoilMultiplier(p.getUniqueId())));
        p.playSound(p.getLocation(), Sound.ITEM_BOTTLE_FILL, 1f, 1f);
        plugin.farmers().stat(p, "honey", 1);
        plugin.farmers().addXp(p, plugin.getConfig().getDouble("farmer.xp.produce", 5));
        p.sendMessage(plugin.lang().prefixed("machine.hive.honey", "food", f.name(), "q", q));
        save();
    }

    // ------------------------------------------------------------------ nevera y acuario

    private boolean accepts(String type, ItemStack it) {
        String fid = plugin.food().foodId(it);
        if (type.equals("aquarium")) {
            Food f = fid == null ? null : plugin.foods().get(fid);
            return f != null && f.group().equalsIgnoreCase("fish");
        }
        return fid != null || Products.isProduct(it);
    }

    private void openStorage(Player p, Machine m) {
        long now = System.currentTimeMillis();
        double factor = m.type.equals("fridge")
                ? Util.clamp(plugin.getConfig().getDouble("machines.fridge-spoil-factor", 0.1), 0, 1) : 0.0;
        long shift = (long) (Math.max(0, now - m.last) * (1 - factor));
        StorageHolder h = new StorageHolder(m.key, m.type);
        String title = plugin.lang().get(m.type.equals("fridge") ? "machine.fridge.title" : "machine.aquarium.title");
        Inventory inv = Bukkit.createInventory(h, 27, title);
        h.inv = inv;
        for (Map.Entry<Integer, ItemStack> en : m.items.entrySet()) {
            ItemStack it = en.getValue().clone();
            if (shift > 0) {
                plugin.food().shift(it, shift);
            }
            inv.setItem(en.getKey(), plugin.food().refresh(it));
        }
        m.last = now;
        p.openInventory(inv);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof StorageHolder h)) {
            return;
        }
        Machine m = machines.get(h.key);
        if (m == null) {
            return;
        }
        m.items.clear();
        for (int i = 0; i < e.getInventory().getSize(); i++) {
            ItemStack it = e.getInventory().getItem(i);
            if (it != null && !it.getType().isAir()) {
                m.items.put(i, it.clone());
            }
        }
        m.last = System.currentTimeMillis();
        save();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof StorageHolder h)) {
            return;
        }
        ItemStack incoming = null;
        if (e.getClickedInventory() == top) {
            if (e.getAction() == InventoryAction.HOTBAR_SWAP) {
                int btn = e.getHotbarButton();
                incoming = btn >= 0 ? e.getWhoClicked().getInventory().getItem(btn)
                        : e.getWhoClicked().getInventory().getItemInOffHand();
            } else {
                incoming = e.getCursor();
            }
        } else if (e.isShiftClick()) {
            incoming = e.getCurrentItem();
        }
        if (incoming != null && !incoming.getType().isAir() && !accepts(h.type, incoming)) {
            e.setCancelled(true);
            e.getWhoClicked().sendMessage(plugin.lang().prefixed(h.type.equals("fridge")
                    ? "machine.fridge.reject" : "machine.aquarium.reject"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof StorageHolder h)) {
            return;
        }
        boolean touchesTop = false;
        for (int slot : e.getRawSlots()) {
            if (slot < top.getSize()) {
                touchesTop = true;
                break;
            }
        }
        ItemStack cur = e.getOldCursor();
        if (touchesTop && cur != null && !cur.getType().isAir() && !accepts(h.type, cur)) {
            e.setCancelled(true);
        }
    }

    /** Texto con el número de máquinas por tipo (para /rancho debug). */
    public String summary() {
        Map<String, Integer> c = new HashMap<>();
        for (Machine m : machines.values()) {
            c.merge(m.type, 1, Integer::sum);
        }
        return Text.color("&7" + c);
    }
}
