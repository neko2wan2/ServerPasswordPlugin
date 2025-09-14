package com.example.serverpassword;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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

        player.sendMessage(ChatColor.RED + "Please type the server password in chat to login.");
        player.sendMessage(ChatColor.GRAY + "Hint: Press 'T' to open chat and enter your password.");

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
        player.setWalkSpeed(0f);
        player.setFlySpeed(0f);
        player.setGameMode(GameMode.ADVENTURE); // prevent breaking/placing blocks

        // Give blindness effect indefinitely while logging in
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, false));
    }

    private void unlockPlayer(Player player) {
        locked.remove(player.getUniqueId());
        player.setInvulnerable(false);
        player.setWalkSpeed(0.2f);
        player.setFlySpeed(0.1f);
        player.setGameMode(GameMode.SURVIVAL);

        // Remove all potion effects including blindness
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        player.sendMessage(ChatColor.GREEN + "Login successful! Welcome to the Valiant Server!");
    }

    // Handle chat for password input only
    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (locked.contains(player.getUniqueId())) {
            event.setCancelled(true); // do not show to others
            String message = event.getMessage();

            if (message.equals(PASSWORD)) {
                Bukkit.getScheduler().runTask(this, () -> unlockPlayer(player));
            } else {
                Bukkit.getScheduler().runTask(this, () -> player.kickPlayer(ChatColor.RED + "Incorrect password!"));
            }
        }
    }

    // Prevent commands while locked
    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (locked.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You must login first.");
        }
    }

    // Prevent movement
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (locked.contains(event.getPlayer().getUniqueId())) {
            if (!event.getFrom().equals(event.getTo())) {
                event.setTo(event.getFrom());
            }
        }
    }

    // Prevent damage to locked players
    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (locked.contains(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    // Prevent locked players from ATTACKING others
    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            if (locked.contains(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    // Prevent block breaking
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (locked.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    // Prevent block placing
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (locked.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    // Prevent all interactions (clicks, item use, etc.)
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (locked.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        locked.remove(event.getPlayer().getUniqueId());
    }
}
