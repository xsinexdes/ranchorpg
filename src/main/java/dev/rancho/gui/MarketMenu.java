package dev.rancho.gui;

import dev.rancho.RanchoPlugin;
import dev.rancho.economy.MarketManager;
import dev.rancho.util.ItemBuilder;
import dev.rancho.util.Util;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/** Mercado: vender productos del inventario, comprar suministros y ver la demanda. */
public final class MarketMenu extends Menu {

    public MarketMenu(RanchoPlugin plugin) {
        super(plugin, "&aMercado del Rancho", 3);
    }

    @Override
    public boolean live() {
        return true;
    }

    @Override
    protected void build() {
        double preview = plugin.market().previewAll(viewer);
        set(10, new ItemBuilder(Material.HOPPER).name("&aVender todo")
                        .lore("&7Vende todos los productos de tu inventario.", "&7Total estimado: &6" + Util.fmt(preview)).build(),
                click -> {
                    plugin.market().sellAll(viewer);
                    refresh();
                });
        set(12, new ItemBuilder(Material.CHEST).name("&eComprar suministros")
                        .lore("&7Pienso, medicinas, fertilizantes, semillas...").build(),
                click -> new ShopMenu(plugin, this).open(viewer));

        List<String> lore = new ArrayList<>();
        lore.add("&7Estación: " + plugin.seasons().name(plugin.seasons().current()));
        for (String g : MarketManager.GROUPS) {
            double d = plugin.market().demand(g);
            lore.add("&7" + g + ": " + (d >= 1.0 ? "&a" : "&c") + "x" + Util.fmt(d * 100 / 100.0));
        }
        set(14, new ItemBuilder(Material.WRITABLE_BOOK).name("&6Demanda actual").lore(lore).build());
        set(16, new ItemBuilder(Material.GOLD_INGOT).name("&6Tus monedas")
                .lore("&f" + Util.fmt(plugin.economy().balance(viewer))
                        + (plugin.economy().usingVault() ? " &8(Vault)" : "")).build());
        set(22, new ItemBuilder(Material.ARROW).name(plugin.lang().get("menu.back")).build(),
                click -> new MainMenu(plugin).open(viewer));
        fillEmpty();
    }
}
