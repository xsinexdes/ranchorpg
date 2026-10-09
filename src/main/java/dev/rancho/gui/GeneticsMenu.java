package dev.rancho.gui;

import dev.rancho.RanchoPlugin;
import dev.rancho.animal.AnimalData;
import dev.rancho.animal.Gene;
import dev.rancho.animal.Mutation;
import dev.rancho.animal.Mutations;
import dev.rancho.animal.Species;
import dev.rancho.util.ItemBuilder;
import dev.rancho.util.Util;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/** Registro genético completo: genes exactos, mutaciones y árbol de padres y abuelos. */
public final class GeneticsMenu extends Menu {

    private static final String[] ROLES = {"Madre", "Padre", "Abuela materna", "Abuelo materno", "Abuela paterna",
            "Abuelo paterno"};

    private final AnimalData d;
    private final Species sp;

    public GeneticsMenu(RanchoPlugin plugin, AnimalData d, Species sp) {
        super(plugin, "&9Genética de " + d.name, 6);
        this.d = d;
        this.sp = sp;
    }

    @Override
    protected void build() {
        set(4, new ItemBuilder(Material.ENCHANTED_BOOK).name("&9" + d.name + " &7- " + sp.name())
                .lore("&7Análisis genético completo.").build());

        Gene[] genes = Gene.values();
        for (int i = 0; i < genes.length; i++) {
            Gene g = genes[i];
            int base = d.genes[g.ordinal()];
            int eff = d.eff(g);
            set(10 + i, new ItemBuilder(g.icon()).name("&e" + g.label())
                    .lore("&7Base: &f" + base, "&7Bono de mutaciones: " + (eff - base >= 0 ? "&a+" : "&c") + (eff - base),
                            "&7Efectivo: &f" + eff + " " + Util.rank(eff), Util.bar(eff, 12)).build());
        }

        int slot = 19;
        for (String id : d.mutations) {
            Mutation m = Mutations.get(id);
            if (m == null || slot > 25) {
                continue;
            }
            List<String> lore = new ArrayList<>();
            lore.add("&7Rareza: " + m.rarity().color() + m.rarity().label());
            if (m.gene() != null) {
                lore.add("&7" + m.gene().label() + ": " + (m.delta() > 0 ? "&a+" : "&c") + m.delta());
            } else {
                lore.add("&7Efecto especial");
            }
            set(slot++, new ItemBuilder(m.positive() ? Material.LIME_DYE : Material.RED_DYE)
                    .name(m.display()).lore(lore).build());
        }
        if (d.mutations.isEmpty()) {
            set(19, new ItemBuilder(Material.GRAY_DYE).name("&7Sin mutaciones").build());
        }

        set(28, new ItemBuilder(Material.OAK_SIGN).name("&6Linaje").lore("&7Padres y abuelos de este animal.").build());
        int[] slots = {37, 38, 40, 41, 43, 44};
        for (int i = 0; i < ROLES.length; i++) {
            String raw = i < d.lineage.size() ? d.lineage.get(i) : "";
            set(slots[i], lineageItem(ROLES[i], raw));
        }
        set(49, new ItemBuilder(Material.ARROW).name(plugin.lang().get("menu.back")).build(),
                click -> new AnimalMenu(plugin, d, sp).open(viewer));
        fillEmpty();
    }

    private org.bukkit.inventory.ItemStack lineageItem(String role, String raw) {
        if (raw == null || raw.isBlank()) {
            return new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("&7" + role + ": desconocido").build();
        }
        String[] p = raw.split("\\|", -1);
        List<String> lore = new ArrayList<>();
        Species s = p.length > 1 ? plugin.species().get(p[1]) : null;
        lore.add("&7Especie: &f" + (s == null ? (p.length > 1 ? p[1] : "?") : s.name()));
        lore.add("&7Nivel: &f" + (p.length > 2 ? p[2] : "?"));
        if (p.length > 3 && !p[3].isBlank()) {
            lore.add("&7Mutaciones heredadas:");
            for (String id : p[3].split(",")) {
                Mutation m = Mutations.get(id.trim());
                if (m != null) {
                    lore.add(" " + m.display());
                }
            }
        }
        return new ItemBuilder(Material.PAPER).name("&e" + role + ": &f" + p[0]).lore(lore).build();
    }
}
