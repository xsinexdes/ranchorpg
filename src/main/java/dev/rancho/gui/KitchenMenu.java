package dev.rancho.gui;

import dev.rancho.RanchoPlugin;
import dev.rancho.food.Food;
import dev.rancho.food.FoodManager;
import dev.rancho.food.KitchenManager;
import dev.rancho.util.ItemBuilder;
import dev.rancho.util.Util;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * Cocina de Rancho: lista de recetas con tus ingredientes, cocción con tiempo y recogida del plato.
 * Con {@code station == null} es solo el libro de cocina (consulta).
 */
public final class KitchenMenu extends Menu {

    private static final int PER_PAGE = 45;

    private final Block station;
    private final Menu parent;
    private int page = 0;

    public KitchenMenu(RanchoPlugin plugin, Block station, Menu parent) {
        super(plugin, titleOf(plugin, station), 6);
        this.station = station;
        this.parent = parent;
    }

    private static String titleOf(RanchoPlugin plugin, Block station) {
        if (station == null) {
            return "&6Libro de recetas";
        }
        String t = plugin.kitchen().typeOf(station);
        if ("cask".equals(t)) return "&5Barrica de Rancho";
        if ("mill".equals(t)) return "&eMolino de Rancho";
        return "&6Cocina de Rancho";
    }

    @Override
    public boolean live() {
        return true;
    }

    private List<Food> recipes() {
        List<Food> out = new ArrayList<>();
        String type = station == null ? null : plugin.kitchen().typeOf(station);
        for (Food f : plugin.foods().all()) {
            if (f.enabled() && f.cookable() && (type == null || f.inStation(type))) {
                out.add(f);
            }
        }
        return out;
    }

    private int cap() {
        return "mill".equals(plugin.kitchen().typeOf(station)) ? 64 : 16;
    }

    @Override
    protected void build() {
        FoodManager fm = plugin.food();
        List<Food> recipes = recipes();
        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < recipes.size(); i++) {
            Food f = recipes.get(start + i);
            List<String> lore = new ArrayList<>();
            lore.add("&7Resultado: &f" + f.outAmount() + "x " + f.name());
            if (station == null) {
                lore.add("&7Se prepara en: &f" + String.join(", ", f.stations()));
            }
            if (f.ageMinutes() > 0) {
                lore.add("&7Mejora +1 nivel cada &f" + Util.fmt0(f.ageMinutes()) + " min &7extra (máx. +" + f.ageMaxBonus() + ")");
            }
            lore.add("&7Cocción: &f" + (f.cookSeconds() <= 0 ? "instantánea" : f.cookSeconds() + " s"));
            lore.add("&7Se pudre en: &f" + FoodManager.fmtTime((long) (f.spoilMinutes() * 60000)));
            if (f.foodPoints() > 0) {
                lore.add("&7Hambre extra: &a+" + f.foodPoints());
            }
            for (Food.Eff e : f.effects()) {
                lore.add("&7Efecto: &b" + fm.effectName(e) + (e.seconds() > 0 ? " &8(" + e.seconds() + "s)" : ""));
            }
            lore.add("");
            lore.add("&7Ingredientes:");
            for (FoodManager.Need n : fm.needs(f)) {
                int have = fm.count(viewer, n);
                boolean ok = have >= n.amount();
                lore.add(" " + (ok ? "&a✔ " : "&c✘ ") + "&f" + fm.tokenName(n.token()) + " x" + n.amount()
                        + " &8(tienes " + have + ")");
            }
            lore.add("");
            lore.add(station == null ? "&8Necesitas una Cocina de Rancho para cocinar."
                    : "&aClick: preparar 1 tanda &8| &aShift: máximo (" + cap() + ")");
            set(i, new ItemBuilder(f.material()).name("&e" + f.name()).lore(lore).build(), click -> {
                if (station == null) {
                    return;
                }
                int batches = click.isShiftClick() ? Math.min(cap(), Math.max(1, fm.maxBatches(viewer, f))) : 1;
                fm.cook(viewer, f, station, batches);
                refresh();
            });
        }

        // estado de la cocina
        if (station != null) {
            KitchenManager.Job job = plugin.kitchen().job(station);
            if (job == null) {
                set(49, new ItemBuilder(Material.CAMPFIRE).name("&aLibre")
                        .lore("&7Elige una receta para empezar.").build());
            } else {
                Food jf = plugin.foods().get(job.foodId);
                String fname = jf == null ? job.foodId : jf.name();
                long left = job.finishAt - System.currentTimeMillis();
                if (left > 0) {
                    set(49, new ItemBuilder(Material.FURNACE).name("&6Cocinando: &f" + fname)
                            .lore("&7Resultado: &f" + job.amount + "x", "&7Tiempo restante: &f"
                                    + Math.max(1, left / 1000) + " s").build());
                } else {
                    set(49, new ItemBuilder(Material.LIME_DYE).name("&a¡" + fname + " listo!")
                                    .lore("&7Click para recoger &f" + job.amount + "x", "&7Calidad: &fNivel " + job.quality).build(),
                            click -> {
                                fm.collect(viewer, station);
                                refresh();
                            });
                }
            }
        }

        set(45, new ItemBuilder(Material.ARROW).name(plugin.lang().get(parent == null ? "menu.close" : "menu.back")).build(),
                click -> {
                    if (parent == null) {
                        viewer.closeInventory();
                    } else {
                        parent.open(viewer);
                    }
                });
        if (page > 0) {
            set(48, new ItemBuilder(Material.SPECTRAL_ARROW).name(plugin.lang().get("menu.prev")).build(),
                    click -> { page--; refresh(); });
        }
        if ((page + 1) * PER_PAGE < recipes.size()) {
            set(50, new ItemBuilder(Material.SPECTRAL_ARROW).name(plugin.lang().get("menu.next")).build(),
                    click -> { page++; refresh(); });
        }
        fillEmpty();
    }
}
