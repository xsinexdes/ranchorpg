package dev.rancho.gui;

import dev.rancho.RanchoPlugin;
import dev.rancho.crop.CropDef;
import dev.rancho.item.CustomItems;
import dev.rancho.util.ItemBuilder;
import dev.rancho.util.Text;
import dev.rancho.util.Util;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Panel admin: entrega cualquier item custom (incluidas las semillas). */
public final class ItemsMenu extends Menu {

    private static final int PER_PAGE = 45;

    private final Menu parent;
    private int page = 0;

    public ItemsMenu(RanchoPlugin plugin, Menu parent) {
        super(plugin, "&6Items de Rancho", 6);
        this.parent = parent;
    }

    private List<ItemStack> items() {
        List<ItemStack> out = new ArrayList<>();
        for (CustomItems.Def d : CustomItems.all()) {
            out.add(CustomItems.create(d.id(), 1));
        }
        for (CropDef c : plugin.cropDefs().all()) {
            if (c.custom()) {
                out.add(CustomItems.createSeed(c, 1));
            }
        }
        return out;
    }

    @Override
    protected void build() {
        List<ItemStack> items = items();
        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < items.size(); i++) {
            ItemStack base = items.get(start + i);
            ItemStack icon = base.clone();
            var meta = icon.getItemMeta();
            List<String> lore = meta.getLore() == null ? new ArrayList<>() : new ArrayList<>(meta.getLore());
            lore.add("");
            lore.add(Text.color("&8Click: 1 · Shift: 16"));
            meta.setLore(lore);
            icon.setItemMeta(meta);
            set(i, icon, click -> {
                ItemStack give = base.clone();
                give.setAmount(click.isShiftClick() ? 16 : 1);
                Util.giveOrDrop(viewer, give);
            });
        }
        set(45, new ItemBuilder(Material.ARROW).name(plugin.lang().get("menu.back")).build(),
                click -> parent.open(viewer));
        if (page > 0) {
            set(48, new ItemBuilder(Material.SPECTRAL_ARROW).name(plugin.lang().get("menu.prev")).build(),
                    click -> { page--; refresh(); });
        }
        if ((page + 1) * PER_PAGE < items.size()) {
            set(50, new ItemBuilder(Material.SPECTRAL_ARROW).name(plugin.lang().get("menu.next")).build(),
                    click -> { page++; refresh(); });
        }
        fillEmpty();
    }
}
