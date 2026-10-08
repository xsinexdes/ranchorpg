package dev.rancho.gui;

import dev.rancho.RanchoPlugin;
import dev.rancho.util.ItemBuilder;
import org.bukkit.Material;

import java.util.List;

/** Lista paginada de todos los mensajes; click para editar uno por chat. */
public final class LangMenu extends Menu {

    private static final int PER_PAGE = 45;

    private final Menu parent;
    private final List<String> keys;
    private int page = 0;

    public LangMenu(RanchoPlugin plugin, Menu parent) {
        super(plugin, plugin.lang().get("menu.messages.title", "page", 1), 6);
        this.parent = parent;
        this.keys = plugin.lang().keys();
    }

    @Override
    protected void build() {
        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < keys.size(); i++) {
            String key = keys.get(start + i);
            set(i, new ItemBuilder(Material.PAPER)
                            .name("&e" + key)
                            .lore("&7Actual: &r" + plugin.lang().raw(key), "", "&8Click para editar")
                            .build(),
                    click -> plugin.prompts().ask(viewer,
                            plugin.lang().prefixed("prompt.ask", "key", key),
                            text -> {
                                plugin.lang().set(key, text);
                                viewer.sendMessage(plugin.lang().prefixed("prompt.saved"));
                                open(viewer);
                            }));
        }

        set(45, new ItemBuilder(Material.ARROW).name(plugin.lang().get("menu.back")).build(),
                click -> parent.open(viewer));
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
