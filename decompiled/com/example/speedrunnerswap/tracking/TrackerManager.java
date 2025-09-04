/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.World$Environment
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.CompassMeta
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 */
package com.example.speedrunnerswap.tracking;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class TrackerManager {
    private final SpeedrunnerSwap plugin;
    private volatile BukkitTask trackerTask;
    private volatile BukkitTask particleTask;
    private boolean isJammed = false;
    private final Object taskLock = new Object();
    private final Map<UUID, Integer> compassSlotCache = new HashMap<UUID, Integer>();

    public TrackerManager(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void startTracking() {
        Object object = this.taskLock;
        synchronized (object) {
            if (this.trackerTask != null) {
                this.trackerTask.cancel();
            }
            int updateTicks = Math.max(10, this.plugin.getConfigManager().getTrackerUpdateTicks());
            this.trackerTask = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, () -> {
                Player activeRunner = this.plugin.getGameManager().getActiveRunner();
                if (activeRunner == null || !activeRunner.isOnline() || !this.plugin.getGameManager().isGameRunning()) {
                    return;
                }
                for (Player hunter : this.plugin.getGameManager().getHunters()) {
                    if (!hunter.isOnline()) continue;
                    this.updateHunterCompass(hunter, activeRunner);
                }
            }, 0L, (long)updateTicks);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void stopTracking() {
        Object object = this.taskLock;
        synchronized (object) {
            if (this.trackerTask != null) {
                this.trackerTask.cancel();
                this.trackerTask = null;
            }
            if (this.particleTask != null) {
                this.particleTask.cancel();
                this.particleTask = null;
            }
        }
    }

    public void giveTrackingCompass(Player hunter) {
        if (hunter == null || !hunter.isOnline()) {
            return;
        }
        if (!hunter.getInventory().contains(Material.COMPASS)) {
            ItemStack compass = new ItemStack(Material.COMPASS);
            hunter.getInventory().addItem(new ItemStack[]{compass});
            this.compassSlotCache.remove(hunter.getUniqueId());
        }
        this.updateCompass(hunter);
    }

    public void updateCompass(Player hunter) {
        Player activeRunner = this.plugin.getGameManager().getActiveRunner();
        if (activeRunner != null && activeRunner.isOnline()) {
            this.updateHunterCompass(hunter, activeRunner);
        }
    }

    private void updateHunterCompass(Player hunter, Player target) {
        if (hunter == null || target == null || !hunter.isOnline() || !target.isOnline()) {
            return;
        }
        try {
            ItemStack compass;
            int slot = this.findCompassSlot(hunter);
            if (slot == -1) {
                this.giveTrackingCompass(hunter);
                slot = this.findCompassSlot(hunter);
                if (slot == -1) {
                    return;
                }
            }
            if ((compass = hunter.getInventory().getItem(slot)) == null || compass.getType() != Material.COMPASS) {
                return;
            }
            World.Environment hunterEnv = hunter.getWorld().getEnvironment();
            World.Environment targetEnv = target.getWorld().getEnvironment();
            if (this.isJammed) {
                int maxDist = this.plugin.getConfig().getInt("tracker.compass_jamming.max_jam_distance", this.plugin.getConfig().getInt("sudden_death.arena.max_jam_distance", 500));
                maxDist = Math.max(10, Math.min(5000, maxDist));
                double angle = Math.random() * Math.PI * 2.0;
                double radius = Math.random() * (double)maxDist;
                Location jam = hunter.getLocation().clone().add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
                jam.setY(Math.max(5.0, Math.min((double)(hunter.getWorld().getMaxHeight() - 5), jam.getY())));
                CompassMeta meta = (CompassMeta)compass.getItemMeta();
                meta.setLodestone(jam);
                meta.setLodestoneTracked(false);
                compass.setItemMeta((ItemMeta)meta);
            } else if (hunterEnv == targetEnv) {
                Location adjustedLoc = target.getLocation();
                hunter.setCompassTarget(adjustedLoc);
            } else if (hunterEnv == World.Environment.NORMAL && targetEnv == World.Environment.NETHER) {
                Location adjustedLoc = new Location(hunter.getWorld(), target.getLocation().getX() / 8.0, Math.min(Math.max(target.getLocation().getY(), 0.0), 128.0), target.getLocation().getZ() / 8.0);
                hunter.setCompassTarget(adjustedLoc);
            } else if (hunterEnv == World.Environment.NETHER && targetEnv == World.Environment.NORMAL) {
                Location adjustedLoc = new Location(hunter.getWorld(), target.getLocation().getX() * 8.0, Math.min(Math.max(target.getLocation().getY(), 0.0), 256.0), target.getLocation().getZ() * 8.0);
                hunter.setCompassTarget(adjustedLoc);
            } else if (targetEnv == World.Environment.THE_END) {
                Location hint = this.plugin.getConfigManager().getEndPortalHint(hunter.getWorld());
                Location adjustedLoc = hint != null ? hint : hunter.getWorld().getSpawnLocation();
                hunter.setCompassTarget(adjustedLoc);
                hunter.sendMessage("\u00a7eTarget is in The End! Compass points to the End Portal hint.");
            } else {
                Location adjustedLoc = hunter.getWorld().getSpawnLocation();
                hunter.setCompassTarget(adjustedLoc);
            }
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("Error updating compass for hunter " + hunter.getName() + ": " + e.getMessage());
        }
    }

    public void updateAllHunterCompasses() {
        Player activeRunner = this.plugin.getGameManager().getActiveRunner();
        if (activeRunner != null && activeRunner.isOnline()) {
            for (Player hunter : this.plugin.getGameManager().getHunters()) {
                if (!hunter.isOnline()) continue;
                this.updateHunterCompass(hunter, activeRunner);
            }
        }
    }

    public void jamCompasses(long durationTicks) {
        this.isJammed = true;
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            this.isJammed = false;
        }, durationTicks);
    }

    private int findCompassSlot(Player hunter) {
        ItemStack cachedItem;
        Integer cached = this.compassSlotCache.get(hunter.getUniqueId());
        if (cached != null && (cachedItem = hunter.getInventory().getItem(cached.intValue())) != null && cachedItem.getType() == Material.COMPASS) {
            return cached;
        }
        for (int i = 0; i < hunter.getInventory().getSize(); ++i) {
            ItemStack item = hunter.getInventory().getItem(i);
            if (item == null || item.getType() != Material.COMPASS) continue;
            this.compassSlotCache.put(hunter.getUniqueId(), i);
            return i;
        }
        this.compassSlotCache.remove(hunter.getUniqueId());
        return -1;
    }
}

