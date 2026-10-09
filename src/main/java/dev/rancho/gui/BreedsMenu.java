package dev.rancho.gui;

import dev.rancho.RanchoPlugin;
import dev.rancho.animal.Breed;
import dev.rancho.animal.Gene;
import dev.rancho.animal.Species;
import dev.rancho.gui.SettingsMenu.Entry;
import dev.rancho.util.ItemBuilder;
import dev.rancho.util.Util;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Admin: razas de cada especie (rasgos, producción, genes). Editar, crear, generar o eliminar. */
public final class BreedsMenu extends Menu {

    private static final int PER_PAGE = 45;

    private final Menu parent;
    private int page = 0;

    public BreedsMenu(RanchoPlugin plugin, Menu parent) {
        super(plugin, "&6Razas", 6);
        this.parent = parent;
    }

    public static List<Entry> entries(RanchoPlugin plugin, String id) {
        String p = "breeds." + id + ".";
        List<Entry> e = new ArrayList<>();
        e.add(Entry.text(p + "name", "Nombre", Material.NAME_TAG));
        e.add(Entry.bool(p + "enabled", "Habilitada", Material.LEVER));
        e.add(Entry.cycle(p + "species", "Especie", Material.EGG, new ArrayList<>(plugin.species().ids())));
        e.add(Entry.num(p + "weight", "Frecuencia al aparecer", Material.COMPARATOR, 5, 0, 100));
        e.add(Entry.num(p + "prod-mult", "Multiplicador de producción", Material.MILK_BUCKET, 0.05, 0.1, 5));
        e.add(Entry.num(p + "meat-mult", "Multiplicador de carne y cuero", Material.COOKED_BEEF, 0.05, 0.1, 5));
        e.add(Entry.num(p + "growth-mult", "Multiplicador de crecimiento", Material.CLOCK, 0.05, 0.1, 5));
        for (Gene g : Gene.values()) {
            e.add(Entry.num(p + "bonus." + g.name(), "Bono de " + g.label(), g.icon(), 1, -50, 50));
        }
        return e;
    }

    @Override
    protected void build() {
        List<Breed> all = plugin.breeds().all();
        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < all.size(); i++) {
            Breed b = all.get(start + i);
            Species sp = plugin.species().get(b.speciesId());
            set(i, new ItemBuilder(sp == null ? Material.EGG : Util.mat(sp.entity().name() + "_SPAWN_EGG", Material.EGG))
                            .name((b.enabled() ? "&a" : "&c") + b.name() + " &8(" + b.id() + ")")
                            .lore("&7Especie: &f" + (sp == null ? b.speciesId() : sp.name()),
                                    "&7Producción x" + b.prodMult() + " · Carne x" + b.meatMult(), "",
                                    "&8Click izq.: editar", "&8Click der.: generar uno aquí",
                                    plugin.breeds().isDefault(b.id()) ? "&8(por defecto)" : "&8Shift+der.: eliminar").build(),
                    click -> {
                        if (click.isShiftClick() && click.isRightClick()) {
                            if (!plugin.breeds().isDefault(b.id())) {
                                plugin.breeds().delete(b.id());
                                refresh();
                            }
                        } else if (click.isRightClick()) {
                            if (sp != null) {
                                plugin.animals().spawn(sp, viewer.getLocation(), b.id());
                            }
                        } else {
                            edit(b).open(viewer);
                        }
                    });
        }
        set(45, new ItemBuilder(Material.ARROW).name(plugin.lang().get("menu.back")).build(),
                click -> parent.open(viewer));
        set(49, new ItemBuilder(Material.NETHER_STAR).name("&aNueva raza")
                        .lore("&7Copia la plantilla de la Holstein.", "&8Escribe el id en el chat.").build(),
                click -> plugin.prompts().ask(viewer, plugin.lang().prefixed("prompt.newid"), text -> {
                    String id = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "");
                    if (id.isEmpty() || plugin.breeds().get(id) != null) {
                        viewer.sendMessage(plugin.lang().prefixed("prompt.invalid-id"));
                        open(viewer);
                        return;
                    }
                    Breed b = plugin.breeds().create(id, text, "holstein");
                    edit(b).open(viewer);
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

    private SettingsMenu edit(Breed b) {
        return new SettingsMenu(plugin, "&6Raza: " + b.name(), entries(plugin, b.id()), this,
                plugin.breeds().cfgPublic(), plugin.breeds()::save);
    }
}
