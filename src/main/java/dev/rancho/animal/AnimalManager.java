package dev.rancho.animal;

import dev.rancho.RanchoPlugin;
import dev.rancho.farmer.Skill;
import dev.rancho.item.CustomItems;
import dev.rancho.quality.Products;
import dev.rancho.season.Season;
import dev.rancho.season.SeasonEvent;
import dev.rancho.util.Keys;
import dev.rancho.util.Text;
import dev.rancho.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Núcleo del sistema de ganado: seguimiento de animales, necesidades, enfermedades, crecimiento,
 * producción, reproducción con genética y mutaciones, y hologramas informativos.
 */
public final class AnimalManager {

    /** Días de Minecraft que representa cada pasada de simulación (100 ticks = 5 s de 1200 s por día). */
    private static final double DT = 5.0 / 1200.0;

    private static final String[] NAMES = {"Margarita", "Lola", "Paca", "Manchas", "Rosita", "Canela", "Estrella",
            "Nube", "Bruno", "Toro", "Copito", "Pinta", "Luna", "Trueno", "Pepa", "Violeta", "Sol", "Mora", "Chispa", "Tula"};

    /** Entorno de un animal en un instante. */
    public record Env(int same, int manure, int sick, boolean sheltered, double temp, double coldStress,
                      double heatStress, double crowd, boolean water) {}

    private final RanchoPlugin plugin;
    private final Map<UUID, AnimalData> data = new HashMap<>();
    private BukkitTask mainTask;
    private BukkitTask holoTask;
    private BukkitTask horseTask;
    private int saveCounter;

    /** True mientras se genera una cría propia, para que el listener de spawn no la inicialice al azar. */
    public boolean spawningChild;

    public AnimalManager(RanchoPlugin plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------ ciclo de vida

    public void start() {
        for (World w : Bukkit.getWorlds()) {
            for (Entity e : w.getEntities()) {
                if (e instanceof LivingEntity le) {
                    track(le);
                } else if (e instanceof TextDisplay td && td.getPersistentDataContainer().has(Keys.HOLO)) {
                    td.remove();
                }
            }
        }
        mainTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickAll, 100L, 20L);
        holoTask = Bukkit.getScheduler().runTaskTimer(plugin, this::holoTick, 20L, 5L);
        horseTask = Bukkit.getScheduler().runTaskTimer(plugin, this::horseTick, 40L, 10L);
    }

    public void shutdown() {
        if (mainTask != null) mainTask.cancel();
        if (holoTask != null) holoTask.cancel();
        if (horseTask != null) horseTask.cancel();
        for (AnimalData d : new ArrayList<>(data.values())) {
            save(d);
            removeHolo(d);
        }
        data.clear();
    }

    public AnimalData get(Entity e) {
        return e == null ? null : data.get(e.getUniqueId());
    }

    public Collection<AnimalData> all() {
        return data.values();
    }

    /** Empieza a gestionar una entidad si su especie está registrada; carga o crea sus datos. */
    public AnimalData track(LivingEntity e) {
        AnimalData existing = data.get(e.getUniqueId());
        if (existing != null) {
            existing.entity = e;
            return existing;
        }
        if (!(e instanceof Animals)) {
            return null;
        }
        String sid = e.getPersistentDataContainer().get(Keys.SPECIES, PersistentDataType.STRING);
        Species sp = sid != null ? plugin.species().get(sid) : plugin.species().forEntity(e.getType());
        if (sp == null || !sp.enabled()) {
            return null;
        }
        String raw = e.getPersistentDataContainer().get(Keys.ANIMAL, PersistentDataType.STRING);
        AnimalData d = AnimalData.parse(raw);
        if (d == null) {
            d = newData(sp, e);
        }
        d.entity = e;
        d.lastTick = System.currentTimeMillis() - (long) (Util.rnd() * 5000);
        d.lastSave = System.currentTimeMillis();
        data.put(e.getUniqueId(), d);
        applyVisuals(d, sp);
        save(d);
        return d;
    }

    /** Guarda y deja de gestionar una entidad (al descargarse su chunk). */
    public void untrack(Entity e) {
        AnimalData d = data.remove(e.getUniqueId());
        if (d != null) {
            save(d);
            removeHolo(d);
        }
    }

    /** Genera un animal de una especie en una ubicación (comando /rancho spawn y menús). */
    public AnimalData spawn(Species sp, Location loc) {
        Entity ent;
        spawningChild = true;
        try {
            ent = loc.getWorld().spawnEntity(loc, sp.entity());
        } finally {
            spawningChild = false;
        }
        if (!(ent instanceof LivingEntity le)) {
            ent.remove();
            return null;
        }
        le.getPersistentDataContainer().set(Keys.SPECIES, PersistentDataType.STRING, sp.id());
        return track(le);
    }

    /** Genera un animal de una raza concreta (menú de razas). */
    public AnimalData spawn(Species sp, Location loc, String breedId) {
        AnimalData d = spawn(sp, loc);
        if (d != null && breedId != null) {
            d.breed = breedId;
            applyVisuals(d, sp);
            save(d);
        }
        return d;
    }

    public void forget(Entity e) {
        AnimalData d = data.remove(e.getUniqueId());
        if (d != null) {
            removeHolo(d);
        }
    }

    public void save(AnimalData d) {
        if (d.entity == null || !d.entity.isValid()) {
            return;
        }
        d.entity.getPersistentDataContainer().set(Keys.ANIMAL, PersistentDataType.STRING, d.serialize());
        d.entity.getPersistentDataContainer().set(Keys.SPECIES, PersistentDataType.STRING, d.speciesId);
    }

    private AnimalData newData(Species sp, LivingEntity e) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        AnimalData d = new AnimalData();
        d.speciesId = sp.id();
        d.breed = plugin.breeds().pick(sp.id());
        d.name = randomName();
        d.female = r.nextBoolean();
        boolean baby = e instanceof Ageable a && !a.isAdult();
        d.age = baby ? Util.rnd() * sp.babyDays() * 0.8
                : sp.adultDays() + Util.rnd() * (sp.oldDays() - sp.adultDays()) * 0.5;
        for (int i = 0; i < d.genes.length; i++) {
            d.genes[i] = Util.clamp((int) Math.round(50 + r.nextGaussian() * 13), 5, 95);
        }
        if (Util.chance(0.03)) {
            d.mutations.add(Mutations.random(Util.chance(0.6)).id());
        }
        d.hunger = 60 + Util.rnd() * 40;
        d.thirst = 60 + Util.rnd() * 40;
        d.hygiene = 70 + Util.rnd() * 30;
        d.happiness = 60 + Util.rnd() * 30;
        d.charge = Util.rnd() * 60;
        return d;
    }

    public static String randomName() {
        return NAMES[Util.rndInt(0, NAMES.length - 1)];
    }

    private Player ownerOf(AnimalData d) {
        if (d.owner == null || d.owner.isEmpty()) {
            return null;
        }
        try {
            return Bukkit.getPlayer(UUID.fromString(d.owner));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private int skill(AnimalData d, Skill s) {
        if (d.owner == null || d.owner.isEmpty()) {
            return 0;
        }
        try {
            return plugin.farmers().skill(UUID.fromString(d.owner), s);
        } catch (IllegalArgumentException ex) {
            return 0;
        }
    }

    // ------------------------------------------------------------------ visuales y atributos

    /** Aplica etapa de vida, tamaño y atributos a la entidad según sus datos. */
    public void applyVisuals(AnimalData d, Species sp) {
        LivingEntity e = d.entity;
        if (e == null || !e.isValid()) {
            return;
        }
        Stage st = d.stage(sp);
        d.lastStage = st;
        if (e instanceof Ageable a) {
            a.setAgeLock(true);
            if (st == Stage.BABY) {
                a.setBaby();
            } else {
                a.setAdult();
            }
        }
        applyAttributes(d, sp);
        if (e instanceof Sheep sh) {
            sh.setSheared(d.charge < 100);
        }
    }

    private double maxHealth(LivingEntity e) {
        AttributeInstance a = e.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        return a == null ? 20.0 : a.getValue();
    }

    private void setAttr(LivingEntity e, Attribute attr, double value) {
        AttributeInstance a = e.getAttribute(attr);
        if (a != null) {
            a.setBaseValue(value);
        }
    }

    private void applyAttributes(AnimalData d, Species sp) {
        LivingEntity e = d.entity;
        FileConfiguration cfg = plugin.getConfig();
        Stage st = d.stage(sp);
        double ratio = e.getHealth() / Math.max(1.0, maxHealth(e));
        double hp;
        if (e instanceof AbstractHorse) {
            double cap = cfg.getDouble("genetics.horse-cap-health", 60);
            hp = sp.baseHealth() + (cap - sp.baseHealth()) * d.eff(Gene.HEALTH) / 100.0;
        } else {
            hp = sp.baseHealth() * (0.75 + 0.75 * d.eff(Gene.HEALTH) / 100.0);
        }
        hp *= switch (st) {
            case BABY -> 0.5;
            case YOUNG -> 0.75;
            case ADULT -> 1.0;
            case OLD -> 0.85;
        };
        hp = Math.max(2.0, hp);
        setAttr(e, Attribute.GENERIC_MAX_HEALTH, hp);
        e.setHealth(Math.min(hp, Math.max(0.5, hp * ratio)));

        double stageScale = switch (st) {
            case BABY -> 1.0;
            case YOUNG -> 0.75;
            case ADULT -> 1.0;
            case OLD -> 0.95;
        };
        setAttr(e, Attribute.GENERIC_SCALE, stageScale * (0.85 + 0.3 * d.eff(Gene.SIZE) / 100.0));
        applySpeed(d);
        if (e instanceof AbstractHorse) {
            double capJ = cfg.getDouble("genetics.horse-cap-jump", 1.0);
            setAttr(e, Attribute.GENERIC_JUMP_STRENGTH, 0.4 + (capJ - 0.4) * d.eff(Gene.JUMP) / 100.0);
        }
    }

    private void applySpeed(AnimalData d) {
        LivingEntity e = d.entity;
        AttributeInstance a = e.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (a == null) {
            return;
        }
        double factor = d.tired ? 0.4 : 1.0;
        if (e instanceof AbstractHorse) {
            double cap = plugin.getConfig().getDouble("genetics.horse-cap-speed", 0.35);
            a.setBaseValue((0.1 + (cap - 0.1) * d.eff(Gene.SPEED) / 100.0) * factor);
        } else {
            a.setBaseValue(a.getDefaultValue() * (0.8 + 0.4 * d.eff(Gene.SPEED) / 100.0) * factor);
        }
    }

    // ------------------------------------------------------------------ entorno

    public boolean sheltered(Location loc) {
        World w = loc.getWorld();
        return w != null && w.getHighestBlockYAt(loc) > loc.getBlockY() + 1;
    }

    private boolean waterNear(Location loc) {
        World w = loc.getWorld();
        int bx = loc.getBlockX(), by = loc.getBlockY(), bz = loc.getBlockZ();
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = -1; y <= 1; y++) {
                    Material m = w.getBlockAt(bx + x, by + y, bz + z).getType();
                    if (m == Material.WATER || m == Material.WATER_CAULDRON) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public Env inspect(AnimalData d, Species sp) {
        LivingEntity e = d.entity;
        Location loc = e.getLocation();
        int same = 0, manure = 0, sick = 0;
        for (Entity n : e.getNearbyEntities(5, 3, 5)) {
            if (n instanceof Item it) {
                if (CustomItems.is(it.getItemStack(), "manure")) {
                    manure++;
                }
            } else if (n != e) {
                AnimalData o = data.get(n.getUniqueId());
                if (o != null && o.speciesId.equals(d.speciesId)) {
                    same++;
                    if (o.disease != Disease.NONE) {
                        sick++;
                    }
                }
            }
        }
        boolean sheltered = sheltered(loc);
        double temp = plugin.temperature().getTemperature(loc);
        if (sheltered) {
            temp = temp + (20.0 - temp) * 0.25;
        }
        double resist = (d.eff(Gene.RESISTANCE) - 50) / 10.0;
        double cold = sp.coldLimit() - resist;
        double heat = sp.heatLimit() + resist;
        double crowd = same > sp.maxDensity() ? (same - sp.maxDensity()) / (double) sp.maxDensity() : 0;
        return new Env(same, manure, sick, sheltered, temp, Math.max(0, cold - temp), Math.max(0, temp - heat),
                crowd, waterNear(loc) || plugin.troughs().waterNear(loc));
    }

    // ------------------------------------------------------------------ simulación

    /**
     * Procesa los animales por turnos (cada uno cada ~5 s) con un tope por pasada para evitar lag.
     * El tiempo simulado de cada animal es el tiempo real transcurrido desde su último turno.
     */
    private void tickAll() {
        long now = System.currentTimeMillis();
        FileConfiguration cfg = plugin.getConfig();
        int budget = Math.max(10, cfg.getInt("animals.max-per-pass", 80));
        double scale = cfg.getDouble("animals.time-scale", 1.0);
        List<AnimalData> due = new ArrayList<>();
        for (AnimalData d : data.values()) {
            if (now - d.lastTick >= 5000) {
                due.add(d);
            }
        }
        due.sort(Comparator.comparingLong(a -> a.lastTick));
        int done = 0;
        for (AnimalData d : due) {
            if (done++ >= budget) {
                break;
            }
            double days = Math.min(0.05, (now - d.lastTick) / 1000.0 / 1200.0) * scale;
            d.lastTick = now;
            try {
                tickAnimal(d, days);
                if (now - d.lastSave >= 30000) {
                    d.lastSave = now;
                    save(d);
                }
            } catch (RuntimeException ex) {
                plugin.getLogger().warning("Error procesando un animal: " + ex);
            }
        }
    }

    private void tickAnimal(AnimalData d, double dtDays) {
        LivingEntity e = d.entity;
        if (e == null || !e.isValid() || e.isDead()) {
            return;
        }
        Species sp = plugin.species().get(d.speciesId);
        if (sp == null || !sp.enabled()) {
            return;
        }
        FileConfiguration cfg = plugin.getConfig();
        double dt = dtDays;
        Season season = plugin.seasons().current();
        Player owner = ownerOf(d);
        double frugal = skill(d, Skill.FRUGAL) * 0.08;
        double healer = skill(d, Skill.HEALER) * 0.10;
        double prodSkill = skill(d, Skill.PRODUCTION) * 0.05;
        Stage before = d.stage(sp);
        Env env = inspect(d, sp);

        // ---- necesidades
        double thirstMul = plugin.seasons().eventActive(SeasonEvent.HEATWAVE) ? 1.5 : 1.0;
        d.hunger = Util.clamp(d.hunger - sp.hungerRate() * (1 - frugal) * dt, 0, 100);
        d.thirst = Util.clamp(d.thirst - sp.thirstRate() * (1 - frugal) * thirstMul * dt, 0, 100);
        d.hygiene = Util.clamp(d.hygiene - sp.hygieneRate() * dt - env.manure() * 40.0 * dt, 0, 100);
        if (env.water()) {
            d.thirst = Math.min(100, d.thirst + 35);
        }
        Block below = e.getLocation().clone().subtract(0, 0.2, 0).getBlock();
        if (sp.grazes() && below.getType() == Material.GRASS_BLOCK && d.hunger < 95 && env.crowd() == 0) {
            d.hunger = Math.min(100, d.hunger + 1.2);
            d.diet = d.diet * 0.98 + 45 * 0.02;
            if (Util.chance(cfg.getDouble("animals.overgraze-chance", 0.02))) {
                below.setType(Material.DIRT);
            }
        }
        if (d.hunger < 55 && plugin.troughs().takeServing(e.getLocation())) {
            d.hunger = Math.min(100, d.hunger + 35);
            d.diet = d.diet * 0.8 + 60 * 0.2;
        }
        if (env.sheltered() && e.getWorld().hasStorm()) {
            d.hygiene = Math.min(100, d.hygiene + 1);
        }

        // ---- salud
        double dh = 0;
        if (d.hunger <= 0) dh -= 2.0; else if (d.hunger < 15) dh -= 0.5;
        if (d.thirst <= 0) dh -= 3.0; else if (d.thirst < 15) dh -= 0.7;
        double climate = env.coldStress() + env.heatStress();
        if (climate > 0) dh -= Math.min(3.0, climate * 0.3);
        if (d.disease != Disease.NONE) dh -= 1.2;
        if (sp.suffersIn(season)) dh -= 0.3;
        if (env.crowd() > 0.5) dh -= 0.3 * env.crowd();
        if (dh == 0 && d.hunger > 50 && d.thirst > 50 && d.hygiene > 40) {
            dh += 0.8 * (1 + healer);
        }
        if (dh != 0) {
            adjustHealth(e, dh);
            if (e.isDead()) {
                return;
            }
        }

        // ---- cuidado y felicidad
        double careNow = (d.hunger + d.thirst + d.hygiene + d.happiness) / 400.0;
        careNow *= (1 - env.crowd() * 0.3);
        if (climate > 0 && !env.sheltered()) careNow *= 0.7;
        if (d.disease != Disease.NONE) careNow *= 0.6;
        d.care = d.care * 0.9 + Util.clamp(careNow, 0, 1) * 100 * 0.1;
        double target = (d.hunger + d.thirst + d.hygiene) / 3.0 - env.crowd() * 30 - climate * 3
                + (env.sheltered() ? 5 : 0) - (d.disease != Disease.NONE ? 20 : 0) + (d.diet - 50) * 0.2;
        d.happiness = Util.clamp(d.happiness + Util.clamp(target - d.happiness, -10, 10), 0, 100);

        // ---- enfermedad
        if (d.immunity > 0) {
            d.immunity -= dt;
        }
        if (d.disease == Disease.NONE && d.immunity <= 0) {
            double chance = 0.004 * (1 + (100 - d.hygiene) / 40.0 + env.crowd() * 2 + climate * 0.1
                    + (d.hasSpecial(Mutation.Special.SICKLY) ? 1.0 : 0.0));
            chance += 0.02 * env.sick();
            chance *= (1 - d.eff(Gene.RESISTANCE) / 150.0) * cfg.getDouble("animals.disease-chance", 1.0);
            if (Util.chance(chance)) {
                Disease[] pool = {Disease.PARASITES, Disease.FLU, Disease.INFECTION, Disease.MANGE, Disease.BLOAT};
                d.disease = pool[Util.rndInt(0, pool.length - 1)];
                if (owner != null && owner.getWorld().equals(e.getWorld())
                        && owner.getLocation().distanceSquared(e.getLocation()) < 64 * 64) {
                    owner.sendMessage(plugin.lang().prefixed("animal.sick", "name", d.name,
                            "disease", d.disease.label()));
                }
            }
        }

        // ---- crecimiento
        double rate = (0.3 + 0.7 * d.care / 100.0) * (d.hasSpecial(Mutation.Special.FAST_GROWTH) ? 1.5 : 1.0)
                * plugin.breeds().growthMult(d.breed)
                * (d.disease != Disease.NONE ? 0.5 : 1.0) * (d.hunger < 10 ? 0.3 : 1.0);
        d.age += dt * rate;
        Stage st = d.stage(sp);
        if (st != before) {
            applyVisuals(d, sp);
            if (owner != null) {
                owner.sendMessage(plugin.lang().prefixed("animal.grew", "name", d.name, "stage", st.label()));
            }
        }

        // ---- nivel
        int maxLevel = 3 + (int) Math.round(d.eff(Gene.QUALITY) / 100.0 * 7);
        if (d.care >= 60 && d.level < maxLevel && st != Stage.BABY) {
            d.xp += dt * (d.care / 100.0);
            if (d.xp >= cfg.getDouble("animals.level-days", 1.0)) {
                d.xp = 0;
                d.level++;
                applyAttributes(d, sp);
                if (owner != null) {
                    owner.sendMessage(plugin.lang().prefixed("animal.level", "name", d.name, "level", d.level));
                }
            }
        }

        // ---- producción
        if (!sp.produce().equals("NONE") && (st == Stage.ADULT || st == Stage.OLD) && sp.producesIn(season)) {
            d.charge = Math.min(100, d.charge + 100.0 * dt / sp.produceDays()
                    * (0.5 + d.eff(Gene.PRODUCTION) / 100.0) * (d.care / 100.0) * (1 + prodSkill)
                    * plugin.breeds().prodMult(d.breed));
        }
        if (sp.produce().equals("EGGS") && d.charge >= 100 && d.hunger > 20) {
            d.charge = 0;
            e.getWorld().dropItemNaturally(e.getLocation(), produceItem(d, sp, 1));
            e.getWorld().playSound(e.getLocation(), Sound.ENTITY_CHICKEN_EGG, 0.8f, 1f);
            if (owner != null) {
                plugin.farmers().stat(owner, "produced", 1);
            }
        }
        if (e instanceof Sheep sh) {
            sh.setSheared(d.charge < 100);
        }

        // ---- reproducción
        if (d.breedCooldown > 0) {
            d.breedCooldown -= dt;
        }
        if (d.pregnant && d.hunger > 20 && d.thirst > 20) {
            d.gestation -= dt * (0.5 + d.care / 200.0);
            if (d.gestation <= 0) {
                giveBirth(d, sp);
            }
        }

        // ---- estiércol
        if ((st == Stage.ADULT || st == Stage.OLD) && d.hunger > 40 && env.manure() < 8
                && Util.chance(dt / cfg.getDouble("animals.manure-days", 0.3))) {
            e.getWorld().dropItem(e.getLocation(), CustomItems.create("manure", 1));
        }

        // ---- recuperación de resistencia (caballos sin jinete)
        if (d.stamina < 100 && e.getPassengers().isEmpty()) {
            d.stamina = Math.min(100, d.stamina + 5);
        }

        // ---- muerte por vejez
        if (d.age >= sp.lifespan()) {
            if (owner != null) {
                owner.sendMessage(plugin.lang().prefixed("animal.old-death", "name", d.name));
            }
            e.setHealth(0);
        }
    }

    private void adjustHealth(LivingEntity e, double pct) {
        double max = maxHealth(e);
        double nh = e.getHealth() + max * pct / 100.0;
        if (nh <= 0) {
            e.setHealth(0);
        } else {
            e.setHealth(Math.min(max, nh));
        }
    }

    // ------------------------------------------------------------------ calidad y producción

    /** Nivel de calidad (1-10) de los productos del animal. */
    public int quality(AnimalData d, Species sp) {
        Stage st = d.stage(sp);
        double ageF = switch (st) {
            case ADULT -> 1.0;
            case OLD -> 0.6;
            case YOUNG -> 0.4;
            case BABY -> 0.2;
        };
        double q = 0.45 * d.level + 0.2 * (d.eff(Gene.QUALITY) / 10.0) + 0.2 * (d.diet / 10.0) + 0.15 * (d.care / 10.0);
        q *= (0.7 + 0.3 * ageF);
        return Util.clamp((int) Math.round(q), 1, 10);
    }

    public ItemStack produceItem(AnimalData d, Species sp, int amount) {
        return Products.create(sp.produceType(), sp.id(), sp.produceName(), sp.produceMaterial(),
                quality(d, sp), sp.producePrice(), amount);
    }

    /** Sustituye los drops vanilla por carne y cuero con nivel de calidad. */
    public void onDeath(LivingEntity e, List<ItemStack> drops) {
        AnimalData d = data.remove(e.getUniqueId());
        if (d == null) {
            return;
        }
        removeHolo(d);
        Species sp = plugin.species().get(d.speciesId);
        drops.clear();
        if (sp == null) {
            return;
        }
        Stage st = d.stage(sp);
        if (st == Stage.BABY) {
            return;
        }
        int q = quality(d, sp);
        double scale = (0.7 + 0.6 * d.eff(Gene.SIZE) / 100.0) * (st == Stage.YOUNG ? 0.5 : 1.0)
                * plugin.breeds().meatMult(d.breed);
        if (sp.meatMax() > 0) {
            int amt = Math.max(1, (int) Math.round(Util.rndInt(sp.meatMin(), sp.meatMax()) * scale));
            drops.add(Products.create("meat", sp.id(), sp.meatName(), sp.meatMaterial(), q, sp.meatPrice(), amt));
        }
        if (sp.leatherMax() > 0) {
            int amt = (int) Math.round(Util.rndInt(sp.leatherMin(), sp.leatherMax()) * scale);
            if (amt > 0) {
                drops.add(Products.create("leather", sp.id(), sp.leatherName(), Material.LEATHER, q,
                        sp.leatherPrice(), amt));
            }
        }
    }

    // ------------------------------------------------------------------ interacciones de jugadores

    /** Alimenta con pienso, forraje o comida de la especie. */
    public boolean feed(Player p, AnimalData d, Species sp, String token) {
        double hunger, happy, quality, health = 0;
        switch (token) {
            case "feed_basic" -> { hunger = 35; happy = 3; quality = 60; }
            case "feed_premium" -> { hunger = 60; happy = 8; quality = 100; health = 5; }
            case "forage" -> { hunger = 25; happy = 2; quality = 50; }
            default -> { hunger = 15; happy = 1; quality = 40; }
        }
        if (d.hunger >= 98) {
            p.sendMessage(plugin.lang().prefixed("animal.full", "name", d.name));
            return false;
        }
        d.hunger = Math.min(100, d.hunger + hunger);
        d.happiness = Math.min(100, d.happiness + happy);
        d.diet = d.diet * 0.75 + quality * 0.25;
        if (health > 0) {
            adjustHealth(d.entity, health);
        }
        d.owner = p.getUniqueId().toString();
        Util.takeOne(p);
        effects(d.entity, Particle.HAPPY_VILLAGER, Sound.ENTITY_GENERIC_EAT);
        p.sendMessage(plugin.lang().prefixed("animal.fed", "name", d.name));
        plugin.farmers().stat(p, "fed", 1);
        plugin.farmers().addXp(p, plugin.getConfig().getDouble("farmer.xp.feed", 2));
        save(d);
        return true;
    }

    public boolean salt(Player p, AnimalData d) {
        if (d.happiness >= 98 && d.disease == Disease.NONE && d.hunger >= 98) {
            p.sendMessage(plugin.lang().prefixed("animal.no-effect"));
            return false;
        }
        d.happiness = Math.min(100, d.happiness + 15);
        d.thirst = Math.max(0, d.thirst - 5);
        adjustHealth(d.entity, 5);
        d.owner = p.getUniqueId().toString();
        Util.takeOne(p);
        effects(d.entity, Particle.HAPPY_VILLAGER, Sound.ENTITY_GENERIC_EAT);
        p.sendMessage(plugin.lang().prefixed("animal.fed", "name", d.name));
        save(d);
        return true;
    }

    public boolean drink(Player p, AnimalData d) {
        if (d.thirst >= 98) {
            p.sendMessage(plugin.lang().prefixed("animal.full", "name", d.name));
            return false;
        }
        d.thirst = Math.min(100, d.thirst + 40);
        d.owner = p.getUniqueId().toString();
        effects(d.entity, Particle.SPLASH, Sound.ENTITY_GENERIC_DRINK);
        p.sendMessage(plugin.lang().prefixed("animal.drink", "name", d.name));
        return true;
    }

    public boolean brush(Player p, AnimalData d) {
        if (d.hygiene >= 98) {
            p.sendMessage(plugin.lang().prefixed("animal.no-effect"));
            return false;
        }
        d.hygiene = Math.min(100, d.hygiene + 40);
        d.happiness = Math.min(100, d.happiness + 5);
        d.owner = p.getUniqueId().toString();
        effects(d.entity, Particle.HAPPY_VILLAGER, Sound.ITEM_BRUSH_BRUSHING_GENERIC);
        p.sendMessage(plugin.lang().prefixed("animal.brushed", "name", d.name));
        plugin.farmers().stat(p, "brushed", 1);
        save(d);
        return true;
    }

    /** Aplica vacuna o medicina. Devuelve true si se consumió el item. */
    public boolean medicate(Player p, AnimalData d, String itemId) {
        if (itemId.equals("vaccine")) {
            d.immunity = plugin.getConfig().getDouble("animals.vaccine-days", 5);
            d.owner = p.getUniqueId().toString();
            Util.takeOne(p);
            effects(d.entity, Particle.HEART, Sound.ENTITY_PLAYER_LEVELUP);
            p.sendMessage(plugin.lang().prefixed("animal.vaccinated", "name", d.name));
            save(d);
            return true;
        }
        if (d.disease == Disease.NONE) {
            p.sendMessage(plugin.lang().prefixed("animal.not-sick", "name", d.name));
            return false;
        }
        if (!itemId.equals(d.disease.cure())) {
            p.sendMessage(plugin.lang().prefixed("animal.wrong-cure", "disease", d.disease.label()));
            return false;
        }
        d.disease = Disease.NONE;
        d.immunity = 1.0;
        adjustHealth(d.entity, 15 * (1 + skill(d, Skill.HEALER) * 0.1));
        d.owner = p.getUniqueId().toString();
        Util.takeOne(p);
        effects(d.entity, Particle.HEART, Sound.ENTITY_PLAYER_LEVELUP);
        p.sendMessage(plugin.lang().prefixed("animal.cured", "name", d.name));
        plugin.farmers().stat(p, "cured", 1);
        plugin.farmers().addXp(p, plugin.getConfig().getDouble("farmer.xp.cure", 15));
        save(d);
        return true;
    }

    /** Ordeña con un cubo vacío. */
    public boolean milk(Player p, AnimalData d, Species sp) {
        if (!sp.produce().equals("MILK")) {
            return false;
        }
        Stage st = d.stage(sp);
        if ((st != Stage.ADULT && st != Stage.OLD) || d.charge < 100) {
            p.sendMessage(plugin.lang().prefixed("animal.milk.wait", "name", d.name,
                    "pct", Util.fmt0(d.charge)));
            return false;
        }
        ItemStack milk = produceItem(d, sp, 1);
        d.charge = 0;
        d.owner = p.getUniqueId().toString();
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand.getAmount() <= 1) {
            p.getInventory().setItemInMainHand(milk);
        } else {
            hand.setAmount(hand.getAmount() - 1);
            Util.giveOrDrop(p, milk);
        }
        p.playSound(p.getLocation(), Sound.ENTITY_COW_MILK, 1f, 1f);
        p.sendMessage(plugin.lang().prefixed("animal.milk.ok", "name", d.name));
        plugin.farmers().stat(p, "produced", 1);
        plugin.farmers().addXp(p, plugin.getConfig().getDouble("farmer.xp.produce", 5));
        save(d);
        return true;
    }

    /** Esquila con tijeras. */
    public boolean shear(Player p, AnimalData d, Species sp) {
        if (!sp.produce().equals("WOOL")) {
            return false;
        }
        Stage st = d.stage(sp);
        if ((st != Stage.ADULT && st != Stage.OLD) || d.charge < 100) {
            p.sendMessage(plugin.lang().prefixed("animal.shear.wait", "name", d.name,
                    "pct", Util.fmt0(d.charge)));
            return false;
        }
        int amount = 1 + (d.eff(Gene.SIZE) > 60 ? 1 : 0) + (Util.chance(skill(d, Skill.PRODUCTION) * 0.05) ? 1 : 0);
        d.charge = 0;
        d.owner = p.getUniqueId().toString();
        d.entity.getWorld().dropItemNaturally(d.entity.getLocation(), produceItem(d, sp, amount));
        if (d.entity instanceof Sheep sh) {
            sh.setSheared(true);
        }
        ItemStack tool = p.getInventory().getItemInMainHand();
        if (tool.getItemMeta() instanceof Damageable dm && p.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            dm.setDamage(dm.getDamage() + 1);
            if (dm.getDamage() >= tool.getType().getMaxDurability()) {
                p.getInventory().setItemInMainHand(null);
                p.playSound(p.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
            } else {
                tool.setItemMeta(dm);
            }
        }
        p.playSound(p.getLocation(), Sound.ENTITY_SHEEP_SHEAR, 1f, 1f);
        p.sendMessage(plugin.lang().prefixed("animal.shear.ok", "name", d.name));
        plugin.farmers().stat(p, "produced", 1);
        plugin.farmers().addXp(p, plugin.getConfig().getDouble("farmer.xp.produce", 5));
        save(d);
        return true;
    }

    // ------------------------------------------------------------------ genética

    /** Suero de mutación: añade una mutación al azar. */
    public boolean serum(Player p, AnimalData d) {
        int max = plugin.getConfig().getInt("genetics.max-mutations", 5);
        if (d.mutations.size() >= max) {
            p.sendMessage(plugin.lang().prefixed("animal.mutation.max", "name", d.name));
            return false;
        }
        boolean positive = Util.chance(plugin.getConfig().getDouble("genetics.serum-positive", 0.65));
        Mutation m = Mutations.random(positive);
        int guard = 0;
        while (d.mutations.contains(m.id()) && guard++ < 20) {
            m = Mutations.random(positive);
        }
        if (d.mutations.contains(m.id())) {
            p.sendMessage(plugin.lang().prefixed("animal.no-effect"));
            return false;
        }
        d.mutations.add(m.id());
        d.owner = p.getUniqueId().toString();
        Util.takeOne(p);
        Species sp = plugin.species().get(d.speciesId);
        if (sp != null) {
            applyAttributes(d, sp);
        }
        effects(d.entity, positive ? Particle.HAPPY_VILLAGER : Particle.SMOKE, Sound.BLOCK_BREWING_STAND_BREW);
        p.sendMessage(plugin.lang().prefixed("animal.mutation.new", "name", d.name,
                "mutation", Text.color(m.display())));
        plugin.farmers().stat(p, "mutations", 1);
        save(d);
        return true;
    }

    /** Estabilizador: elimina una mutación negativa o protege la próxima cría de una hembra gestante. */
    public boolean stabilize(Player p, AnimalData d) {
        List<String> negatives = new ArrayList<>();
        for (String id : d.mutations) {
            Mutation m = Mutations.get(id);
            if (m != null && !m.positive()) {
                negatives.add(id);
            }
        }
        if (!negatives.isEmpty()) {
            String id = negatives.get(Util.rndInt(0, negatives.size() - 1));
            d.mutations.remove(id);
            Species sp = plugin.species().get(d.speciesId);
            if (sp != null) {
                applyAttributes(d, sp);
            }
            p.sendMessage(plugin.lang().prefixed("animal.stabilized.removed", "name", d.name,
                    "mutation", Text.color(Mutations.get(id).display())));
        } else if (d.female && d.pregnant && !d.stabilized) {
            d.stabilized = true;
            p.sendMessage(plugin.lang().prefixed("animal.stabilized.protected", "name", d.name));
        } else {
            p.sendMessage(plugin.lang().prefixed("animal.no-effect"));
            return false;
        }
        Util.takeOne(p);
        effects(d.entity, Particle.HAPPY_VILLAGER, Sound.BLOCK_BEACON_ACTIVATE);
        save(d);
        return true;
    }

    // ------------------------------------------------------------------ reproducción

    private String breedProblem(AnimalData o, Species sp, boolean female) {
        FileConfiguration cfg = plugin.getConfig();
        if (o.stage(sp) != Stage.ADULT) return "No es adulto";
        if (o.hasSpecial(Mutation.Special.INFERTILE)) return "Es infértil";
        if (o.breedCooldown > 0) return "Está en descanso reproductivo";
        if (o.happiness < cfg.getDouble("animals.breed-min-happiness", 70)) return "No es lo bastante feliz";
        if (o.hunger < 40 || o.thirst < 40) return "Tiene hambre o sed";
        if (female && o.pregnant) return "Ya está gestando";
        return null;
    }

    private AnimalData findMate(AnimalData d, Species sp, boolean wantFemale) {
        for (Entity n : d.entity.getNearbyEntities(8, 4, 8)) {
            AnimalData o = data.get(n.getUniqueId());
            if (o != null && o != d && o.speciesId.equals(d.speciesId) && o.female == wantFemale
                    && breedProblem(o, sp, wantFemale) == null) {
                return o;
            }
        }
        return null;
    }

    /** Inicia la gestación usando un Suplemento de Cría. */
    public boolean breed(Player p, AnimalData clicked, Species sp) {
        if (!sp.breedsIn(plugin.seasons().current())) {
            p.sendMessage(plugin.lang().prefixed("animal.breed.season", "name", clicked.name));
            return false;
        }
        String problem = breedProblem(clicked, sp, clicked.female);
        if (problem != null) {
            p.sendMessage(plugin.lang().prefixed("animal.breed.fail", "name", clicked.name, "reason", problem));
            return false;
        }
        AnimalData mate = findMate(clicked, sp, !clicked.female);
        if (mate == null) {
            p.sendMessage(plugin.lang().prefixed("animal.breed.nomate", "name", clicked.name));
            return false;
        }
        AnimalData mom = clicked.female ? clicked : mate;
        AnimalData dad = clicked.female ? mate : clicked;
        mom.pregnant = true;
        mom.gestation = sp.gestationDays() * (1 - skill(mom, Skill.BREEDER) * 0.04);
        mom.sire = sireSnapshot(dad);
        mom.owner = p.getUniqueId().toString();
        mom.breedCooldown = 1.0;
        dad.breedCooldown = 1.0;
        Util.takeOne(p);
        effects(mom.entity, Particle.HEART, Sound.ENTITY_PLAYER_LEVELUP);
        effects(dad.entity, Particle.HEART, Sound.ENTITY_PLAYER_LEVELUP);
        p.sendMessage(plugin.lang().prefixed("animal.breed.ok", "mother", mom.name, "father", dad.name));
        plugin.farmers().addXp(p, plugin.getConfig().getDouble("farmer.xp.breed", 10));
        save(mom);
        save(dad);
        return true;
    }

    private String sireSnapshot(AnimalData dad) {
        StringBuilder g = new StringBuilder();
        for (int i = 0; i < dad.genes.length; i++) {
            if (i > 0) g.append(',');
            g.append(dad.genes[i]);
        }
        String gp1 = dad.lineage.size() > 0 ? dad.lineage.get(0) : "";
        String gp2 = dad.lineage.size() > 1 ? dad.lineage.get(1) : "";
        return g + "/" + String.join(",", dad.mutations) + "/" + dad.describe() + "/" + gp1 + "/" + gp2 + "/" + dad.breed;
    }

    private void giveBirth(AnimalData mom, Species sp) {
        LivingEntity me = mom.entity;
        FileConfiguration cfg = plugin.getConfig();
        String[] s = mom.sire.split("/", -1);
        int[] dadGenes = new int[Gene.values().length];
        String[] gs = s.length > 0 ? s[0].split(",") : new String[0];
        for (int i = 0; i < dadGenes.length; i++) {
            try {
                dadGenes[i] = i < gs.length ? Integer.parseInt(gs[i].trim()) : 50;
            } catch (NumberFormatException ex) {
                dadGenes[i] = 50;
            }
        }
        List<String> dadMut = new ArrayList<>();
        if (s.length > 1) {
            for (String id : s[1].split(",")) {
                if (!id.isBlank() && Mutations.get(id.trim()) != null) dadMut.add(id.trim());
            }
        }
        String dadDesc = s.length > 2 ? s[2] : "";
        String dadP1 = s.length > 3 ? s[3] : "";
        String dadP2 = s.length > 4 ? s[4] : "";
        String dadBreed = s.length > 5 ? s[5] : "";

        int litter = Util.rndInt(sp.litterMin(), sp.litterMax());
        if (mom.eff(Gene.FERTILITY) > 75 && Util.chance(0.3)) {
            litter++;
        }
        boolean protectedBirth = mom.stabilized;
        Player owner = ownerOf(mom);
        int born = 0;
        for (int i = 0; i < litter; i++) {
            Entity ent;
            spawningChild = true;
            try {
                ent = me.getWorld().spawnEntity(me.getLocation(), sp.entity());
            } finally {
                spawningChild = false;
            }
            if (!(ent instanceof LivingEntity child)) {
                ent.remove();
                continue;
            }
            AnimalData c = new AnimalData();
            c.speciesId = sp.id();
            c.name = randomName();
            c.female = Util.chance(0.5);
            c.owner = mom.owner;
            String cb = Util.chance(0.5) ? mom.breed : dadBreed;
            c.breed = cb == null || cb.isEmpty() ? mom.breed : cb;
            c.hunger = 70;
            c.thirst = 70;
            c.hygiene = 90;
            c.happiness = 75;
            for (int g = 0; g < c.genes.length; g++) {
                double r = Util.rnd();
                int a = mom.genes[g];
                int b = dadGenes[g];
                int v = r < 0.4 ? a : r < 0.8 ? b : (a + b) / 2;
                c.genes[g] = Util.clamp(v + Util.rndInt(-4, 4), 0, 100);
            }
            Set<String> muts = new LinkedHashSet<>();
            for (String id : mom.mutations) if (Util.chance(0.5)) muts.add(id);
            for (String id : dadMut) if (Util.chance(0.5)) muts.add(id);
            if (protectedBirth) {
                muts.removeIf(id -> {
                    Mutation m = Mutations.get(id);
                    return m != null && !m.positive();
                });
            } else if (Util.chance(cfg.getDouble("genetics.mutation-chance", 0.10))) {
                muts.add(Mutations.random(Util.chance(0.5)).id());
                if (owner != null) {
                    plugin.farmers().stat(owner, "mutations", 1);
                }
            }
            int max = cfg.getInt("genetics.max-mutations", 5);
            for (String id : muts) {
                if (c.mutations.size() < max) c.mutations.add(id);
            }
            c.lineage.add(mom.describe());
            c.lineage.add(dadDesc);
            c.lineage.add(mom.lineage.size() > 0 ? mom.lineage.get(0) : "");
            c.lineage.add(mom.lineage.size() > 1 ? mom.lineage.get(1) : "");
            c.lineage.add(dadP1);
            c.lineage.add(dadP2);
            c.entity = child;
            c.lastTick = System.currentTimeMillis();
            data.put(child.getUniqueId(), c);
            applyVisuals(c, sp);
            save(c);
            born++;
        }
        mom.pregnant = false;
        mom.stabilized = false;
        mom.sire = "";
        mom.hunger = Math.max(0, mom.hunger - 20);
        effects(me, Particle.HEART, Sound.ENTITY_CHICKEN_EGG);
        if (owner != null && born > 0) {
            owner.sendMessage(plugin.lang().prefixed("animal.born", "name", mom.name, "count", born));
            plugin.farmers().stat(owner, "born", born);
            plugin.farmers().addXp(owner, plugin.getConfig().getDouble("farmer.xp.birth", 20) * born);
        }
        save(mom);
    }

    // ------------------------------------------------------------------ información para menús

    public List<String> needs(AnimalData d, Species sp) {
        List<String> l = new ArrayList<>();
        Env env = inspect(d, sp);
        if (d.hunger < 35) l.add("&cTiene hambre: dale pienso, forraje o su comida");
        if (d.thirst < 35) l.add("&bTiene sed: necesita agua cerca (abrevadero)");
        if (d.hygiene < 40) l.add("&6Está sucio: usa el Cepillo y limpia el estiércol");
        if (env.crowd() > 0) l.add("&cHacinamiento: necesita más espacio (" + env.same() + "/" + sp.maxDensity() + ")");
        if (env.coldStress() > 0) l.add("&bDemasiado frío (" + Util.fmt(env.temp()) + "°C): necesita refugio");
        if (env.heatStress() > 0) l.add("&cDemasiado calor (" + Util.fmt(env.temp()) + "°C): necesita refugio y agua");
        if (!env.sheltered() && sp.suffersIn(plugin.seasons().current())) {
            l.add("&6Sufre en esta estación: necesita refugio");
        }
        if (d.disease != Disease.NONE) {
            CustomItems.Def cure = CustomItems.get(d.disease.cure());
            l.add("&2Enfermo de " + d.disease.label() + ": necesita " + (cure == null ? "medicina"
                    : Text.color(plugin.lang().raw("item." + cure.id() + ".name"))));
        }
        if (d.happiness < 40) l.add("&eEstá triste: mejora su cuidado");
        if (d.pregnant) l.add("&dEstá gestando: no le falte comida ni agua");
        if (l.isEmpty()) l.add("&aNo necesita nada ahora mismo");
        return l;
    }

    // ------------------------------------------------------------------ hologramas

    private void effects(LivingEntity e, Particle particle, Sound sound) {
        Location l = e.getLocation().add(0, e.getHeight() * 0.8, 0);
        e.getWorld().spawnParticle(particle, l, 6, 0.3, 0.3, 0.3, 0.02);
        e.getWorld().playSound(e.getLocation(), sound, 0.7f, 1f);
    }

    private void removeHolo(AnimalData d) {
        if (d.holo != null) {
            d.holo.remove();
            d.holo = null;
            d.holoText = "";
        }
    }

    private void holoTick() {
        FileConfiguration cfg = plugin.getConfig();
        boolean enabled = cfg.getBoolean("animals.hologram", true);
        double range = cfg.getDouble("animals.hologram-range", 40);
        double r2 = range * range;
        for (AnimalData d : new ArrayList<>(data.values())) {
            LivingEntity e = d.entity;
            if (e == null || !e.isValid()) {
                removeHolo(d);
                continue;
            }
            boolean near = false;
            if (enabled) {
                Location el = e.getLocation();
                for (Player p : e.getWorld().getPlayers()) {
                    if (p.getLocation().distanceSquared(el) < r2) {
                        near = true;
                        break;
                    }
                }
            }
            if (!near) {
                removeHolo(d);
                continue;
            }
            Species sp = plugin.species().get(d.speciesId);
            if (sp == null) {
                continue;
            }
            Location target = holoLocation(e);
            if (d.holo == null || !d.holo.isValid()) {
                d.holo = spawnHolo(target);
                d.holoText = "";
            } else if (d.holo.getLocation().distanceSquared(target) > 0.0025) {
                d.holo.teleport(target);
            }
            String text = holoText(d, sp);
            if (!text.equals(d.holoText)) {
                d.holoText = text;
                d.holo.setText(text);
            }
        }
    }

    private Location holoLocation(LivingEntity e) {
        return new Location(e.getWorld(), e.getLocation().getX(), e.getBoundingBox().getMaxY() + 0.3,
                e.getLocation().getZ());
    }

    private TextDisplay spawnHolo(Location loc) {
        return loc.getWorld().spawn(loc, TextDisplay.class, t -> {
            t.setBillboard(Display.Billboard.CENTER);
            t.setPersistent(false);
            t.setSeeThrough(false);
            t.setShadowed(false);
            t.setBackgroundColor(Color.fromARGB(110, 0, 0, 0));
            t.setViewRange(0.6f);
            t.setTeleportDuration(3);
            t.setAlignment(TextDisplay.TextAlignment.CENTER);
            t.setLineWidth(220);
            t.getPersistentDataContainer().set(Keys.HOLO, PersistentDataType.BYTE, (byte) 1);
        });
    }

    private String holoText(AnimalData d, Species sp) {
        LivingEntity e = d.entity;
        String name = d.name;
        String custom = e.getCustomName();
        if (custom != null && !custom.isBlank()) {
            name = net.md_5.bungee.api.ChatColor.stripColor(custom);
            d.name = Util.clean(name);
            e.setCustomNameVisible(false);
        }
        Stage st = d.stage(sp);
        double pct = Util.clamp(e.getHealth() / Math.max(1.0, maxHealth(e)) * 100.0, 0, 100);
        StringBuilder sb = new StringBuilder();
        sb.append("&6").append(name).append(" &7[Nv &e").append(d.level).append("&7]\n");
        dev.rancho.animal.Breed br = plugin.breeds().get(d.breed);
        sb.append("&f").append(sp.name()).append(br == null ? "" : " &8(" + br.name() + ")").append(" &7· ").append(st.color()).append(st.label())
                .append(" &7· ").append(d.female ? "&d♀" : "&b♂").append('\n');
        sb.append("&c❤ ").append(Util.bar(pct, 10)).append(" &f").append(Util.fmt0(pct)).append('%');
        if (!d.mutations.isEmpty()) {
            sb.append("\n&5✦ ");
            int shown = 0;
            for (String id : d.mutations) {
                Mutation m = Mutations.get(id);
                if (m == null) continue;
                if (shown < 2) {
                    sb.append(m.display()).append(' ');
                }
                shown++;
            }
            if (shown > 2) {
                sb.append("&7+").append(shown - 2);
            }
        }
        if (d.disease != Disease.NONE) {
            sb.append("\n&2☣ ").append(d.disease.label());
        }
        if (d.pregnant) {
            sb.append("\n&d♥ Gestando");
        }
        return Text.color(sb.toString());
    }

    // ------------------------------------------------------------------ caballos

    private void horseTick() {
        for (AnimalData d : data.values()) {
            LivingEntity e = d.entity;
            if (!(e instanceof AbstractHorse) || !e.isValid()) {
                continue;
            }
            Player rider = null;
            for (Entity pass : e.getPassengers()) {
                if (pass instanceof Player pl) {
                    rider = pl;
                    break;
                }
            }
            if (rider == null) {
                continue;
            }
            Location l = e.getLocation();
            double dist = Math.hypot(l.getX() - d.lastX, l.getZ() - d.lastZ) * 2.0;
            d.lastX = l.getX();
            d.lastZ = l.getZ();
            if (dist > 3.0) {
                d.stamina -= (dist - 3.0) * 0.9 * (1.4 - d.eff(Gene.RESISTANCE) / 100.0) * 0.5;
            } else {
                d.stamina += 2.0;
            }
            d.stamina = Util.clamp(d.stamina, 0, 100);
            if (d.stamina <= 5 && !d.tired) {
                d.tired = true;
                applySpeed(d);
            } else if (d.stamina >= 30 && d.tired) {
                d.tired = false;
                applySpeed(d);
            }
            Util.actionBar(rider, Text.color("&6Resistencia " + Util.bar(d.stamina, 10)
                    + (d.tired ? " &c¡Cansado!" : "")));
        }
    }
}
