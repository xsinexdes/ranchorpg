package dev.rancho.gui;

import dev.rancho.RanchoPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Permite pedir texto al jugador por chat desde un menú (para editar mensajes). */
public final class ChatPrompt implements Listener {

    private final RanchoPlugin plugin;
    private final Map<UUID, Consumer<String>> pending = new ConcurrentHashMap<>();

    public ChatPrompt(RanchoPlugin plugin) {
        this.plugin = plugin;
    }

    /** Cierra el menú, muestra {@code message} y ejecuta {@code onInput} con lo que escriba. */
    public void ask(Player player, String message, Consumer<String> onInput) {
        pending.put(player.getUniqueId(), onInput);
        Bukkit.getScheduler().runTask(plugin, () -> player.closeInventory());
        player.sendMessage(message);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Consumer<String> callback = pending.remove(event.getPlayer().getUniqueId());
        if (callback == null) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        String text = event.getMessage();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (text.equalsIgnoreCase("cancelar")) {
                player.sendMessage(plugin.lang().prefixed("prompt.cancelled"));
            } else {
                callback.accept(text);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pending.remove(event.getPlayer().getUniqueId());
    }
}
