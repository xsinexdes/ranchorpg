package dev.rancho.gui;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Crea inventarios con título en componente (solo Paper): fondo medieval + título con tipografía propia. */
final class PaperGui {

    private PaperGui() {}

    static Inventory create(InventoryHolder holder, int rows, String plainTitle, String theme) {
        Component title = Component.text(GuiGlyphs.prefix(theme, rows))
                .font(Key.key("rancho", "gui"))
                .color(NamedTextColor.WHITE)
                .append(Component.text(plainTitle)
                        .font(Key.key("rancho", "title"))
                        .color(TextColor.color(0xF0D078)));
        return Bukkit.createInventory(holder, rows * 9, title);
    }
}
