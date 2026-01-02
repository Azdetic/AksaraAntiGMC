package com.aksaraproject.antigmc.commands;

import com.aksaraproject.antigmc.AksaraAntiGMC;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

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
            sender.sendMessage(plugin.getConfigManager().getMessage("prefix") + "&eUsage: /aksaraantigmc <restoreinventory|reload>");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.getConfigManager().debug("Reload command executed");
            plugin.reloadConfig();
            plugin.getConfigManager().loadMessages();
            sender.sendMessage(plugin.getConfigManager().getMessage("admin.reload"));
            return true;
        }

        if (args[0].equalsIgnoreCase("restoreinventory")) {
            plugin.getConfigManager().debug("RestoreInventory command executed");
            if (args.length < 2) {
                sender.sendMessage(plugin.getConfigManager().getMessage("prefix") + "&eUsage: /aksaraantigmc restoreinventory <player> [gamemode]");
                return true;
            }
            
            String playerName = args[1];
            plugin.getConfigManager().debug("Attempting to restore inventory for: " + playerName);
            
            Player target = plugin.getServer().getPlayer(playerName);
            if (target == null) {
                plugin.getConfigManager().debug("Player not found or offline: " + playerName);
                 sender.sendMessage(plugin.getConfigManager().getMessage("prefix") + "&cPlayer must be online to restore directly (for safety).");
                 return true;
            }
            
            // Assume restoring Survival inventory by default
            ItemStack[] items = plugin.getDatabaseManager().loadLatestInventory(target.getUniqueId(), GameMode.SURVIVAL);
            if (items != null) {
                plugin.getConfigManager().debug("Restoring inventory with " + items.length + " slots");
                plugin.getInventoryManager().restoreInventory(target, items);
                sender.sendMessage(plugin.getConfigManager().getMessage("admin.restore-success")
                        .replace("%player%", target.getName())
                        .replace("%date%", "Latest Backup"));
            } else {
                plugin.getConfigManager().debug("No inventory data found to restore");
                sender.sendMessage(plugin.getConfigManager().getMessage("admin.restore-fail"));
            }
            return true;
        }

        return true;
    }
}
