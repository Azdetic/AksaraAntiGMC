package com.aksaraproject.antigmc.commands;

import com.aksaraproject.antigmc.AksaraAntiGMC;
import com.aksaraproject.antigmc.database.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MainCommand implements CommandExecutor {

    private final AksaraAntiGMC plugin;

    public MainCommand(AksaraAntiGMC plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        plugin.getConfigManager().debug("MainCommand executed by " + sender.getName() + " with args: " + String.join(", ", args));
        
        if (!sender.hasPermission("aksaraantigmc.admin")) {
            plugin.getConfigManager().debug("Permission denied for " + sender.getName());
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.getConfigManager().debug("Reload command executed");
            plugin.reloadConfig();
            plugin.getConfigManager().loadMessages();
            sender.sendMessage(plugin.getConfigManager().getMessage("admin.reload"));
            return true;
        }

        if (args[0].equalsIgnoreCase("restoreinventory") || args[0].equalsIgnoreCase("restore")) {
            return handleRestoreInventory(sender, args);
        }

        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.getConfigManager().getMessage("prefix") + "&e=== AksaraAntiGMC Commands ===");
        sender.sendMessage("&7/aksaraantigmc reload &8- &fReload config");
        sender.sendMessage("&7/aksaraantigmc restore <player> &8- &fList inventory backups");
        sender.sendMessage("&7/aksaraantigmc restore <player> <id> &8- &fRestore specific backup");
    }

    private boolean handleRestoreInventory(CommandSender sender, String[] args) {
        plugin.getConfigManager().debug("RestoreInventory command executed");
        
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getMessage("prefix") + "&eUsage: /aksaraantigmc restore <player> [backup_id]");
            return true;
        }
        
        String playerName = args[1];
        plugin.getConfigManager().debug("Looking up player: " + playerName);
        
        // Try to get player (online first, then offline)
        Player onlinePlayer = plugin.getServer().getPlayer(playerName);
        OfflinePlayer offlinePlayer = null;
        java.util.UUID playerUUID = null;
        
        if (onlinePlayer != null) {
            playerUUID = onlinePlayer.getUniqueId();
        } else {
            // Try offline player
            @SuppressWarnings("deprecation")
            OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
            if (op.hasPlayedBefore()) {
                offlinePlayer = op;
                playerUUID = op.getUniqueId();
            }
        }
        
        if (playerUUID == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("prefix") + "&cPlayer not found: " + playerName);
            return true;
        }
        
        // If no backup ID provided, list available backups
        if (args.length < 3) {
            return listBackups(sender, playerName, playerUUID);
        }
        
        // Restore specific backup by ID
        int backupId;
        try {
            backupId = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getConfigManager().getMessage("prefix") + "&cInvalid backup ID: " + args[2]);
            return true;
        }
        
        return restoreBackup(sender, playerName, playerUUID, backupId, onlinePlayer);
    }

    private boolean listBackups(CommandSender sender, String playerName, java.util.UUID playerUUID) {
        List<DatabaseManager.InventoryRecord> history = plugin.getDatabaseManager().getInventoryHistory(playerUUID);
        
        if (history.isEmpty()) {
            sender.sendMessage(plugin.getConfigManager().getMessage("prefix") + "&cNo inventory backups found for " + playerName);
            return true;
        }
        
        sender.sendMessage(plugin.getConfigManager().getMessage("prefix") + "&e=== Inventory Backups for " + playerName + " ===");
        sender.sendMessage("&7ID | Date | Gamemode");
        sender.sendMessage("&8" + "─".repeat(40));
        
        for (DatabaseManager.InventoryRecord record : history) {
            String color = record.gamemode.equals("CREATIVE") ? "&b" : "&a";
            sender.sendMessage("&f" + record.id + " &8| &7" + record.formattedDate + " &8| " + color + record.gamemode);
        }
        
        sender.sendMessage("&8" + "─".repeat(40));
        sender.sendMessage("&eUse: &f/aksaraantigmc restore " + playerName + " <id>");
        return true;
    }

    private boolean restoreBackup(CommandSender sender, String playerName, java.util.UUID playerUUID, int backupId, Player onlinePlayer) {
        // Check if player is online for restore
        if (onlinePlayer == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("prefix") + "&cPlayer must be online to restore inventory.");
            return true;
        }
        
        ItemStack[] items = plugin.getDatabaseManager().loadInventoryById(backupId);
        
        if (items == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("prefix") + "&cBackup ID " + backupId + " not found.");
            return true;
        }
        
        plugin.getConfigManager().debug("Restoring inventory ID " + backupId + " with " + items.length + " slots");
        plugin.getInventoryManager().restoreInventory(onlinePlayer, items);
        
        sender.sendMessage(plugin.getConfigManager().getMessage("admin.restore-success")
                .replace("%player%", onlinePlayer.getName())
                .replace("%date%", "Backup #" + backupId));
        
        onlinePlayer.sendMessage(plugin.getConfigManager().getMessage("prefix") + "&aYour inventory has been restored by an admin.");
        
        return true;
    }
}
