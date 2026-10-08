package dev.rancho.crop;

import dev.rancho.RanchoPlugin;
import dev.rancho.farmer.Skill;
import dev.rancho.item.CustomItems;
import dev.rancho.quality.Products;
import dev.rancho.season.SeasonEvent;
import dev.rancho.util.Saver;
import dev.rancho.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Sistema de cultivo RPG: crecimiento por requisitos, tierra, riego, fertilizante e invernaderos. */
public final class CropManager {

    /** Resultado de evaluar los requisitos de un cultivo. */
    public record Eval(boolean greenhouse, boolean season, double temp, double tempScore, boolean light,
                       boolean water, boolean fertilizer, boolean improved) {
        public boolean allOk() {
            return season && tempScore > 0 && light && water && fertilizer && improved;
        }
    }

    /** Bloque que anula clima y estación (Invernadero o Maceta). */
    public record Controller(String world, int x, int y, int z, String type) {}

    /** Días de Minecraft por pasada de simulación (5 s). */
    private static final double DT = 5.0 / 1200.0;

    private final RanchoPlugin plugin;
    private final File file;
    private final Map<String, CropData> crops = new HashMap<>();
    private final Map<String, SoilData> soils = new HashMap<>();
    private final Map<String, Controller> controllers = new HashMap<>();
    private boolean dirty;
    private BukkitTask tickTask;
    private BukkitTask saveTask;

    public CropManager(RanchoPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data/crops.yml");
        load();
    }

    // ------------------------------------------------------------------ persistencia

    private void load() {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        for (String line : y.getStringList("crops")) {
            String[] p = line.split("\\|", -1);
            if (p.length < 8) continue;
            try {
                CropData c = new CropData(p[0], p[1]);
                c.progress = Double.parseDouble(p[2]);
                c.qualitySum = Double.parseDouble(p[3]);
                c.samples = Integer.parseInt(p[4]);
                c.wither = Double.parseDouble(p[5]);
                c.withered = p[6].equals("1");
                c.owner = p[7];
                crops.put(c.key, c);
            } catch (NumberFormatException ignored) {
                // línea corrupta: se omite
            }
        }
        for (String line : y.getStringList("soils")) {
            String[] p = line.split("\\|", -1);
            if (p.length < 4) continue;
            try {
                SoilData s = new SoilData();
                s.water = Double.parseDouble(p[1]);
                s.fertility = Double.parseDouble(p[2]);
                s.improved = p[3].equals("1");
                soils.put(p[0], s);
            } catch (NumberFormatException ignored) {
                // línea corrupta: se omite
            }
        }
        for (String line : y.getStringList("controllers")) {
            String[] p = line.split("\\|", -1);
            if (p.length < 2) continue;
            String[] k = p[0].split(";");
            if (k.length < 4) continue;
            try {
                controllers.put(p[0], new Controller(k[0], Integer.parseInt(k[1]), Integer.parseInt(k[2]),
                        Integer.parseInt(k[3]), p[1]));
            } catch (NumberFormatException ignored) {
                // línea corrupta: se omite
            }
        }
    }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        List<String> cl = new ArrayList<>();
        for (CropData c : crops.values()) {
            cl.add(c.key + "|" + c.cropId + "|" + c.progress + "|" + c.qualitySum + "|" + c.samples + "|"
                    + c.wither + "|" + (c.withered ? "1" : "0") + "|" + c.owner);
        }
        List<String> sl = new ArrayList<>();
        for (Map.Entry<String, SoilData> e : soils.entrySet()) {
            SoilData s = e.getValue();
            sl.add(e.getKey() + "|" + s.water + "|" + s.fertility + "|" + (s.improved ? "1" : "0"));
        }
        List<String> kl = new ArrayList<>();
        for (Map.Entry<String, Controller> e : controllers.entrySet()) {
            kl.add(e.getKey() + "|" + e.getValue().type());
        }
        y.set("crops", cl);
        y.set("soils", sl);
        y.set("controllers", kl);
        Saver.save(plugin, y, file);
        dirty = false;
    }

    public void start() {
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 100L, 100L);
        saveTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (dirty) {
                save();
            }
        }, 6000L, 6000L);
    }

    public void shutdown() {
        if (tickTask != null) tickTask.cancel();
        if (saveTask != null) saveTask.cancel();
        save();
    }

    // ------------------------------------------------------------------ acceso

    public CropData at(Block b) {
        return crops.get(CropData.key(b));
    }

    public SoilData soilAt(Block farmland) {
        String key = CropData.key(farmland);
        SoilData s = soils.get(key);
        if (s == null) {
            s = new SoilData();
            soils.put(key, s);
            dirty = true;
        }
        return s;
    }

    public SoilData soilIfAny(Block farmland) {
        return soils.get(CropData.key(farmland));
    }

    public void remove(Block b) {
        if (crops.remove(CropData.key(b)) != null) {
            dirty = true;
        }
    }

    public int count() {
        return crops.size();
    }

    // ------------------------------------------------------------------ controladores (invernadero / maceta)

    public void addController(Block b, String type) {
        controllers.put(CropData.key(b), new Controller(b.getWorld().getName(), b.getX(), b.getY(), b.getZ(), type));
        dirty = true;
    }

    public String controllerAt(Block b) {
        Controller c = controllers.get(CropData.key(b));
        return c == null ? null : c.type();
    }

    public void removeController(Block b) {
        if (controllers.remove(CropData.key(b)) != null) {
            dirty = true;
        }
    }

    /** True si el bloque está dentro del rango de un invernadero o maceta. */
    public boolean inGreenhouse(Block b) {
        FileConfiguration cfg = plugin.getConfig();
        String w = b.getWorld().getName();
        for (Controller c : controllers.values()) {
            if (!c.world().equals(w)) continue;
            int r = c.type().equals("greenhouse") ? cfg.getInt("crops.greenhouse-radius", 6)
                    : cfg.getInt("crops.pot-radius", 2);
            if (Math.abs(c.x() - b.getX()) <= r && Math.abs(c.z() - b.getZ()) <= r
                    && Math.abs(c.y() - b.getY()) <= 4) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ evaluación

    public Eval evaluate(CropData c, CropDef def, Block b, SoilData soil) {
        boolean gh = inGreenhouse(b);
        boolean season = gh || def.growsIn(plugin.seasons().current());
        double temp = gh ? (def.tempMin() + def.tempMax()) / 2.0 : plugin.temperature().getTemperature(b.getLocation());
        double tempScore = 1.0;
        if (temp < def.tempMin()) {
            tempScore = Math.max(0, 1 - (def.tempMin() - temp) / 8.0);
        } else if (temp > def.tempMax()) {
            tempScore = Math.max(0, 1 - (temp - def.tempMax()) / 8.0);
        }
        boolean light = gh || (b.getLightLevel() >= def.lightMin() && (!def.needsSun() || b.getLightFromSky() >= 14));
        boolean water = !def.needsWater() || soil.water >= 20;
        boolean fert = !def.needsFertilizer() || soil.fertility > 0;
        boolean improved = !def.improvedSoil() || soil.improved;
        return new Eval(gh, season, temp, tempScore, light, water, fert, improved);
    }

    // ------------------------------------------------------------------ simulación

    private void tick() {
        double dt = DT * plugin.getConfig().getDouble("crops.time-scale", 1.0);
        for (CropData c : new ArrayList<>(crops.values())) {
            World w = c.world();
            if (w == null || !w.isChunkLoaded(c.x() >> 4, c.z() >> 4)) {
                continue;
            }
            try {
                step(c, dt);
            } catch (RuntimeException ex) {
                plugin.getLogger().warning("Error procesando un cultivo: " + ex);
            }
        }
    }

    private void step(CropData c, double dt) {
        CropDef def = plugin.cropDefs().get(c.cropId);
        Block b = c.block();
        if (def == null || b == null || b.getType() != def.block()) {
            crops.remove(c.key);
            dirty = true;
            return;
        }
        Block soilBlock = b.getRelative(BlockFace.DOWN);
        if (soilBlock.getType() != Material.FARMLAND) {
            crops.remove(c.key);
            dirty = true;
            return;
        }
        FileConfiguration cfg = plugin.getConfig();
        SoilData soil = soilAt(soilBlock);

        // ---- agua de la tierra
        boolean drought = plugin.seasons().eventActive(SeasonEvent.DROUGHT);
        if (soilBlock.getBlockData() instanceof org.bukkit.block.data.type.Farmland fl
                && fl.getMoisture() >= fl.getMaximumMoisture()) {
            soil.water = 100;
        } else if (b.getWorld().hasStorm() && b.getLightFromSky() >= 14 && !drought) {
            soil.water = 100;
        } else {
            soil.water = Math.max(0, soil.water - cfg.getDouble("crops.water-decay-per-day", 60)
                    * (drought ? 3.0 : 1.0) * dt);
        }

        Eval ev = evaluate(c, def, b, soil);

        // ---- marchitez
        boolean stress = !ev.season() || ev.tempScore() <= 0 || !ev.water();
        if (stress) {
            c.wither += dt / Math.max(0.1, cfg.getDouble("crops.wither-days", 1.0));
        } else {
            c.wither = Math.max(0, c.wither - dt * 2);
        }
        if (c.wither >= 1.0 && !c.withered) {
            c.withered = true;
            setAge(b, 0);
            b.getWorld().spawnParticle(Particle.SMOKE, b.getLocation().add(0.5, 0.5, 0.5), 8, 0.2, 0.2, 0.2, 0.01);
        }
        dirty = true;
        if (c.withered || c.progress >= 1.0) {
            return;
        }

        // ---- crecimiento
        if (ev.allOk()) {
            double owner = 0;
            if (!c.owner.isEmpty()) {
                try {
                    owner = plugin.farmers().skill(java.util.UUID.fromString(c.owner), Skill.GREEN_THUMB) * 0.05;
                } catch (IllegalArgumentException ignored) {
                    owner = 0;
                }
            }
            double mult = cfg.getDouble("crops.growth-multiplier", 1.0) * (1 + owner)
                    * (plugin.seasons().eventActive(SeasonEvent.HARVEST) ? 1.2 : 1.0);
            double inc = dt / def.growthDays() * mult * (0.5 + 0.5 * ev.tempScore());
            c.progress = Math.min(1.0, c.progress + inc);
            if (def.needsFertilizer()) {
                soil.fertility = Math.max(0, soil.fertility - inc * 60.0);
            }
            double score = 0.5 * ev.tempScore() + 0.25 * (soil.water / 100.0)
                    + 0.15 * (soil.fertility > 0 ? 1.0 : 0.4) + 0.10 * (soil.improved ? 1.0 : 0.5);
            c.qualitySum += score;
            c.samples++;
            setAge(b, c.progress);
        }
    }

    private void setAge(Block b, double progress) {
        if (b.getBlockData() instanceof Ageable a) {
            int age = progress >= 1.0 ? a.getMaximumAge() : (int) Math.floor(progress * a.getMaximumAge());
            a.setAge(Math.max(0, Math.min(a.getMaximumAge(), age)));
            b.setBlockData(a, false);
        }
    }

    /** Minutos reales que faltan para madurar (con las condiciones actuales), o -1 si está detenido. */
    public double etaMinutes(CropData c, CropDef def, Eval ev) {
        if (c.progress >= 1.0) return 0;
        if (c.withered || !ev.allOk()) return -1;
        double perPass = DT / def.growthDays() * plugin.getConfig().getDouble("crops.growth-multiplier", 1.0)
                * (0.5 + 0.5 * ev.tempScore());
        if (perPass <= 0) return -1;
        return (1.0 - c.progress) / perPass * 5.0 / 60.0;
    }

    // ------------------------------------------------------------------ plantar y cosechar

    /** Registra un cultivo vanilla ya existente (campo natural) con su progreso actual. */
    public CropData registerExisting(Block b) {
        CropDef def = plugin.cropDefs().forVanillaBlock(b.getType());
        if (def == null || !(b.getBlockData() instanceof Ageable a)) {
            return null;
        }
        CropData c = new CropData(CropData.key(b), def.id());
        c.progress = a.getMaximumAge() == 0 ? 0 : a.getAge() / (double) a.getMaximumAge();
        crops.put(c.key, c);
        dirty = true;
        return c;
    }

    public boolean plant(Player p, Block farmland, CropDef def) {
        Block above = farmland.getRelative(BlockFace.UP);
        if (!above.getType().isAir()) {
            return false;
        }
        SoilData soil = soilAt(farmland);
        if (def.improvedSoil() && !soil.improved) {
            p.sendMessage(plugin.lang().prefixed("crop.need-improved", "crop", def.name()));
            return false;
        }
        if (def.needsFertilizer() && soil.fertility <= 0) {
            p.sendMessage(plugin.lang().prefixed("crop.need-fertilizer", "crop", def.name()));
            return false;
        }
        above.setType(def.block(), false);
        setAge(above, 0);
        CropData c = new CropData(CropData.key(above), def.id());
        c.owner = p.getUniqueId().toString();
        crops.put(c.key, c);
        dirty = true;
        Util.takeOne(p);
        p.playSound(p.getLocation(), Sound.ITEM_CROP_PLANT, 1f, 1f);
        plugin.farmers().stat(p, "planted", 1);
        plugin.farmers().addXp(p, plugin.getConfig().getDouble("farmer.xp.plant", 1));
        return true;
    }

    /** Calcula los drops de una cosecha y elimina el cultivo del registro. */
    public List<ItemStack> harvest(Player p, CropData c) {
        List<ItemStack> drops = new ArrayList<>();
        CropDef def = plugin.cropDefs().get(c.cropId);
        crops.remove(c.key);
        dirty = true;
        if (def == null) {
            return drops;
        }
        if (c.mature()) {
            int q = Util.clamp((int) Math.round(1 + 9 * Math.pow(c.avgScore(), 1.5)), 1, 10);
            if (plugin.seasons().eventActive(SeasonEvent.HARVEST)) {
                q = Math.min(10, q + 1);
            }
            int amount = Util.rndInt(def.harvestMin(), def.harvestMax());
            drops.add(Products.create("crop", def.id(), def.productName(), def.productMaterial(), q,
                    def.price(), amount));
            drops.add(seed(def, 1 + (Util.chance(0.5) ? 1 : 0)));
            if (p != null) {
                plugin.farmers().stat(p, "harvest", amount);
                plugin.farmers().addXp(p, plugin.getConfig().getDouble("farmer.xp.harvest", 3) * amount);
            }
        } else if (c.withered) {
            if (Util.chance(0.3)) drops.add(seed(def, 1));
        } else if (Util.chance(0.5)) {
            drops.add(seed(def, 1));
        }
        return drops;
    }

    public ItemStack seed(CropDef def, int amount) {
        return def.custom() ? CustomItems.createSeed(def, amount) : new ItemStack(def.seedMaterial(), amount);
    }

    // ------------------------------------------------------------------ tierra

    public boolean water(Player p, Block farmland) {
        SoilData s = soilAt(farmland);
        s.water = 100;
        dirty = true;
        farmland.getWorld().spawnParticle(Particle.SPLASH, farmland.getLocation().add(0.5, 1.1, 0.5), 12, 0.3, 0.1, 0.3, 0.01);
        p.playSound(p.getLocation(), Sound.ITEM_BUCKET_EMPTY, 0.5f, 1.5f);
        plugin.farmers().stat(p, "watered", 1);
        return true;
    }

    public boolean fertilize(Player p, Block farmland, String itemId) {
        SoilData s = soilAt(farmland);
        if (s.fertility >= 100) {
            p.sendMessage(plugin.lang().prefixed("crop.soil-full"));
            return false;
        }
        double amount = switch (itemId) {
            case "fertilizer_advanced" -> 100;
            case "fertilizer_basic" -> 60;
            default -> 30;
        };
        s.fertility = Math.min(100, s.fertility + amount);
        dirty = true;
        Util.takeOne(p);
        farmland.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, farmland.getLocation().add(0.5, 1.1, 0.5), 8, 0.3, 0.1, 0.3, 0);
        p.playSound(p.getLocation(), Sound.ITEM_BONE_MEAL_USE, 1f, 1f);
        p.sendMessage(plugin.lang().prefixed("crop.fertilized", "fert", Util.fmt0(s.fertility)));
        return true;
    }

    public boolean improve(Player p, Block farmland) {
        SoilData s = soilAt(farmland);
        if (s.improved) {
            p.sendMessage(plugin.lang().prefixed("crop.already-improved"));
            return false;
        }
        s.improved = true;
        dirty = true;
        Util.takeOne(p);
        farmland.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, farmland.getLocation().add(0.5, 1.1, 0.5), 10, 0.3, 0.1, 0.3, 0);
        p.playSound(p.getLocation(), Sound.BLOCK_ROOTED_DIRT_PLACE, 1f, 1f);
        p.sendMessage(plugin.lang().prefixed("crop.improved"));
        return true;
    }
}
