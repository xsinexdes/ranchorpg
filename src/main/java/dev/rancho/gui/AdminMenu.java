package dev.rancho.gui;

import dev.rancho.RanchoPlugin;
import dev.rancho.crop.CropDef;
import dev.rancho.economy.MarketManager;
import dev.rancho.gui.SettingsMenu.Entry;
import dev.rancho.item.CustomItems;
import dev.rancho.season.Season;
import dev.rancho.season.SeasonEvent;
import dev.rancho.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

/** Panel /rancho admin: puerta de entrada a TODA la configuración por GUI. */
public final class AdminMenu extends Menu {

    public AdminMenu(RanchoPlugin plugin) {
        super(plugin, plugin.lang().get("menu.admin.title"), 4);
    }

    @Override
    protected void build() {
        button(10, Material.CLOCK, "&6Estaciones", "Duración, tiempo real, BossBar y temperatura.", () -> seasons());
        button(11, Material.LIGHTNING_ROD, "&6Eventos de estación", "Cosecha, helada, ola de calor, sequía.", () -> events());
        button(12, Material.BLAZE_POWDER, "&6Temperatura", "Bioma, clima, hora, bloques y jugadores.", () -> temperature());
        button(13, Material.COW_SPAWN_EGG, "&6Ganado (general)", "Tiempo, enfermedades, hologramas...", () -> animals());
        button(14, Material.FERMENTED_SPIDER_EYE, "&6Genética", "Mutaciones y límites de caballos.", () -> genetics());
        button(15, Material.WHEAT, "&6Cultivos (general)", "Crecimiento, riego, invernaderos.", () -> cropSettings());
        button(16, Material.EMERALD, "&6Granjero y economía", "XP, Vault y mercado.", () -> economy());

        set(19, new ItemBuilder(Material.SADDLE).name("&6Especies").lore("&7Edita, crea o elimina especies.").build(),
                click -> new SpeciesMenu(plugin, this).open(viewer));
        set(20, new ItemBuilder(Material.BEETROOT_SEEDS).name("&6Definición de cultivos")
                .lore("&7Edita, crea o elimina cultivos.").build(),
                click -> new CropsMenu(plugin, this).open(viewer));
        set(21, new ItemBuilder(Material.DIAMOND).name("&6Niveles de calidad")
                .lore("&7Nombre, color, valor y efectos de los 10 niveles.").build(),
                click -> new LevelsMenu(plugin, this).open(viewer));
        button(22, Material.GOLD_INGOT, "&6Demanda del mercado", "Demanda por estación y grupo.", () -> demand());
        button(23, Material.CHEST, "&6Precios de la tienda", "Precio de cada item y semilla.", () -> shop());
        set(24, new ItemBuilder(Material.WRITABLE_BOOK).name("&6Mensajes").lore("&7Edita todos los textos.").build(),
                click -> new LangMenu(plugin, this).open(viewer));
        set(25, new ItemBuilder(Material.ENDER_CHEST).name("&6Items de Rancho").lore("&7Obtén cualquier item o semilla.").build(),
                click -> new ItemsMenu(plugin, this).open(viewer));

        set(31, new ItemBuilder(Material.ARROW).name(plugin.lang().get("menu.back")).build(),
                click -> new MainMenu(plugin).open(viewer));
        fillEmpty();
    }

    private void button(int slot, Material icon, String name, String lore, java.util.function.Supplier<Menu> target) {
        set(slot, new ItemBuilder(icon).name(name).lore("&7" + lore).build(), click -> target.get().open(viewer));
    }

    private SettingsMenu cfgMenu(String title, List<Entry> entries) {
        return SettingsMenu.forConfig(plugin, title, entries, this);
    }

    // ------------------------------------------------------------------ submenús

    private Menu seasons() {
        List<Entry> e = new ArrayList<>();
        e.add(Entry.num("seasons.length-days", "Duración de cada estación (días)", Material.CLOCK, 1, 1, 365));
        e.add(Entry.bool("seasons.use-real-time", "Usar días reales (no de Minecraft)", Material.COMPASS));
        e.add(Entry.bool("seasons.bossbar", "BossBar de estación", Material.DRAGON_HEAD));
        e.add(Entry.bool("seasons.title", "Título al cambiar de estación", Material.NAME_TAG));
        for (Season s : Season.values()) {
            e.add(Entry.num("seasons.offset." + s.key(), "Temperatura " + s.key() + " (°C)", s.icon(), 1, -40, 40));
        }
        final AdminMenu self = this;
        return new SettingsMenu(plugin, plugin.lang().get("menu.seasons.title"), e, self, plugin.getConfig(), () -> {
            plugin.saveConfig();
            plugin.applySettings();
        }) {
            @Override
            protected void buildExtra() {
                set(36, new ItemBuilder(plugin.seasons().current().icon())
                                .name("&eEstación actual: " + plugin.seasons().name(plugin.seasons().current()))
                                .lore("&8Click para pasar a la siguiente").build(),
                        click -> {
                            plugin.seasons().setSeason(plugin.seasons().current().next());
                            refresh();
                        });
                set(44, new ItemBuilder(Material.SUGAR).name("&eAvanzar un día")
                                .lore("&7Día: &f" + plugin.seasons().day() + "&7/&f" + plugin.seasons().length()).build(),
                        click -> {
                            plugin.seasons().advance(1);
                            refresh();
                        });
            }
        };
    }

    private Menu events() {
        List<Entry> e = new ArrayList<>();
        Material[] icons = {Material.HAY_BLOCK, Material.SNOW_BLOCK, Material.MAGMA_BLOCK, Material.DEAD_BUSH};
        int i = 0;
        for (SeasonEvent ev : SeasonEvent.values()) {
            String p = "events." + ev.key() + ".";
            String n = plugin.lang().get("event." + ev.key());
            e.add(Entry.bool(p + "enabled", n + ": activado", icons[i]));
            e.add(Entry.num(p + "chance", n + ": probabilidad por día (%)", icons[i], 1, 0, 100));
            e.add(Entry.num(p + "duration-days", n + ": duración (días)", icons[i], 1, 1, 30));
            i++;
        }
        e.add(Entry.num("events.frost.temp-offset", "Helada: ajuste de temperatura", Material.ICE, 1, -40, 0));
        e.add(Entry.num("events.heatwave.temp-offset", "Ola de calor: ajuste de temperatura", Material.LAVA_BUCKET, 1, 0, 40));
        final AdminMenu self = this;
        return new SettingsMenu(plugin, "&6Eventos de estación", e, self, plugin.getConfig(), () -> {
            plugin.saveConfig();
            plugin.applySettings();
        }) {
            @Override
            protected void buildExtra() {
                int slot = 36;
                for (SeasonEvent ev : SeasonEvent.values()) {
                    if (slot > 38) break;
                    final SeasonEvent fev = ev;
                    set(slot++, new ItemBuilder(Material.FIREWORK_ROCKET)
                                    .name("&eForzar: " + plugin.lang().get("event." + ev.key())).build(),
                            click -> {
                                plugin.seasons().startEvent(fev, plugin.getConfig()
                                        .getInt("events." + fev.key() + ".duration-days", 2));
                                refresh();
                            });
                }
                set(42, new ItemBuilder(Material.MILK_BUCKET).name("&eTerminar evento actual").build(), click -> {
                    plugin.seasons().endEvent();
                    refresh();
                });
            }
        };
    }

    private Menu temperature() {
        List<Entry> e = new ArrayList<>();
        e.add(Entry.num("temperature.biome-scale", "Escala del bioma", Material.GRASS_BLOCK, 1, 5, 60));
        e.add(Entry.num("temperature.biome-offset", "Desfase del bioma (°C)", Material.SAND, 1, -40, 40));
        e.add(Entry.num("temperature.day-amplitude", "Variación día/noche (°C)", Material.DAYLIGHT_DETECTOR, 1, 0, 20));
        e.add(Entry.num("temperature.rain-offset", "Lluvia (°C)", Material.WATER_BUCKET, 1, -20, 20));
        e.add(Entry.num("temperature.thunder-offset", "Tormenta (°C)", Material.LIGHTNING_ROD, 1, -20, 20));
        e.add(Entry.num("temperature.lava-bonus", "Lava cerca (°C)", Material.LAVA_BUCKET, 1, -20, 30));
        e.add(Entry.num("temperature.fire-bonus", "Fuego/hoguera cerca (°C)", Material.CAMPFIRE, 1, -20, 30));
        e.add(Entry.num("temperature.torch-bonus", "Antorchas cerca (°C)", Material.TORCH, 1, -20, 30));
        e.add(Entry.num("temperature.ice-bonus", "Hielo/nieve cerca (°C)", Material.PACKED_ICE, 1, -30, 20));
        e.add(Entry.num("temperature.water-bonus", "Agua cerca (°C)", Material.WATER_BUCKET, 1, -20, 20));
        e.add(Entry.num("temperature.scan-radius", "Radio de escaneo (bloques)", Material.SPYGLASS, 1, 0, 5));
        e.add(Entry.bool("temperature.players.enabled", "La temperatura afecta a jugadores", Material.LEATHER_CHESTPLATE));
        e.add(Entry.num("temperature.players.cold-threshold", "Jugadores: frío bajo (°C)", Material.SNOWBALL, 1, -40, 20));
        e.add(Entry.num("temperature.players.heat-threshold", "Jugadores: calor sobre (°C)", Material.BLAZE_POWDER, 1, 20, 80));
        return cfgMenu(plugin.lang().get("menu.temperature.title"), e);
    }

    private Menu animals() {
        List<Entry> e = new ArrayList<>();
        e.add(Entry.num("animals.time-scale", "Velocidad de simulación del ganado", Material.CLOCK, 0.25, 0.1, 20));
        e.add(Entry.num("animals.level-days", "Días por nivel de animal", Material.EXPERIENCE_BOTTLE, 0.25, 0.1, 30));
        e.add(Entry.num("animals.breed-min-happiness", "Felicidad mínima para criar", Material.HONEY_BOTTLE, 5, 0, 100));
        e.add(Entry.num("animals.vaccine-days", "Días de inmunidad de la vacuna", Material.PRISMARINE_SHARD, 1, 1, 60));
        e.add(Entry.num("animals.disease-chance", "Multiplicador de enfermedades", Material.POISONOUS_POTATO, 0.25, 0, 10));
        e.add(Entry.num("animals.manure-days", "Días entre estiércol", Material.COCOA_BEANS, 0.1, 0.05, 5));
        e.add(Entry.num("animals.overgraze-chance", "Probabilidad de sobrepastoreo", Material.GRASS_BLOCK, 0.01, 0, 1));
        e.add(Entry.bool("animals.hologram", "Hologramas sobre los animales", Material.NAME_TAG));
        e.add(Entry.num("animals.hologram-range", "Distancia de hologramas (bloques)", Material.SPYGLASS, 5, 8, 100));
        e.add(Entry.bool("animals.actionbar", "ActionBar al mirar animal o cultivo", Material.PAPER));
        return cfgMenu("&6Ganado (general)", e);
    }

    private Menu genetics() {
        List<Entry> e = new ArrayList<>();
        e.add(Entry.num("genetics.mutation-chance", "Probabilidad de mutación al nacer", Material.FERMENTED_SPIDER_EYE, 0.02, 0, 1));
        e.add(Entry.num("genetics.serum-positive", "Prob. de mutación positiva (suero)", Material.GLOWSTONE_DUST, 0.05, 0, 1));
        e.add(Entry.num("genetics.max-mutations", "Máximo de mutaciones por animal", Material.AMETHYST_SHARD, 1, 1, 12));
        e.add(Entry.num("genetics.horse-cap-health", "Caballos: tope de vida", Material.GOLDEN_APPLE, 1, 15, 200));
        e.add(Entry.num("genetics.horse-cap-speed", "Caballos: tope de velocidad", Material.SUGAR, 0.01, 0.1, 0.6));
        e.add(Entry.num("genetics.horse-cap-jump", "Caballos: tope de salto", Material.RABBIT_FOOT, 0.05, 0.4, 2.0));
        return cfgMenu("&6Genética", e);
    }

    private Menu cropSettings() {
        List<Entry> e = new ArrayList<>();
        e.add(Entry.num("crops.time-scale", "Velocidad de simulación de cultivos", Material.CLOCK, 0.25, 0.1, 20));
        e.add(Entry.num("crops.growth-multiplier", "Multiplicador de crecimiento", Material.WHEAT, 0.1, 0.1, 10));
        e.add(Entry.num("crops.water-decay-per-day", "Pérdida de agua por día", Material.WATER_BUCKET, 5, 0, 300));
        e.add(Entry.num("crops.wither-days", "Días de estrés hasta marchitar", Material.DEAD_BUSH, 0.25, 0.1, 10));
        e.add(Entry.num("crops.can-uses", "Usos de la regadera", Material.BUCKET, 1, 1, 200));
        e.add(Entry.num("crops.greenhouse-radius", "Radio del Invernadero", Material.SEA_LANTERN, 1, 1, 32));
        e.add(Entry.num("crops.pot-radius", "Radio de la Maceta", Material.DECORATED_POT, 1, 0, 8));
        return cfgMenu("&6Cultivos (general)", e);
    }

    private Menu economy() {
        List<Entry> e = new ArrayList<>();
        e.add(Entry.bool("economy.use-vault", "Usar Vault si está instalado", Material.EMERALD));
        e.add(Entry.num("farmer.xp-multiplier", "Multiplicador de XP de granjero", Material.EXPERIENCE_BOTTLE, 0.25, 0.1, 20));
        e.add(Entry.num("farmer.xp.feed", "XP por alimentar", Material.WHEAT, 1, 0, 100));
        e.add(Entry.num("farmer.xp.cure", "XP por curar", Material.GHAST_TEAR, 1, 0, 100));
        e.add(Entry.num("farmer.xp.produce", "XP por producir", Material.MILK_BUCKET, 1, 0, 100));
        e.add(Entry.num("farmer.xp.breed", "XP por criar", Material.EGG, 1, 0, 100));
        e.add(Entry.num("farmer.xp.birth", "XP por cría nacida", Material.EGG, 1, 0, 200));
        e.add(Entry.num("farmer.xp.plant", "XP por plantar", Material.WHEAT_SEEDS, 0.5, 0, 100));
        e.add(Entry.num("farmer.xp.harvest", "XP por cosechar", Material.WHEAT, 0.5, 0, 100));
        e.add(Entry.num("farmer.xp.sell", "XP por vender", Material.GOLD_INGOT, 0.25, 0, 100));
        e.add(Entry.num("market.dynamic-drop", "Caída de demanda por unidad vendida", Material.REDSTONE, 0.001, 0, 0.1));
        e.add(Entry.num("market.dynamic-min", "Demanda dinámica mínima", Material.REDSTONE, 0.05, 0.1, 1));
        e.add(Entry.num("market.dynamic-recover", "Recuperación por minuto", Material.GLOWSTONE_DUST, 0.005, 0, 0.5));
        return cfgMenu("&6Granjero y economía", e);
    }

    private Menu demand() {
        List<Entry> e = new ArrayList<>();
        for (String g : MarketManager.GROUPS) {
            for (Season s : Season.values()) {
                e.add(Entry.num("market.demand." + g + "." + s.key(), g + " en " + s.key(), s.icon(), 0.05, 0.1, 5));
            }
        }
        return cfgMenu("&6Demanda del mercado", e);
    }

    private Menu shop() {
        FileConfiguration cfg = plugin.getConfig();
        List<Entry> e = new ArrayList<>();
        for (CustomItems.Def d : CustomItems.all()) {
            if (d.price() <= 0) {
                continue;
            }
            String path = "shop." + d.id();
            if (!cfg.contains(path)) {
                cfg.set(path, d.price());
            }
            e.add(Entry.num(path, d.id(), d.mat(), 5, 0, 100000));
        }
        for (CropDef c : plugin.cropDefs().all()) {
            if (!c.custom()) {
                continue;
            }
            String path = "shop.seed_" + c.id();
            if (!cfg.contains(path)) {
                cfg.set(path, plugin.market().seedPrice(c.id()));
            }
            e.add(Entry.num(path, "Semilla: " + c.name(), c.seedMaterial(), 5, 0, 100000));
        }
        return cfgMenu("&6Precios de la tienda", e);
    }
}
