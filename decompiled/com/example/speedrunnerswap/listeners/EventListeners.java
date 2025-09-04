/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.event.player.AsyncChatEvent
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
 *  org.bukkit.Material
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.EntityDamageEvent
 *  org.bukkit.event.entity.PlayerDeathEvent
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryType
 *  org.bukkit.event.player.PlayerChangedWorldEvent
 *  org.bukkit.event.player.PlayerDropItemEvent
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerMoveEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.event.player.PlayerRespawnEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 */
package com.example.speedrunnerswap.listeners;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class EventListeners
implements Listener {
    private final SpeedrunnerSwap plugin;
    private volatile long lastHotPotatoTriggerMs = 0L;

    public EventListeners(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (this.plugin.getGameManager() != null && this.plugin.getGameManager().isGameRunning()) {
            this.plugin.getGameManager().updateTeams();
            if (this.plugin.getGameManager().isHunter(player) && this.plugin.getTrackerManager() != null) {
                this.plugin.getTrackerManager().giveTrackingCompass(player);
            }
        }
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onRunnerDamaged(EntityDamageEvent event) {
        if (!this.plugin.getGameManager().isGameRunning()) {
            return;
        }
        if (!this.plugin.getConfigManager().isHotPotatoModeEnabled()) {
            return;
        }
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        Player player = (Player)entity;
        if (!this.plugin.getGameManager().isRunner(player)) {
            return;
        }
        if (!player.equals((Object)this.plugin.getGameManager().getActiveRunner())) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - this.lastHotPotatoTriggerMs < 1000L) {
            return;
        }
        this.lastHotPotatoTriggerMs = now;
        this.plugin.getGameManager().triggerImmediateSwap();
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (this.plugin.getGameManager().isGameRunning() && this.plugin.getGameManager().isHunter(player)) {
            event.getDrops().removeIf(item -> item != null && item.getType() == Material.COMPASS);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        if (this.plugin.getGameManager().isGameRunning() && this.plugin.getGameManager().isHunter(player) && droppedItem.getType() == Material.COMPASS) {
            event.setCancelled(true);
            player.sendMessage((Component)Component.text((String)"\u00a7cYou cannot drop your tracking compass!"));
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (this.plugin.getGameManager().isGameRunning() && this.plugin.getGameManager().isHunter(player)) {
            this.plugin.getTrackerManager().giveTrackingCompass(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        this.plugin.getGameManager().handlePlayerQuit(player);
        this.plugin.getGameManager().updateTeams();
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player active;
        Player player = event.getPlayer();
        if (!this.plugin.getGameManager().isGameRunning()) {
            return;
        }
        if (this.plugin.getGameManager().isHunter(player)) {
            this.plugin.getTrackerManager().giveTrackingCompass(player);
        }
        if ((active = this.plugin.getGameManager().getActiveRunner()) != null && active.equals((Object)player)) {
            this.plugin.getTrackerManager().updateAllHunterCompasses();
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player)event.getWhoClicked();
        Inventory inventory = event.getClickedInventory();
        ItemStack clickedItem = event.getCurrentItem();
        if (inventory == null || clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (this.isPluginGuiTitle(title)) {
            return;
        }
        if (this.plugin.getGameManager().isGameRunning() && this.plugin.getGameManager().isRunner(player)) {
            if (this.plugin.getGameManager().getActiveRunner() != player) {
                event.setCancelled(true);
                player.sendMessage((Component)Component.text((String)"\u00a7cYou cannot interact with items while inactive!"));
                return;
            }
            if (event.getView().getType() != InventoryType.WORKBENCH) {
                this.plugin.getServer().getScheduler().runTask((Plugin)this.plugin, () -> this.syncRunnerInventories(player));
            }
        }
    }

    private boolean isPluginGuiTitle(String title) {
        if (title == null || title.isEmpty()) {
            return false;
        }
        return title.contains("SpeedrunnerSwap") || title.contains("Main Menu") || title.contains("Team Selector") || title.contains("Settings") || title.contains("Kits") || title.contains("Effects") || title.contains("Power-ups") || title.contains("Power-up Durations") || title.contains("World Border") || title.contains("Bounty") || title.contains("Last Stand") || title.contains("Compass") || title.contains("Sudden Death") || title.contains("Statistics") || title.contains("Edit ") && title.contains(" Kit");
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (this.plugin.getGameManager().isGameRunning() && this.plugin.getGameManager().isRunner(player) && this.plugin.getGameManager().getActiveRunner() != player) {
            player.sendMessage((Component)Component.text((String)"\u00a7c[SpeedrunnerSwap] You cannot chat while inactive."));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (this.plugin.getGameManager().isGameRunning() && this.plugin.getGameManager().isRunner(player) && this.plugin.getGameManager().getActiveRunner() != player && event.getTo() != null && (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getY() != event.getTo().getY() || event.getFrom().getZ() != event.getTo().getZ())) {
            event.setCancelled(true);
        }
    }

    private void syncRunnerInventories(Player sourcePlayer) {
        if (!this.plugin.getGameManager().isGameRunning() || !this.plugin.getGameManager().isRunner(sourcePlayer)) {
            return;
        }
        if (sourcePlayer.getOpenInventory().getType() == InventoryType.WORKBENCH) {
            return;
        }
        ItemStack[] contents = sourcePlayer.getInventory().getContents();
        ItemStack[] armor = sourcePlayer.getInventory().getArmorContents();
        ItemStack offhand = sourcePlayer.getInventory().getItemInOffHand();
        for (Player runner : this.plugin.getGameManager().getRunners()) {
            if (runner == sourcePlayer || !runner.isOnline() || runner.getOpenInventory().getType() == InventoryType.WORKBENCH) continue;
            runner.getInventory().setContents((ItemStack[])contents.clone());
            runner.getInventory().setArmorContents((ItemStack[])armor.clone());
            runner.getInventory().setItemInOffHand(offhand.clone());
            runner.updateInventory();
        }
    }
}

