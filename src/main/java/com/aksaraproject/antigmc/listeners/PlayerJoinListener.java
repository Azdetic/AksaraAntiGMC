package com.aksaraproject.antigmc.listeners;

import com.aksaraproject.antigmc.AksaraAntiGMC;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Listener to handle player join/quit events for Creative mode safety
 * - Tracks players who quit in Creative mode
 * - Forces them to Survival on rejoin
 */
public class PlayerJoinListener implements Listener {

    private final AksaraAntiGMC plugin;
    // Track players who quit while in Creative mode
    private final Set<UUID> creativePlayers = new HashSet<>();

    public PlayerJoinListener(AksaraAntiGMC plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        plugin.getConfigManager().debug("=== PlayerJoinEvent ===");
        plugin.getConfigManager().debug("Player: " + player.getName());
        plugin.getConfigManager().debug("Current GameMode: " + player.getGameMode());
        plugin.getConfigManager().debug("Is OP: " + player.isOp());
        plugin.getConfigManager().debug("Was Creative on quit: " + creativePlayers.contains(player.getUniqueId()));
        
        // If player is OP, skip safety measures
        if (player.isOp()) {
            plugin.getConfigManager().debug("Player is OP, skipping Creative safety check");
            return;
        }
        
        // If player joins in Creative mode OR was in Creative when they quit, force to Survival
        if (player.getGameMode() == GameMode.CREATIVE || creativePlayers.contains(player.getUniqueId())) {
            plugin.getConfigManager().debug("Player was in Creative mode, forcing to Survival for safety");
            
            // Clear any pending confirmations
            if (plugin.getConfirmationManager().hasPendingConfirmation(player.getUniqueId())) {
                plugin.getConfirmationManager().cancelConfirmation(player.getUniqueId());
            }
            
            // Load their survival inventory
            plugin.getInventoryManager().handleGamemodeChange(player, GameMode.SURVIVAL);
            player.setGameMode(GameMode.SURVIVAL);
            
            player.sendMessage(plugin.getConfigManager().getMessage("prefix") + 
                    "&eYou have been set to Survival mode for safety.");
            
            // Remove from tracking
            creativePlayers.remove(player.getUniqueId());
            plugin.getConfigManager().debug("Player gamemode set to SURVIVAL");
        }
        
        plugin.getConfigManager().debug("=== End PlayerJoinEvent ===");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        plugin.getConfigManager().debug("=== PlayerQuitEvent ===");
        plugin.getConfigManager().debug("Player: " + player.getName());
        plugin.getConfigManager().debug("Current GameMode: " + player.getGameMode());
        
        // Track if player quits in Creative mode
        if (player.getGameMode() == GameMode.CREATIVE && !player.isOp()) {
            creativePlayers.add(player.getUniqueId());
            plugin.getConfigManager().debug("Player quit in Creative mode, tracking for safety on rejoin");
        } else {
            creativePlayers.remove(player.getUniqueId());
        }
        
        plugin.getConfigManager().debug("=== End PlayerQuitEvent ===");
    }
}
