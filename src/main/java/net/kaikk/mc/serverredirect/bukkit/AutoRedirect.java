package net.kaikk.mc.serverredirect.bukkit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

public record AutoRedirect(ServerRedirect plugin) implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        FileConfiguration config = plugin.getConfig();
        plugin.getLogger().info("Player " + player.getName() + " joined");

        if (!config.getBoolean("auto-redirect.enabled", true)) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (ServerRedirect.isUsingServerRedirect(player)) {
                startRedirectCountdown(player);
            } else {
                player.sendMessage("Please install mod (server redirect)");
            }
        }, 40);
    }

    private void startRedirectCountdown(Player player) {
        FileConfiguration config = plugin.getConfig();
        int delayTicks = 100;
        final AtomicInteger ticksLeft = new AtomicInteger(delayTicks);

        String raw = config.getString("auto-redirect.target-address", "127.0.0.1:25565").trim();
        final String address = raw.replaceAll("[^0-9.:]", "");
        float volume = 1F;
        float pitch = 1F;

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int currentTicks = ticksLeft.get();

            if (currentTicks <= 0) {
                player.sendActionBar((ComponentLike)
                        Component.text("Connecting...", NamedTextColor.GREEN));
                Bukkit.getScheduler().runTaskLater(plugin, () -> finalizeRedirect(player, address), 20);
                return;
            }

            if (currentTicks % 20 == 0) {
                try {
                    player.sendActionBar(Component.text("Connecting in: " + (currentTicks / 20), NamedTextColor.AQUA));
                    player.playSound(player.getLocation(), Sound.valueOf(String.valueOf(Sound.ENTITY_EXPERIENCE_ORB_PICKUP)), volume, pitch);
                } catch (Exception ignored) {}
            }

            ticksLeft.addAndGet(-20);
        }, 0L, 20L);
        Bukkit.getScheduler().runTaskLater(plugin, task::cancel, delayTicks + 1);
    }

    private void finalizeRedirect(Player player, String address) {
        String[] parts = address.split(":");
        String ip = parts[0];
        int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 25565;

        plugin.getLogger().info("Trying redirect " + player.getName() + " on " + ip + ", " + port);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (isServerReachable(ip, port)) {
                Bukkit.getScheduler().runTask(plugin, () -> ServerRedirect.sendTo(player, address));
            } else {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendActionBar((ComponentLike)
                            Component.text("Server currently offline", NamedTextColor.DARK_RED));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> startRedirectCountdown(player), 600);
                });
            }
        });
    }

    private boolean isServerReachable(String ip, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), 5000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}