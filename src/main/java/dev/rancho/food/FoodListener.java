package dev.rancho.food;

import dev.rancho.RanchoPlugin;
import dev.rancho.gui.KitchenMenu;
import dev.rancho.item.CustomItems;
import dev.rancho.quality.Products;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/** Eventos de la cocina y la comida: abrir la cocina, comer, pudrirse al abrir o recoger. */
public final class FoodListener implements Listener {

    private final RanchoPlugin plugin;

    public FoodListener(RanchoPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || e.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block b = e.getClickedBlock();
        if (b == null || !plugin.kitchen().isStation(b)) {
            return;
        }
        Player p = e.getPlayer();
        ItemStack item = e.getItem();
        if (p.isSneaking() && item != null && !item.getType().isAir()) {
            return; // agachado con un bloque en la mano: se puede colocar
        }
        e.setCancelled(true);
        new KitchenMenu(plugin, b, null).open(p);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        String id = CustomItems.id(e.getItemInHand());
        if (id != null && (id.equals("kitchen") || id.equals("cask") || id.equals("mill"))) {
            plugin.kitchen().add(e.getBlockPlaced(), id);
            e.getPlayer().sendMessage(plugin.lang().prefixed("food." + id + "-placed"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlaceFood(BlockPlaceEvent e) {
        if (plugin.food().foodId(e.getItemInHand()) != null) {
            e.setCancelled(true); // la comida (tarta, etc.) no se coloca como bloque
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        if (!plugin.kitchen().isStation(b)) {
            return;
        }
        e.setDropItems(false);
        String type = plugin.kitchen().typeOf(b);
        KitchenManager.Job job = plugin.kitchen().job(b);
        plugin.kitchen().remove(b);
        if (e.getPlayer().getGameMode() == GameMode.CREATIVE) {
            return;
        }
        b.getWorld().dropItemNaturally(b.getLocation().add(0.5, 0.5, 0.5), CustomItems.create(type == null ? "kitchen" : type, 1));
        if (job != null && System.currentTimeMillis() >= job.finishAt) {
            Food f = plugin.foods().get(job.foodId);
            if (f != null) {
                for (ItemStack out : plugin.food().outputs(f, job.quality, job.amount, job.finishAt, job.mult)) {
                    b.getWorld().dropItemNaturally(b.getLocation().add(0.5, 0.5, 0.5), out);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent e) {
        ItemStack it = e.getItem();
        String id = plugin.food().foodId(it);
        if (id == null) {
            return;
        }
        Player p = e.getPlayer();
        double fr = plugin.food().freshness(it);
        if (id.equals("rotten") || (fr != -1 && fr <= 0)) {
            plugin.getServer().getScheduler().runTask(plugin, () -> plugin.food().rottenEffects(p));
            return;
        }
        Food f = plugin.foods().get(id);
        if (f == null) {
            return;
        }
        int q = Math.max(1, Products.quality(it));
        double sat = f.saturation();
        int points = f.foodPoints();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            p.setFoodLevel(Math.min(20, p.getFoodLevel() + points));
            p.setSaturation((float) Math.min(p.getFoodLevel(), p.getSaturation() + sat));
            plugin.food().applyQualityBonus(p, q, 0);
            plugin.food().applyEffects(p, f);
        });
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent e) {
        plugin.food().refreshInventory(e.getInventory());
        if (e.getPlayer() instanceof Player p) {
            plugin.food().refreshInventory(p.getInventory());
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player)) {
            return;
        }
        ItemStack stack = e.getItem().getItemStack();
        if (plugin.food().foodId(stack) == null) {
            return;
        }
        ItemStack r = plugin.food().refresh(stack);
        if (r != stack) {
            e.getItem().setItemStack(r);
        }
    }
}
