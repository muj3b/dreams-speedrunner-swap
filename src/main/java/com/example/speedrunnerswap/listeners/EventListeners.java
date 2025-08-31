package com.example.speedrunnerswap.listeners;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import com.example.speedrunnerswap.models.PlayerState;

public class EventListeners implements Listener {
    
    private final SpeedrunnerSwap plugin;
    
    public EventListeners(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // If the player is a hunter, give them a tracking compass
        if (plugin.getGameManager().isGameRunning() && plugin.getGameManager().isHunter(player)) {
            plugin.getTrackerManager().giveTrackingCompass(player);
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
            player.sendMessage("§cYou cannot drop your tracking compass!");
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
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getClickedInventory();
        ItemStack clickedItem = event.getCurrentItem();

        if (inventory == null || clickedItem == null || clickedItem.getType() == Material.AIR) return;

        String title = event.getView().getTitle();
        event.setCancelled(true);

        // Main Menu
        if (title.equals(plugin.getConfigManager().getGuiMainMenuTitle())) {
            switch (clickedItem.getType()) {
                case PLAYER_HEAD:
                    plugin.getGuiManager().openTeamSelector(player);
                    break;
                case COMPARATOR:
                    plugin.getGuiManager().openSettingsMenu(player);
                    break;
                case GREEN_CONCRETE:
                    if (!plugin.getGameManager().isGameRunning()) {
                        if (plugin.getGameManager().canStartGame()) {
                            plugin.getGameManager().startGame();
                            player.closeInventory();
                        } else {
                            player.sendMessage("§cCannot start game: Insufficient players or teams not set up properly");
                        }
                    }
                    break;
                case RED_CONCRETE:
                    if (plugin.getGameManager().isGameRunning()) {
                        plugin.getGameManager().endGame(PlayerState.Team.NONE);
                        player.closeInventory();
                    }
                    break;
                case YELLOW_CONCRETE:
                case LIME_CONCRETE:
                    if (plugin.getGameManager().isGameRunning()) {
                        if (plugin.getGameManager().isGamePaused()) {
                            plugin.getGameManager().resumeGame();
                        } else {
                            plugin.getGameManager().pauseGame();
                        }
                        player.closeInventory();
                    }
                    break;
            }
        }
        // Team Selector
        else if (title.equals(plugin.getConfigManager().getGuiTeamSelectorTitle())) {
            switch (clickedItem.getType()) {
                case ARROW:
                    plugin.getGuiManager().openMainMenu(player);
                    break;
                case DIAMOND_BOOTS:
                    plugin.getGameManager().getPlayerState(player).setSelectedTeam(PlayerState.Team.RUNNER);
                    plugin.getGuiManager().openTeamSelector(player);
                    break;
                case IRON_SWORD:
                    plugin.getGameManager().getPlayerState(player).setSelectedTeam(PlayerState.Team.HUNTER);
                    plugin.getGuiManager().openTeamSelector(player);
                    break;
                case PLAYER_HEAD:
                    String targetName = clickedItem.getItemMeta().getDisplayName().substring(2);
                    Player targetPlayer = Bukkit.getPlayer(targetName);
                    if (targetPlayer != null) {
                        PlayerState.Team selectedTeam = plugin.getGameManager().getPlayerState(player).getSelectedTeam();
                        if (selectedTeam != PlayerState.Team.NONE) {
                            if (selectedTeam == PlayerState.Team.RUNNER) {
                                plugin.getGameManager().addRunner(targetPlayer);
                            } else {
                                plugin.getGameManager().addHunter(targetPlayer);
                            }
                            plugin.getGuiManager().openTeamSelector(player);
                        } else {
                            player.sendMessage("§cPlease select a team first (Runner or Hunter)");
                        }
                    }
                    break;
            }
        }
        // Settings Menu
        else if (title.equals(plugin.getConfigManager().getGuiSettingsTitle())) {
            switch (clickedItem.getType()) {
                case ARROW:
                    plugin.getGuiManager().openMainMenu(player);
                    break;
                case CLOCK:
                case REPEATER:
                    plugin.getConfigManager().setRandomizeSwap(!plugin.getConfigManager().isRandomizeSwap());
                    plugin.getGuiManager().openSettingsMenu(player);
                    break;
                case LIME_WOOL:
                    int currentInterval = plugin.getConfigManager().getSwapInterval();
                    plugin.getConfigManager().setSwapInterval(Math.min(currentInterval + 5, 300));
                    plugin.getGuiManager().openSettingsMenu(player);
                    break;
                case RED_WOOL:
                    currentInterval = plugin.getConfigManager().getSwapInterval();
                    plugin.getConfigManager().setSwapInterval(Math.max(currentInterval - 5, 10));
                    plugin.getGuiManager().openSettingsMenu(player);
                    break;
                case ANVIL:
                case BARRIER:
                    int slot = event.getSlot();
                    if (slot == 14) {
                        plugin.getConfigManager().setSafeSwapEnabled(!plugin.getConfigManager().isSafeSwapEnabled());
                    } else if (slot == 15) {
                        plugin.getConfigManager().setTrackerEnabled(!plugin.getConfigManager().isTrackerEnabled());
                    } else if (slot == 31) {
                        plugin.getConfigManager().setBroadcastsEnabled(!plugin.getConfigManager().isBroadcastsEnabled());
                    } else if (slot == 34) {
                        plugin.getConfigManager().setVoiceChatIntegrationEnabled(!plugin.getConfigManager().isVoiceChatIntegrationEnabled());
                    }
                    plugin.getGuiManager().openSettingsMenu(player);
                    break;
                case ENDER_EYE:
                case POTION:
                    String currentMode = plugin.getConfigManager().getFreezeMode();
                    plugin.getConfigManager().setFreezeMode(currentMode.equals("SPECTATOR") ? "EFFECTS" : "SPECTATOR");
                    plugin.getGuiManager().openSettingsMenu(player);
                    break;
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        
        // If the player is an inactive runner, cancel their chat messages
        if (plugin.getGameManager().isGameRunning() && 
            plugin.getGameManager().isRunner(player) && 
            plugin.getGameManager().getActiveRunner() != player) {
            
            // Only send message to the player
            player.sendMessage("§c[SpeedrunnerSwap] You cannot chat while inactive.");
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
    
    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        if (plugin.getGameManager().isGameRunning() &&
            player.equals(plugin.getGameManager().getActiveRunner())) {
            // Update compass for all hunters when the active runner uses a portal
            for (Player hunter : plugin.getGameManager().getHunters()) {
                if (hunter.isOnline()) {
                    plugin.getTrackerManager().updateCompass(hunter);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (plugin.getGameManager().isGameRunning() &&
            player.equals(plugin.getGameManager().getActiveRunner())) {
            // Update compass for all hunters when the active runner changes world
            for (Player hunter : plugin.getGameManager().getHunters()) {
                if (hunter.isOnline()) {
                    plugin.getTrackerManager().updateCompass(hunter);
                }
            }
        }
    }
}