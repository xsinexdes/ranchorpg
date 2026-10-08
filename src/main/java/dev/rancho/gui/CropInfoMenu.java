package dev.rancho.gui;

import dev.rancho.RanchoPlugin;
import dev.rancho.crop.CropData;
import dev.rancho.crop.CropDef;
import dev.rancho.crop.CropManager;
import dev.rancho.crop.SoilData;
import dev.rancho.season.Season;
import dev.rancho.util.ItemBuilder;
import dev.rancho.util.Util;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

/** Información de un cultivo: qué es, qué necesita y cuánto falta para crecer. */
public final class CropInfoMenu extends Menu {

    private final CropData c;

    public CropInfoMenu(RanchoPlugin plugin, CropData c) {
        super(plugin, "&aInformación del cultivo", 3);
        this.c = c;
    }

    @Override
    public boolean live() {
        return true;
    }

    private String ok(boolean b) {
        return b ? "&a✔" : "&c✘";
    }

    @Override
    protected void build() {
        CropDef def = plugin.cropDefs().get(c.cropId);
        Block b = c.block();
        if (def == null || b == null || b.getType() != def.block()) {
            set(13, new ItemBuilder(Material.BARRIER).name("&cEste cultivo ya no existe").build());
            fillEmpty();
            return;
        }
        SoilData soil = plugin.crops().soilAt(b.getRelative(BlockFace.DOWN));
        CropManager.Eval ev = plugin.crops().evaluate(c, def, b, soil);

        set(4, new ItemBuilder(def.productMaterial()).name("&a" + def.name())
                .lore(Util.bar(c.progress * 100, 12) + " &f" + Util.fmt0(c.progress * 100) + "%",
                        c.withered ? "&cMarchito" : c.mature() ? "&a¡Listo para cosechar!" : "&7Creciendo",
                        ev.greenhouse() ? "&bProtegido por invernadero" : "&7Al aire libre").build());

        StringBuilder seasons = new StringBuilder();
        for (Season s : Season.values()) {
            if (def.growsIn(s)) {
                seasons.append(plugin.seasons().name(s)).append(' ');
            }
        }
        set(10, new ItemBuilder(Material.CLOCK).name(ok(ev.season()) + " &eEstación")
                .lore("&7Crece en: " + seasons, "&7Ahora: " + plugin.seasons().name(plugin.seasons().current())).build());
        set(11, new ItemBuilder(Material.BLAZE_POWDER).name(ok(ev.tempScore() > 0) + " &eTemperatura")
                .lore("&7Ideal: &f" + Util.fmt0(def.tempMin()) + "°C a " + Util.fmt0(def.tempMax()) + "°C",
                        "&7Actual: &f" + Util.fmt(ev.temp()) + "°C").build());
        set(12, new ItemBuilder(Material.SUNFLOWER).name(ok(ev.light()) + " &eLuz")
                .lore("&7Nivel de luz mínimo: &f" + def.lightMin(), def.needsSun() ? "&7Necesita sol directo" : "&7No necesita sol directo",
                        "&7Luz actual: &f" + b.getLightLevel()).build());
        set(13, new ItemBuilder(Material.WATER_BUCKET).name(ok(ev.water()) + " &eAgua")
                .lore(def.needsWater() ? "&7Necesita riego" : "&7No necesita riego",
                        "&7Humedad de la tierra: &f" + Util.fmt0(soil.water) + "%").build());
        set(14, new ItemBuilder(Material.BONE_MEAL).name(ok(ev.fertilizer()) + " &eFertilizante")
                .lore(def.needsFertilizer() ? "&7Necesita fertilizante" : "&7No necesita fertilizante",
                        "&7Fertilidad: &f" + Util.fmt0(soil.fertility) + "%").build());
        set(15, new ItemBuilder(Material.COARSE_DIRT).name(ok(ev.improved()) + " &eTierra mejorada")
                .lore(def.improvedSoil() ? "&7Requiere tierra mejorada" : "&7No la requiere",
                        "&7Esta tierra: " + (soil.improved ? "&aMejorada" : "&7Normal")).build());

        double eta = plugin.crops().etaMinutes(c, def, ev);
        String etaText = c.progress >= 1.0 ? "&aListo" : eta < 0 ? "&cDetenido: falta algún requisito"
                : "&f~" + Util.fmt(eta) + " minutos";
        set(16, new ItemBuilder(Material.SPYGLASS).name("&eTiempo restante").lore(etaText,
                "&7Calidad estimada: &fNivel " + Util.clamp((int) Math.round(1 + 9 * Math.pow(c.avgScore(), 1.5)), 1, 10),
                "&7Marchitez: " + Util.bar(100 - Math.min(1, c.wither) * 100, 10)).build());
        set(22, new ItemBuilder(Material.BARRIER).name(plugin.lang().get("menu.close")).build(),
                click -> viewer.closeInventory());
        fillEmpty();
    }
}
