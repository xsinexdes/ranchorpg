package dev.rancho.gui;

import dev.rancho.RanchoPlugin;
import dev.rancho.food.Food;
import dev.rancho.gui.SettingsMenu.Entry;
import dev.rancho.util.ItemBuilder;
import dev.rancho.util.Util;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Admin: lista paginada de comidas y recetas; editar, crear, probar o eliminar. */
public final class FoodsMenu extends Menu {

    private static final int PER_PAGE = 45;

    private final Menu parent;
    private int page = 0;

    public FoodsMenu(RanchoPlugin plugin, Menu parent) {
        super(plugin, "&6Comidas y recetas", 6);
        this.parent = parent;
    }

    public static List<Entry> entries(String id) {
        String p = "foods." + id + ".";
        List<Entry> e = new ArrayList<>();
        e.add(Entry.text(p + "name", "Nombre", Material.NAME_TAG));
        e.add(Entry.bool(p + "enabled", "Habilitada", Material.LEVER));
        e.add(Entry.text(p + "material", "Material del item", Material.BREAD));
        e.add(Entry.num(p + "food-points", "Hambre extra al comer", Material.COOKED_BEEF, 1, 0, 20));
        e.add(Entry.num(p + "saturation", "Saturación extra", Material.GOLDEN_CARROT, 0.5, 0, 20));
        e.add(Entry.num(p + "spoil-minutes", "Minutos reales hasta pudrirse", Material.ROTTEN_FLESH, 15, 1, 100000));
        e.add(Entry.num(p + "out-amount", "Unidades por tanda", Material.CHEST, 1, 1, 64));
        e.add(Entry.num(p + "cook-seconds", "Segundos de cocción (0 = instantánea)", Material.CLOCK, 5, 0, 600));
        e.add(Entry.text(p + "station", "Estación (kitchen, cask, mill; separadas por coma)", Material.SMOKER));
        e.add(Entry.num(p + "age-minutes", "Barrica: minutos para +1 nivel (0 = no mejora)", Material.CLOCK, 5, 0, 10000));
        e.add(Entry.num(p + "age-max-bonus", "Barrica: bonus máximo de niveles", Material.EXPERIENCE_BOTTLE, 1, 0, 9));
        e.add(Entry.text(p + "group", "Grupo (ej. fish) para recetas group:fish", Material.NAME_TAG));
        e.add(Entry.num(p + "fish-weight", "Pesca: peso (0 = no es pez)", Material.FISHING_ROD, 1, 0, 200));
        e.add(Entry.text(p + "fish-seasons", "Pesca: estaciones (spring,summer,autumn,winter)", Material.CLOCK));
        e.add(Entry.cycle(p + "fish-env", "Pesca: agua", Material.WATER_BUCKET, List.of("ANY", "FRESH", "SALT")));
        e.add(Entry.cycle(p + "fish-time", "Pesca: hora", Material.DAYLIGHT_DETECTOR, List.of("ANY", "DAY", "NIGHT")));
        e.add(Entry.text(p + "ingredients", "Ingredientes (MATERIAL:n, food:id:n, item:id:n)", Material.WHEAT));
        e.add(Entry.text(p + "effects", "Efectos (clave:segundos:potencia, ...)", Material.POTION));
        return e;
    }

    @Override
    protected void build() {
        List<Food> all = plugin.foods().all();
        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < all.size(); i++) {
            Food f = all.get(start + i);
            set(i, new ItemBuilder(f.material()).name((f.enabled() ? "&a" : "&c") + f.name() + " &8(" + f.id() + ")")
                            .lore("&7Ingredientes: &f" + (f.ingredients().isEmpty() ? "ninguno" : String.join(", ", f.ingredients())),
                                    "&7Efectos: &f" + (f.effects().isEmpty() ? "ninguno" : f.effects().size()),
                                    "", "&8Click izq.: editar", "&8Click der.: recibir una de prueba",
                                    plugin.foods().isDefault(f.id()) ? "&8(por defecto)" : "&8Shift+der.: eliminar").build(),
                    click -> {
                        if (click.isShiftClick() && click.isRightClick()) {
                            if (!plugin.foods().isDefault(f.id())) {
                                plugin.foods().delete(f.id());
                                refresh();
                            }
                        } else if (click.isRightClick()) {
                            Util.giveOrDrop(viewer, plugin.food().create(f, 5, 4, System.currentTimeMillis(), 1.0));
                        } else {
                            edit(f).open(viewer);
                        }
                    });
        }
        set(45, new ItemBuilder(Material.ARROW).name(plugin.lang().get("menu.back")).build(),
                click -> parent.open(viewer));
        set(49, new ItemBuilder(Material.NETHER_STAR).name("&aNueva comida")
                        .lore("&7Copia la plantilla del Pan Rústico.", "&8Escribe el id en el chat.").build(),
                click -> plugin.prompts().ask(viewer, plugin.lang().prefixed("prompt.newid"), text -> {
                    String id = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "");
                    if (id.isEmpty() || plugin.foods().get(id) != null) {
                        viewer.sendMessage(plugin.lang().prefixed("prompt.invalid-id"));
                        open(viewer);
                        return;
                    }
                    Food f = plugin.foods().create(id, text, "bread_rustic");
                    edit(f).open(viewer);
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

    private SettingsMenu edit(Food f) {
        return new SettingsMenu(plugin, "&6Comida: " + f.name(), entries(f.id()), this,
                plugin.foods().cfgPublic(), plugin.foods()::save);
    }
}
