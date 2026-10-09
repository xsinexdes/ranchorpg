package dev.rancho.food;

import dev.rancho.RanchoPlugin;
import dev.rancho.farmer.Skill;
import dev.rancho.item.CustomItems;
import dev.rancho.quality.Products;
import dev.rancho.quality.QualityLevels;
import dev.rancho.util.Keys;
import dev.rancho.util.Text;
import dev.rancho.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Cocina y comida: ingredientes, cocción con tiempo, frescura (la comida se pudre con el tiempo real)
 * y efectos de las comidas (vanilla y custom: abrigo, frescor, XP de granjero, cosecha afortunada, curación).
 */
public final class FoodManager {

    /** Ingrediente de una receta. */
    public record Need(String token, int amount) {}

    private static final Map<String, String> NAMES = new HashMap<>();

    static {
        NAMES.put("WHEAT", "Trigo");
        NAMES.put("WATER_BUCKET", "Cubo de agua");
        NAMES.put("MILK_BUCKET", "Cubo de leche");
        NAMES.put("EGG", "Huevo");
        NAMES.put("SUGAR", "Azúcar");
        NAMES.put("BOWL", "Cuenco");
        NAMES.put("GLASS_BOTTLE", "Botella de cristal");
        NAMES.put("CARROT", "Zanahoria");
        NAMES.put("POTATO", "Papa");
        NAMES.put("BAKED_POTATO", "Mazorca de maíz / papa asada");
        NAMES.put("COOKED_BEEF", "Carne de res cocida");
        NAMES.put("APPLE", "Manzana");
        NAMES.put("SWEET_BERRIES", "Bayas dulces / Uva");
        NAMES.put("DRIED_KELP", "Alga seca / Arroz");
        NAMES.put("PUMPKIN", "Calabaza");
        NAMES.put("MELON_SLICE", "Rodaja de sandía");
        NAMES.put("GOLD_NUGGET", "Pepita de oro");
        NAMES.put("GOLDEN_CARROT", "Zanahoria dorada");
        NAMES.put("GLOW_BERRIES", "Bayas luminosas");
        NAMES.put("BEETROOT", "Remolacha");
        NAMES.put("SNOWBALL", "Bola de nieve");
        NAMES.put("AZURE_BLUET", "Aciano / Flor Mágica");
        NAMES.put("AMETHYST_SHARD", "Fragmento de amatista");
    }

    private final RanchoPlugin plugin;
    private final Map<UUID, Map<String, Long>> active = new HashMap<>();
    private BukkitTask task;

    public FoodManager(RanchoPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            for (Player p : Bukkit.getOnlinePlayers()) {
                refreshInventory(p.getInventory());
                Map<String, Long> m = active.get(p.getUniqueId());
                if (m != null) {
                    m.values().removeIf(t -> t < now);
                }
            }
        }, 600L, 600L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
    }

    // ------------------------------------------------------------------ ingredientes

    public List<Need> needs(Food f) {
        List<Need> out = new ArrayList<>();
        for (String tok : f.ingredients()) {
            String token = tok;
            int amount = 1;
            int idx = tok.lastIndexOf(':');
            if (idx > 0) {
                try {
                    amount = Math.max(1, Integer.parseInt(tok.substring(idx + 1).trim()));
                    token = tok.substring(0, idx).trim();
                } catch (NumberFormatException ex) {
                    token = tok;
                }
            }
            out.add(new Need(token, amount));
        }
        return out;
    }

    public String foodId(ItemStack it) {
        if (it == null || !it.hasItemMeta()) {
            return null;
        }
        return it.getItemMeta().getPersistentDataContainer().get(Keys.FOOD, PersistentDataType.STRING);
    }

    public boolean matches(ItemStack it, String token) {
        if (it == null || it.getType().isAir()) {
            return false;
        }
        String t = token.trim();
        String lower = t.toLowerCase(Locale.ROOT);
        if (lower.startsWith("food:")) {
            String id = foodId(it);
            return id != null && id.equalsIgnoreCase(t.substring(5));
        }
        if (lower.startsWith("item:")) {
            String id = CustomItems.id(it);
            return id != null && id.equalsIgnoreCase(t.substring(5));
        }
        if (lower.startsWith("group:")) {
            String id = foodId(it);
            Food def = id == null ? null : plugin.foods().get(id);
            return def != null && def.group().equalsIgnoreCase(t.substring(6));
        }
        Material m = Material.matchMaterial(t);
        return m != null && it.getType() == m && foodId(it) == null && CustomItems.id(it) == null;
    }

    public int count(Player p, Need n) {
        int c = 0;
        for (ItemStack it : p.getInventory().getStorageContents()) {
            if (matches(it, n.token())) {
                c += it.getAmount();
            }
        }
        return c;
    }

    public int maxBatches(Player p, Food f) {
        int max = Integer.MAX_VALUE;
        for (Need n : needs(f)) {
            max = Math.min(max, count(p, n) / n.amount());
        }
        return max == Integer.MAX_VALUE ? 0 : max;
    }

    public String tokenName(String token) {
        String low = token.toLowerCase(Locale.ROOT);
        if (low.startsWith("food:")) {
            Food f = plugin.foods().get(token.substring(5));
            return f == null ? token : f.name();
        }
        if (low.startsWith("group:")) {
            String g = low.substring(6);
            return g.equals("fish") ? "Cualquier pescado" : "Cualquier " + g;
        }
        if (low.startsWith("item:")) {
            return net.md_5.bungee.api.ChatColor.stripColor(
                    plugin.lang().get("item." + token.substring(5) + ".name"));
        }
        String n = NAMES.get(token.toUpperCase(Locale.ROOT));
        return n != null ? n : Util.title(token);
    }

    public double spoilMultiplier(UUID id) {
        return plugin.getConfig().getDouble("food.spoil-multiplier", 1.0)
                * (1 + 0.05 * plugin.farmers().skill(id, Skill.CHEF));
    }

    // ------------------------------------------------------------------ cocinar

    /** Empieza a cocinar {@code batches} tandas de {@code f} en la cocina. */
    public boolean cook(Player p, Food f, Block station, int batches) {
        KitchenManager k = plugin.kitchen();
        if (k.job(station) != null) {
            p.sendMessage(plugin.lang().prefixed("food.busy"));
            return false;
        }
        String type = k.typeOf(station);
        if (type == null || !f.inStation(type)) {
            p.sendMessage(plugin.lang().prefixed("food.wrong-station"));
            return false;
        }
        int max = maxBatches(p, f);
        if (max <= 0) {
            p.sendMessage(plugin.lang().prefixed("food.missing"));
            return false;
        }
        int b = Math.max(1, Math.min(batches, max));

        ItemStack[] inv = p.getInventory().getStorageContents();
        double qSum = 0;
        int qCount = 0;
        int buckets = 0;
        for (Need n : needs(f)) {
            int remaining = n.amount() * b;
            for (int i = 0; i < inv.length && remaining > 0; i++) {
                ItemStack it = inv[i];
                if (!matches(it, n.token())) {
                    continue;
                }
                int take = Math.min(remaining, it.getAmount());
                int q = Products.quality(it);
                if (q > 0) {
                    qSum += (double) q * take;
                    qCount += take;
                }
                String up = n.token().toUpperCase(Locale.ROOT);
                if (up.equals("WATER_BUCKET") || up.equals("MILK_BUCKET")) {
                    buckets += take;
                }
                if (it.getAmount() <= take) {
                    inv[i] = null;
                } else {
                    it.setAmount(it.getAmount() - take);
                }
                remaining -= take;
            }
        }
        p.getInventory().setStorageContents(inv);
        while (buckets > 0) {
            int n = Math.min(16, buckets);
            Util.giveOrDrop(p, new ItemStack(Material.BUCKET, n));
            buckets -= n;
        }
        int quality = qCount > 0 ? Util.clamp((int) Math.round(qSum / qCount), 1, 10) : 3;
        int total = f.outAmount() * b;
        double mult = spoilMultiplier(p.getUniqueId());
        long now = System.currentTimeMillis();

        if (f.cookSeconds() <= 0) {
            give(p, f, quality, total, now, mult);
            finishStats(p, b, total);
            p.sendMessage(plugin.lang().prefixed("food.cooked", "amount", total, "food", f.name(), "q", quality));
            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_SMOKER_SMOKE, 1f, 1f);
            return true;
        }
        long ms = (long) (f.cookSeconds() * 1000L * (1 + 0.15 * (b - 1)));
        k.start(station, new KitchenManager.Job(f.id(), total, b, quality, now + ms,
                p.getUniqueId().toString(), mult));
        p.sendMessage(plugin.lang().prefixed("food.started", "food", f.name(), "secs", ms / 1000));
        p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_SMOKER_SMOKE, 1f, 1f);
        return true;
    }

    /** Recoge el plato terminado de la cocina. */
    public boolean collect(Player p, Block station) {
        KitchenManager k = plugin.kitchen();
        KitchenManager.Job j = k.job(station);
        if (j == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now < j.finishAt) {
            p.sendMessage(plugin.lang().prefixed("food.wait", "time", fmtTime(j.finishAt - now)));
            return false;
        }
        Food f = plugin.foods().get(j.foodId);
        k.clear(station);
        if (f == null) {
            return false;
        }
        int quality = j.quality;
        long created = j.finishAt;
        if (f.ageMinutes() > 0) { // la barrica mejora el producto cuanto más reposa
            long step = Math.max(1L, (long) (f.ageMinutes() * 60000.0));
            int bonus = (int) Math.min(f.ageMaxBonus(), Math.max(0, now - j.finishAt) / step);
            if (bonus > 0) {
                quality = Util.clamp(quality + bonus, 1, 10);
                p.sendMessage(plugin.lang().prefixed("food.aged", "bonus", bonus));
            }
            created = now;
        }
        give(p, f, quality, j.amount, created, j.mult);
        finishStats(p, j.batches, j.amount);
        p.sendMessage(plugin.lang().prefixed("food.cooked", "amount", j.amount, "food", f.name(), "q", quality));
        p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
        return true;
    }

    /** Entrega los items de un plato terminado (usado también al romper una cocina). */
    public List<ItemStack> outputs(Food f, int quality, int amount, long createdAt, double mult) {
        List<ItemStack> out = new ArrayList<>();
        int stack = Math.max(1, Math.min(16, f.material().getMaxStackSize()));
        int left = amount;
        while (left > 0) {
            int n = Math.min(stack, left);
            out.add(create(f, quality, n, createdAt, mult));
            left -= n;
        }
        return out;
    }

    private void give(Player p, Food f, int quality, int amount, long createdAt, double mult) {
        for (ItemStack it : outputs(f, quality, amount, createdAt, mult)) {
            Util.giveOrDrop(p, it);
        }
    }

    private void finishStats(Player p, int batches, int total) {
        plugin.farmers().stat(p, "cooked", total);
        plugin.farmers().addXp(p, plugin.getConfig().getDouble("farmer.xp.cook", 3) * batches);
    }

    // ------------------------------------------------------------------ items de comida y frescura

    public ItemStack create(Food f, int quality, int amount, long createdAt, double mult) {
        ItemStack it = Products.create("food", f.id(), f.name(), f.material(), quality, 0, amount);
        ItemMeta m = it.getItemMeta();
        if (m == null) {
            return it;
        }
        PersistentDataContainer pdc = m.getPersistentDataContainer();
        long spoilMs = (long) (f.spoilMinutes() * 60000.0 * mult);
        pdc.set(Keys.FOOD, PersistentDataType.STRING, f.id());
        pdc.set(Keys.CREATED, PersistentDataType.LONG, createdAt);
        pdc.set(Keys.SPOIL, PersistentDataType.LONG, spoilMs);

        List<String> lore = m.getLore() == null ? new ArrayList<>() : new ArrayList<>(m.getLore());
        if (f.foodPoints() > 0) {
            lore.add(plugin.lang().get("food.lore.points", "points", f.foodPoints()));
        }
        for (Food.Eff e : f.effects()) {
            lore.add(plugin.lang().get("food.lore.effect", "effect", effectName(e), "secs", e.seconds()));
        }
        double fr = 1.0 - (System.currentTimeMillis() - createdAt) / (double) spoilMs;
        lore.add(freshLine(fr, spoilMs));
        m.setLore(lore);
        it.setItemMeta(m);
        return it;
    }

    private String freshLine(double fr, long spoilMs) {
        double clamped = Math.max(0, Math.min(1, fr));
        return plugin.lang().get("food.fresh", "bar", Util.bar(clamped * 100, 10),
                "pct", (int) Math.round(clamped * 100), "time", fmtTime((long) (clamped * spoilMs)));
    }

    public static String fmtTime(long ms) {
        long min = ms / 60000;
        if (min >= 60) {
            return (min / 60) + " h " + (min % 60) + " min";
        }
        if (min < 1) {
            return "menos de 1 min";
        }
        return min + " min";
    }

    /** Frescura 0-1 (puede ser negativa si ya se pasó); -1 si el item no tiene caducidad. */
    public double freshness(ItemStack it) {
        if (it == null || !it.hasItemMeta()) {
            return -1;
        }
        PersistentDataContainer pdc = it.getItemMeta().getPersistentDataContainer();
        Long created = pdc.get(Keys.CREATED, PersistentDataType.LONG);
        Long spoil = pdc.get(Keys.SPOIL, PersistentDataType.LONG);
        if (created == null || spoil == null || spoil <= 0) {
            return -1;
        }
        return 1.0 - (System.currentTimeMillis() - created) / (double) spoil;
    }

    /** Actualiza la frescura del item; si se pudrió devuelve la comida podrida. */
    public ItemStack refresh(ItemStack it) {
        String id = foodId(it);
        if (id == null || id.equals("rotten")) {
            return it;
        }
        double fr = freshness(it);
        if (fr == -1) {
            return it;
        }
        if (fr <= 0) {
            return rotten(it.getAmount());
        }
        ItemMeta m = it.getItemMeta();
        int want = plugin.models().get("food:" + id);
        int have = m.hasCustomModelData() ? m.getCustomModelData() : 0;
        if (have != want) { // sincroniza el modelo si el admin lo cambió
            m.setCustomModelData(want > 0 ? want : null);
            it.setItemMeta(m);
            m = it.getItemMeta();
        }
        PersistentDataContainer pdc = m.getPersistentDataContainer();
        int pct = (int) Math.round(fr * 100);
        Integer shown = pdc.get(Keys.SHOWN, PersistentDataType.INTEGER);
        if (shown != null && Math.abs(shown - pct) < 5) {
            return it;
        }
        Long spoil = pdc.get(Keys.SPOIL, PersistentDataType.LONG);
        List<String> lore = m.getLore() == null ? new ArrayList<>() : new ArrayList<>(m.getLore());
        if (!lore.isEmpty() && spoil != null) {
            lore.set(lore.size() - 1, freshLine(fr, spoil));
            m.setLore(lore);
        }
        pdc.set(Keys.SHOWN, PersistentDataType.INTEGER, pct);
        it.setItemMeta(m);
        return it;
    }

    /** Retrasa la caducidad de un item (para neveras y acuarios). */
    public void shift(ItemStack it, long ms) {
        if (foodId(it) == null || ms <= 0) {
            return;
        }
        ItemMeta m = it.getItemMeta();
        PersistentDataContainer pdc = m.getPersistentDataContainer();
        Long created = pdc.get(Keys.CREATED, PersistentDataType.LONG);
        if (created == null) {
            return;
        }
        pdc.set(Keys.CREATED, PersistentDataType.LONG, created + ms);
        pdc.set(Keys.SHOWN, PersistentDataType.INTEGER, -100); // fuerza actualizar la barra de frescura
        it.setItemMeta(m);
    }

    public void refreshInventory(Inventory inv) {
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s == null || foodId(s) == null) {
                continue;
            }
            ItemStack r = refresh(s);
            if (r != s) {
                inv.setItem(i, r);
            }
        }
    }

    public ItemStack rotten(int amount) {
        ItemStack r = new ItemStack(Material.ROTTEN_FLESH, Math.max(1, amount));
        ItemMeta m = r.getItemMeta();
        if (m != null) {
            m.setDisplayName(plugin.lang().get("food.rotten.name"));
            List<String> lore = new ArrayList<>();
            for (String line : plugin.lang().raw("food.rotten.lore").split("\\|")) {
                lore.add(Text.color(line));
            }
            m.setLore(lore);
            m.getPersistentDataContainer().set(Keys.FOOD, PersistentDataType.STRING, "rotten");
            r.setItemMeta(m);
        }
        return r;
    }

    // ------------------------------------------------------------------ efectos

    public String effectName(Food.Eff e) {
        return switch (e.key()) {
            case "warm" -> "Abrigo (inmune al frío)";
            case "cool" -> "Frescor (inmune al calor)";
            case "farmer_xp" -> "XP de granjero +50%";
            case "lucky_harvest" -> "Cosecha afortunada";
            case "heal" -> "Curación";
            default -> Util.title(e.key());
        };
    }

    public void applyEffects(Player p, Food f) {
        for (Food.Eff e : f.effects()) {
            applyEffect(p, e);
        }
    }

    public void applyEffect(Player p, Food.Eff e) {
        switch (e.key()) {
            case "warm", "cool", "farmer_xp", "lucky_harvest" -> active
                    .computeIfAbsent(p.getUniqueId(), k -> new HashMap<>())
                    .put(e.key(), System.currentTimeMillis() + e.seconds() * 1000L);
            case "heal" -> {
                AttributeInstance a = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                double max = a == null ? 20.0 : a.getValue();
                p.setHealth(Math.min(max, p.getHealth() + Math.max(1, e.amp()) * 2.0));
            }
            default -> {
                PotionEffectType t = PotionEffectType.getByKey(NamespacedKey.minecraft(e.key()));
                if (t != null && e.seconds() > 0) {
                    p.addPotionEffect(new PotionEffect(t, e.seconds() * 20, e.amp(), true, true, true));
                }
            }
        }
    }

    /** True si el jugador tiene activo un efecto custom (warm, cool, farmer_xp, lucky_harvest). */
    public boolean has(UUID id, String key) {
        Map<String, Long> m = active.get(id);
        if (m == null) {
            return false;
        }
        Long t = m.get(key);
        return t != null && t > System.currentTimeMillis();
    }

    /** Bono por nivel de calidad: saturación extra y buff del nivel. */
    public void applyQualityBonus(Player p, int q, int baseFood) {
        QualityLevels lv = plugin.levels();
        int sat = lv.saturation(q) + baseFood;
        if (sat > 0) {
            p.setFoodLevel(Math.min(20, p.getFoodLevel() + sat));
            p.setSaturation(Math.min(p.getFoodLevel(), p.getSaturation() + sat));
        }
        PotionEffectType type = lv.effectType(q);
        if (type != null) {
            p.addPotionEffect(new PotionEffect(type, lv.effectSeconds(q) * 20, lv.effectAmp(q), true, true, true));
        }
    }

    public void rottenEffects(Player p) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 120, 0, true, true, true));
        p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 240, 0, true, true, true));
        p.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 200, 0, true, true, true));
        p.sendMessage(plugin.lang().prefixed("food.eat-rotten"));
    }
}
