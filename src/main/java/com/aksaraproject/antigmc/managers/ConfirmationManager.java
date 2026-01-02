package com.aksaraproject.antigmc.managers;

import com.aksaraproject.antigmc.AksaraAntiGMC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ConfirmationManager {

    private final AksaraAntiGMC plugin;
    private final Map<UUID, PendingConfirmation> pendingConfirmations;

    public ConfirmationManager(AksaraAntiGMC plugin) {
        this.plugin = plugin;
        this.pendingConfirmations = new HashMap<>();
    }

    /**
     * Add a pending confirmation request for a player
     * @param player The target player who needs to confirm
     * @param adminName The name of the admin who initiated the request
     */
    public void addPendingConfirmation(Player player, String adminName) {
        UUID playerId = player.getUniqueId();
        
        // Cancel existing confirmation if any
        if (pendingConfirmations.containsKey(playerId)) {
            cancelConfirmation(playerId);
        }

        // Get timeout from config
        int timeoutSeconds = plugin.getConfig().getInt("gmc-confirmation.timeout-seconds", 30);
        
        // Create expiration task
        BukkitTask expirationTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pendingConfirmations.containsKey(playerId)) {
                Player p = Bukkit.getPlayer(playerId);
                if (p != null && p.isOnline()) {
                    String expiredMessage = plugin.getConfigManager().getMessage("gmc.confirmation-expired");
                    p.sendMessage(expiredMessage);
                }
                pendingConfirmations.remove(playerId);
                plugin.getConfigManager().debug("GMC confirmation expired for " + player.getName());
            }
        }, timeoutSeconds * 20L); // Convert seconds to ticks (20 ticks = 1 second)

        // Store the pending confirmation
        PendingConfirmation confirmation = new PendingConfirmation(adminName, expirationTask, timeoutSeconds);
        pendingConfirmations.put(playerId, confirmation);
        
        plugin.getConfigManager().debug("Added pending GMC confirmation for " + player.getName() + 
                " (timeout: " + timeoutSeconds + " seconds, admin: " + adminName + ")");
    }

    /**
     * Check if a player has a pending confirmation
     * @param playerId The player's UUID
     * @return true if there is a pending confirmation
     */
    public boolean hasPendingConfirmation(UUID playerId) {
        return pendingConfirmations.containsKey(playerId);
    }

    /**
     * Get the pending confirmation for a player
     * @param playerId The player's UUID
     * @return The PendingConfirmation or null if none exists
     */
    public PendingConfirmation getPendingConfirmation(UUID playerId) {
        return pendingConfirmations.get(playerId);
    }

    /**
     * Remove and cancel a pending confirmation
     * @param playerId The player's UUID
     */
    public void cancelConfirmation(UUID playerId) {
        PendingConfirmation confirmation = pendingConfirmations.remove(playerId);
        if (confirmation != null) {
            confirmation.getExpirationTask().cancel();
            plugin.getConfigManager().debug("Cancelled GMC confirmation for player UUID: " + playerId);
        }
    }

    /**
     * Clear all pending confirmations (used on plugin disable)
     */
    public void clearAll() {
        for (PendingConfirmation confirmation : pendingConfirmations.values()) {
            confirmation.getExpirationTask().cancel();
        }
        pendingConfirmations.clear();
        plugin.getConfigManager().debug("Cleared all pending GMC confirmations");
    }

    /**
     * Inner class to store pending confirmation data
     */
    public static class PendingConfirmation {
        private final String adminName;
        private final BukkitTask expirationTask;
        private final int timeoutSeconds;

        public PendingConfirmation(String adminName, BukkitTask expirationTask, int timeoutSeconds) {
            this.adminName = adminName;
            this.expirationTask = expirationTask;
            this.timeoutSeconds = timeoutSeconds;
        }

        public String getAdminName() {
            return adminName;
        }

        public BukkitTask getExpirationTask() {
            return expirationTask;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }
    }
}
