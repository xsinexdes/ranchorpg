package dev.rancho.gui;

import dev.rancho.RanchoPlugin;
import dev.rancho.crop.CropDef;
import dev.rancho.item.CustomItems;
import dev.rancho.util.ItemBuilder;
import dev.rancho.util.Util;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Tienda: compra de items custom y semillas, paginada. */
public final class ShopMenu extends Menu {

    private static final int PER_PAGE = 45;

    private record Offer(String id, ItemStack stack, double price) {}

    private final Menu parent;
    private int page = 0;

    public ShopMenu(RanchoPlugin plugin, Menu parent) {
        super(plugin, "&eTienda del Rancho", 6);
        this.parent = parent;
    }

    private List<Offer> offers() {
        List<Offer> out = new ArrayList<>();
        for (CustomItems.Def d : CustomItems.all()) {
            double price = plugin.market().shopPrice(d.id());
            if (price > 0) {
                out.add(new Offer(d.id(), CustomItems.create(d.id(), 1), price));
            }
        }
        for (CropDef c : plugin.cropDefs().all()) {
            if (c.custom() && c.enabled()) {
                out.add(new Offer("seed_" + c.id(), CustomItems.createSeed(c, 1), plugin.market().seedPrice(c.id())));
            }
        }
        return out;
    }

    @Override
    protected void build() {
        List<Offer> offers = offers();
        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < offers.size(); i++) {
            Offer o = offers.get(start + i);
            ItemStack icon = o.stack().clone();
            var meta = icon.getItemMeta();
            List<String> lore = meta.getLore() == null ? new ArrayList<>() : new ArrayList<>(meta.getLore());
            lore.add("");
            lore.add(dev.rancho.util.Text.color("&7Precio: &6" + Util.fmt(o.price()) + " &8(Shift: x16)"));
            meta.setLore(lore);
            icon.setItemMeta(meta);
            set(i, icon, click -> buy(o, click.isShiftClick() ? 16 : 1));
        }
        set(45, new ItemBuilder(Material.ARROW).name(plugin.lang().get("menu.back")).build(),
                click -> parent.open(viewer));
        set(49, new ItemBuilder(Material.GOLD_INGOT).name("&6Monedas: &f" + Util.fmt(plugin.economy().balance(viewer))).build());
        if (page > 0) {
            set(48, new ItemBuilder(Material.SPECTRAL_ARROW).name(plugin.lang().get("menu.prev")).build(),
                    click -> { page--; refresh(); });
        }
        if ((page + 1) * PER_PAGE < offers.size()) {
            set(50, new ItemBuilder(Material.SPECTRAL_ARROW).name(plugin.lang().get("menu.next")).build(),
                    click -> { page++; refresh(); });
        }
        fillEmpty();
    }

    private void buy(Offer o, int amount) {
        double total = o.price() * amount;
        if (!plugin.economy().withdraw(viewer, total)) {
            viewer.sendMessage(plugin.lang().prefixed("market.no-money", "price", Util.fmt(total)));
            return;
        }
        ItemStack give = o.stack().clone();
        give.setAmount(amount);
        Util.giveOrDrop(viewer, give);
        viewer.sendMessage(plugin.lang().prefixed("market.bought", "amount", amount, "price", Util.fmt(total)));
        refresh();
    }
}
