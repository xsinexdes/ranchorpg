package dev.rancho.gui;

import dev.rancho.RanchoPlugin;
import dev.rancho.animal.AnimalData;
import dev.rancho.animal.Species;
import dev.rancho.crop.CropData;
import dev.rancho.crop.CropDef;
import dev.rancho.crop.CropRegistry;
import dev.rancho.util.Text;
import dev.rancho.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;

/** ActionBar opcional con información al mirar un animal o un cultivo. */
public final class LookInfo {

    private final RanchoPlugin plugin;
    private BukkitTask task;

    public LookInfo(RanchoPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 10L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
    }

    private void tick() {
        if (!plugin.getConfig().getBoolean("animals.actionbar", false)) {
            return;
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            RayTraceResult r = p.getWorld().rayTraceEntities(p.getEyeLocation(), p.getEyeLocation().getDirection(),
                    6, 0.3, ent -> ent != p && plugin.animals().get(ent) != null);
            if (r != null && r.getHitEntity() != null) {
                Entity ent = r.getHitEntity();
                AnimalData d = plugin.animals().get(ent);
                Species sp = d == null ? null : plugin.species().get(d.speciesId);
                if (d != null && sp != null) {
                    Util.actionBar(p, Text.color("&6" + d.name + " &7Nv&e" + d.level + " &8| &eSaciedad &f"
                            + Util.fmt0(d.hunger) + "% &8| &bSed &f" + Util.fmt0(d.thirst) + "% &8| &dFelicidad &f"
                            + Util.fmt0(d.happiness) + "%"));
                    continue;
                }
            }
            Block b = p.getTargetBlockExact(6);
            if (b != null && CropRegistry.isCropBlock(b.getType())) {
                CropData c = plugin.crops().at(b);
                CropDef def = c == null ? null : plugin.cropDefs().get(c.cropId);
                if (c != null && def != null) {
                    Util.actionBar(p, Text.color("&a" + def.name() + " &8| &f" + Util.fmt0(c.progress * 100) + "%"
                            + (c.withered ? " &c(marchito)" : "") + (c.pest ? " &c(plaga)" : "")));
                }
            }
        }
    }
}
