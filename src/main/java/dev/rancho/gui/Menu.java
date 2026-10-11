package dev.rancho.gui;

import dev.rancho.RanchoPlugin;
import dev.rancho.util.ItemBuilder;
import dev.rancho.util.Text;
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
    protected Inventory inventory;
    protected Player viewer;
    private final String title;
    private final int rows;
    /** True si este jugador ve la interfaz personalizada (los paneles de relleno se omiten). */
    protected boolean custom;
    private final Map<Integer, Consumer<InventoryClickEvent>> actions = new HashMap<>();

    protected Menu(RanchoPlugin plugin, String title, int rows) {
        this.plugin = plugin;
        this.title = title;
        this.rows = rows;
    }

    /** Tema visual de la interfaz personalizada: main, admin, farm, kitchen, cask o mill. */
    protected String theme() {
        return "main";
    }

    /** Si es true, el menú se redibuja cada segundo mientras está abierto. */
    public boolean live() {
        return false;
    }

    /** Dibuja el contenido del menú usando {@link #set}. */
    protected abstract void build();

    public final void open(Player player) {
        this.viewer = player;
        this.custom = GuiTitles.customFor(plugin, player);
        this.inventory = GuiTitles.create(plugin, this, rows, title, theme(), custom);
        redraw();
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
    }

    /** Vuelve a dibujar el menú sin cerrarlo. */
    public final void refresh() {
        if (inventory != null) {
            redraw();
        }
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

    /** Color de los paneles de relleno del menú (cada menú puede tener el suyo). */
    protected Material pane() {
        return Material.GRAY_STAINED_GLASS_PANE;
    }

    /** Rellena los huecos vacíos con paneles de cristal. */
    protected final void fillEmpty() {
        if (custom) {
            return; // la interfaz personalizada ya dibuja sus propias casillas
        }
        ItemStack glass = new ItemBuilder(pane()).name(" ").build();
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
