package com.example.serverpassword;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class ServerPassword extends JavaPlugin implements Listener {

    private final String PASSWORD = "valiant123!@#"; // your password here
    private final Set<UUID> locked = new HashSet<>();
    private final Map<UUID, StringBuilder> inputs = new HashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("ServerPassword enabled.");
    }

    @Override
    public void onDisable() {
        locked.clear();
        inputs.clear();
        getLogger().info("ServerPassword disabled.");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        lockPlayer(player);

        openPasswordGUI(player);

        // Kick after 15 seconds if still locked
        Bukkit.getScheduler().runTaskLater(this, new Runnable() {
            @Override
            public void run() {
                if (locked.contains(player.getUniqueId()) && player.isOnline()) {
                    player.kickPlayer(ChatColor.RED + "Login timed out!");
                }
            }
        }, 20L * 15); // 15s
    }

    private void lockPlayer(Player player) {
        locked.add(player.getUniqueId());
        inputs.put(player.getUniqueId(), new StringBuilder());
        player.setInvulnerable(true);
        player.setWalkSpeed(0f);
        player.setFlySpeed(0f);
    }

    private void unlockPlayer(Player player) {
        locked.remove(player.getUniqueId());
        inputs.remove(player.getUniqueId());
        player.setInvulnerable(false);
        player.setWalkSpeed(0.2f);
        player.setFlySpeed(0.1f);
        player.sendMessage(ChatColor.GREEN + "Login successful! Welcome.");
    }

    private void openPasswordGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.DARK_RED + "Enter Password");

        // Add "Submit" button
        ItemStack submit = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta meta = submit.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Submit");
        submit.setItemMeta(meta);
        gui.setItem(26, submit);

        // Add some placeholder items for input (digits 0–8)
        for (int i = 0; i < 9; i++) {
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta im = item.getItemMeta();
            im.setDisplayName(ChatColor.YELLOW + String.valueOf(i));
            item.setItemMeta(im);
            gui.setItem(i, item);
        }

        player.openInventory(gui);
    }

    // Handle clicks in the password GUI
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (!locked.contains(player.getUniqueId())) return;
        if (!event.getView().getTitle().contains("Enter Password")) return;

        event.setCancelled(true); // Prevent item pickup

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

        if (name.equalsIgnoreCase("Submit")) {
            String attempt = inputs.get(player.getUniqueId()).toString();
            if (attempt.equals(PASSWORD)) {
                unlockPlayer(player);
                player.closeInventory();
            } else {
                player.kickPlayer(ChatColor.RED + "Incorrect password!");
            }
        } else {
            // Append input (digits for now)
            inputs.get(player.getUniqueId()).append(name);
            player.sendMessage(ChatColor.YELLOW + "Password: " + inputs.get(player.getUniqueId()));
        }
    }

    // If they close GUI while still locked → kick
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        if (locked.contains(player.getUniqueId())) {
            player.kickPlayer(ChatColor.RED + "You must enter the password!");
        }
    }

    // Cancel movement, chat, commands, damage, block actions
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (locked.contains(event.getPlayer().getUniqueId())) {
            if (!event.getFrom().toVector().equals(event.getTo().toVector())) {
                event.setTo(event.getFrom());
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (locked.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (locked.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (locked.contains(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (locked.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (locked.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        locked.remove(event.getPlayer().getUniqueId());
        inputs.remove(event.getPlayer().getUniqueId());
    }
}
