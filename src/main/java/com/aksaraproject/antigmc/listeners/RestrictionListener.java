package com.aksaraproject.antigmc.listeners;

import com.aksaraproject.antigmc.AksaraAntiGMC;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class RestrictionListener implements Listener {

    private final AksaraAntiGMC plugin;

    public RestrictionListener(AksaraAntiGMC plugin) {
        this.plugin = plugin;
    }
    
    // Helper to check if player is monitored (in GMC) and in whitelisted world
    private boolean isRestricted(Player player) {
        // First check if plugin is active in this world
        if (!plugin.getConfigManager().isWorldEnabled(player.getWorld().getName())) {
            return false;  // Plugin not active in this world
        }
        
        boolean restricted = player.getGameMode() == GameMode.CREATIVE && !player.hasPermission("aksaraantigmc.bypass");
        plugin.getConfigManager().debug("isRestricted check for " + player.getName() + ": " + restricted);
        return restricted;
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!isRestricted(player)) return;
        
        InventoryType type = event.getInventory().getType();
        plugin.getConfigManager().debug("[RestrictionListener] Player " + player.getName() + " attempting to open " + type);
        boolean blocked = false;
        String reason = "";

        // Chests, Barrels
        if (plugin.getConfigManager().isRestrictionEnabled("block-chest-access") && 
            (type == InventoryType.CHEST || type == InventoryType.BARREL)) {
            blocked = true;
            reason = "chest/barrel";
        }
        // Ender Chest
        if (plugin.getConfigManager().isRestrictionEnabled("block-ender-chest") && type == InventoryType.ENDER_CHEST) {
            blocked = true;
            reason = "ender chest";
        }
        // Shulker Box
        if (plugin.getConfigManager().isRestrictionEnabled("block-shulker") && type == InventoryType.SHULKER_BOX) {
            blocked = true;
            reason = "shulker box";
        }
        // Dispenser
        if (plugin.getConfigManager().isRestrictionEnabled("block-dispenser") && type == InventoryType.DISPENSER) {
            blocked = true;
            reason = "dispenser";
        }
        // Dropper
        if (plugin.getConfigManager().isRestrictionEnabled("block-dropper") && type == InventoryType.DROPPER) {
            blocked = true;
            reason = "dropper";
        }
        // Hopper
        if (plugin.getConfigManager().isRestrictionEnabled("block-hopper") && type == InventoryType.HOPPER) {
            blocked = true;
            reason = "hopper";
        }
        // Furnaces (Furnace, Blast Furnace, Smoker)
        if (plugin.getConfigManager().isRestrictionEnabled("block-furnace") && 
            (type == InventoryType.FURNACE || type == InventoryType.BLAST_FURNACE || type == InventoryType.SMOKER)) {
            blocked = true;
            reason = "furnace";
        }
        
        if (blocked) {
            plugin.getConfigManager().debug("[RestrictionListener] BLOCKED: " + reason + " access for " + player.getName());
            event.setCancelled(true);
            player.sendMessage(plugin.getConfigManager().getMessage("prefix") + "&cYou cannot open this in Creative mode!");
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!isRestricted(event.getPlayer())) return;

        plugin.getConfigManager().debug("PlayerInteractEntityEvent: " + event.getPlayer().getName() + " interacting with " + event.getRightClicked().getType());
        
        if (event.getRightClicked() instanceof ItemFrame) {
            if (plugin.getConfigManager().isRestrictionEnabled("block-item-frame")) {
                plugin.getConfigManager().debug("Blocked: item frame interaction");
                event.setCancelled(true);
                event.getPlayer().sendMessage(plugin.getConfigManager().getMessage("prefix") + "&cYou cannot interact with Item Frames in Creative!");
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!isRestricted(event.getPlayer())) return;
        
        // Flower Pot restriction (placing items into pots)
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            Material type = event.getClickedBlock().getType();
            plugin.getConfigManager().debug("PlayerInteractEvent: " + event.getPlayer().getName() + " interacting with block " + type);
            
            if (type.name().contains("POT") && type != Material.POTATOES) { // POTTED_... or FLOWER_POT
                 if (plugin.getConfigManager().isRestrictionEnabled("block-flower-pot")) {
                     plugin.getConfigManager().debug("Blocked: flower pot interaction");
                     event.setCancelled(true);
                     event.getPlayer().sendMessage(plugin.getConfigManager().getMessage("prefix") + "&cYou cannot use Flower Pots in Creative!");
                 }
            }
        }
        
        // Bundle restriction
        if (event.getItem() != null && event.getItem().getType() == Material.BUNDLE) {
             plugin.getConfigManager().debug("PlayerInteractEvent: Bundle interaction detected");
             if (plugin.getConfigManager().isRestrictionEnabled("block-bundle")) {
                 event.setCancelled(true);
                 if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                      plugin.getConfigManager().debug("Blocked: bundle usage");
                      event.getPlayer().sendMessage(plugin.getConfigManager().getMessage("prefix") + "&cBundles are disabled in Creative!");
                 }
             }
        }
    }

    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (!isRestricted(event.getPlayer())) return;

        String msg = event.getMessage().toLowerCase();
        String cmd = msg.split(" ")[0];
        plugin.getConfigManager().debug("PlayerCommandPreprocessEvent: " + event.getPlayer().getName() + " command: " + cmd);
        
        for (String blocked : plugin.getConfigManager().getBlockedCommands()) {
            if (cmd.equals(blocked.toLowerCase())) {
                plugin.getConfigManager().debug("Blocked command: " + cmd);
                event.setCancelled(true);
                event.getPlayer().sendMessage(plugin.getConfigManager().getMessage("prefix") + "&cThis command is blocked in Creative mode.");
                return;
            }
        }
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!isRestricted(player)) return;
        
        plugin.getConfigManager().debug("[RestrictionListener] === DROP ATTEMPT ===");
        plugin.getConfigManager().debug("[RestrictionListener] Player: " + player.getName());
        plugin.getConfigManager().debug("[RestrictionListener] Item: " + event.getItemDrop().getItemStack().getType() + 
                " x" + event.getItemDrop().getItemStack().getAmount());
        
        if (plugin.getConfigManager().isRestrictionEnabled("block-item-drop")) {
            plugin.getConfigManager().debug("[RestrictionListener] RESULT: BLOCKED - item drop");
            
            // Cancel the event
            event.setCancelled(true);
            
            // Remove the dropped item entity to prevent dupe
            event.getItemDrop().remove();
            
            // Update player inventory to sync
            player.updateInventory();
            
            player.sendMessage(plugin.getConfigManager().getMessage("prefix") + "&cYou cannot drop items in Creative mode!");
            plugin.getConfigManager().debug("[RestrictionListener] === DROP BLOCKED ===");
        }
    }

    @EventHandler
    public void onPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isRestricted(player)) return;
        
        plugin.getConfigManager().debug("[RestrictionListener] === PICKUP ATTEMPT ===");
        plugin.getConfigManager().debug("[RestrictionListener] Player: " + player.getName());
        plugin.getConfigManager().debug("[RestrictionListener] Item: " + event.getItem().getItemStack().getType() + 
                " x" + event.getItem().getItemStack().getAmount());
        
        if (plugin.getConfigManager().isRestrictionEnabled("block-pickup-items")) {
            plugin.getConfigManager().debug("[RestrictionListener] RESULT: BLOCKED - item pickup");
            event.setCancelled(true);
            plugin.getConfigManager().debug("[RestrictionListener] === PICKUP BLOCKED ===");
        }
    }
}
