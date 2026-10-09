package dev.rancho.gui;

import dev.rancho.RanchoPlugin;
import dev.rancho.gui.SettingsMenu.Entry;
import dev.rancho.quality.QualityLevels;
import dev.rancho.util.ItemBuilder;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/** Edita los 10 niveles de calidad: nombre, color, lore, valor, saturación y efecto. */
public final class LevelsMenu extends Menu {

    public static final List<String> EFFECTS = List.of("none", "speed", "haste", "strength", "jump_boost",
            "regeneration", "resistance", "fire_resistance", "water_breathing", "night_vision", "absorption",
            "luck", "slow_falling", "health_boost", "saturation");

    private final Menu parent;

    public LevelsMenu(RanchoPlugin plugin, Menu parent) {
        super(plugin, "&6Niveles de calidad", 4);
        this.parent = parent;
    }

    @Override
    protected void build() {
        QualityLevels lv = plugin.levels();
        for (int q = 1; q <= QualityLevels.MAX; q++) {
            final int level = q;
            int slot = q <= 5 ? 9 + q : 14 + q;
            set(slot, new ItemBuilder(Material.PAPER).amount(q)
                            .name(lv.color(q) + "Nivel " + q + " - " + lv.name(q))
                            .lore("&7" + lv.lore(q), "&7Saturación extra: &f+" + lv.saturation(q),
                                    "&7Efecto: &f" + lv.effectKey(q) + " " + lv.effectSeconds(q) + "s", "",
                                    "&8Click para editar").build(),
                    click -> edit(level).open(viewer));
        }
        set(31, new ItemBuilder(Material.ARROW).name(plugin.lang().get("menu.back")).build(),
                click -> parent.open(viewer));
        fillEmpty();
    }

    private SettingsMenu edit(int q) {
        String p = "levels." + q + ".";
        List<Entry> e = new ArrayList<>();
        e.add(Entry.text(p + "name", "Nombre del nivel", Material.NAME_TAG));
        e.add(Entry.text(p + "color", "Color (códigos &)", Material.PINK_DYE));
        e.add(Entry.text(p + "lore", "Descripción (lore)", Material.WRITABLE_BOOK));
        e.add(Entry.num(p + "saturation", "Saturación extra al consumir", Material.COOKED_BEEF, 1, 0, 20));
        e.add(Entry.cycle(p + "effect", "Buff temporal", Material.POTION, EFFECTS));
        e.add(Entry.num(p + "effect-seconds", "Duración del buff (s)", Material.CLOCK, 5, 0, 600));
        e.add(Entry.num(p + "effect-amp", "Potencia del buff", Material.REDSTONE, 1, 0, 5));
        return new SettingsMenu(plugin, "&6Nivel " + q, e, this, plugin.levels().cfg(), plugin.levels()::save);
    }
}
