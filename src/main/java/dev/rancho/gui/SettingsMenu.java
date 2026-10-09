package dev.rancho.gui;

import dev.rancho.RanchoPlugin;
import dev.rancho.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Menú genérico para editar valores de cualquier YAML sin tocar archivos.
 * Booleanos: click alterna. Números: izq. resta, der. suma, Shift x10.
 * Textos: click y escribe en el chat. Selección: click cambia a la siguiente opción.
 */
public class SettingsMenu extends Menu {

    public enum Kind { NUMBER, BOOL, TEXT, CYCLE }

    /** Una opción editable. */
    public record Entry(Kind kind, String path, String label, Material icon, double step, double min, double max,
                        List<String> options) {

        public static Entry num(String path, String label, Material icon, double step, double min, double max) {
            return new Entry(Kind.NUMBER, path, label, icon, step, min, max, List.of());
        }

        public static Entry bool(String path, String label, Material icon) {
            return new Entry(Kind.BOOL, path, label, icon, 0, 0, 0, List.of());
        }

        public static Entry text(String path, String label, Material icon) {
            return new Entry(Kind.TEXT, path, label, icon, 0, 0, 0, List.of());
        }

        public static Entry cycle(String path, String label, Material icon, List<String> options) {
            return new Entry(Kind.CYCLE, path, label, icon, 0, 0, 0, options);
        }
    }

    private static final int PER_PAGE = 21;

    private final List<Entry> entries;
    private final Menu parent;
    private final FileConfiguration cfg;
    private final Runnable onChange;
    private int page = 0;

    public SettingsMenu(RanchoPlugin plugin, String title, List<Entry> entries, Menu parent,
                        FileConfiguration cfg, Runnable onChange) {
        super(plugin, title, 5);
        this.entries = new ArrayList<>(entries);
        this.parent = parent;
        this.cfg = cfg;
        this.onChange = onChange;
    }

    /** Atajo para editar config.yml con aplicación inmediata. */
    public static SettingsMenu forConfig(RanchoPlugin plugin, String title, List<Entry> entries, Menu parent) {
        return new SettingsMenu(plugin, title, entries, parent, plugin.getConfig(), () -> {
            plugin.saveConfig();
            plugin.applySettings();
        });
    }

    @Override
    protected void build() {
        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < entries.size(); i++) {
            Entry e = entries.get(start + i);
            int slot = 10 + (i / 7) * 9 + (i % 7);
            Object value = cfg.get(e.path());
            ItemBuilder item = new ItemBuilder(e.icon()).name("&e" + e.label());
            switch (e.kind()) {
                case BOOL -> item.lore("&7Estado: " + (Boolean.TRUE.equals(value) ? "&aActivado" : "&cDesactivado"),
                        "", "&8Click para alternar");
                case NUMBER -> item.lore("&7Valor: &f" + value, "", "&8Click izq.: &c-" + fmt(e.step()),
                        "&8Click der.: &a+" + fmt(e.step()), "&8Shift: x10");
                case TEXT -> item.lore("&7Valor: &r" + value, "", "&8Click para editar en el chat");
                case CYCLE -> item.lore("&7Valor: &f" + value, "", "&8Click izq.: siguiente", "&8Click der.: anterior");
            }
            set(slot, item.build(), click -> edit(e, click));
        }

        set(40, new ItemBuilder(Material.ARROW).name(plugin.lang().get("menu.back")).build(),
                click -> parent.open(viewer));
        if (page > 0) {
            set(39, new ItemBuilder(Material.SPECTRAL_ARROW).name(plugin.lang().get("menu.prev")).build(),
                    click -> { page--; refresh(); });
        }
        if ((page + 1) * PER_PAGE < entries.size()) {
            set(41, new ItemBuilder(Material.SPECTRAL_ARROW).name(plugin.lang().get("menu.next")).build(),
                    click -> { page++; refresh(); });
        }
        buildExtra();
        fillEmpty();
    }

    /** Punto de extensión para añadir botones propios (slots 36-38 y 42-44). */
    protected void buildExtra() {}

    private void edit(Entry e, InventoryClickEvent click) {
        Object value = cfg.get(e.path());
        switch (e.kind()) {
            case BOOL -> cfg.set(e.path(), !Boolean.TRUE.equals(value));
            case NUMBER -> {
                double cur = value instanceof Number n ? n.doubleValue() : 0;
                double mult = click.isShiftClick() ? 10 : 1;
                double delta = (click.isLeftClick() ? -1 : 1) * e.step() * mult;
                double next = Math.max(e.min(), Math.min(e.max(), cur + delta));
                if (value instanceof Integer) {
                    cfg.set(e.path(), (int) Math.round(next));
                } else {
                    cfg.set(e.path(), Math.round(next * 100.0) / 100.0);
                }
            }
            case CYCLE -> {
                if (e.options().isEmpty()) {
                    return;
                }
                int idx = e.options().indexOf(String.valueOf(value));
                int n = e.options().size();
                idx = click.isLeftClick() ? (idx + 1) % n : (idx - 1 + n) % n;
                cfg.set(e.path(), e.options().get(idx));
            }
            case TEXT -> {
                plugin.prompts().ask(viewer, plugin.lang().prefixed("prompt.value", "key", e.label()), text -> {
                    cfg.set(e.path(), text);
                    onChange.run();
                    open(viewer);
                });
                return;
            }
        }
        onChange.run();
        refresh();
    }

    private static String fmt(double d) {
        return d == Math.floor(d) ? String.valueOf((long) d) : String.valueOf(d);
    }
}
