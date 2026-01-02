package com.aksaraproject.antigmc.listeners;

import com.aksaraproject.antigmc.AksaraAntiGMC;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Listener to tag all items obtained in Creative mode with a lore
 * Also handles blocking spawn eggs
 */
public class CreativeItemListener implements Listener {

    private final AksaraAntiGMC plugin;

    public CreativeItemListener(AksaraAntiGMC plugin) {
        this.plugin = plugin;
    }

    /**
     * Handle InventoryCreativeEvent - This is fired when:
     * - Player picks item from creative inventory
     * - Player middle-clicks a block (pick block)
     * - Player creates an item in creative mode
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCreativeEvent(InventoryCreativeEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (player.isOp()) return; // OPs can bypass
        if (player.hasPermission("aksaraantigmc.bypass")) return;
        
        plugin.getConfigManager().debug("[CreativeItemListener] === CREATIVE EVENT ===");
        plugin.getConfigManager().debug("[CreativeItemListener] Player: " + player.getName());
        plugin.getConfigManager().debug("[CreativeItemListener] Slot: " + event.getSlot());
        plugin.getConfigManager().debug("[CreativeItemListener] SlotType: " + event.getSlotType());
        plugin.getConfigManager().debug("[CreativeItemListener] Action: " + event.getClick());
        
        // Get the item being placed into player's inventory
        ItemStack cursorItem = event.getCursor();
        ItemStack currentItem = event.getCurrentItem();
        
        plugin.getConfigManager().debug("[CreativeItemListener] Cursor: " + (cursorItem != null ? cursorItem.getType() + " x" + cursorItem.getAmount() : "null/air"));
        plugin.getConfigManager().debug("[CreativeItemListener] Current: " + (currentItem != null ? currentItem.getType() + " x" + currentItem.getAmount() : "null/air"));
        
        // Process cursor item (this is what the player is holding/placing)
        if (cursorItem != null && !cursorItem.getType().isAir()) {
            // Check if it's a spawn egg - BLOCK IT
            if (cursorItem.getType().name().contains("SPAWN_EGG")) {
                if (plugin.getConfigManager().isRestrictionEnabled("block-spawn-eggs")) {
                    plugin.getConfigManager().debug("[CreativeItemListener] BLOCKED: spawn egg");
                    event.setCancelled(true);
                    player.sendMessage(plugin.getConfigManager().getMessage("prefix") + "&cYou cannot use spawn eggs in Creative mode!");
                    return;
                }
            }
            
            // Add lore to the cursor item
            plugin.getConfigManager().debug("[CreativeItemListener] Adding lore to cursor item: " + cursorItem.getType());
            addCreativeLore(player, cursorItem);
            event.setCursor(cursorItem);
        }
        
        // Also process current item (for middle-click pick block)
        if (currentItem != null && !currentItem.getType().isAir()) {
            // Check if it's a spawn egg - BLOCK IT
            if (currentItem.getType().name().contains("SPAWN_EGG")) {
                if (plugin.getConfigManager().isRestrictionEnabled("block-spawn-eggs")) {
                    plugin.getConfigManager().debug("[CreativeItemListener] BLOCKED: spawn egg (current)");
                    event.setCancelled(true);
                    player.sendMessage(plugin.getConfigManager().getMessage("prefix") + "&cYou cannot use spawn eggs in Creative mode!");
                    return;
                }
            }
            
            // Add lore to the current item
            plugin.getConfigManager().debug("[CreativeItemListener] Adding lore to current item: " + currentItem.getType());
            addCreativeLore(player, currentItem);
            event.setCurrentItem(currentItem);
        }
        
        plugin.getConfigManager().debug("[CreativeItemListener] === END CREATIVE EVENT ===");
    }

    /**
     * Handle inventory clicks to add lore to any item player clicks on in Creative
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (player.getGameMode() != GameMode.CREATIVE) return;
        if (player.isOp()) return;
        if (player.hasPermission("aksaraantigmc.bypass")) return;
        
        ItemStack item = event.getCurrentItem();
        if (item != null && !item.getType().isAir()) {
            plugin.getConfigManager().debug("[CreativeItemListener] InventoryClick: " + player.getName() + " - " + item.getType());
            addCreativeLore(player, item);
        }
        
        ItemStack cursor = event.getCursor();
        if (cursor != null && !cursor.getType().isAir()) {
            addCreativeLore(player, cursor);
        }
    }

    /**
     * Block spawn egg usage via PlayerInteractEvent
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.CREATIVE) return;
        if (player.isOp()) return;
        if (player.hasPermission("aksaraantigmc.bypass")) return;
        
        ItemStack item = event.getItem();
        if (item != null && item.getType().name().contains("SPAWN_EGG")) {
            if (plugin.getConfigManager().isRestrictionEnabled("block-spawn-eggs")) {
                plugin.getConfigManager().debug("[CreativeItemListener] BLOCKED: spawn egg usage by " + player.getName());
                event.setCancelled(true);
                player.sendMessage(plugin.getConfigManager().getMessage("prefix") + "&cYou cannot use spawn eggs in Creative mode!");
            }
        }
    }
    
    /**
     * Add creative lore to an item
     */
    private void addCreativeLore(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            plugin.getConfigManager().debug("[CreativeItemListener] Item has no meta, skipping lore for " + item.getType());
            return;
        }
        
        String loreFormat = plugin.getConfigManager().getMessage("items.lore-format")
                .replace("%player%", player.getName());
        
        // Check if already has lore to avoid duplicates
        List<String> lore = meta.hasLore() ? meta.getLore() : Collections.emptyList();
        
        // Check for existing creative lore
        for (String line : lore) {
            if (line != null && org.bukkit.ChatColor.stripColor(line).contains("Creative Items By")) {
                plugin.getConfigManager().debug("[CreativeItemListener] Item already has creative lore, skipping");
                return; 
            }
        }
        
        // Add lore
        List<String> newLore = new ArrayList<>(lore);
        newLore.add(loreFormat);
        meta.setLore(newLore);
        
        item.setItemMeta(meta);
        plugin.getConfigManager().debug("[CreativeItemListener] Added lore to " + item.getType() + ": " + loreFormat);
    }
}
