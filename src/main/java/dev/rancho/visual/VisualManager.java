package dev.rancho.visual;

import dev.rancho.RanchoPlugin;
import dev.rancho.food.Food;
import dev.rancho.food.KitchenManager;
import dev.rancho.item.CustomItems;
import dev.rancho.util.Keys;
import dev.rancho.util.Saver;
import dev.rancho.util.Text;
import dev.rancho.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Modelos 3D de las mesas y máquinas (resource pack) mostrados con entidades ItemDisplay,
 * con animaciones (rueda del molino, aspersor, plato flotante, peces nadando) y hologramas de estado.
 */
public final class VisualManager implements Listener {

    /** Números de CustomModelData por defecto de los modelos (ver el resource pack). */
    public static final Map<String, Integer> DEFAULT_MODELS = new HashMap<>();

    static {
        DEFAULT_MODELS.put("kitchen", 7001);
        DEFAULT_MODELS.put("cask", 7002);
        DEFAULT_MODELS.put("mill", 7003);
        DEFAULT_MODELS.put("fridge", 7004);
        DEFAULT_MODELS.put("sprinkler", 7005);
        DEFAULT_MODELS.put("scarecrow", 7006);
        DEFAULT_MODELS.put("hive", 7007);
        DEFAULT_MODELS.put("aquarium", 7008);
        DEFAULT_MODELS.put("feeder", 7009);
        DEFAULT_MODELS.put("waterer", 7010);
        DEFAULT_MODELS.put("greenhouse", 7011);
        DEFAULT_MODELS.put("pot", 7012);
        DEFAULT_MODELS.put("mill_wheel", 7103);
        DEFAULT_MODELS.put("sprinkler_head", 7105);
    }

    private static final float S = 1.002f;

    private static final class Vis {
        ItemDisplay base;
        ItemDisplay part;
        boolean partCustom;
        String partModel = "";
        TextDisplay text;
        String lastText = "";
        int stage;
    }

    private final RanchoPlugin plugin;
    private final File file;
    private final Map<String, Vis> visuals = new HashMap<>();
    private final Map<String, Integer> facing = new HashMap<>();
    private BukkitTask task;
    private int ticks;
    private final Set<UUID> withPack = new HashSet<>();

    public VisualManager(RanchoPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data/visuals.yml");
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        for (String line : y.getStringList("facing")) {
            String[] p = line.split("\\|", -1);
            if (p.length >= 2) {
                try {
                    facing.put(p[0], Integer.parseInt(p[1]));
                } catch (NumberFormatException ignored) {
                    // entrada corrupta
                }
            }
        }
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 40L, 20L);
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
        }
        removeAll();
        saveFacing();
    }

    private void saveFacing() {
        YamlConfiguration y = new YamlConfiguration();
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, Integer> e : facing.entrySet()) {
            lines.add(e.getKey() + "|" + e.getValue());
        }
        y.set("facing", lines);
        Saver.save(plugin, y, file);
    }

    // ------------------------------------------------------------------ resource pack

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        sync(e.getPlayer());
        if (plugin.getConfig().getBoolean("visuals.send-on-join", true)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> sendPack(e.getPlayer()), 40L);
        }
    }

    /** ¿Ha cargado este jugador el resource pack? (para usar las interfaces personalizadas) */
    public boolean hasPack(UUID id) {
        return withPack.contains(id) || plugin.getConfig().getBoolean("visuals.gui-for-all", false)
                || plugin.farmers().data(id).modelsForced;
    }

    /** Muestra u oculta los modelos 3D de las mesas según si el jugador tiene el pack (los demás ven los bloques normales). */
    private void applyVisibility(org.bukkit.entity.Entity e) {
        if (e == null || !e.isValid()) {
            return;
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (hasPack(p.getUniqueId())) {
                p.showEntity(plugin, e);
            } else {
                p.hideEntity(plugin, e);
            }
        }
    }

    /** Reaplica la visibilidad de todos los modelos para un jugador (al entrar, al cargar el pack o con /rancho modelos). */
    public void sync(Player p) {
        boolean pack = hasPack(p.getUniqueId());
        for (Vis v : visuals.values()) {
            for (org.bukkit.entity.Entity ent : new org.bukkit.entity.Entity[]{v.base, v.partCustom ? v.part : null}) {
                if (ent == null || !ent.isValid()) {
                    continue;
                }
                if (pack) {
                    p.showEntity(plugin, ent);
                } else {
                    p.hideEntity(plugin, ent);
                }
            }
        }
    }

    @EventHandler
    public void onPackStatus(PlayerResourcePackStatusEvent e) {
        switch (e.getStatus()) {
            case SUCCESSFULLY_LOADED -> withPack.add(e.getPlayer().getUniqueId());
            case DECLINED, FAILED_DOWNLOAD, INVALID_URL, FAILED_RELOAD, DISCARDED ->
                    withPack.remove(e.getPlayer().getUniqueId());
            default -> { }
        }
        final Player pl = e.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> sync(pl));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        withPack.remove(e.getPlayer().getUniqueId());
    }

    /** Envía el resource pack configurado (visuals.resource-pack-url) al jugador. */
    public boolean sendPack(Player p) {
        FileConfiguration cfg = plugin.getConfig();
        String url = cfg.getString("visuals.resource-pack-url", "").trim();
        if (url.isEmpty() || !p.isOnline()) {
            return false;
        }
        byte[] hash = parseHex(cfg.getString("visuals.resource-pack-sha1", "").trim());
        String prompt = Text.color(cfg.getString("visuals.resource-pack-prompt", "&6RanchoRPG: pack con modelos 3D"));
        p.setResourcePack(url, hash, prompt, cfg.getBoolean("visuals.resource-pack-force", false));
        return true;
    }

    private static byte[] parseHex(String hex) {
        if (hex.length() != 40) {
            return null;
        }
        byte[] out = new byte[20];
        try {
            for (int i = 0; i < 20; i++) {
                out[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
            }
        } catch (NumberFormatException ex) {
            return null;
        }
        return out;
    }

    // ------------------------------------------------------------------ orientación

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        String id = CustomItems.id(e.getItemInHand());
        if (id == null || !CustomItems.isPlaceable(id)) {
            return;
        }
        float yaw = e.getPlayer().getLocation().getYaw() + 180f;
        int snapped = (Math.round(yaw / 90f) * 90) % 360;
        facing.put(key(e.getBlockPlaced()), snapped < 0 ? snapped + 360 : snapped);
        saveFacing();
        Bukkit.getScheduler().runTaskLater(plugin, this::tick, 3L); // el modelo aparece enseguida
    }

    // ------------------------------------------------------------------ utilidades

    private static String key(Block b) {
        return b.getWorld().getName() + ";" + b.getX() + ";" + b.getY() + ";" + b.getZ();
    }

    private static Block blockOf(String key) {
        String[] k = key.split(";");
        if (k.length < 4) {
            return null;
        }
        World w = Bukkit.getWorld(k[0]);
        if (w == null) {
            return null;
        }
        try {
            return w.getBlockAt(Integer.parseInt(k[1]), Integer.parseInt(k[2]), Integer.parseInt(k[3]));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private int modelNumber(String name) {
        return plugin.getConfig().getInt("visuals.models." + name, DEFAULT_MODELS.getOrDefault(name, 0));
    }

    private ItemStack modelItem(String name) {
        ItemStack it = new ItemStack(Material.PAPER);
        ItemMeta m = it.getItemMeta();
        if (m != null) {
            m.setCustomModelData(modelNumber(name));
            it.setItemMeta(m);
        }
        return it;
    }

    private void remove(Vis v) {
        if (v.base != null) v.base.remove();
        if (v.part != null) v.part.remove();
        if (v.text != null) v.text.remove();
    }

    private void removeAll() {
        for (Vis v : visuals.values()) {
            remove(v);
        }
        visuals.clear();
    }

    // ------------------------------------------------------------------ bucle principal (1 s)

    private void tick() {
        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.getBoolean("visuals.enabled", true)) {
            if (!visuals.isEmpty()) {
                removeAll();
            }
            return;
        }
        ticks++;
        Map<String, String> all = new HashMap<>();
        all.putAll(plugin.kitchen().typesSnapshot());
        all.putAll(plugin.machines().typesSnapshot());
        all.putAll(plugin.troughs().typesSnapshot());
        all.putAll(plugin.crops().controllerSnapshot());

        for (Iterator<Map.Entry<String, Vis>> it = visuals.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Vis> en = it.next();
            if (!all.containsKey(en.getKey())) {
                remove(en.getValue());
                it.remove();
            }
        }
        if (ticks % 60 == 0 && facing.keySet().removeIf(k -> !all.containsKey(k))) {
            saveFacing();
        }

        double range = cfg.getDouble("visuals.range", 48);
        double r2 = range * range;
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        for (Map.Entry<String, String> en : all.entrySet()) {
            String key = en.getKey();
            Block b = blockOf(key);
            if (b == null || !b.getWorld().isChunkLoaded(b.getX() >> 4, b.getZ() >> 4)) {
                drop(key);
                continue;
            }
            Location center = b.getLocation().add(0.5, 0.5, 0.5);
            boolean near = false;
            for (Player p : players) {
                if (p.getWorld().equals(b.getWorld()) && p.getLocation().distanceSquared(center) <= r2) {
                    near = true;
                    break;
                }
            }
            if (!near) {
                drop(key);
                continue;
            }
            Vis v = visuals.get(key);
            if (v == null || v.base == null || !v.base.isValid()) {
                v = spawn(key, en.getValue(), b);
                visuals.put(key, v);
            }
            try {
                animate(v, key, en.getValue(), b, players);
            } catch (RuntimeException ex) {
                plugin.getLogger().warning("Error animando un modelo 3D: " + ex);
            }
        }
    }

    private void drop(String key) {
        Vis v = visuals.remove(key);
        if (v != null) {
            remove(v);
        }
    }

    private Vis spawn(String key, String type, Block b) {
        Vis v = new Vis();
        FileConfiguration cfg = plugin.getConfig();
        Location loc = b.getLocation().add(0.5, 0.5, 0.5);
        int yaw = facing.getOrDefault(key, 0);
        loc.setYaw((float) (yaw + cfg.getDouble("visuals.yaw-offset", 0)));
        float view = (float) Math.max(0.2, cfg.getDouble("visuals.range", 48) / 64.0);
        v.base = b.getWorld().spawn(loc, ItemDisplay.class, id -> {
            id.setItemStack(modelItem(type));
            id.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            id.setPersistent(false);
            id.setViewRange(view);
            id.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(S, S, S),
                    new AxisAngle4f()));
            id.setBrightness(new Display.Brightness(12, 12));
            id.getPersistentDataContainer().set(Keys.HOLO, PersistentDataType.BYTE, (byte) 1);
        });
        applyVisibility(v.base);
        return v;
    }

    // ------------------------------------------------------------------ animaciones

    private ItemDisplay spawnPart(Block b, ItemStack item, double dy, float scale, int yaw) {
        float view = (float) Math.max(0.2, plugin.getConfig().getDouble("visuals.range", 48) / 64.0);
        Location loc = b.getLocation().add(0.5, 0.5 + dy, 0.5);
        loc.setYaw(yaw);
        return b.getWorld().spawn(loc, ItemDisplay.class, id -> {
            id.setItemStack(item);
            id.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            id.setPersistent(false);
            id.setViewRange(view);
            id.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(scale, scale, scale),
                    new AxisAngle4f()));
            id.setBrightness(new Display.Brightness(13, 13));
            id.getPersistentDataContainer().set(Keys.HOLO, PersistentDataType.BYTE, (byte) 1);
        });
    }

    private void ensurePart(Vis v, Block b, String key, String model, ItemStack item, double dy, float scale) {
        if (v.part != null && v.part.isValid() && v.partModel.equals(model)) {
            return;
        }
        if (v.part != null) {
            v.part.remove();
        }
        v.part = spawnPart(b, item, dy, scale, facing.getOrDefault(key, 0));
        v.partModel = model;
        v.partCustom = model.equals("mill_wheel") || model.equals("sprinkler_head");
        if (v.partCustom) {
            applyVisibility(v.part);
        }
    }

    private void clearPart(Vis v) {
        if (v.part != null) {
            v.part.remove();
            v.part = null;
            v.partModel = "";
            v.partCustom = false;
        }
    }

    private void spin(ItemDisplay d, int stage, float scale, Vector3f translation) {
        d.setTransformation(new Transformation(translation, new AxisAngle4f(stage * (float) (Math.PI / 2), 0f, 1f, 0f),
                new Vector3f(scale, scale, scale), new AxisAngle4f()));
        d.setInterpolationDelay(0);
        d.setInterpolationDuration(20);
    }

    private void setText(Vis v, Block b, double dy, String text) {
        if (text == null || text.isEmpty()) {
            if (v.text != null) {
                v.text.remove();
                v.text = null;
                v.lastText = "";
            }
            return;
        }
        if (v.text == null || !v.text.isValid()) {
            Location loc = b.getLocation().add(0.5, 0.5 + dy, 0.5);
            v.text = b.getWorld().spawn(loc, TextDisplay.class, t -> {
                t.setBillboard(Display.Billboard.CENTER);
                t.setPersistent(false);
                t.setSeeThrough(false);
                t.setShadowed(false);
                t.setBackgroundColor(Color.fromARGB(120, 0, 0, 0));
                t.setViewRange(0.3f);
                t.setAlignment(TextDisplay.TextAlignment.CENTER);
                t.setLineWidth(200);
                t.getPersistentDataContainer().set(Keys.HOLO, PersistentDataType.BYTE, (byte) 1);
            });
            v.lastText = "";
        }
        if (!text.equals(v.lastText)) {
            v.lastText = text;
            v.text.setText(Text.color(text));
        }
    }

    private boolean playerNear(Location loc, double r, List<Player> players) {
        double r2 = r * r;
        for (Player p : players) {
            if (p.getWorld().equals(loc.getWorld()) && p.getLocation().distanceSquared(loc) <= r2) {
                return true;
            }
        }
        return false;
    }

    private void animate(Vis v, String key, String type, Block b, List<Player> players) {
        Block above = b.getRelative(BlockFace.UP);
        int bl = Math.max(above.getLightFromBlocks(), 5);
        v.base.setBrightness(new Display.Brightness(bl, above.getLightFromSky()));
        v.stage++;
        Location center = b.getLocation().add(0.5, 0.5, 0.5);
        World w = b.getWorld();
        long now = System.currentTimeMillis();

        switch (type) {
            case "kitchen", "cask", "mill" -> {
                KitchenManager.Job job = plugin.kitchen().jobAt(key);
                Food f = job == null ? null : plugin.foods().get(job.foodId);
                boolean working = job != null && now < job.finishAt;
                boolean ready = job != null && now >= job.finishAt;

                if (type.equals("mill")) {
                    ensurePart(v, b, key, "mill_wheel", modelItem("mill_wheel"), 0, S);
                    if (working) {
                        spin(v.part, v.stage, S, new Vector3f());
                        w.spawnParticle(Particle.CLOUD, center.clone().add(0, 0.8, 0), 2, 0.3, 0.1, 0.3, 0.005);
                    }
                } else if (f != null) {
                    Material mat = f.material();
                    ensurePart(v, b, key, "dish", new ItemStack(mat), 1.35, 0.42f);
                    float bob = (v.stage % 2 == 0) ? 0.06f : -0.02f;
                    spin(v.part, v.stage, 0.42f, new Vector3f(0f, bob, 0f));
                } else {
                    clearPart(v);
                }

                if (working && type.equals("kitchen")) {
                    w.spawnParticle(Particle.FLAME, center.clone().add(0, 0.55, 0), 3, 0.25, 0.02, 0.25, 0.005);
                    w.spawnParticle(Particle.SMOKE, center.clone().add(0.2, 1.2, 0.2), 2, 0.1, 0.1, 0.1, 0.01);
                } else if (working && type.equals("cask")) {
                    w.spawnParticle(Particle.BUBBLE_POP, center.clone().add(0, 0.6, 0), 4, 0.3, 0.1, 0.3, 0.01);
                }
                if (ready && f != null && v.stage % 2 == 0) {
                    w.spawnParticle(Particle.HAPPY_VILLAGER, center.clone().add(0, 1.1, 0), 4, 0.3, 0.2, 0.3, 0);
                }

                double dy = switch (type) {
                    case "kitchen" -> 2.15;
                    case "mill" -> 1.95;
                    default -> 1.45;
                };
                if (f != null && playerNear(center, 14, players)) {
                    if (working) {
                        double total = Math.max(1000.0, f.cookSeconds() * 1000.0 * (1 + 0.15 * (job.batches - 1)));
                        double pct = Util.clamp((1.0 - (job.finishAt - now) / total) * 100.0, 0, 100);
                        setText(v, b, dy, "&6" + f.name() + " &7x" + job.amount + "\n" + Util.bar(pct, 12) + "\n&7"
                                + Math.max(1, (job.finishAt - now) / 1000) + " s");
                    } else {
                        String extra = "";
                        if (f.ageMinutes() > 0) {
                            long step = Math.max(1L, (long) (f.ageMinutes() * 60000.0));
                            long bonus = Math.min(f.ageMaxBonus(), (now - job.finishAt) / step);
                            extra = "\n&5Reposo: +" + bonus + " nivel(es)";
                        }
                        setText(v, b, dy, "&a¡" + f.name() + " listo!\n&7Click derecho para recoger" + extra);
                    }
                } else {
                    setText(v, b, dy, null);
                }
            }
            case "sprinkler" -> {
                double charges = plugin.machines().valueAt(key);
                ensurePart(v, b, key, "sprinkler_head", modelItem("sprinkler_head"), 0, S);
                boolean working = charges >= 1;
                if (working) {
                    spin(v.part, v.stage, S, new Vector3f());
                    for (int i = 0; i < 6; i++) {
                        double a = (v.stage * 0.9) + i * Math.PI / 3;
                        w.spawnParticle(Particle.SPLASH, center.clone().add(Math.cos(a) * 1.6, 1.3, Math.sin(a) * 1.6),
                                2, 0.05, 0.05, 0.05, 0);
                    }
                }
                setText(v, b, 1.75, playerNear(center, 8, players)
                        ? "&3Aspersor &f" + Util.fmt0(charges) + "&7/&f32" : null);
            }
            case "hive" -> {
                double pct = plugin.machines().valueAt(key);
                setText(v, b, 1.5, playerNear(center, 8, players)
                        ? "&eColmena &f" + Util.fmt0(pct) + "%\n" + Util.bar(pct, 10) + "\n&7Flores: &f"
                        + (int) plugin.machines().flowersAt(key) : null);
            }
            case "aquarium" -> {
                ItemStack fish = plugin.machines().firstItemAt(key);
                if (fish == null) {
                    clearPart(v);
                } else {
                    ensurePart(v, b, key, "fish:" + fish.getType(), new ItemStack(fish.getType()), 0, 0.32f);
                    int corner = (v.stage / 2) % 4;
                    float ox = (corner == 0 || corner == 3) ? 0.28f : -0.28f;
                    float oz = (corner < 2) ? 0.28f : -0.28f;
                    v.part.setTransformation(new Transformation(new Vector3f(ox, -0.05f, oz),
                            new AxisAngle4f((float) (corner * Math.PI / 2), 0f, 1f, 0f), new Vector3f(0.32f, 0.32f, 0.32f),
                            new AxisAngle4f()));
                    v.part.setInterpolationDelay(0);
                    v.part.setInterpolationDuration(40);
                    if (v.stage % 3 == 0) {
                        w.spawnParticle(Particle.BUBBLE, center.clone().add(0, 0.2, 0), 2, 0.25, 0.1, 0.25, 0.01);
                    }
                }
            }
            case "greenhouse" -> {
                if (v.stage % 3 == 0) {
                    w.spawnParticle(Particle.HAPPY_VILLAGER, center.clone().add(0, 1.2, 0), 2, 0.4, 0.3, 0.4, 0);
                }
            }
            default -> { }
        }
    }
}
