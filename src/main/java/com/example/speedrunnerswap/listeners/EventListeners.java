package com.example.speedrunnerswap.listeners;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import java.time.Duration;

public class EventListeners implements Listener {
    
    private final SpeedrunnerSwap plugin;
    // Simple debounce for hot potato swap triggers
    private volatile long lastHotPotatoTriggerMs = 0L;
    
    public EventListeners(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        if (plugin.getGameManager() != null && plugin.getGameManager().isGameRunning()) {
            // Handle task mode rejoin logic and resume if needed
            plugin.getGameManager().handlePlayerJoin(player);
            plugin.getGameManager().updateTeams();
            // If the player is a hunter, give them a tracking compass
            if (plugin.getGameManager().isHunter(player) && plugin.getTrackerManager() != null) {
                plugin.getTrackerManager().giveTrackingCompass(player);
            }
        }
    }

    @org.bukkit.event.EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRunnerDamaged(org.bukkit.event.entity.EntityDamageEvent event) {
        if (!plugin.getGameManager().isGameRunning()) return;
        if (!plugin.getConfigManager().isHotPotatoModeEnabled()) return;

        if (!(event.getEntity() instanceof Player player)) return;
        if (!plugin.getGameManager().isRunner(player)) return;
        if (!player.equals(plugin.getGameManager().getActiveRunner())) return;

        // Debounce to avoid cascading swaps from multi-hit damage events
        long now = System.currentTimeMillis();
        if (now - lastHotPotatoTriggerMs < 1000) return; // 1s cooldown
        lastHotPotatoTriggerMs = now;

        // Trigger an immediate swap on next tick
        plugin.getGameManager().triggerImmediateSwap();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // Suppress death messages during active games to maintain mystery
        if (plugin.getGameManager().isGameRunning()) {
            event.deathMessage(Component.empty());
        }

        // Ensure hunters don't drop tracking compasses on death
        if (plugin.getGameManager().isGameRunning() && plugin.getGameManager().isHunter(player)) {
            event.getDrops().removeIf(item -> item != null && item.getType() == Material.COMPASS);
        }

        // Show pop-up title when a runner dies, then end the game
        if (plugin.getGameManager().isGameRunning() && plugin.getGameManager().isRunner(player)) {
            Component titleText = Component.text("bro speedrunners died bro no shot")
                    .color(NamedTextColor.RED)
                    .decorate(TextDecoration.BOLD);
            Component subText = Component.text("");
            Title deathTitle = Title.title(
                    titleText,
                    subText,
                    Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(1600), Duration.ofMillis(200))
            );
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                p.showTitle(deathTitle);
            }

            // End the game shortly after the title displays
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (plugin.getGameManager().isGameRunning()) {
                    plugin.getGameManager().stopGame(); // no winner, ends cleanly
                }
            }, 20L);
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!plugin.getGameManager().isGameRunning() || !plugin.getGameManager().isHunter(player)) return;

        ItemStack dragged = event.getOldCursor();
        if (dragged == null || dragged.getType() != Material.COMPASS) return;

        // Allow dragging the compass within the player's own inventory only.
        // If any affected slot is in the top (container) inventory, cancel.
        int topSize = event.getView().getTopInventory() != null ? event.getView().getTopInventory().getSize() : 0;
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < topSize) {
                event.setCancelled(true);
                player.sendMessage("§cYou cannot store your tracking compass in containers!");
                return;
            }
        }
        // Otherwise, drag stays within bottom inventory and is allowed.
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAnyDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!plugin.getGameManager().isGameRunning()) return;
        // Cancel any damage to inactive runners in cages
        if (plugin.getGameManager().isRunner(victim) && plugin.getGameManager().getActiveRunner() != victim) {
            if ("CAGE".equalsIgnoreCase(plugin.getConfigManager().getFreezeMode())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getGameManager().isGameRunning()) return;

        // If a hunter changes world, (re)give/update their compass
        if (plugin.getGameManager().isHunter(player)) {
            plugin.getTrackerManager().giveTrackingCompass(player);
        }

        // If the active runner changes world, push updates to all hunters
        Player active = plugin.getGameManager().getActiveRunner();
        if (active != null && active.equals(player)) {
            plugin.getTrackerManager().updateAllHunterCompasses();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getClickedInventory();
        ItemStack clickedItem = event.getCurrentItem();
    
        if (inventory == null || clickedItem == null || clickedItem.getType() == Material.AIR) return;
    
        // If this is one of our plugin GUIs, let the dedicated GuiListener handle it
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (isPluginGuiTitle(title)) {
            return; // Do not cancel or handle here
        }
    
        // Hunters: allow moving compass within their own inventory, but block
        // placing it into any container/top inventory or quick-moving into it.
        if (plugin.getGameManager().isGameRunning() && plugin.getGameManager().isHunter(player)) {
            boolean clickedInPlayerInv = inventory.equals(player.getInventory());

            // If the item on cursor is a compass and the click targets a container, block
            ItemStack cursor = event.getCursor();
            if (cursor != null && cursor.getType() == Material.COMPASS && !clickedInPlayerInv) {
                event.setCancelled(true);
                player.sendMessage("§cYou cannot store your tracking compass in containers!");
                return;
            }

            // If the clicked item is a compass, only allow moves inside the player inventory.
            if (clickedItem.getType() == Material.COMPASS) {
                // Disallow shift-click (would move to top/container)
                if (event.isShiftClick()) {
                    event.setCancelled(true);
                    player.sendMessage("§cYou cannot quick-move your tracking compass!");
                    return;
                }

                // Disallow placing into any container/top inventory
                if (!clickedInPlayerInv) {
                    event.setCancelled(true);
                    player.sendMessage("§cYou cannot store your tracking compass in containers!");
                    return;
                }
                // Otherwise allow normal rearranging inside player inventory
            }

            // Block hotbar-swap that would push a compass into the container
            InventoryAction action = event.getAction();
            if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                if (clickedItem.getType() == Material.COMPASS) {
                    event.setCancelled(true);
                    player.sendMessage("§cYou cannot quick-move your tracking compass!");
                    return;
                }
            }
            // Avoid referencing deprecated HOTBAR_MOVE_AND_READD constant directly
            String actName = action.name();
            if (action == InventoryAction.HOTBAR_SWAP || "HOTBAR_MOVE_AND_READD".equals(actName)) {
                int hb = event.getHotbarButton();
                if (hb >= 0) {
                    ItemStack hotbarItem = player.getInventory().getItem(hb);
                    if (hotbarItem != null && hotbarItem.getType() == Material.COMPASS && !clickedInPlayerInv) {
                        event.setCancelled(true);
                        player.sendMessage("§cYou cannot store your tracking compass in containers!");
                        return;
                    }
                }
            }
        }

        // For non-GUI inventories, enforce runner interaction rules and sync
        if (plugin.getGameManager().isGameRunning() && plugin.getGameManager().isRunner(player)) {
            if (plugin.getGameManager().getActiveRunner() != player) {
                // Inactive runners can't interact
                event.setCancelled(true);
                player.sendMessage("§cYou cannot interact with items while inactive!");
                return;
            } else {
                // Do not sync inventories between runners. Inactive runners should
                // never see or receive the active runner's inventory until swapped in.
                // Intentionally no-op here.
            }
        }
    }
    
    private boolean isPluginGuiTitle(String title) {
        if (title == null || title.isEmpty()) return false;
        return title.contains("SpeedrunnerSwap") ||
               title.contains("Main Menu") ||
               title.contains("Team Selector") ||
               title.contains("Settings") ||
               title.contains("Kits") ||
               title.contains("Effects") ||
               title.contains("Power-ups") ||
               title.contains("Power-up Durations") ||
               title.contains("World Border") ||
               title.contains("Bounty") ||
               title.contains("Last Stand") ||
               title.contains("Compass") ||
               title.contains("Sudden Death") ||
               title.contains("Statistics") ||
               title.contains("Dangerous Blocks") ||
               title.contains("Edit ") && title.contains(" Kit");
    }

    // GUI clicks are exclusively handled by GuiListener to avoid duplication.

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncChatEvent event) {
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

    // Fallback for servers where Paper's AsyncChatEvent may not fire
    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChatLegacy(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getGameManager().isGameRunning()) return;
        if (!plugin.getGameManager().isRunner(player)) return;
        if (plugin.getGameManager().getActiveRunner() == player) return;
        player.sendMessage("§c[SpeedrunnerSwap] You cannot chat while inactive.");
        event.setCancelled(true);
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerAdvancementDone(org.bukkit.event.player.PlayerAdvancementDoneEvent event) {
        // Suppress advancement messages during active games to maintain mystery
        if (plugin.getGameManager().isGameRunning()) {
            event.message(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerBedEnter(org.bukkit.event.player.PlayerBedEnterEvent event) {
        Player player = event.getPlayer();
        
        // If game is running and sleep mechanic is enabled, control who can sleep
        if (plugin.getGameManager().isGameRunning() && plugin.getConfigManager().isSinglePlayerSleepEnabled()) {
            // If this is a runner but not the active one, cancel the bed enter
            if (plugin.getGameManager().isRunner(player) && !player.equals(plugin.getGameManager().getActiveRunner())) {
                event.setCancelled(true);
                player.sendMessage("§cOnly the active runner can sleep!");
                return;
            }
            // If this is not a runner at all (hunter or spectator), cancel
            if (!plugin.getGameManager().isRunner(player) && !plugin.getGameManager().isHunter(player)) {
                event.setCancelled(true);
                return;
            }
            
            // If we reach here, it's the active runner entering bed - schedule night skip check
            if (plugin.getGameManager().isRunner(player) && player.equals(plugin.getGameManager().getActiveRunner())) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline() && player.isSleeping()) {
                        org.bukkit.World world = player.getWorld();
                        if (world.getTime() > 12000 && world.getTime() < 24000) {
                            world.setTime(0); // Set to day
                            world.setStorm(false);
                            world.setThundering(false);
                            // Notify players
                            for (Player p : plugin.getServer().getOnlinePlayers()) {
                                p.sendMessage("§eThe night has been skipped by the active runner!");
                            }
                        }
                    }
                }, 20L); // Check after 1 second
            }
        }
    }

    // Removed inventory sync method: inactive runners will not receive
    // the active runner's inventory outside of swap logic.

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!plugin.getGameManager().isGameRunning()) return;
        if (!"CAGE".equalsIgnoreCase(plugin.getConfigManager().getFreezeMode())) return;

        if (event.getEntity() instanceof Player victim) {
            org.bukkit.entity.Entity damagerEntity = event.getDamager();
            Player attacker = null;
            if (damagerEntity instanceof Player p) attacker = p;
            else if (damagerEntity instanceof org.bukkit.entity.Projectile proj && proj.getShooter() instanceof Player p2) attacker = p2;

            if (attacker != null) {
                // Cancel PvP only when both players are occupants of the shared cage
                try {
                    if (plugin.getGameManager().areBothPlayersInSharedCage(attacker, victim)) {
                        event.setCancelled(true);
                    }
                } catch (Throwable ignored) {}
            }
        }
    }
}
