package dev.rancho;

import dev.rancho.animal.AnimalListener;
import dev.rancho.animal.AnimalManager;
import dev.rancho.animal.SpeciesRegistry;
import dev.rancho.api.RanchoExpansion;
import dev.rancho.climate.PlayerClimate;
import dev.rancho.climate.TemperatureManager;
import dev.rancho.command.RanchoCommand;
import dev.rancho.config.Lang;
import dev.rancho.config.Messages;
import dev.rancho.crop.CropListener;
import dev.rancho.crop.CropManager;
import dev.rancho.crop.CropRegistry;
import dev.rancho.economy.EconomyBridge;
import dev.rancho.economy.MarketManager;
import dev.rancho.farmer.FarmerManager;
import dev.rancho.gui.ChatPrompt;
import dev.rancho.gui.LookInfo;
import dev.rancho.gui.Menu;
import dev.rancho.gui.MenuListener;
import dev.rancho.item.RecipeRegistry;
import dev.rancho.quality.QualityLevels;
import dev.rancho.season.SeasonManager;
import dev.rancho.util.Keys;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

/** Clase principal de RanchoRPG: crea y conecta todos los módulos. */
public final class RanchoPlugin extends JavaPlugin {

    private static RanchoPlugin instance;

    private Lang lang;
    private QualityLevels levels;
    private SeasonManager seasons;
    private TemperatureManager temperature;
    private PlayerClimate playerClimate;
    private SpeciesRegistry species;
    private CropRegistry cropDefs;
    private AnimalManager animals;
    private CropManager crops;
    private FarmerManager farmers;
    private EconomyBridge economy;
    private MarketManager market;
    private RecipeRegistry recipes;
    private ChatPrompt prompts;
    private LookInfo lookInfo;
    private BukkitTask menuTask;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        Keys.init(this);
        Messages.init();

        lang = new Lang(this);
        levels = new QualityLevels(this);
        species = new SpeciesRegistry(this);
        cropDefs = new CropRegistry(this);
        seasons = new SeasonManager(this);
        temperature = new TemperatureManager(this);
        playerClimate = new PlayerClimate(this);
        farmers = new FarmerManager(this);
        economy = new EconomyBridge(this);
        market = new MarketManager(this);
        animals = new AnimalManager(this);
        crops = new CropManager(this);
        prompts = new ChatPrompt(this);
        lookInfo = new LookInfo(this);
        recipes = new RecipeRegistry(this);
        recipes.register();

        var pm = getServer().getPluginManager();
        pm.registerEvents(new MenuListener(), this);
        pm.registerEvents(prompts, this);
        pm.registerEvents(seasons, this);
        pm.registerEvents(farmers, this);
        pm.registerEvents(new AnimalListener(this), this);
        pm.registerEvents(new CropListener(this), this);

        RanchoCommand command = new RanchoCommand(this);
        PluginCommand pc = getCommand("rancho");
        if (pc != null) {
            pc.setExecutor(command);
            pc.setTabCompleter(command);
        }

        if (pm.getPlugin("PlaceholderAPI") != null) {
            new RanchoExpansion(this).register();
            getLogger().info("PlaceholderAPI detectado: placeholders %rancho_*% registrados.");
        }

        seasons.start();
        farmers.start();
        market.start();
        crops.start();
        playerClimate.start();
        lookInfo.start();
        // Los animales se registran tras cargar los mundos
        Bukkit.getScheduler().runTask(this, () -> animals.start());
        menuTask = Bukkit.getScheduler().runTaskTimer(this, this::refreshMenus, 20L, 20L);
        getLogger().info("RanchoRPG activado.");
    }

    @Override
    public void onDisable() {
        if (menuTask != null) menuTask.cancel();
        if (lookInfo != null) lookInfo.stop();
        if (playerClimate != null) playerClimate.stop();
        if (animals != null) animals.shutdown();
        if (crops != null) crops.shutdown();
        if (market != null) market.shutdown();
        if (farmers != null) farmers.shutdown();
        if (seasons != null) seasons.shutdown();
        if (recipes != null) recipes.unregister();
    }

    /** Redibuja los menús "en vivo" (estadísticas de animales, cultivos...) mientras estén abiertos. */
    private void refreshMenus() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof Menu m && m.live()) {
                m.refresh();
            }
        }
    }

    /** Recarga config.yml, lang.yml y los archivos de datos de definiciones. */
    public void reloadAll() {
        reloadConfig();
        lang.load();
        levels.load();
        species.load();
        cropDefs.load();
        economy.hook();
        recipes.register();
        applySettings();
    }

    /** Aplica en memoria los valores actuales de la config (tras editarlos por GUI). */
    public void applySettings() {
        seasons.reload();
        temperature.reload();
    }

    public static RanchoPlugin get() { return instance; }
    public Lang lang() { return lang; }
    public QualityLevels levels() { return levels; }
    public SeasonManager seasons() { return seasons; }
    public TemperatureManager temperature() { return temperature; }
    public SpeciesRegistry species() { return species; }
    public CropRegistry cropDefs() { return cropDefs; }
    public AnimalManager animals() { return animals; }
    public CropManager crops() { return crops; }
    public FarmerManager farmers() { return farmers; }
    public EconomyBridge economy() { return economy; }
    public MarketManager market() { return market; }
    public ChatPrompt prompts() { return prompts; }
    public List<NamespacedKey> recipeKeys() { return recipes.keys(); }
}
