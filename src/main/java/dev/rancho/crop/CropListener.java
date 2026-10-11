package dev.rancho.crop;

import dev.rancho.RanchoPlugin;
import dev.rancho.gui.CropInfoMenu;
import dev.rancho.item.CustomItems;
import dev.rancho.quality.Products;
import dev.rancho.quality.QualityLevels;
import dev.rancho.util.Text;
import dev.rancho.util.Util;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;

/** Eventos de cultivo: plantar, regar, fertilizar, cosechar, invernaderos y consumo de productos. */
public final class CropListener implements Listener {

    private final RanchoPlugin plugin;

    public CropListener(RanchoPlugin plugin) {
        this.plugin = plugin;
    }

    private CropManager crops() {
        return plugin.crops();
    }

    /** Devuelve la tierra de labranza del bloque clicado (o la que hay bajo el cultivo). */
    private Block farmlandOf(Block b) {
        if (b == null) {
            return null;
        }
        if (b.getType() == Material.FARMLAND) {
            return b;
        }
        if (CropRegistry.isCropBlock(b.getType())) {
            Block below = b.getRelative(BlockFace.DOWN);
            return below.getType() == Material.FARMLAND ? below : null;
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_BLOCK && a != Action.RIGHT_CLICK_AIR) {
            return;
        }
        Player p = e.getPlayer();
        ItemStack item = e.getItem();
        Block block = e.getClickedBlock();
        String cid = CustomItems.id(item);

        if (cid != null) {
            handleCustom(e, p, item, block, cid);
            return;
        }
        if (item == null) {
            return;
        }

        // Semillas vanilla sobre tierra de labranza: las gestiona el sistema RPG
        if (a == Action.RIGHT_CLICK_BLOCK && block != null && block.getType() == Material.FARMLAND
                && e.getBlockFace() == BlockFace.UP && !Products.isProduct(item)) {
            CropDef def = plugin.cropDefs().forVanillaSeed(item.getType());
            if (def != null) {
                e.setCancelled(true);
                crops().plant(p, block, def);
                return;
            }
        }
        // Polvo de hueso vanilla: sustituido por los fertilizantes custom
        if (item.getType() == Material.BONE_MEAL && block != null && CropRegistry.isCropBlock(block.getType())) {
            e.setCancelled(true);
            return;
        }
        // Productos no comestibles (huevos, flores...) no deben usarse como en vanilla
        if (Products.isProduct(item) && !item.getType().isEdible()) {
            e.setUseItemInHand(Event.Result.DENY);
            if (a == Action.RIGHT_CLICK_BLOCK) {
                e.setUseInteractedBlock(Event.Result.DENY);
            }
        }
    }

    private void handleCustom(PlayerInteractEvent e, Player p, ItemStack item, Block block, String cid) {
        if (CustomItems.isPlaceable(cid)) {
            return; // se colocan como bloque (ver onPlace)
        }
        e.setCancelled(true);
        boolean onBlock = e.getAction() == Action.RIGHT_CLICK_BLOCK;
        Block farmland = onBlock ? farmlandOf(block) : null;

        switch (cid) {
            case "thermometer" -> {
                double t = plugin.temperature().getTemperature(p.getLocation());
                Util.actionBar(p, plugin.lang().get("info.temperature",
                        "color", dev.rancho.climate.TemperatureManager.colorFor(t).replace('&', '§'),
                        "temp", Util.fmt(t)));
            }
            case "notebook" -> {
                if (block == null) {
                    return;
                }
                Block cropBlock = CropRegistry.isCropBlock(block.getType()) ? block
                        : block.getType() == Material.FARMLAND ? block.getRelative(BlockFace.UP) : null;
                CropData c = cropBlock == null ? null : crops().at(cropBlock);
                if (c == null && cropBlock != null) {
                    c = crops().registerExisting(cropBlock);
                }
                if (c != null) {
                    new CropInfoMenu(plugin, c).open(p);
                } else if (farmland != null) {
                    SoilData s = crops().soilIfAny(farmland);
                    Util.actionBar(p, plugin.lang().get("crop.soil-info",
                            "water", Util.fmt0(s == null ? 0 : s.water),
                            "fert", Util.fmt0(s == null ? 0 : s.fertility),
                            "improved", plugin.lang().get(s != null && s.improved ? "word.yes" : "word.no")));
                }
            }
            case "watering_can" -> {
                int uses = CustomItems.uses(item);
                if (farmland != null) {
                    if (uses <= 0) {
                        p.sendMessage(plugin.lang().prefixed("crop.can-empty"));
                        return;
                    }
                    crops().water(p, farmland);
                    CustomItems.setUses(item, uses - 1);
                    return;
                }
                RayTraceResult r = p.getWorld().rayTraceBlocks(p.getEyeLocation(), p.getEyeLocation().getDirection(),
                        5, FluidCollisionMode.SOURCE_ONLY, true);
                if (r != null && r.getHitBlock() != null && r.getHitBlock().getType() == Material.WATER) {
                    CustomItems.setUses(item, plugin.getConfig().getInt("crops.can-uses", 20));
                    p.playSound(p.getLocation(), Sound_FILL, 1f, 1f);
                    p.sendMessage(plugin.lang().prefixed("crop.can-filled"));
                }
            }
            case "fertilizer_basic", "fertilizer_advanced", "manure" -> {
                if (farmland != null) {
                    crops().fertilize(p, farmland, cid);
                }
            }
            case "pesticide" -> {
                if (block == null) {
                    return;
                }
                Block cropBlock = CropRegistry.isCropBlock(block.getType()) ? block
                        : block.getType() == Material.FARMLAND ? block.getRelative(BlockFace.UP) : null;
                CropData target = cropBlock == null ? null : crops().at(cropBlock);
                if (target != null) {
                    crops().curePest(p, target);
                }
            }
            case "improved_soil" -> {
                if (farmland != null) {
                    crops().improve(p, farmland);
                }
            }
            case "seed" -> {
                if (block != null && block.getType() == Material.FARMLAND && e.getBlockFace() == BlockFace.UP) {
                    CropDef def = plugin.cropDefs().get(CustomItems.seedCrop(item));
                    if (def != null && def.enabled()) {
                        crops().plant(p, block, def);
                    }
                }
            }
            default -> { }
        }
    }

    private static final org.bukkit.Sound Sound_FILL = org.bukkit.Sound.ITEM_BUCKET_FILL;

    @EventHandler(priority = EventPriority.HIGH)
    public void onGrow(BlockGrowEvent e) {
        Block b = e.getBlock();
        if (!CropRegistry.isCropBlock(b.getType())) {
            return;
        }
        if (crops().at(b) == null) {
            crops().registerExisting(b);
        }
        e.setCancelled(true); // el crecimiento vanilla se reemplaza por el del plugin
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFertilize(BlockFertilizeEvent e) {
        if (CropRegistry.isCropBlock(e.getBlock().getType())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        Player p = e.getPlayer();

        String ctype = crops().controllerAt(b);
        if (ctype != null) {
            e.setDropItems(false);
            crops().removeController(b);
            if (p.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                b.getWorld().dropItemNaturally(b.getLocation().add(0.5, 0.5, 0.5), CustomItems.create(ctype, 1));
            }
            return;
        }

        if (!CropRegistry.isCropBlock(b.getType())) {
            // Si se rompe la tierra de labranza, el cultivo de encima se elimina en el siguiente tick
            return;
        }
        CropData c = crops().at(b);
        if (c == null) {
            c = crops().registerExisting(b);
        }
        if (c == null) {
            return;
        }
        e.setDropItems(false);
        if (p.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            crops().remove(b);
            return;
        }
        for (ItemStack drop : crops().harvest(p, c)) {
            b.getWorld().dropItemNaturally(b.getLocation().add(0.5, 0.3, 0.5), drop);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlace(BlockPlaceEvent e) {
        if (crops().isPlanting()) {
            return; // comprobación interna de permisos al plantar
        }
        ItemStack item = e.getItemInHand();
        String cid = CustomItems.id(item);
        if (cid != null) {
            if (cid.equals("greenhouse") || cid.equals("pot")) {
                crops().addController(e.getBlockPlaced(), cid);
                e.getPlayer().sendMessage(plugin.lang().prefixed("crop.controller-placed",
                        "range", cid.equals("greenhouse") ? plugin.getConfig().getInt("crops.greenhouse-radius", 6)
                                : plugin.getConfig().getInt("crops.pot-radius", 2)));
            } else if (!CustomItems.isPlaceable(cid)) {
                e.setCancelled(true);
            }
            return;
        }
        if (Products.isProduct(item)) {
            e.setCancelled(true); // los productos (zanahoria, flor...) no se plantan
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onConsume(PlayerItemConsumeEvent e) {
        ItemStack it = e.getItem();
        if (CustomItems.id(it) != null) {
            e.setCancelled(true);
            return;
        }
        if ("food".equals(Products.type(it))) {
            return; // la comida de la cocina la gestiona FoodListener
        }
        int q = Products.quality(it);
        if (q <= 0) {
            return;
        }
        Player p = e.getPlayer();
        QualityLevels lv = plugin.levels();
        if (it.getType() == Material.MILK_BUCKET) {
            e.setCancelled(true);
            if (p.getInventory().getItemInMainHand().isSimilar(it)) {
                p.getInventory().setItemInMainHand(new ItemStack(Material.BUCKET));
            } else {
                p.getInventory().setItemInOffHand(new ItemStack(Material.BUCKET));
            }
            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_DRINK, 1f, 1f);
            applyBonus(p, lv, q, 3);
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> applyBonus(p, lv, q, 0));
    }

    private void applyBonus(Player p, QualityLevels lv, int q, int baseFood) {
        int sat = lv.saturation(q) + baseFood;
        if (sat > 0) {
            p.setFoodLevel(Math.min(20, p.getFoodLevel() + sat));
            p.setSaturation(Math.min(p.getFoodLevel(), p.getSaturation() + sat));
        }
        PotionEffectType type = lv.effectType(q);
        if (type != null) {
            p.addPotionEffect(new PotionEffect(type, lv.effectSeconds(q) * 20, lv.effectAmp(q), true, true, true));
            p.sendMessage(plugin.lang().prefixed("product.buff", "level", Text.color(lv.color(q) + lv.name(q)),
                    "effect", Util.title(lv.effectKey(q))));
        }
    }
}
