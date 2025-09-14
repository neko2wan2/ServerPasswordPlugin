package com.example.serverpassword;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ServerPassword extends JavaPlugin implements Listener {

    private final String PASSWORD = "valiant123!@#";
    private final Set<UUID> locked = new HashSet<>();
    private ProtocolManager protocolManager;

    @Override
    public void onEnable() {
        protocolManager = ProtocolLibrary.getProtocolManager();
        Bukkit.getPluginManager().registerEvents(this, this);
        registerPacketListener();
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

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (locked.contains(player.getUniqueId()) && player.isOnline()) {
                player.kickPlayer(ChatColor.RED + "Login timed out!");
            }
        }, 20L * 15); // 15 seconds
    }

    private void lockPlayer(Player player) {
        locked.add(player.getUniqueId());

        // Fully freeze player with potion effects
        player.setInvulnerable(true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 10, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 128, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, Integer.MAX_VALUE, 255, false, false));
    }

    private void unlockPlayer(Player player) {
        locked.remove(player.getUniqueId());
        player.setInvulnerable(false);

        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        player.sendMessage(ChatColor.GREEN + "Login successful! Welcome.");
    }

    // Handle chat for password input
    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (locked.contains(player.getUniqueId())) {
            event.setCancelled(true); // hide message
            if (event.getMessage().equals(PASSWORD)) {
                Bukkit.getScheduler().runTask(this, () -> unlockPlayer(player));
            } else {
                Bukkit.getScheduler().runTask(this, () -> player.kickPlayer(ChatColor.RED + "Incorrect password!"));
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        locked.remove(event.getPlayer().getUniqueId());
    }

    // --- PROTOCOLLIB PACKET LISTENER ---
    private void registerPacketListener() {
        PacketListener listener = new PacketAdapter(this, PacketType.Play.Client.FLYING,
                PacketType.Play.Client.POSITION,
                PacketType.Play.Client.POSITION_LOOK,
                PacketType.Play.Client.LOOK,
                PacketType.Play.Client.ARM_ANIMATION,
                PacketType.Play.Client.USE_ENTITY,
                PacketType.Play.Client.ENTITY_ACTION,
                PacketType.Play.Client.WINDOW_CLICK,
                PacketType.Play.Client.HELD_ITEM_SLOT,
                PacketType.Play.Client.BLOCK_DIG,
                PacketType.Play.Client.BLOCK_PLACE) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                if (locked.contains(player.getUniqueId())) {
                    event.setCancelled(true); // block all client input except chat
                }
            }
        };
        protocolManager.addPacketListener(listener);
    }
}
