package com.aksaraproject.antigmc.commands;

import com.aksaraproject.antigmc.AksaraAntiGMC;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GMCCommand implements CommandExecutor, TabCompleter {

    private final AksaraAntiGMC plugin;

    public GMCCommand(AksaraAntiGMC plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        plugin.getConfigManager().debug("[GMCCommand] Executed by " + sender.getName() + " with args: [" + String.join(", ", args) + "]");

        // Handle "/gmc confirm" command - Any player with pending confirmation can use this
        if (args.length > 0 && args[0].equalsIgnoreCase("confirm")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("&cConsole cannot confirm GMC.");
                return true;
            }

            Player player = (Player) sender;
            plugin.getConfigManager().debug("[GMCCommand] === CONFIRM ATTEMPT ===");
            plugin.getConfigManager().debug("[GMCCommand] Player: " + player.getName());
            plugin.getConfigManager().debug("[GMCCommand] UUID: " + player.getUniqueId());
            plugin.getConfigManager().debug("[GMCCommand] Has pending confirmation: " + plugin.getConfirmationManager().hasPendingConfirmation(player.getUniqueId()));
            
            if (!plugin.getConfirmationManager().hasPendingConfirmation(player.getUniqueId())) {
                String noPendingMsg = plugin.getConfigManager().getMessage("gmc.no-pending-confirmation");
                player.sendMessage(noPendingMsg);
                plugin.getConfigManager().debug("[GMCCommand] RESULT: No pending confirmation found");
                return true;
            }

            // Confirmation exists, proceed with gamemode change
            plugin.getConfigManager().debug("[GMCCommand] RESULT: Confirmation found - proceeding with gamemode change");
            
            // NOTE: Don't cancel confirmation yet! GameModeListener needs to see it
            // Switch Gamemode and Inventory
            plugin.getConfigManager().debug("[GMCCommand] Swapping inventory for " + player.getName());
            plugin.getInventoryManager().handleGamemodeChange(player, GameMode.CREATIVE);
            player.setGameMode(GameMode.CREATIVE);
            plugin.getConfigManager().debug("[GMCCommand] Gamemode changed to CREATIVE for " + player.getName());
            
            // NOW cancel the confirmation after gamemode change is complete
            plugin.getConfirmationManager().cancelConfirmation(player.getUniqueId());
            
            player.sendMessage(plugin.getConfigManager().getMessage("gmc.switched"));
            plugin.getConfigManager().debug("[GMCCommand] === CONFIRM COMPLETE ===");
            return true;
        }

        // For other commands, check permission
        if (!sender.hasPermission("aksaraantigmc.gmc")) {
            plugin.getConfigManager().debug("Permission denied for " + sender.getName() + " (aksaraantigmc.gmc)");
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        // Check if confirmation system is enabled
        boolean confirmationEnabled = plugin.getConfig().getBoolean("gmc-confirmation.enabled", true);
        plugin.getConfigManager().debug("Confirmation system enabled: " + confirmationEnabled);

        // Regular GMC command logic
        Player target;
        boolean isAdminTargeting = false;
        
        if (args.length > 0) {
            // Admin is targeting another player
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                plugin.getConfigManager().debug("Target player not found: " + args[0]);
                sender.sendMessage(plugin.getConfigManager().getMessage("prefix") + "&cPlayer not found.");
                return true;
            }
            // Check if targeting self
            if (sender instanceof Player && target.equals(sender)) {
                isAdminTargeting = false; // Self-targeting
                plugin.getConfigManager().debug("Player " + sender.getName() + " targeting self");
            } else {
                isAdminTargeting = true;
                plugin.getConfigManager().debug("Admin " + sender.getName() + " targeting player: " + target.getName());
            }
        } else if (sender instanceof Player) {
            // Player is using /gmc on themselves (self-GMC)
            target = (Player) sender;
            plugin.getConfigManager().debug("Player " + sender.getName() + " using /gmc on self");
        } else {
            sender.sendMessage("&cConsole must specify a player.");
            return true;
        }

        plugin.getConfigManager().debug("Target player: " + target.getName() + ", Current Gamemode: " + target.getGameMode() + ", IsAdminTargeting: " + isAdminTargeting);

        // Check if already GMC
        if (target.getGameMode() == GameMode.CREATIVE) {
            plugin.getConfigManager().debug("Target already in Creative mode.");
            sender.sendMessage(plugin.getConfigManager().getMessage("prefix") + "&cTarget is already in Creative mode.");
            return true;
        }

        // Check if target is OP - OPs can bypass confirmation
        if (target.isOp()) {
            plugin.getConfigManager().debug("Target " + target.getName() + " is OP, bypassing confirmation and changing gamemode immediately");
            plugin.getInventoryManager().handleGamemodeChange(target, GameMode.CREATIVE);
            target.setGameMode(GameMode.CREATIVE);
            target.sendMessage(plugin.getConfigManager().getMessage("gmc.switched"));
            if (!sender.equals(target)) {
                sender.sendMessage(plugin.getConfigManager().getMessage("prefix") + "&aSuccessfully set gamemode for " + target.getName());
            }
            return true;
        }

        // Spam Warning 5x with sound effect
        String warningMsg = plugin.getConfigManager().getMessage("gmc.warning-message");
        plugin.getConfigManager().debug("[GMCCommand] Sending 5 warning messages to " + target.getName());
        for (int i = 0; i < 5; i++) {
            target.sendMessage(warningMsg);
            // Play warning sound
            target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.5f);
        }

        // Title and Subtitle
        String titleText = plugin.getConfigManager().getRawMessage("gmc.title");
        String subtitleText = plugin.getConfigManager().getRawMessage("gmc.subtitle");
        plugin.getConfigManager().debug("Sending title: " + titleText + ", subtitle: " + subtitleText);
        target.sendTitle(titleText, subtitleText, 10, 70, 20);

        // If confirmation is enabled, use confirmation system
        if (confirmationEnabled) {
            int timeoutSeconds = plugin.getConfig().getInt("gmc-confirmation.timeout-seconds", 30);
            
            // Add pending confirmation
            plugin.getConfirmationManager().addPendingConfirmation(target, sender.getName());
            
            // Send confirmation instruction to target player
            String confirmationMsg = plugin.getConfigManager().getMessage("gmc.confirmation-required")
                    .replace("%seconds%", String.valueOf(timeoutSeconds));
            target.sendMessage(confirmationMsg);
            
            // Notify admin/sender
            String confirmationSentMsg = plugin.getConfigManager().getMessage("gmc.confirmation-sent")
                    .replace("%player%", target.getName())
                    .replace("%seconds%", String.valueOf(timeoutSeconds));
            sender.sendMessage(confirmationSentMsg);
            
            plugin.getConfigManager().debug("Pending confirmation created for " + target.getName() + 
                    " by " + sender.getName() + " (timeout: " + timeoutSeconds + "s)");
        } else {
            // Confirmation disabled: execute immediately
            plugin.getConfigManager().debug("Confirmation disabled, initiating immediate gamemode change for " + target.getName());
            plugin.getInventoryManager().handleGamemodeChange(target, GameMode.CREATIVE);
            target.setGameMode(GameMode.CREATIVE);
            plugin.getConfigManager().debug("Gamemode changed to CREATIVE for " + target.getName());
            
            target.sendMessage(plugin.getConfigManager().getMessage("gmc.switched"));
            if (!sender.equals(target)) {
                sender.sendMessage(plugin.getConfigManager().getMessage("prefix") + "&aSuccessfully set gamemode for " + target.getName());
            }
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            
            // Add "confirm" if player has pending confirmation
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (plugin.getConfirmationManager().hasPendingConfirmation(player.getUniqueId())) {
                    if ("confirm".startsWith(input)) {
                        completions.add("confirm");
                    }
                }
            }
            
            // Add online player names if sender has admin permission
            if (sender.hasPermission("aksaraantigmc.gmc")) {
                List<String> playerNames = Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(input))
                        .collect(Collectors.toList());
                completions.addAll(playerNames);
            }
        }
        
        return completions;
    }
}
