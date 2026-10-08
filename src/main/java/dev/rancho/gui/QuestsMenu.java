package dev.rancho.gui;

import dev.rancho.RanchoPlugin;
import dev.rancho.farmer.FarmerManager;
import dev.rancho.farmer.PlayerData;
import dev.rancho.util.ItemBuilder;
import dev.rancho.util.Util;
import org.bukkit.Material;

import java.util.Map;

/** Misiones activas del jugador. */
public final class QuestsMenu extends Menu {

    public QuestsMenu(RanchoPlugin plugin) {
        super(plugin, "&eMisiones de Rancho", 3);
    }

    @Override
    public boolean live() {
        return true;
    }

    @Override
    protected void build() {
        plugin.farmers().ensureQuests(viewer);
        PlayerData pd = plugin.farmers().data(viewer);
        int slot = 11;
        for (Map.Entry<String, Integer> e : pd.quests.entrySet()) {
            FarmerManager.QuestDef q = plugin.farmers().quest(e.getKey());
            if (q == null) {
                continue;
            }
            double pct = e.getValue() * 100.0 / q.target();
            set(slot, new ItemBuilder(Material.FILLED_MAP).name("&e" + q.desc())
                    .lore(Util.bar(pct, 12) + " &f" + e.getValue() + "&7/&f" + q.target(), "",
                            "&7Recompensa: &6" + Util.fmt0(q.coins()) + " monedas &7y &a" + q.xp() + " XP").build());
            slot += 2;
        }
        set(22, new ItemBuilder(Material.ARROW).name(plugin.lang().get("menu.back")).build(),
                click -> new MainMenu(plugin).open(viewer));
        fillEmpty();
    }
}
