package dev.rancho.gui;

import dev.rancho.RanchoPlugin;
import dev.rancho.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Elige entre la interfaz personalizada (fondo medieval pintado en el resource pack, solo Paper) y el
 * inventario normal de siempre, según la configuración y si el jugador tiene el pack cargado.
 */
public final class GuiTitles {

    private static final boolean PAPER = classExists("net.kyori.adventure.text.Component");

    private GuiTitles() {}

    private static boolean classExists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    /** ¿Se debe usar la interfaz personalizada para este jugador? */
    public static boolean customFor(RanchoPlugin plugin, Player p) {
        if (!PAPER || !plugin.getConfig().getBoolean("visuals.gui", true)) {
            return false;
        }
        return plugin.getConfig().getBoolean("visuals.gui-for-all", false)
                || plugin.visuals().hasPack(p.getUniqueId());
    }

    static Inventory create(RanchoPlugin plugin, InventoryHolder holder, int rows, String legacyTitle, String theme,
                            boolean custom) {
        String colored = Text.color(legacyTitle);
        if (custom && GuiGlyphs.supported(theme, rows)) {
            try {
                return PaperGui.create(holder, rows, net.md_5.bungee.api.ChatColor.stripColor(colored), theme);
            } catch (Throwable ex) {
                plugin.getLogger().warning("No se pudo crear la interfaz personalizada: " + ex);
            }
        }
        return Bukkit.createInventory(holder, rows * 9, colored);
    }
}
