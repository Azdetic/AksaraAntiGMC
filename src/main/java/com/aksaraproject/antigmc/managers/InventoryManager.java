package com.aksaraproject.antigmc.managers;

import com.aksaraproject.antigmc.AksaraAntiGMC;
import com.aksaraproject.antigmc.database.DatabaseManager;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class InventoryManager {

    private final AksaraAntiGMC plugin;
    private final DatabaseManager databaseManager;

    public InventoryManager(AksaraAntiGMC plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    public void handleGamemodeChange(Player player, GameMode newMode) {
        GameMode currentMode = player.getGameMode();
        UUID uuid = player.getUniqueId();

        plugin.getConfigManager().debug("handleGamemodeChange called for " + player.getName());
        plugin.getConfigManager().debug("Current mode: " + currentMode + ", New mode: " + newMode);

        // 1. Save current inventory
        ItemStack[] currentContent = player.getInventory().getContents();
        plugin.getConfigManager().debug("Saving current inventory with " + currentContent.length + " slots");
        
        databaseManager.saveInventory(uuid, currentMode, currentContent);
        plugin.getConfigManager().debug("Inventory saved to database for mode: " + currentMode);
        
        // 2. Clear inventory
        player.getInventory().clear();
        plugin.getConfigManager().debug("Inventory cleared for " + player.getName());
        
        // 3. Load target inventory
        ItemStack[] savedInventory = databaseManager.loadLatestInventory(uuid, newMode);
        if (savedInventory != null) {
            plugin.getConfigManager().debug("Loading saved inventory for mode: " + newMode + " (" + savedInventory.length + " slots)");
            player.getInventory().setContents(savedInventory);
        } else {
            plugin.getConfigManager().debug("No saved inventory found for mode: " + newMode + ", starting fresh");
        }
    }
    
    public void restoreInventory(Player player, ItemStack[] items) {
        plugin.getConfigManager().debug("Restoring inventory for " + player.getName() + " with " + items.length + " slots");
        player.getInventory().setContents(items);
    }
}
