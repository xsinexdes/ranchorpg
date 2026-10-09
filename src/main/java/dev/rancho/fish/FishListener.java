package dev.rancho.fish;

import dev.rancho.RanchoPlugin;
import dev.rancho.food.Food;
import dev.rancho.util.Util;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Pesca RPG: los peces vanilla se sustituyen por peces del plugin (con nivel de calidad) según la
 * estación, el agua (dulce o salada), la hora y los encantamientos de la caña.
 */
public final class FishListener implements Listener {

    private final RanchoPlugin plugin;

    public FishListener(RanchoPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean isSalt(Location loc) {
        try {
            String biome = loc.getBlock().getBiome().getKey().getKey();
            return biome.contains("ocean") || biome.contains("beach");
        } catch (RuntimeException ex) {
            return false;
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFish(PlayerFishEvent e) {
        if (e.getState() != PlayerFishEvent.State.CAUGHT_FISH || !(e.getCaught() instanceof Item caught)) {
            return;
        }
        Material m = caught.getItemStack().getType();
        if (m != Material.COD && m != Material.SALMON && m != Material.TROPICAL_FISH && m != Material.PUFFERFISH) {
            return; // tesoros y basura se quedan como en vanilla
        }
        Player p = e.getPlayer();
        Location loc = e.getHook().getLocation();
        World w = loc.getWorld();
        if (w == null) {
            return;
        }
        String season = plugin.seasons().current().key();
        boolean night = w.getTime() >= 13000 && w.getTime() < 23000;
        boolean salt = isSalt(loc);

        List<Food> pool = new ArrayList<>();
        int total = 0;
        for (Food f : plugin.foods().all()) {
            if (!f.enabled() || f.fishWeight() <= 0 || !f.fishIn(season)) continue;
            if (f.fishEnv().equals("SALT") && !salt) continue;
            if (f.fishEnv().equals("FRESH") && salt) continue;
            if (f.fishTime().equals("NIGHT") && !night) continue;
            if (f.fishTime().equals("DAY") && night) continue;
            pool.add(f);
            total += f.fishWeight();
        }
        if (pool.isEmpty()) {
            return;
        }
        int roll = Util.rndInt(1, total);
        Food chosen = pool.get(0);
        for (Food f : pool) {
            roll -= f.fishWeight();
            if (roll <= 0) {
                chosen = f;
                break;
            }
        }

        ItemStack rod = p.getInventory().getItemInMainHand().getType() == Material.FISHING_ROD
                ? p.getInventory().getItemInMainHand() : p.getInventory().getItemInOffHand();
        int luck = rod.getEnchantmentLevel(Enchantment.LUCK_OF_THE_SEA);
        int lure = rod.getEnchantmentLevel(Enchantment.LURE);
        double q = 1 + Util.rnd() * 4 + luck * 1.5 + lure * 0.7 + (w.hasStorm() ? 1 : 0) + (night ? 0.5 : 0);
        int quality = Util.clamp((int) Math.round(q), 1, 10);

        caught.setItemStack(plugin.food().create(chosen, quality, 1, System.currentTimeMillis(),
                plugin.food().spoilMultiplier(p.getUniqueId())));
        plugin.farmers().stat(p, "fished", 1);
        plugin.farmers().addXp(p, plugin.getConfig().getDouble("farmer.xp.fish", 4));
        p.sendMessage(plugin.lang().prefixed("fish.caught", "fish", chosen.name(), "q", quality));
    }
}
