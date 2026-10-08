package dev.rancho.animal;

import dev.rancho.RanchoPlugin;
import dev.rancho.gui.AnimalMenu;
import dev.rancho.gui.GeneticsMenu;
import dev.rancho.gui.MarketMenu;
import dev.rancho.item.CustomItems;
import dev.rancho.util.Keys;
import dev.rancho.util.Util;
import org.bukkit.Material;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/** Eventos del ganado: seguimiento de entidades, interacción con jugadores y reemplazo de la cría vanilla. */
public final class AnimalListener implements Listener {

    private final RanchoPlugin plugin;

    public AnimalListener(RanchoPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent e) {
        for (Entity ent : e.getEntities()) {
            if (ent instanceof LivingEntity le) {
                plugin.animals().track(le);
            } else if (ent instanceof TextDisplay td && td.getPersistentDataContainer().has(Keys.HOLO)) {
                td.remove();
            }
        }
    }

    @EventHandler
    public void onEntitiesUnload(EntitiesUnloadEvent e) {
        for (Entity ent : e.getEntities()) {
            plugin.animals().untrack(ent);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSpawn(CreatureSpawnEvent e) {
        LivingEntity le = e.getEntity();
        if (plugin.animals().spawningChild) {
            return;
        }
        Species sp = plugin.species().forEntity(le.getType());
        if (sp == null) {
            return;
        }
        if (e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.BREEDING) {
            e.setCancelled(true); // la cría vanilla está desactivada
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (le.isValid()) {
                plugin.animals().track(le);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBreed(EntityBreedEvent e) {
        if (plugin.species().forEntity(e.getEntity().getType()) != null) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEggDrop(EntityDropItemEvent e) {
        if (e.getEntity() instanceof LivingEntity le && plugin.animals().get(le) != null
                && e.getItemDrop().getItemStack().getType() == Material.EGG) {
            e.setCancelled(true); // los huevos los produce el sistema RPG
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onShear(PlayerShearEntityEvent e) {
        if (plugin.animals().get(e.getEntity()) != null) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        if (plugin.animals().get(e.getEntity()) != null) {
            plugin.animals().onDeath(e.getEntity(), e.getDrops());
        }
    }

    private boolean passthrough(Material m) {
        String n = m.name();
        return m == Material.LEAD || m == Material.NAME_TAG || m == Material.SADDLE || m == Material.CHEST
                || n.endsWith("HORSE_ARMOR") || n.endsWith("CARPET");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player p = e.getPlayer();
        Entity ent = e.getRightClicked();

        if (ent instanceof Villager v && v.getPersistentDataContainer().has(Keys.NPC)) {
            e.setCancelled(true);
            new MarketMenu(plugin).open(p);
            return;
        }

        AnimalData d = plugin.animals().get(ent);
        if (d == null) {
            return;
        }
        Species sp = plugin.species().get(d.speciesId);
        if (sp == null) {
            return;
        }
        ItemStack item = p.getInventory().getItemInMainHand();
        Material type = item.getType();
        String cid = CustomItems.id(item);
        boolean horse = ent instanceof AbstractHorse;

        if (cid == null && passthrough(type)) {
            return; // correa, etiqueta, montura y armadura funcionan como en vanilla
        }
        if (type.isAir() && horse && !p.isSneaking()) {
            return; // click vacío en caballo = montar; agáchate para abrir su menú
        }
        e.setCancelled(true);

        AnimalManager mgr = plugin.animals();
        if (cid != null) {
            switch (cid) {
                case "feed_basic", "feed_premium", "forage" -> {
                    if (sp.acceptsFood(cid)) {
                        mgr.feed(p, d, sp, cid);
                    } else {
                        p.sendMessage(plugin.lang().prefixed("animal.wont-eat", "name", d.name));
                    }
                }
                case "salt" -> mgr.salt(p, d);
                case "vaccine", "antiparasitic", "antibiotic" -> mgr.medicate(p, d, cid);
                case "brush" -> mgr.brush(p, d);
                case "breed_supplement" -> mgr.breed(p, d, sp);
                case "mutation_serum" -> mgr.serum(p, d);
                case "stabilizer" -> mgr.stabilize(p, d);
                case "gene_analysis" -> {
                    if (!d.analyzed) {
                        d.analyzed = true;
                        Util.takeOne(p);
                        mgr.save(d);
                        p.sendMessage(plugin.lang().prefixed("animal.analysis.done", "name", d.name));
                    }
                    new GeneticsMenu(plugin, d, sp).open(p);
                }
                case "notebook" -> new AnimalMenu(plugin, d, sp).open(p);
                default -> { }
            }
            return;
        }

        if (type.isAir()) {
            new AnimalMenu(plugin, d, sp).open(p);
        } else if (type == Material.BUCKET) {
            mgr.milk(p, d, sp);
        } else if (type == Material.WATER_BUCKET) {
            mgr.drink(p, d);
        } else if (type == Material.SHEARS) {
            mgr.shear(p, d, sp);
        } else if (sp.acceptsFood(type.name())) {
            mgr.feed(p, d, sp, type.name());
        } else {
            p.sendMessage(plugin.lang().prefixed("animal.wont-eat", "name", d.name));
        }
    }
}
