package com.example.speedrunnerswap.listeners;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class EventListeners implements Listener {
    
    private final SpeedrunnerSwap plugin;
    
    public EventListeners(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // If the player is a hunter, give them a tracking compass
        if (plugin.getGameManager() != null && plugin.getGameManager().isGameRunning()) {
            plugin.getGameManager().updateTeams();
            if (plugin.getGameManager().isHunter(player) && plugin.getTrackerManager() != null) {
                plugin.getTrackerManager().giveTrackingCompass(player);
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack droppedItem = event.getItemDrop().getItemStack();

        if (plugin.getGameManager().isGameRunning() &&
            plugin.getGameManager().isHunter(player) &&
            droppedItem.getType() == Material.COMPASS) {
            event.setCancelled(true);
            player.sendMessage(Component.text("§cYou cannot drop your tracking compass!"));
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        if (plugin.getGameManager().isGameRunning() &&
            plugin.getGameManager().isHunter(player)) {
            plugin.getTrackerManager().giveTrackingCompass(player);
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getGameManager().handlePlayerQuit(player);
        plugin.getGameManager().updateTeams();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getClickedInventory();
        ItemStack clickedItem = event.getCurrentItem();

        if (inventory == null || clickedItem == null || clickedItem.getType() == Material.AIR) return;

        Component viewTitle = event.getView().title();
        Component mainMenuTitle = Component.text(plugin.getConfigManager().getGuiMainMenuTitle());
        Component teamSelectorTitle = Component.text(plugin.getConfigManager().getGuiTeamSelectorTitle());
        Component settingsTitle = Component.text(plugin.getConfigManager().getGuiSettingsTitle());

        // Handle GUI clicks
        if (viewTitle.equals(mainMenuTitle) ||
            viewTitle.equals(teamSelectorTitle) ||
            viewTitle.equals(settingsTitle)) {
            event.setCancelled(true);
            handleGuiClick(event);
        } else {
            // For non-GUI inventories
            if (plugin.getGameManager().isGameRunning() && 
                plugin.getGameManager().isRunner(player)) {
                
                if (plugin.getGameManager().getActiveRunner() != player) {
                    // Inactive runners can't interact
                    event.setCancelled(true);
                    player.sendMessage(Component.text("§cYou cannot interact with items while inactive!"));
                    return;
                } else {
                    // Active runner inventory updates
                    if (event.getView().getType() != InventoryType.WORKBENCH) {
                        // Schedule sync for next tick to let the current operation complete
                        plugin.getServer().getScheduler().runTask(plugin, () -> syncRunnerInventories(player));
                    }
                }
            }
        }
    }

    private void handleGuiClick(InventoryClickEvent event) {
        ItemStack clickedItem = event.getCurrentItem();
        String viewTitle = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        if (viewTitle.contains("Team Selector")) {
            handleTeamSelectorClick(event);
        } else if (viewTitle.contains("Settings")) {
            handleSettingsClick(event);
        }
        // Add other menu handlers as needed
    }

    private void handleTeamSelectorClick(InventoryClickEvent event) {
        // Team selector click logic
    }

    private void handleSettingsClick(InventoryClickEvent event) {
        // Settings menu click logic
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        
        // If the player is an inactive runner, cancel their chat messages
        if (plugin.getGameManager().isGameRunning() && 
            plugin.getGameManager().isRunner(player) && 
            plugin.getGameManager().getActiveRunner() != player) {
            
            // Only send message to the player
            player.sendMessage(Component.text("§c[SpeedrunnerSwap] You cannot chat while inactive."));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        // If the player is an inactive runner, prevent movement
        if (plugin.getGameManager().isGameRunning() && 
            plugin.getGameManager().isRunner(player) && 
            plugin.getGameManager().getActiveRunner() != player) {
            
            // Check if getTo() is not null to prevent NullPointerException
            if (event.getTo() != null) {
                // Only cancel if the player is actually trying to move (not just looking around)
                if (event.getFrom().getX() != event.getTo().getX() || 
                    event.getFrom().getY() != event.getTo().getY() || 
                    event.getFrom().getZ() != event.getTo().getZ()) {
                    
                    event.setCancelled(true);
                }
            }
        }
    }

    /**
     * Synchronize inventories between all runners, with crafting protection
     * @param sourcePlayer The player whose inventory should be copied to others
     */
    private void syncRunnerInventories(Player sourcePlayer) {
        if (!plugin.getGameManager().isGameRunning() || !plugin.getGameManager().isRunner(sourcePlayer)) return;
        
        // Don't sync while crafting to avoid state corruption
        if (sourcePlayer.getOpenInventory().getType() == InventoryType.WORKBENCH) {
            return;
        }
        
        ItemStack[] contents = sourcePlayer.getInventory().getContents();
        ItemStack[] armor = sourcePlayer.getInventory().getArmorContents();
        ItemStack offhand = sourcePlayer.getInventory().getItemInOffHand();
        
        for (Player runner : plugin.getGameManager().getRunners()) {
            if (runner != sourcePlayer && runner.isOnline() && 
                runner.getOpenInventory().getType() != InventoryType.WORKBENCH) {
                    
                runner.getInventory().setContents(contents.clone());
                runner.getInventory().setArmorContents(armor.clone());
                runner.getInventory().setItemInOffHand(offhand.clone());
                runner.updateInventory();
            }
        }
    }
}
