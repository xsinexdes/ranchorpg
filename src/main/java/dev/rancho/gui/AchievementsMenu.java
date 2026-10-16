package dev.rancho.gui;

import dev.rancho.RanchoPlugin;
import dev.rancho.farmer.FarmerManager;
import dev.rancho.farmer.PlayerData;
import dev.rancho.util.ItemBuilder;
import dev.rancho.util.Util;
import org.bukkit.Material;

/** Lista de logros del rancho. */
public final class AchievementsMenu extends Menu {

    public AchievementsMenu(RanchoPlugin plugin) {
        super(plugin, "&6Logros", 4);
    }

    @Override
    protected void build() {
        PlayerData pd = plugin.farmers().data(viewer);
        int i = 0;
        for (FarmerManager.Achievement a : FarmerManager.ACHIEVEMENTS) {
            boolean done = pd.achievements.contains(a.id());
            int slot = 10 + (i / 7) * 9 + (i % 7);
            set(slot, new ItemBuilder(done ? Material.LIME_DYE : Material.GRAY_DYE)
                    .name((done ? "&a" : "&7") + a.name())
                    .lore("&7" + a.desc(),
                            done ? "&aConseguido" : "&8Progreso: &f" + Math.min(pd.stat(a.stat()), a.target()) + "&8/&f" + a.target(),
                            "&7Recompensa: &6" + Util.fmt0(a.reward()) + " monedas").build());
            i++;
        }
        set(31, new ItemBuilder(Material.ARROW).name(plugin.lang().get("menu.back")).build(),
                click -> new MainMenu(plugin).open(viewer));
        fillEmpty();
    }
}
