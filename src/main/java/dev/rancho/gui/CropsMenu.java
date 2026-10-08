package dev.rancho.gui;

import dev.rancho.RanchoPlugin;
import dev.rancho.crop.CropDef;
import dev.rancho.gui.SettingsMenu.Entry;
import dev.rancho.item.CustomItems;
import dev.rancho.util.ItemBuilder;
import dev.rancho.util.Util;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Lista paginada de cultivos: editar, crear, obtener semilla o eliminar. */
public final class CropsMenu extends Menu {

    private static final int PER_PAGE = 45;

    private final Menu parent;
    private int page = 0;

    public CropsMenu(RanchoPlugin plugin, Menu parent) {
        super(plugin, "&6Cultivos", 6);
        this.parent = parent;
    }

    /** Entradas editables de un cultivo. */
    public static List<Entry> entries(String id) {
        String p = "crops." + id + ".";
        List<Entry> e = new ArrayList<>();
        e.add(Entry.text(p + "name", "Nombre", Material.NAME_TAG));
        e.add(Entry.bool(p + "enabled", "Habilitado", Material.LEVER));
        e.add(Entry.cycle(p + "block", "Bloque de cultivo", Material.WHEAT,
                List.of("WHEAT", "CARROTS", "POTATOES", "BEETROOTS")));
        e.add(Entry.text(p + "seed-material", "Material de la semilla", Material.WHEAT_SEEDS));
        e.add(Entry.text(p + "product-material", "Material del producto", Material.WHEAT));
        e.add(Entry.text(p + "product-name", "Nombre del producto", Material.NAME_TAG));
        e.add(Entry.num(p + "growth-days", "Días de crecimiento", Material.CLOCK, 0.25, 0.1, 60));
        e.add(Entry.num(p + "temp-min", "Temperatura mínima (°C)", Material.SNOWBALL, 1, -50, 60));
        e.add(Entry.num(p + "temp-max", "Temperatura máxima (°C)", Material.BLAZE_POWDER, 1, -50, 80));
        e.add(Entry.num(p + "light-min", "Nivel de luz mínimo", Material.TORCH, 1, 0, 15));
        e.add(Entry.bool(p + "needs-sun", "Necesita sol directo", Material.SUNFLOWER));
        e.add(Entry.bool(p + "needs-water", "Necesita riego", Material.WATER_BUCKET));
        e.add(Entry.bool(p + "needs-fertilizer", "Necesita fertilizante", Material.BONE_MEAL));
        e.add(Entry.bool(p + "improved-soil", "Necesita tierra mejorada", Material.COARSE_DIRT));
        e.add(Entry.num(p + "price", "Precio base", Material.GOLD_NUGGET, 1, 0, 10000));
        e.add(Entry.num(p + "harvest-min", "Cosecha mínima", Material.WHEAT, 1, 1, 20));
        e.add(Entry.num(p + "harvest-max", "Cosecha máxima", Material.WHEAT, 1, 1, 30));
        String[] seasons = {"spring", "summer", "autumn", "winter"};
        String[] names = {"Primavera", "Verano", "Otoño", "Invierno"};
        for (int i = 0; i < 4; i++) {
            e.add(Entry.bool(p + "season." + seasons[i], "Crece en " + names[i], Material.GRASS_BLOCK));
        }
        return e;
    }

    @Override
    protected void build() {
        List<CropDef> all = plugin.cropDefs().all();
        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < all.size(); i++) {
            CropDef def = all.get(start + i);
            set(i, new ItemBuilder(def.productMaterial())
                            .name((def.enabled() ? "&a" : "&c") + def.name() + " &8(" + def.id() + ")")
                            .lore("&7Bloque: &f" + def.block(), "&7Crecimiento: &f" + def.growthDays() + " días",
                                    "", "&8Click izq.: editar", def.custom() ? "&8Click der.: recibir semilla" : "&8(cultivo vanilla)",
                                    plugin.cropDefs().isDefault(def.id()) ? "&8(por defecto)" : "&8Shift+der.: eliminar").build(),
                    click -> {
                        if (click.isShiftClick() && click.isRightClick()) {
                            if (!plugin.cropDefs().isDefault(def.id())) {
                                plugin.cropDefs().delete(def.id());
                                refresh();
                            }
                        } else if (click.isRightClick()) {
                            if (def.custom()) {
                                Util.giveOrDrop(viewer, CustomItems.createSeed(def, 8));
                            }
                        } else {
                            edit(def).open(viewer);
                        }
                    });
        }
        set(45, new ItemBuilder(Material.ARROW).name(plugin.lang().get("menu.back")).build(),
                click -> parent.open(viewer));
        set(49, new ItemBuilder(Material.NETHER_STAR).name("&aNuevo cultivo")
                        .lore("&7Copia la plantilla del Trigo.", "&8Escribe el id en el chat.").build(),
                click -> plugin.prompts().ask(viewer, plugin.lang().prefixed("prompt.newid"), text -> {
                    String id = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "");
                    if (id.isEmpty() || plugin.cropDefs().get(id) != null) {
                        viewer.sendMessage(plugin.lang().prefixed("prompt.invalid-id"));
                        open(viewer);
                        return;
                    }
                    CropDef def = plugin.cropDefs().create(id, text, "wheat");
                    edit(def).open(viewer);
                }));
        if (page > 0) {
            set(48, new ItemBuilder(Material.SPECTRAL_ARROW).name(plugin.lang().get("menu.prev")).build(),
                    click -> { page--; refresh(); });
        }
        if ((page + 1) * PER_PAGE < all.size()) {
            set(50, new ItemBuilder(Material.SPECTRAL_ARROW).name(plugin.lang().get("menu.next")).build(),
                    click -> { page++; refresh(); });
        }
        fillEmpty();
    }

    private SettingsMenu edit(CropDef def) {
        return new SettingsMenu(plugin, "&6Cultivo: " + def.name(), entries(def.id()), this,
                plugin.cropDefs().cfgPublic(), plugin.cropDefs()::save);
    }
}
