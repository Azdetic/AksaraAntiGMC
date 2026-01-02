package com.aksaraproject.antigmc.database;

import com.aksaraproject.antigmc.AksaraAntiGMC;
import org.bukkit.GameMode;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {

    private final AksaraAntiGMC plugin;
    private Connection connection;

    public DatabaseManager(AksaraAntiGMC plugin) {
        this.plugin = plugin;
        initializeDatabase();
    }

    private void initializeDatabase() {
        File dataFolder = new File(plugin.getDataFolder(), plugin.getConfig().getString("database.filename", "inventory_data.db"));
        if (!dataFolder.getParentFile().exists()) {
            dataFolder.getParentFile().mkdirs();
        }

        plugin.getLogger().info("[DEBUG] Initializing SQLite database at: " + dataFolder.getAbsolutePath());

        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dataFolder.getAbsolutePath());
            plugin.getLogger().info("[DEBUG] SQLite connection established successfully");
            createTable();
        } catch (SQLException | ClassNotFoundException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not connect to SQLite database!", e);
        }
    }

    private void createTable() {
        String query = "CREATE TABLE IF NOT EXISTS inventory_data (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "uuid TEXT NOT NULL," +
                "gamemode TEXT NOT NULL," +
                "timestamp LONG NOT NULL," +
                "data TEXT NOT NULL" +
                ");";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(query);
            plugin.getLogger().info("[DEBUG] Database table 'inventory_data' created/verified");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not create table!", e);
        }
    }

    public void saveInventory(UUID uuid, GameMode gamemode, ItemStack[] items) {
        plugin.getLogger().info("[DEBUG] saveInventory called for UUID: " + uuid + ", Gamemode: " + gamemode);
        String data = toBase64(items);
        String query = "INSERT INTO inventory_data (uuid, gamemode, timestamp, data) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, gamemode.name());
            pstmt.setLong(3, System.currentTimeMillis());
            pstmt.setString(4, data);
            pstmt.executeUpdate();
            plugin.getLogger().info("[DEBUG] Inventory saved successfully. Data length: " + data.length() + " chars");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save inventory!", e);
        }
    }
    
    // Retrieve the LATEST inventory for a player in a specific gamemode
    public ItemStack[] loadLatestInventory(UUID uuid, GameMode gamemode) {
        plugin.getLogger().info("[DEBUG] loadLatestInventory called for UUID: " + uuid + ", Gamemode: " + gamemode);
        String query = "SELECT data FROM inventory_data WHERE uuid = ? AND gamemode = ? ORDER BY timestamp DESC LIMIT 1";
        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, gamemode.name());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    plugin.getLogger().info("[DEBUG] Found inventory data, deserializing...");
                    ItemStack[] items = fromBase64(rs.getString("data"));
                    plugin.getLogger().info("[DEBUG] Deserialized " + items.length + " slots");
                    return items;
                } else {
                    plugin.getLogger().info("[DEBUG] No inventory data found for this UUID/Gamemode");
                }
            }
        } catch (SQLException | IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not load inventory!", e);
        }
        return null;
    }

    /**
     * Inner class to hold inventory record info
     */
    public static class InventoryRecord {
        public final int id;
        public final String gamemode;
        public final long timestamp;
        public final String formattedDate;
        
        public InventoryRecord(int id, String gamemode, long timestamp) {
            this.id = id;
            this.gamemode = gamemode;
            this.timestamp = timestamp;
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            this.formattedDate = sdf.format(new java.util.Date(timestamp));
        }
    }

    /**
     * Get all inventory history for a player (for restore selection)
     */
    public List<InventoryRecord> getInventoryHistory(UUID uuid) {
        plugin.getLogger().info("[DEBUG] getInventoryHistory called for UUID: " + uuid);
        List<InventoryRecord> records = new ArrayList<>();
        String query = "SELECT id, gamemode, timestamp FROM inventory_data WHERE uuid = ? ORDER BY timestamp DESC LIMIT 20";
        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, uuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    records.add(new InventoryRecord(
                        rs.getInt("id"),
                        rs.getString("gamemode"),
                        rs.getLong("timestamp")
                    ));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not get inventory history!", e);
        }
        plugin.getLogger().info("[DEBUG] Found " + records.size() + " inventory records");
        return records;
    }

    /**
     * Load inventory by specific ID (for restore by date selection)
     */
    public ItemStack[] loadInventoryById(int id) {
        plugin.getLogger().info("[DEBUG] loadInventoryById called for ID: " + id);
        String query = "SELECT data FROM inventory_data WHERE id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    plugin.getLogger().info("[DEBUG] Found inventory data for ID " + id);
                    return fromBase64(rs.getString("data"));
                }
            }
        } catch (SQLException | IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not load inventory by ID!", e);
        }
        return null;
    }

    // Helper method to convert ItemStack array to Base64 String
    public static String toBase64(ItemStack[] items) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            
            dataOutput.writeInt(items.length);
            
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }
            
            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }

    // Helper method to convert Base64 String to ItemStack array
    public static ItemStack[] fromBase64(String data) throws IOException {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            
            ItemStack[] items = new ItemStack[dataInput.readInt()];
            
            for (int i = 0; i < items.length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }
            
            dataInput.close();
            return items;
        } catch (ClassNotFoundException e) {
            throw new IOException("Unable to decode class type.", e);
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                plugin.getLogger().info("[DEBUG] Closing database connection");
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
