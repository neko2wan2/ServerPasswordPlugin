package com.example.serverpassword;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.HashSet;
import java.util.Set;

public class ServerPassword extends JavaPlugin implements Listener {

    private final String PASSWORD = "valiant123!@#"; // your password here
    private Set<Player> loggedIn = new HashSet<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("ServerPassword enabled.");
    }

    @Override
    public void onDisable() {
        loggedIn.clear();
        getLogger().info("ServerPassword disabled.");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        loggedIn.remove(player);

        player.sendMessage(ChatColor.RED + "Please login with /login <password> or you will be kicked.");
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!loggedIn.contains(player) && player.isOnline()) {
                player.kickPlayer(ChatColor.RED + "You failed to login with the server password.");
            }
        }, 200L); // 10 seconds
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        loggedIn.remove(event.getPlayer());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        Player player = (Player) sender;

        if (cmd.getName().equalsIgnoreCase("login")) {
            if (args.length == 0) {
                player.sendMessage(ChatColor.RED + "Usage: /login <password>");
                return true;
            }
            if (args[0].equals(PASSWORD)) {
                loggedIn.add(player);
                player.sendMessage(ChatColor.GREEN + "Login successful! Welcome.");
            } else {
                player.kickPlayer(ChatColor.RED + "Incorrect password!");
            }
            return true;
        }
        return false;
    }
}
