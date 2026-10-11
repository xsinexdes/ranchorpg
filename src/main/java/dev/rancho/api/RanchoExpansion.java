package dev.rancho.api;

import dev.rancho.RanchoPlugin;
import dev.rancho.util.Util;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** Placeholders de PlaceholderAPI: %rancho_season%, %rancho_season_day%, %rancho_temperature%, %rancho_farmer_level%... */
public final class RanchoExpansion extends PlaceholderExpansion {

    private final RanchoPlugin plugin;

    public RanchoExpansion(RanchoPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "rancho";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Kurjr";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        switch (params.toLowerCase()) {
            case "season":
                return org.bukkit.ChatColor.stripColor(plugin.seasons().name(plugin.seasons().current()));
            case "season_day":
                return String.valueOf(plugin.seasons().day());
            case "season_length":
                return String.valueOf(plugin.seasons().length());
            case "temperature":
                if (player instanceof Player p) {
                    return Util.fmt(plugin.temperature().getTemperature(p.getLocation()));
                }
                return "";
            case "farmer_level":
                return player == null ? "" : String.valueOf(plugin.farmers().level(player.getUniqueId()));
            case "farmer_xp":
                return player == null ? "" : Util.fmt0(plugin.farmers().data(player.getUniqueId()).xp);
            case "coins":
                if (player instanceof Player p) {
                    return Util.fmt(plugin.economy().balance(p));
                }
                return "";
            default:
                return null;
        }
    }
}
