package com.aksaraproject.antigmc.listeners;

import com.aksaraproject.antigmc.AksaraAntiGMC;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

/**
 * Listener to detect all gamemode changes (including via /gamemode command)
 * Ensures inventory swap happens for any Creative mode entry, unless player is OP
 */
public class GameModeListener implements Listener {

    private final AksaraAntiGMC plugin;

    public GameModeListener(AksaraAntiGMC plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        if (event.isCancelled()) return;
        
        Player player = event.getPlayer();
        GameMode newMode = event.getNewGameMode();
        GameMode oldMode = player.getGameMode();

        plugin.getConfigManager().debug("=== GameModeChangeEvent ===");
        plugin.getConfigManager().debug("Player: " + player.getName());
        plugin.getConfigManager().debug("From: " + oldMode + " To: " + newMode);
        plugin.getConfigManager().debug("Is OP: " + player.isOp());
        plugin.getConfigManager().debug("Has Pending Confirmation: " + plugin.getConfirmationManager().hasPendingConfirmation(player.getUniqueId()));

        // Only OP players can bypass the restrictions
        if (player.isOp()) {
            plugin.getConfigManager().debug("Player " + player.getName() + " is OP, allowing gamemode change without restrictions");
            return;
        }

        // If changing TO Creative mode and not already in Creative
        if (newMode == GameMode.CREATIVE && oldMode != GameMode.CREATIVE) {
            plugin.getConfigManager().debug("Player " + player.getName() + " attempting to enter Creative mode via external command/plugin");
            
            // Check if confirmation system is enabled
            boolean confirmationEnabled = plugin.getConfig().getBoolean("gmc-confirmation.enabled", true);
            plugin.getConfigManager().debug("Confirmation system enabled: " + confirmationEnabled);
            
            // If player has pending confirmation, they're coming from /gmc confirm - ALLOW
            if (plugin.getConfirmationManager().hasPendingConfirmation(player.getUniqueId())) {
                plugin.getConfigManager().debug("Player has pending confirmation, allowing gamemode change (from /gmc confirm)");
                // The confirmation is already cancelled in GMCCommand, don't cancel here
                return;
            }
            
            // No pending confirmation - this is an unauthorized attempt
            if (confirmationEnabled) {
                plugin.getConfigManager().debug("No pending confirmation - cancelling event and creating confirmation request");
                event.setCancelled(true);
                
                // Create pending confirmation for this player
                int timeoutSeconds = plugin.getConfig().getInt("gmc-confirmation.timeout-seconds", 30);
                plugin.getConfirmationManager().addPendingConfirmation(player, "Server");
                
                // Send warning messages to player
                String warningMsg = plugin.getConfigManager().getMessage("gmc.warning-message");
                for (int i = 0; i < 5; i++) {
                    player.sendMessage(warningMsg);
                }
                
                // Title and Subtitle
                String titleText = plugin.getConfigManager().getRawMessage("gmc.title");
                String subtitleText = plugin.getConfigManager().getRawMessage("gmc.subtitle");
                player.sendTitle(titleText, subtitleText, 10, 70, 20);
                
                // Send confirmation instruction
                String confirmationMsg = plugin.getConfigManager().getMessage("gmc.confirmation-required")
                        .replace("%seconds%", String.valueOf(timeoutSeconds));
                player.sendMessage(confirmationMsg);
                
                // Send unauthorized message
                player.sendMessage(plugin.getConfigManager().getMessage("gmc.unauthorized"));
                
                plugin.getConfigManager().debug("Created confirmation request for " + player.getName() + " (timeout: " + timeoutSeconds + "s)");
                return;
            } else {
                // Confirmation is disabled, handle inventory swap
                plugin.getConfigManager().debug("Confirmation disabled, handling inventory swap for " + player.getName());
                plugin.getInventoryManager().handleGamemodeChange(player, GameMode.CREATIVE);
                
                String warningMsg = plugin.getConfigManager().getMessage("gmc.warning-message");
                for (int i = 0; i < 5; i++) {
                    player.sendMessage(warningMsg);
                }
                
                String titleText = plugin.getConfigManager().getRawMessage("gmc.title");
                String subtitleText = plugin.getConfigManager().getRawMessage("gmc.subtitle");
                player.sendTitle(titleText, subtitleText, 10, 70, 20);
                
                player.sendMessage(plugin.getConfigManager().getMessage("gmc.switched"));
            }
        }
        
        // If changing FROM Creative mode TO another mode
        if (oldMode == GameMode.CREATIVE && newMode != GameMode.CREATIVE) {
            plugin.getConfigManager().debug("Player " + player.getName() + " leaving Creative mode to " + newMode);
            plugin.getConfigManager().debug("Handling inventory swap back to " + newMode);
            
            // Handle inventory swap back
            plugin.getInventoryManager().handleGamemodeChange(player, newMode);
        }
        
        plugin.getConfigManager().debug("=== End GameModeChangeEvent ===");
    }
}
