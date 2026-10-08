package dev.rancho.command;

import dev.rancho.RanchoPlugin;
import dev.rancho.animal.Species;
import dev.rancho.climate.TemperatureManager;
import dev.rancho.crop.CropDef;
import dev.rancho.gui.AdminMenu;
import dev.rancho.gui.MainMenu;
import dev.rancho.item.CustomItems;
import dev.rancho.util.Keys;
import dev.rancho.util.Util;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

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
            case "npc" -> npc(sender, admin);
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

    private void npc(CommandSender sender, boolean admin) {
        var lang = plugin.lang();
        if (!admin) {
            sender.sendMessage(lang.prefixed("no-permission"));
            return;
        }
        if (!(sender instanceof Player p)) {
            sender.sendMessage(lang.prefixed("only-players"));
            return;
        }
        p.getWorld().spawn(p.getLocation(), Villager.class, v -> {
            v.setAI(false);
            v.setInvulnerable(true);
            v.setSilent(true);
            v.setPersistent(true);
            v.setProfession(Villager.Profession.FARMER);
            v.setCustomName(lang.get("npc.name"));
            v.setCustomNameVisible(true);
            v.getPersistentDataContainer().set(Keys.NPC, PersistentDataType.BYTE, (byte) 1);
        });
        sender.sendMessage(lang.prefixed("command.npc"));
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
                subs.addAll(List.of("admin", "reload", "give", "spawn", "npc"));
            }
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
            }
        }
        return out;
    }
}
