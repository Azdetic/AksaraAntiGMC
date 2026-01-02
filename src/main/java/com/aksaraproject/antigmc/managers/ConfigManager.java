package com.aksaraproject.antigmc.managers;

import com.aksaraproject.antigmc.AksaraAntiGMC;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;

public class ConfigManager {

    private final AksaraAntiGMC plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;

    public ConfigManager(AksaraAntiGMC plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    public void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public String getMessage(String path) {
        String msg = messagesConfig.getString(path);
        if (msg == null) return path;
        String prefix = messagesConfig.getString("prefix", "");
        return color(prefix + msg);
    }
    
    public String getRawMessage(String path) {
        String msg = messagesConfig.getString(path);
        return msg == null ? path : color(msg);
    }

    private String color(String text) {
        // Use Bukkit's standard translation to convert & color codes to §
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
    }
    
    public List<String> getBlockedCommands() {
        return plugin.getConfig().getStringList("blocked-commands");
    }

    public boolean isRestrictionEnabled(String restriction) {
        return plugin.getConfig().getBoolean("restrictions." + restriction, true);
    }
    
    public boolean isDebugEnabled() {
        return plugin.getConfig().getBoolean("debug", false);
    }
    
    public void debug(String message) {
        if (isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG] " + message);
        }
    }
}
