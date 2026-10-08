package dev.rancho.gui;

import dev.rancho.RanchoPlugin;
import dev.rancho.animal.Species;
import dev.rancho.gui.SettingsMenu.Entry;
import dev.rancho.util.ItemBuilder;
import dev.rancho.util.Util;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Lista paginada de especies: editar, crear, generar o eliminar. */
public final class SpeciesMenu extends Menu {

    private static final int PER_PAGE = 45;

    public static final List<String> ENTITY_TYPES = List.of("COW", "SHEEP", "PIG", "CHICKEN", "HORSE", "RABBIT",
            "GOAT", "LLAMA", "DONKEY", "MULE", "CAMEL", "MOOSHROOM");

    private final Menu parent;
    private int page = 0;

    public SpeciesMenu(RanchoPlugin plugin, Menu parent) {
        super(plugin, "&6Especies de ganado", 6);
        this.parent = parent;
    }

    /** Entradas editables de una especie. */
    public static List<Entry> entries(String id) {
        String p = "species." + id + ".";
        List<Entry> e = new ArrayList<>();
        e.add(Entry.text(p + "name", "Nombre", Material.NAME_TAG));
        e.add(Entry.cycle(p + "entity", "Tipo de entidad", Material.EGG, ENTITY_TYPES));
        e.add(Entry.bool(p + "enabled", "Habilitada", Material.LEVER));
        e.add(Entry.num(p + "base-health", "Vida base", Material.GOLDEN_APPLE, 1, 1, 200));
        e.add(Entry.num(p + "baby-days", "Días como cría", Material.CLOCK, 0.25, 0.1, 60));
        e.add(Entry.num(p + "adult-days", "Edad adulta (días)", Material.CLOCK, 0.5, 0.2, 100));
        e.add(Entry.num(p + "old-days", "Edad de vejez (días)", Material.CLOCK, 1, 1, 300));
        e.add(Entry.num(p + "lifespan-days", "Esperanza de vida (días)", Material.CLOCK, 1, 1, 400));
        e.add(Entry.num(p + "gestation-days", "Gestación (días)", Material.EGG, 0.25, 0.05, 30));
        e.add(Entry.num(p + "litter-min", "Crías mínimas", Material.EGG, 1, 1, 10));
        e.add(Entry.num(p + "litter-max", "Crías máximas", Material.EGG, 1, 1, 12));
        e.add(Entry.num(p + "max-density", "Animales máx. cerca (espacio)", Material.OAK_FENCE, 1, 1, 50));
        e.add(Entry.num(p + "hunger-rate", "Hambre por día", Material.WHEAT, 5, 0, 500));
        e.add(Entry.num(p + "thirst-rate", "Sed por día", Material.WATER_BUCKET, 5, 0, 500));
        e.add(Entry.num(p + "hygiene-rate", "Suciedad por día", Material.SPONGE, 2, 0, 200));
        e.add(Entry.num(p + "cold-limit", "Límite de frío (°C)", Material.SNOWBALL, 1, -50, 50));
        e.add(Entry.num(p + "heat-limit", "Límite de calor (°C)", Material.BLAZE_POWDER, 1, -10, 80));
        e.add(Entry.bool(p + "grazes", "Pasta en hierba", Material.GRASS_BLOCK));
        e.add(Entry.text(p + "feed", "Comida (Material o id de item)", Material.HAY_BLOCK));
        e.add(Entry.text(p + "meat-name", "Nombre de la carne", Material.COOKED_BEEF));
        e.add(Entry.text(p + "meat-material", "Material de la carne", Material.COOKED_BEEF));
        e.add(Entry.num(p + "meat-min", "Carne mínima", Material.COOKED_BEEF, 1, 0, 20));
        e.add(Entry.num(p + "meat-max", "Carne máxima", Material.COOKED_BEEF, 1, 0, 30));
        e.add(Entry.num(p + "meat-price", "Precio base carne", Material.GOLD_NUGGET, 1, 0, 5000));
        e.add(Entry.text(p + "leather-name", "Nombre del cuero", Material.LEATHER));
        e.add(Entry.num(p + "leather-min", "Cuero mínimo", Material.LEATHER, 1, 0, 20));
        e.add(Entry.num(p + "leather-max", "Cuero máximo", Material.LEATHER, 1, 0, 30));
        e.add(Entry.num(p + "leather-price", "Precio base cuero", Material.GOLD_NUGGET, 1, 0, 5000));
        e.add(Entry.cycle(p + "produce", "Producción (NONE/MILK/WOOL/EGGS)", Material.BUCKET,
                List.of("NONE", "MILK", "WOOL", "EGGS")));
        e.add(Entry.text(p + "produce-name", "Nombre del producto", Material.MILK_BUCKET));
        e.add(Entry.text(p + "produce-material", "Material del producto", Material.MILK_BUCKET));
        e.add(Entry.num(p + "produce-days", "Días entre producciones", Material.CLOCK, 0.1, 0.05, 30));
        e.add(Entry.num(p + "produce-price", "Precio base producto", Material.GOLD_NUGGET, 1, 0, 5000));
        String[] seasons = {"spring", "summer", "autumn", "winter"};
        String[] names = {"Primavera", "Verano", "Otoño", "Invierno"};
        for (int i = 0; i < 4; i++) {
            e.add(Entry.bool(p + "produce." + seasons[i], "Produce en " + names[i], Material.WHEAT));
        }
        for (int i = 0; i < 4; i++) {
            e.add(Entry.bool(p + "breed." + seasons[i], "Se reproduce en " + names[i], Material.EGG));
        }
        for (int i = 0; i < 4; i++) {
            e.add(Entry.bool(p + "suffer." + seasons[i], "Sufre en " + names[i], Material.POISONOUS_POTATO));
        }
        return e;
    }

    @Override
    protected void build() {
        List<Species> all = plugin.species().all();
        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < all.size(); i++) {
            Species sp = all.get(start + i);
            set(i, new ItemBuilder(Util.mat(sp.entity().name() + "_SPAWN_EGG", Material.EGG))
                            .name((sp.enabled() ? "&a" : "&c") + sp.name() + " &8(" + sp.id() + ")")
                            .lore("&7Entidad: &f" + sp.entity(), "", "&8Click izq.: editar",
                                    "&8Click der.: generar uno aquí",
                                    plugin.species().isDefault(sp.id()) ? "&8(especie por defecto)" : "&8Shift+der.: eliminar").build(),
                    click -> {
                        if (click.isShiftClick() && click.isRightClick()) {
                            if (!plugin.species().isDefault(sp.id())) {
                                plugin.species().delete(sp.id());
                                refresh();
                            }
                        } else if (click.isRightClick()) {
                            plugin.animals().spawn(sp, viewer.getLocation());
                        } else {
                            edit(sp).open(viewer);
                        }
                    });
        }
        set(45, new ItemBuilder(Material.ARROW).name(plugin.lang().get("menu.back")).build(),
                click -> parent.open(viewer));
        set(49, new ItemBuilder(Material.NETHER_STAR).name("&aNueva especie")
                        .lore("&7Copia la plantilla de la Vaca.", "&8Escribe el id en el chat.").build(),
                click -> plugin.prompts().ask(viewer, plugin.lang().prefixed("prompt.newid"), text -> {
                    String id = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "");
                    if (id.isEmpty() || plugin.species().get(id) != null) {
                        viewer.sendMessage(plugin.lang().prefixed("prompt.invalid-id"));
                        open(viewer);
                        return;
                    }
                    Species sp = plugin.species().create(id, text, "cow");
                    edit(sp).open(viewer);
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

    private SettingsMenu edit(Species sp) {
        return new SettingsMenu(plugin, "&6Especie: " + sp.name(), entries(sp.id()), this,
                plugin.species().cfgPublic(), plugin.species()::save);
    }
}
