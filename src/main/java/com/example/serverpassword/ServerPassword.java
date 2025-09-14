package com.example.serverpassword;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ServerPassword extends JavaPlugin implements Listener {

    private final String PASSWORD = "valiant123!@#"; // your password here
    private final Set<UUID> locked = new HashSet<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("ServerPassword enabled.");
    }

    @Override
    public void onDisable() {
        locked.clear();
        getLogger().info("ServerPassword disabled.");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        lockPlayer(player);

        player.sendMessage(ChatColor.RED + "Please type the server password in chat.");

        // Kick after 15 seconds if still locked
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (locked.contains(player.getUniqueId()) && player.isOnline()) {
                player.kickPlayer(ChatColor.RED + "Login timed out!");
            }
        }, 20L * 15); // 15 seconds
    }

    private void lockPlayer(Player player) {
        locked.add(player.getUniqueId());
        player.setInvulnerable(true);
    }

    private void unlockPlayer(Player player) {
        locked.remove(player.getUniqueId());
        player.setInvulnerable(false);
        player.sendMessage(ChatColor.GREEN + "Login successful! Welcome.");
    }

    // Movement freeze
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (locked.contains(event.getPlayer().getUniqueId())) {
            if (event.getFrom().getBlockX() != event.getTo().getBlockX()
             || event.getFrom().getBlockY() != event.getTo().getBlockY()
             || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
                event.setTo(event.getFrom());
            }
        }
    }

    // Handle chat password input
    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!locked.contains(player.getUniqueId())) return;

        event.setCancelled(true); // don’t broadcast password

        String message = event.getMessage().trim();
        if (message.equals(PASSWORD)) {
            Bukkit.getScheduler().runTask(this, () -> unlockPlayer(player));
        } else {
            Bukkit.getScheduler().runTask(this, () ->
                    player.kickPlayer(ChatColor.RED + "Incorrect password!"));
        }
    }

    // Block commands
    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (locked.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    // Block damage
    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (locked.contains(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    // Block block breaking
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (locked.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    // Block block placing
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (locked.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    // Cleanup on quit
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        locked.remove(event.getPlayer().getUniqueId());
    }
}
