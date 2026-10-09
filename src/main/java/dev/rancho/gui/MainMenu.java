package dev.rancho.gui;

import dev.rancho.RanchoPlugin;
import dev.rancho.climate.TemperatureManager;
import dev.rancho.farmer.PlayerData;
import dev.rancho.item.CustomItems;
import dev.rancho.util.ItemBuilder;
import dev.rancho.util.Util;
import org.bukkit.Material;

/** Menú principal /rancho. */
public final class MainMenu extends Menu {

    public MainMenu(RanchoPlugin plugin) {
        super(plugin, plugin.lang().get("menu.main.title"), 4);
    }

    @Override
    public boolean live() {
        return true;
    }

    @Override
    protected void build() {
        var seasons = plugin.seasons();
        var lore = new java.util.ArrayList<String>();
        lore.add("&7Día &f" + seasons.day() + "&7/&f" + seasons.length());
        if (seasons.event() != null) {
            lore.add("&6Evento: " + plugin.lang().get("event." + seasons.event().key())
                    + " &7(" + seasons.eventDays() + " d)");
        }
        set(10, new ItemBuilder(seasons.current().icon()).name(seasons.name(seasons.current())).lore(lore).build());

        double temp = plugin.temperature().getTemperature(viewer.getLocation());
        set(11, new ItemBuilder(Material.BLAZE_POWDER).name("&6Temperatura")
                .lore(plugin.lang().get("info.temperature", "color",
                        TemperatureManager.colorFor(temp).replace('&', '§'), "temp", Util.fmt(temp))).build());

        PlayerData pd = plugin.farmers().data(viewer);
        double need = plugin.farmers().xpNeeded(pd.level);
        set(12, new ItemBuilder(Material.EXPERIENCE_BOTTLE).name("&aGranjero nivel " + pd.level)
                .lore("&7XP: &f" + Util.fmt0(pd.xp) + "&7/&f" + Util.fmt0(need), Util.bar(pd.xp / need * 100, 12),
                        "&7Puntos de habilidad: &e" + pd.skillPoints,
                        "&7Monedas: &6" + Util.fmt(plugin.economy().balance(viewer))).build());

        set(14, new ItemBuilder(Material.COOKED_BEEF).name("&6Libro de cocina")
                        .lore("&7Todas las recetas, tiempos y efectos.", "&8Cocina en una Cocina de Rancho.").build(),
                click -> new KitchenMenu(plugin, null, this).open(viewer));
        set(15, new ItemBuilder(Material.ENCHANTED_BOOK).name("&dHabilidades").lore("&7Gasta tus puntos de habilidad.").build(),
                click -> new SkillsMenu(plugin).open(viewer));
        set(16, new ItemBuilder(Material.MAP).name("&eMisiones").lore("&7Misiones simples de rancho.").build(),
                click -> new QuestsMenu(plugin).open(viewer));
        set(20, new ItemBuilder(Material.TOTEM_OF_UNDYING).name("&6Logros").lore("&7Tus logros del rancho.").build(),
                click -> new AchievementsMenu(plugin).open(viewer));
        set(21, new ItemBuilder(Material.BOOK).name("&6Libreta de Rancho")
                .lore("&7Click para recibir una libreta.", "&8Úsala sobre animales y cultivos.").build(),
                click -> { Util.giveOrDrop(viewer, CustomItems.create("notebook", 1)); });
        set(22, new ItemBuilder(Material.COMPASS).name("&cTermómetro")
                .lore("&7Click para recibir un termómetro.").build(),
                click -> { Util.giveOrDrop(viewer, CustomItems.create("thermometer", 1)); });

        if (viewer.hasPermission("rancho.admin")) {
            set(31, new ItemBuilder(Material.COMMAND_BLOCK).name("&4Administración").build(),
                    click -> new AdminMenu(plugin).open(viewer));
        }
        set(35, new ItemBuilder(Material.BARRIER).name(plugin.lang().get("menu.close")).build(),
                click -> viewer.closeInventory());
        fillEmpty();
    }
}
