package dev.rancho.gui;

import dev.rancho.RanchoPlugin;
import dev.rancho.util.ItemBuilder;
import dev.rancho.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/** Base de todos los menús GUI: cada slot puede tener una acción asociada. */
public abstract class Menu implements InventoryHolder {

    protected final RanchoPlugin plugin;
    protected final Inventory inventory;
    protected Player viewer;
    private final Map<Integer, Consumer<InventoryClickEvent>> actions = new HashMap<>();

    protected Menu(RanchoPlugin plugin, String title, int rows) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, rows * 9, Text.color(title));
    }

    /** Si es true, el menú se redibuja cada segundo mientras está abierto. */
    public boolean live() {
        return false;
    }

    /** Dibuja el contenido del menú usando {@link #set}. */
    protected abstract void build();

    public final void open(Player player) {
        this.viewer = player;
        redraw();
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
    }

    /** Vuelve a dibujar el menú sin cerrarlo. */
    public final void refresh() {
        redraw();
    }

    private void redraw() {
        inventory.clear();
        actions.clear();
        build();
    }

    protected final void set(int slot, ItemStack item) {
        inventory.setItem(slot, item);
    }

    protected final void set(int slot, ItemStack item, Consumer<InventoryClickEvent> action) {
        inventory.setItem(slot, item);
        actions.put(slot, action);
    }

    /** Rellena los huecos vacíos con paneles de cristal. */
    protected final void fillEmpty() {
        ItemStack glass = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, glass);
            }
        }
    }

    final void handleClick(InventoryClickEvent event) {
        Consumer<InventoryClickEvent> action = actions.get(event.getRawSlot());
        if (action != null) {
            if (event.getWhoClicked() instanceof Player p) {
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.0f);
            }
            action.accept(event);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
