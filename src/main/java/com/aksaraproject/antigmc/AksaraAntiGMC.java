package com.aksaraproject.antigmc;

import com.aksaraproject.antigmc.database.DatabaseManager;
import com.aksaraproject.antigmc.managers.ConfigManager;
import com.aksaraproject.antigmc.managers.ConfirmationManager;
import com.aksaraproject.antigmc.managers.InventoryManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class AksaraAntiGMC extends JavaPlugin {

    private static AksaraAntiGMC instance;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private InventoryManager inventoryManager;
    private ConfirmationManager confirmationManager;

    @Override
    public void onEnable() {
        instance = this;
        
        // Save default config
        saveDefaultConfig();
        
        // Initialize Managers
        this.configManager = new ConfigManager(this);
        this.databaseManager = new DatabaseManager(this);
        this.inventoryManager = new InventoryManager(this, databaseManager);
        this.confirmationManager = new ConfirmationManager(this);
        
        // Register Commands
        com.aksaraproject.antigmc.commands.GMCCommand gmcCommand = new com.aksaraproject.antigmc.commands.GMCCommand(this);
        getCommand("gmc").setExecutor(gmcCommand);
        getCommand("gmc").setTabCompleter(gmcCommand);
        getCommand("aksaraantigmc").setExecutor(new com.aksaraproject.antigmc.commands.MainCommand(this));

        // Register Listeners
        getServer().getPluginManager().registerEvents(new com.aksaraproject.antigmc.listeners.RestrictionListener(this), this);
        getServer().getPluginManager().registerEvents(new com.aksaraproject.antigmc.listeners.CreativeItemListener(this), this);
        getServer().getPluginManager().registerEvents(new com.aksaraproject.antigmc.listeners.GameModeListener(this), this);
        getServer().getPluginManager().registerEvents(new com.aksaraproject.antigmc.listeners.PlayerJoinListener(this), this);

        getLogger().info("AksaraAntiGMC has been enabled!");
    }

    @Override
    public void onDisable() {
        if (confirmationManager != null) {
            confirmationManager.clearAll();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("AksaraAntiGMC has been disabled!");
    }

    public static AksaraAntiGMC getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() { return configManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public InventoryManager getInventoryManager() { return inventoryManager; }
    public ConfirmationManager getConfirmationManager() { return confirmationManager; }
}
