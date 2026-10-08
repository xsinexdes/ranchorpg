package dev.rancho.gui;

import dev.rancho.RanchoPlugin;
import dev.rancho.animal.AnimalData;
import dev.rancho.animal.AnimalManager;
import dev.rancho.animal.Disease;
import dev.rancho.animal.Gene;
import dev.rancho.animal.Mutation;
import dev.rancho.animal.Mutations;
import dev.rancho.animal.Species;
import dev.rancho.animal.Stage;
import dev.rancho.util.ItemBuilder;
import dev.rancho.util.Util;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/** Menú con TODAS las estadísticas de un animal. Se actualiza en tiempo real. */
public final class AnimalMenu extends Menu {

    private final AnimalData d;
    private final Species sp;

    public AnimalMenu(RanchoPlugin plugin, AnimalData d, Species sp) {
        super(plugin, "&6" + d.name + " &7- " + sp.name(), 5);
        this.d = d;
        this.sp = sp;
    }

    @Override
    public boolean live() {
        return true;
    }

    @Override
    protected void build() {
        if (d.entity == null || !d.entity.isValid()) {
            viewer.closeInventory();
            return;
        }
        Stage st = d.stage(sp);
        AnimalManager.Env env = plugin.animals().inspect(d, sp);
        double hp = d.entity.getHealth();
        double maxHp = d.entity.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH) == null ? 20
                : d.entity.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
        double hpPct = hp / Math.max(1, maxHp) * 100;

        set(4, new ItemBuilder(Util.mat(sp.entity().name() + "_SPAWN_EGG", Material.EGG))
                .name("&6" + d.name + " &7[Nv &e" + d.level + "&7]")
                .lore("&7Especie: &f" + sp.name(), "&7Etapa: " + st.color() + st.label(),
                        "&7Sexo: " + (d.female ? "&dHembra" : "&bMacho"),
                        d.pregnant ? "&dGestando: &f" + Util.fmt(d.gestation) + " días" : "&7No gestante").build());

        set(10, stat(Material.WHEAT, "&eSaciedad", d.hunger));
        set(11, stat(Material.WATER_BUCKET, "&bSed (hidratación)", d.thirst));
        set(12, stat(Material.HONEY_BOTTLE, "&dFelicidad", d.happiness));
        set(13, stat(Material.SPONGE, "&6Higiene", d.hygiene));
        set(14, new ItemBuilder(Material.GLISTERING_MELON_SLICE).name("&cSalud")
                .lore(Util.bar(hpPct, 12) + " &f" + Util.fmt(hp) + "&7/&f" + Util.fmt(maxHp)).build());
        set(15, new ItemBuilder(Material.EXPERIENCE_BOTTLE).name("&aNivel " + d.level)
                .lore("&7Calidad de productos: &f" + plugin.animals().quality(d, sp) + "&7/10",
                        "&7Alimentación: " + Util.bar(d.diet, 10),
                        "&7Cuidado general: " + Util.bar(d.care, 10)).build());
        double next = st == Stage.BABY ? sp.babyDays() : st == Stage.YOUNG ? sp.adultDays()
                : st == Stage.ADULT ? sp.oldDays() : sp.lifespan();
        set(16, new ItemBuilder(Material.CLOCK).name("&bEdad")
                .lore("&7Edad: &f" + Util.fmt(d.age) + " días", "&7Siguiente etapa en: &f"
                        + Util.fmt(Math.max(0, next - d.age)) + " días").build());

        // genética (rangos; los números exactos requieren Análisis Genético)
        List<String> genes = new ArrayList<>();
        for (Gene g : Gene.values()) {
            int v = d.eff(g);
            genes.add("&7" + g.label() + ": " + (d.analyzed ? "&f" + v + " " : "") + Util.rank(v));
        }
        set(19, new ItemBuilder(Material.AMETHYST_SHARD).name("&5Genética")
                .lore(genes).build());

        List<String> muts = new ArrayList<>();
        if (d.mutations.isEmpty()) {
            muts.add("&7Sin mutaciones");
        }
        for (String id : d.mutations) {
            Mutation m = Mutations.get(id);
            if (m != null) {
                muts.add(m.display() + " &8(" + m.rarity().label() + ")");
            }
        }
        set(20, new ItemBuilder(Material.FERMENTED_SPIDER_EYE).name("&dMutaciones").lore(muts).build());

        if (d.disease == Disease.NONE) {
            set(21, new ItemBuilder(Material.LIME_DYE).name("&aSano")
                    .lore(d.immunity > 0 ? "&7Inmune: &f" + Util.fmt(d.immunity) + " días" : "&7Sin inmunidad").build());
        } else {
            set(21, new ItemBuilder(Material.POISONOUS_POTATO).name("&2" + d.disease.label())
                    .lore("&7Pierde salud y crece más lento.", "&7Cura: &f" + Util.title(d.disease.cure())).build());
        }

        List<String> prod = new ArrayList<>();
        if (sp.produce().equals("NONE")) {
            prod.add("&7Esta especie no produce de forma continua.");
        } else {
            prod.add("&7Producto: &f" + sp.produceName());
            prod.add("&7Carga: " + Util.bar(d.charge, 10) + " &f" + Util.fmt0(d.charge) + "%");
            prod.add("&7Producción estacional: " + (sp.producesIn(plugin.seasons().current()) ? "&aSí" : "&cNo"));
        }
        prod.add("&7Calidad estimada: &fNivel " + plugin.animals().quality(d, sp));
        prod.add("&7Carne estimada: &f" + sp.meatMin() + "-" + sp.meatMax() + " x " + sp.meatName());
        set(22, new ItemBuilder(sp.produceMaterial()).name("&6Producción estimada").lore(prod).build());

        set(23, new ItemBuilder(Material.EGG).name("&dReproducción")
                .lore("&7Descanso: &f" + Util.fmt(Math.max(0, d.breedCooldown)) + " días",
                        "&7Temporada de cría: " + (sp.breedsIn(plugin.seasons().current()) ? "&aSí" : "&cNo"),
                        "&8Usa un Suplemento de Cría con pareja cerca.").build());

        set(24, new ItemBuilder(Material.COMPASS).name("&bEntorno")
                .lore("&7Temperatura: &f" + Util.fmt(env.temp()) + "°C (límite " + Util.fmt0(sp.coldLimit())
                                + " a " + Util.fmt0(sp.heatLimit()) + ")",
                        "&7Refugio: " + (env.sheltered() ? "&aSí" : "&cNo"),
                        "&7Agua cerca: " + (env.water() ? "&aSí" : "&cNo"),
                        "&7Animales cerca: &f" + env.same() + "&7/&f" + sp.maxDensity(),
                        "&7Estiércol cerca: &f" + env.manure()).build());

        List<String> needs = new ArrayList<>();
        needs.addAll(plugin.animals().needs(d, sp));
        set(25, new ItemBuilder(Material.PAPER).name("&6Lo que necesita ahora mismo").lore(needs).build());

        set(31, new ItemBuilder(Material.ENCHANTED_BOOK).name("&9Registro genético")
                .lore(d.analyzed ? "&7Click para ver genes, mutaciones y linaje."
                        : "&cRequiere un Análisis Genético (úsalo sobre el animal).").build(),
                click -> {
                    if (d.analyzed) {
                        new GeneticsMenu(plugin, d, sp).open(viewer);
                    }
                });
        set(40, new ItemBuilder(Material.BARRIER).name(plugin.lang().get("menu.close")).build(),
                click -> viewer.closeInventory());
        fillEmpty();
    }

    private org.bukkit.inventory.ItemStack stat(Material m, String name, double v) {
        return new ItemBuilder(m).name(name).lore(Util.bar(v, 12) + " &f" + Util.fmt0(v) + "%").build();
    }
}
