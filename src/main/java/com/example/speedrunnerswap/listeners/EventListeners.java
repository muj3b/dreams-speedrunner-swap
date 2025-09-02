package com.example.speedrunnerswap.listeners;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import com.example.speedrunnerswap.models.PlayerState;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
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
        if (plugin.getGameManager().isGameRunning()) {
            if (plugin.getGameManager() != null) {
                plugin.getGameManager().updateTeams();
            }
            if (plugin.getGameManager().isHunter(player)) {
                plugin.getTrackerManager().giveTrackingCompass(player);
            }
        } else {
            // Do nothing if game is not running
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

    @EventHandler
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
        // Only cancel clicks in plugin GUIs
        if (viewTitle.equals(mainMenuTitle) ||
            viewTitle.equals(teamSelectorTitle) ||
            viewTitle.equals(settingsTitle)) {
            event.setCancelled(true);
        } else {
            // For non-GUI inventories, check if the player is an inactive runner
            if (plugin.getGameManager().isGameRunning() && plugin.getGameManager().isRunner(player) && plugin.getGameManager().getActiveRunner() != player && event.getInventory() != null) {
                event.setCancelled(true);
                player.sendMessage(Component.text("§cYou cannot interact with items while inactive!"));
                return;
            }
            return; // Allow normal inventory interaction
        }

        // Main Menu
        if (viewTitle.equals(mainMenuTitle)) {
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
                default:
                    break;
            }
        }
        // Team Selector
        else if (viewTitle.equals(teamSelectorTitle)) {
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
                    Component nameComponent = clickedItem.getItemMeta().displayName();
                    String targetName = nameComponent != null ? PlainTextComponentSerializer.plainText().serialize(nameComponent).substring(2) : "";
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
                default:
                    break;
            }
        }
        // Settings Menu
        else if (viewTitle.equals(settingsTitle)) {
            switch (clickedItem.getType()) {
                case ARROW:
                    plugin.getGuiManager().openMainMenu(player);
                    break;
                case CLOCK:
                case REPEATER:
                    plugin.getConfigManager().setSwapRandomized(!plugin.getConfigManager().isSwapRandomized());
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
                // Add cases for all other materials that might be clicked
                case BOLT_ARMOR_TRIM_SMITHING_TEMPLATE:
                case BONE:
                 case WATER_BUCKET:
                case SMOOTH_SANDSTONE_STAIRS:
                case RED_SANDSTONE_WALL:
                case GOLDEN_SHOVEL:
                case GOLDEN_APPLE:
                case RESIN_BRICK_STAIRS:
                case ACACIA_SAPLING:
                case DRAGON_WALL_HEAD:
                case BRICKS:
                case LANTERN:
                case SANDSTONE:
                case GRAY_GLAZED_TERRACOTTA:
                case COOKED_SALMON:
                case STRUCTURE_VOID:
                case SMALL_AMETHYST_BUD:
                case REDSTONE_ORE:
                case YELLOW_GLAZED_TERRACOTTA:
                case BRAIN_CORAL:
                case LIGHT_WEIGHTED_PRESSURE_PLATE:
                case SANDSTONE_WALL:
                case ACACIA_WALL_SIGN:
                case NETHERITE_SWORD:
                case SPRUCE_FENCE:
                case LEATHER_BOOTS:
                case FISHING_ROD:
                case DIAMOND:
                case NETHER_BRICK:
                case BAMBOO_RAFT:
                case ELYTRA:
                case BOOK:
                case WEATHERED_CHISELED_COPPER:
                case FRIEND_POTTERY_SHERD:
                    // Ignore clicks on these items
                    break;
                default:
                    // Handle any other materials not explicitly listed
                    break;
            }
        }
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        // Only sync for active runner
        if (plugin.getGameManager().isGameRunning() &&
            plugin.getGameManager().isRunner(player) &&
            plugin.getGameManager().getActiveRunner() == player) {
            syncRunnerInventories(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        
        // Only sync for active runner
        if (plugin.getGameManager().isGameRunning() &&
            plugin.getGameManager().isRunner(player) &&
            plugin.getGameManager().getActiveRunner() == player) {
            // Schedule sync for next tick to ensure inventory is updated
            Bukkit.getScheduler().runTask(plugin, () -> syncRunnerInventories(player));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        
        // Prevent inactive runners from taking any damage
        if (plugin.getGameManager().isGameRunning() &&
            plugin.getGameManager().isRunner(player) &&
            !player.equals(plugin.getGameManager().getActiveRunner())) {
            event.setCancelled(true);
        }
        
        // Sync health from active runner to inactive runners
        if (plugin.getGameManager().isGameRunning() &&
            player.equals(plugin.getGameManager().getActiveRunner())) {
            double health = player.getHealth();
            for (Player runner : plugin.getGameManager().getRunners()) {
                if (runner != player && runner.isOnline()) {
                    runner.setHealth(health);
                }
            }
        }
    }

    /**
     * Synchronize inventories between all runners
     * @param sourcePlayer The player whose inventory should be copied to others
     */
    private void syncRunnerInventories(Player sourcePlayer) {
        if (!plugin.getGameManager().isGameRunning() || !plugin.getGameManager().isRunner(sourcePlayer)) return;
        
        ItemStack[] contents = sourcePlayer.getInventory().getContents();
        ItemStack[] armor = sourcePlayer.getInventory().getArmorContents();
        ItemStack offhand = sourcePlayer.getInventory().getItemInOffHand();
        
        for (Player runner : plugin.getGameManager().getRunners()) {
            if (runner != sourcePlayer && runner.isOnline()) {
                runner.getInventory().setContents(contents.clone());
                runner.getInventory().setArmorContents(armor.clone());
                runner.getInventory().setItemInOffHand(offhand.clone());
                runner.updateInventory();
            }
        }
    }
}