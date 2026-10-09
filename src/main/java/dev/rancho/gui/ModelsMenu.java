package dev.rancho.gui;

import dev.rancho.RanchoPlugin;
import dev.rancho.animal.Species;
import dev.rancho.crop.CropDef;
import dev.rancho.food.Food;
import dev.rancho.item.CustomItems;
import dev.rancho.quality.Products;
import dev.rancho.util.ItemBuilder;
import dev.rancho.util.Text;
import dev.rancho.util.Util;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** Admin: asigna el CustomModelData (resource pack) de cada item, comida, semilla y producto. */
public final class ModelsMenu extends Menu {

    private static final int PER_PAGE = 45;

    private final Menu parent;
    private int page = 0;

    public ModelsMenu(RanchoPlugin plugin, Menu parent) {
        super(plugin, "&6Modelos (resource pack)", 6);
        this.parent = parent;
    }

    /** Item de muestra para una clave de modelo (o null si no existe). */
    public static ItemStack sample(RanchoPlugin plugin, String key) {
        int i = key.indexOf(':');
        if (i < 0) {
            return null;
        }
        String type = key.substring(0, i);
        String id = key.substring(i + 1);
        switch (type) {
            case "item":
                return CustomItems.get(id) == null ? null : CustomItems.create(id, 1);
            case "food": {
                Food f = plugin.foods().get(id);
                return f == null ? null : plugin.food().create(f, 5, 1, System.currentTimeMillis(), 1.0);
            }
            case "seed": {
                CropDef c = plugin.cropDefs().get(id);
                return c == null ? null : CustomItems.createSeed(c, 1);
            }
            case "crop": {
                CropDef c = plugin.cropDefs().get(id);
                return c == null ? null : Products.create("crop", id, c.productName(), c.productMaterial(), 5, c.price(), 1);
            }
            case "meat": {
                Species s = plugin.species().get(id);
                return s == null ? null : Products.create("meat", id, s.meatName(), s.meatMaterial(), 5, s.meatPrice(), 1);
            }
            case "leather": {
                Species s = plugin.species().get(id);
                return s == null ? null : Products.create("leather", id, s.leatherName(), Material.LEATHER, 5, s.leatherPrice(), 1);
            }
            case "produce": {
                Species s = plugin.species().get(id);
                return s == null ? null : Products.create(s.produceType(), id, s.produceName(), s.produceMaterial(), 5,
                        s.producePrice(), 1);
            }
            default:
                return null;
        }
    }

    @Override
    protected void build() {
        List<String> keys = plugin.models().allKeys();
        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < keys.size(); i++) {
            String key = keys.get(start + i);
            ItemStack base = sample(plugin, key);
            ItemStack icon = base == null ? new ItemBuilder(Material.PAPER).name("&e" + key).build() : base.clone();
            ItemMeta meta = icon.getItemMeta();
            List<String> lore = meta.getLore() == null ? new ArrayList<>() : new ArrayList<>(meta.getLore());
            lore.add("");
            lore.add(Text.color("&6Clave: &f" + key));
            int cur = plugin.models().get(key);
            lore.add(Text.color("&6CustomModelData: &f" + (cur > 0 ? String.valueOf(cur) : "ninguno")));
            lore.add(Text.color("&8Click izq.: cambiar (chat) · Click der.: quitar"));
            lore.add(Text.color("&8Shift+izq.: recibir una muestra"));
            meta.setLore(lore);
            icon.setItemMeta(meta);
            set(i, icon, click -> {
                if (click.isRightClick()) {
                    plugin.models().set(key, 0);
                    viewer.sendMessage(plugin.lang().prefixed("model.reset", "key", key));
                    refresh();
                } else if (click.isShiftClick()) {
                    ItemStack sample = sample(plugin, key);
                    if (sample != null) {
                        Util.giveOrDrop(viewer, sample);
                    }
                } else {
                    plugin.prompts().ask(viewer, plugin.lang().prefixed("model.prompt", "key", key), text -> {
                        try {
                            int n = Integer.parseInt(text.trim());
                            plugin.models().set(key, Math.max(0, n));
                            viewer.sendMessage(plugin.lang().prefixed("model.set", "key", key, "value", Math.max(0, n)));
                        } catch (NumberFormatException ex) {
                            viewer.sendMessage(plugin.lang().prefixed("model.invalid"));
                        }
                        open(viewer);
                    });
                }
            });
        }
        set(45, new ItemBuilder(Material.ARROW).name(plugin.lang().get("menu.back")).build(),
                click -> parent.open(viewer));
        set(49, new ItemBuilder(Material.PAINTING).name("&6Cómo funciona")
                .lore("&7Pon el número que uses en tu resource pack", "&7(custom_model_data) para cada item.",
                        "&7También: &f/rancho model hand <número>", "&8Los items ya creados se actualizan al refrescarse.").build());
        if (page > 0) {
            set(48, new ItemBuilder(Material.SPECTRAL_ARROW).name(plugin.lang().get("menu.prev")).build(),
                    click -> { page--; refresh(); });
        }
        if ((page + 1) * PER_PAGE < keys.size()) {
            set(50, new ItemBuilder(Material.SPECTRAL_ARROW).name(plugin.lang().get("menu.next")).build(),
                    click -> { page++; refresh(); });
        }
        fillEmpty();
    }
}
