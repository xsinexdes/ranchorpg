package dev.rancho.gui;

import dev.rancho.RanchoPlugin;
import dev.rancho.farmer.PlayerData;
import dev.rancho.farmer.Skill;
import dev.rancho.util.ItemBuilder;
import org.bukkit.Material;

/** Árbol simple de habilidades del Granjero. */
public final class SkillsMenu extends Menu {

    public SkillsMenu(RanchoPlugin plugin) {
        super(plugin, "&dHabilidades de Granjero", 3);
    }

    @Override
    protected void build() {
        PlayerData pd = plugin.farmers().data(viewer);
        set(4, new ItemBuilder(Material.EXPERIENCE_BOTTLE).name("&aNivel " + pd.level)
                .lore("&7Puntos disponibles: &e" + pd.skillPoints).build());
        Skill[] skills = Skill.values();
        for (int i = 0; i < skills.length; i++) {
            Skill s = skills[i];
            int lvl = pd.skill(s);
            set(10 + i, new ItemBuilder(s.icon()).name("&e" + s.label() + " &7[" + lvl + "/" + Skill.MAX_LEVEL + "]")
                    .lore("&7" + s.description(), "", lvl >= Skill.MAX_LEVEL ? "&aNivel máximo" : "&8Click para mejorar (1 punto)")
                    .build(), click -> {
                plugin.farmers().upgrade(viewer, s);
                refresh();
            });
        }
        set(22, new ItemBuilder(Material.ARROW).name(plugin.lang().get("menu.back")).build(),
                click -> new MainMenu(plugin).open(viewer));
        fillEmpty();
    }
}
