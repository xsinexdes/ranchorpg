package dev.rancho.command;

import dev.rancho.RanchoPlugin;
import dev.rancho.animal.Species;
import dev.rancho.climate.TemperatureManager;
import dev.rancho.crop.CropDef;
import dev.rancho.gui.AdminMenu;
import dev.rancho.gui.MainMenu;
import dev.rancho.item.CustomItems;
import dev.rancho.util.Text;
import dev.rancho.util.Util;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** /rancho [admin|info|temp|libreta|termometro|give|spawn|npc|reload] */
public final class RanchoCommand implements TabExecutor {

    private final RanchoPlugin plugin;

    public RanchoCommand(RanchoPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        var lang = plugin.lang();

        if (args.length == 0) {
            if (!sender.hasPermission("rancho.use")) {
                sender.sendMessage(lang.prefixed("no-permission"));
            } else if (sender instanceof Player p) {
                new MainMenu(plugin).open(p);
            } else {
                sendInfo(sender);
            }
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        boolean admin = sender.hasPermission("rancho.admin");
        switch (sub) {
            case "admin" -> {
                if (!admin) {
                    sender.sendMessage(lang.prefixed("no-permission"));
                } else if (sender instanceof Player p) {
                    new AdminMenu(plugin).open(p);
                } else {
                    sender.sendMessage(lang.prefixed("only-players"));
                }
            }
            case "reload" -> {
                if (!admin) {
                    sender.sendMessage(lang.prefixed("no-permission"));
                } else {
                    plugin.reloadAll();
                    sender.sendMessage(lang.prefixed("reloaded"));
                }
            }
            case "info", "temp" -> {
                if (!sender.hasPermission("rancho.use")) {
                    sender.sendMessage(lang.prefixed("no-permission"));
                } else {
                    sendInfo(sender);
                }
            }
            case "libreta", "termometro" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage(lang.prefixed("only-players"));
                } else if (!sender.hasPermission("rancho.use")) {
                    sender.sendMessage(lang.prefixed("no-permission"));
                } else {
                    Util.giveOrDrop(p, CustomItems.create(sub.equals("libreta") ? "notebook" : "thermometer", 1));
                }
            }
            case "give" -> give(sender, args, admin);
            case "spawn" -> spawn(sender, args, admin);
            case "debug" -> debug(sender, admin);
            case "model" -> model(sender, args, admin);
            case "feria", "fair" -> fairCommand(sender, args, admin);
            default -> sender.sendMessage(lang.prefixed("unknown-subcommand"));
        }
        return true;
    }

    private void give(CommandSender sender, String[] args, boolean admin) {
        var lang = plugin.lang();
        if (!admin) {
            sender.sendMessage(lang.prefixed("no-permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(lang.prefixed("command.give-usage"));
            return;
        }
        int amount = 1;
        if (args.length > 2) {
            try {
                amount = Math.max(1, Math.min(64, Integer.parseInt(args[2])));
            } catch (NumberFormatException ex) {
                amount = 1;
            }
        }
        Player target = args.length > 3 ? plugin.getServer().getPlayerExact(args[3])
                : sender instanceof Player p ? p : null;
        if (target == null) {
            sender.sendMessage(lang.prefixed("command.no-player"));
            return;
        }
        ItemStack item;
        String id = args[1].toLowerCase(Locale.ROOT);
        if (id.startsWith("seed_")) {
            CropDef def = plugin.cropDefs().get(id.substring(5));
            if (def == null) {
                sender.sendMessage(lang.prefixed("command.unknown-item"));
                return;
            }
            item = CustomItems.createSeed(def, amount);
        } else if (CustomItems.get(id) != null) {
            item = CustomItems.create(id, amount);
        } else {
            sender.sendMessage(lang.prefixed("command.unknown-item"));
            return;
        }
        Util.giveOrDrop(target, item);
        sender.sendMessage(lang.prefixed("command.given", "amount", amount, "item", id, "player", target.getName()));
    }

    private void spawn(CommandSender sender, String[] args, boolean admin) {
        var lang = plugin.lang();
        if (!admin) {
            sender.sendMessage(lang.prefixed("no-permission"));
            return;
        }
        if (!(sender instanceof Player p)) {
            sender.sendMessage(lang.prefixed("only-players"));
            return;
        }
        Species sp = args.length > 1 ? plugin.species().get(args[1].toLowerCase(Locale.ROOT)) : null;
        if (sp == null) {
            sender.sendMessage(lang.prefixed("command.unknown-species"));
            return;
        }
        int n = 1;
        if (args.length > 2) {
            try {
                n = Math.max(1, Math.min(20, Integer.parseInt(args[2])));
            } catch (NumberFormatException ex) {
                n = 1;
            }
        }
        for (int i = 0; i < n; i++) {
            plugin.animals().spawn(sp, p.getLocation());
        }
        sender.sendMessage(lang.prefixed("command.spawned", "amount", n, "species", sp.name()));
    }

    /**
     * /rancho model hand &lt;n&gt; · /rancho model &lt;clave&gt; &lt;n&gt; · /rancho model list.
     * Asigna el CustomModelData de un item del plugin (resource pack).
     */
    private void model(CommandSender sender, String[] args, boolean admin) {
        var lang = plugin.lang();
        if (!admin) {
            sender.sendMessage(lang.prefixed("no-permission"));
            return;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("list")) {
            if (sender instanceof Player p) {
                new dev.rancho.gui.ModelsMenu(plugin, new AdminMenu(plugin)).open(p);
            } else {
                for (String k : plugin.models().allKeys()) {
                    sender.sendMessage(k + " = " + plugin.models().get(k));
                }
            }
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(lang.prefixed("model.usage"));
            return;
        }
        int value;
        try {
            value = Math.max(0, Integer.parseInt(args[2].trim()));
        } catch (NumberFormatException ex) {
            sender.sendMessage(lang.prefixed("model.invalid"));
            return;
        }
        String key;
        Player holder = null;
        if (args[1].equalsIgnoreCase("hand")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(lang.prefixed("only-players"));
                return;
            }
            holder = p;
            key = plugin.models().keyOf(p.getInventory().getItemInMainHand());
            if (key == null) {
                sender.sendMessage(lang.prefixed("model.no-item"));
                return;
            }
        } else {
            key = args[1].toLowerCase(Locale.ROOT);
            if (!plugin.models().isValidKey(key)) {
                sender.sendMessage(lang.prefixed("model.unknown"));
                return;
            }
        }
        plugin.models().set(key, value);
        sender.sendMessage(lang.prefixed("model.set", "key", key, "value", value));
        if (holder != null) { // actualiza al instante el item que sostienes
            ItemStack hand = holder.getInventory().getItemInMainHand();
            var meta = hand.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(value > 0 ? value : null);
                hand.setItemMeta(meta);
            }
        }
    }

    /** Muestra los datos internos del animal o cultivo al que miras. */
    private void debug(CommandSender sender, boolean admin) {
        var lang = plugin.lang();
        if (!admin) {
            sender.sendMessage(lang.prefixed("no-permission"));
            return;
        }
        if (!(sender instanceof Player p)) {
            sender.sendMessage(lang.prefixed("only-players"));
            return;
        }
        var r = p.getWorld().rayTraceEntities(p.getEyeLocation(), p.getEyeLocation().getDirection(), 8, 0.3,
                ent -> ent != p && plugin.animals().get(ent) != null);
        if (r != null && r.getHitEntity() != null) {
            var d = plugin.animals().get(r.getHitEntity());
            p.sendMessage(Text.color("&6[Debug animal] &f" + d.name + " &7(" + d.speciesId + ")"));
            p.sendMessage(Text.color("&7Edad &f" + Util.fmt(d.age) + " &7Nv &f" + d.level + " &7Cuidado &f" + Util.fmt0(d.care)
                    + " &7Dieta &f" + Util.fmt0(d.diet) + " &7Carga &f" + Util.fmt0(d.charge)));
            p.sendMessage(Text.color("&7Hambre &f" + Util.fmt0(d.hunger) + " &7Sed &f" + Util.fmt0(d.thirst)
                    + " &7Higiene &f" + Util.fmt0(d.hygiene) + " &7Felicidad &f" + Util.fmt0(d.happiness)));
            p.sendMessage(Text.color("&7Enfermedad &f" + d.disease + " &7Inmune &f" + Util.fmt(d.immunity)
                    + " &7Gestante &f" + d.pregnant + " &7Dueño &f" + (d.owner.isEmpty() ? "-" : d.owner)));
            p.sendMessage(Text.color("&7Mutaciones &f" + d.mutations + " &7Genes &f" + java.util.Arrays.toString(d.genes)));
            return;
        }
        var b = p.getTargetBlockExact(8);
        if (b != null) {
            var c = plugin.crops().at(b);
            if (c != null) {
                p.sendMessage(Text.color("&6[Debug cultivo] &f" + c.cropId + " &7Progreso &f" + Util.fmt(c.progress * 100)
                        + "% &7Marchitez &f" + Util.fmt(c.wither) + " &7Marchito &f" + c.withered
                        + " &7Calidad media &f" + Util.fmt(c.avgScore())));
                return;
            }
        }
        p.sendMessage(Text.color("&6[Debug] &7Animales gestionados: &f" + plugin.animals().all().size()
                + " &7Cultivos: &f" + plugin.crops().count() + " &7Máquinas: " + plugin.machines().summary()));
    }

    /** /rancho feria [start|stop]: estado para todos; abrir o cancelar solo admins. */
    private void fairCommand(CommandSender sender, String[] args, boolean admin) {
        var lang = plugin.lang();
        var fair = plugin.fair();
        String sub = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "status";
        if (sub.equals("start") || sub.equals("stop")) {
            if (!admin) {
                sender.sendMessage(lang.prefixed("no-permission"));
                return;
            }
            if (sub.equals("start")) {
                fair.begin();
            } else if (fair.isActive()) {
                fair.cancel();
            }
            return;
        }
        if (fair.isActive()) {
            sender.sendMessage(lang.prefixed("fair.status-on", "time",
                    dev.rancho.food.FoodManager.fmtTime(fair.remainingMs())));
        } else {
            sender.sendMessage(lang.prefixed("fair.status-off"));
        }
    }

    private void sendInfo(CommandSender sender) {
        var lang = plugin.lang();
        var seasons = plugin.seasons();
        sender.sendMessage(lang.get("info.header"));
        sender.sendMessage(lang.get("info.season", "season", seasons.name(seasons.current()),
                "day", seasons.day(), "length", seasons.length()));
        if (sender instanceof Player p) {
            double temp = plugin.temperature().getTemperature(p.getLocation());
            sender.sendMessage(lang.get("info.temperature",
                    "color", TemperatureManager.colorFor(temp).replace('&', '§'), "temp", Util.fmt(temp)));
            var pd = plugin.farmers().data(p);
            sender.sendMessage(lang.get("info.farmer", "level", pd.level, "xp", Util.fmt0(pd.xp),
                    "coins", Util.fmt(plugin.economy().balance(p))));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        boolean admin = sender.hasPermission("rancho.admin");
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("info", "temp", "libreta", "termometro"));
            if (admin) {
                subs.addAll(List.of("admin", "reload", "give", "spawn", "debug", "model"));
            }
            subs.add("feria");
            for (String s : subs) {
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(s);
            }
        } else if (args.length == 2 && admin) {
            String pre = args[1].toLowerCase(Locale.ROOT);
            if (args[0].equalsIgnoreCase("give")) {
                for (String id : CustomItems.ids()) if (id.startsWith(pre)) out.add(id);
                for (CropDef c : plugin.cropDefs().all()) if (c.custom() && ("seed_" + c.id()).startsWith(pre)) out.add("seed_" + c.id());
            } else if (args[0].equalsIgnoreCase("spawn")) {
                for (String id : plugin.species().ids()) if (id.startsWith(pre)) out.add(id);
            } else if (args[0].equalsIgnoreCase("model")) {
                for (String k : List.of("hand", "list")) if (k.startsWith(pre)) out.add(k);
                for (String k : plugin.models().allKeys()) if (k.startsWith(pre)) out.add(k);
            }
        }
        return out;
    }
}
