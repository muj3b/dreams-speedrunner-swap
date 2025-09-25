package com.example.speedrunnerswap.tracking;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
// Particle functionality removed - imports intentionally omitted

/**
 * Manager for tracking the active runner
 */
public class TrackerManager {
    
    private final SpeedrunnerSwap plugin;
    private volatile BukkitTask trackerTask;
    private boolean isJammed = false;
    private final Object taskLock = new Object();
    // Cache last known compass slot to avoid full scans each update
    private final java.util.Map<java.util.UUID, Integer> compassSlotCache = new java.util.HashMap<>();
    // Track hunters we've already notified about End portal hint this game
    private final java.util.Set<java.util.UUID> endHintNotifiedOnce = new java.util.HashSet<>();
    private volatile org.bukkit.Location lastRunnerOverworldLocation;
    private volatile org.bukkit.Location lastRunnerEndPortalLocation;
    
    public TrackerManager(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
    }

    /**
     * Cleanup per-player caches when they leave the server.
     */
    public void cleanupPlayer(java.util.UUID id) {
        if (id == null) return;
        compassSlotCache.remove(id);
        endHintNotifiedOnce.remove(id);
    }
    
    /**
     * Start tracking the active runner
     */
    public void startTracking() {
        if (!plugin.getConfigManager().isTrackerEnabled()) {
            stopTracking();
            return;
        }
        synchronized (taskLock) {
            if (trackerTask != null) {
                trackerTask.cancel();
            }

            // Respect configured update rate with a sane minimum
            int updateTicks = Math.max(10, plugin.getConfigManager().getTrackerUpdateTicks());

            trackerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (!plugin.getConfigManager().isTrackerEnabled()) {
                    stopTracking();
                    return;
                }
                if (!hasOnlineHunters()) {
                    stopTracking();
                    return;
                }
                Player activeRunner = plugin.getGameManager().getActiveRunner();
                if (activeRunner == null || !activeRunner.isOnline() || !plugin.getGameManager().isGameRunning()) {
                    return;
                }
                cacheRunnerPositions(activeRunner);

                // Update compass for all hunters regardless of dimension
                for (Player hunter : plugin.getGameManager().getHunters()) {
                    if (hunter.isOnline()) {
                        updateHunterCompass(hunter, activeRunner);
                    }
                }
            }, 0L, updateTicks);
        }
    }

    // Particle trails have been removed intentionally. Hunters receive only the tracking compass.
    // If a future toggle is desired, reintroduce scheduled particleTask logic here guarded by config.

    /**
     * Stop tracking
     */
    public void stopTracking() {
        synchronized (taskLock) {
            if (trackerTask != null) {
                trackerTask.cancel();
                trackerTask = null;
            }
        }
    }
    
    /**
     * Give a tracking compass to a hunter
     * @param hunter The hunter to give the compass to
     */
    public void giveTrackingCompass(Player hunter) {
        if (hunter == null || !hunter.isOnline()) {
            return;
        }
        
        // Avoid duplicates
        if (!hasCompass(hunter)) {
            ItemStack compass = new ItemStack(Material.COMPASS);
            hunter.getInventory().addItem(compass);
            // Reset cache so we find it quickly next time
            compassSlotCache.remove(hunter.getUniqueId());
        }
        // Update the compass target now
        updateCompass(hunter);
    }
    
    /**
     * Update the compass for a hunter
     * @param hunter The hunter to update the compass for
     */
    public void updateCompass(Player hunter) {
        Player activeRunner = plugin.getGameManager().getActiveRunner();
        if (activeRunner != null && activeRunner.isOnline()) {
            updateHunterCompass(hunter, activeRunner);
        }
    }
    
    /**
     * Update a hunter's compass to point to the active runner
     * @param hunter The hunter to update
     * @param target The target to track
     */
    private void updateHunterCompass(Player hunter, Player activeRunner) {
        if (hunter == null || activeRunner == null) return;
        if (!hunter.isOnline()) return;
        // If tracking is jammed, skip updating targets temporarily
        if (isJammed) return;

        org.bukkit.World hw = hunter.getWorld();
        org.bukkit.World rw = activeRunner.getWorld();

        org.bukkit.Location target;
        if (hw.equals(rw)) {
            target = activeRunner.getLocation();
        } else {
            // Overworld <-> Nether scaling; End = hint + neutral fallback
            org.bukkit.World.Environment he = hw.getEnvironment();
            org.bukkit.World.Environment re = rw.getEnvironment();
            org.bukkit.Location rloc = activeRunner.getLocation();

            if (re == org.bukkit.World.Environment.THE_END) {
                if (lastRunnerEndPortalLocation != null && lastRunnerEndPortalLocation.getWorld() != null) {
                    target = lastRunnerEndPortalLocation.clone();
                } else {
                    if (endHintNotifiedOnce.add(hunter.getUniqueId())) {
                        hunter.sendMessage("§eTracker: Runner is in §5The End§e. Fallback compass target set to spawn.");
                    }
                    target = hw.getSpawnLocation();
                }
            } else if (he == org.bukkit.World.Environment.NORMAL && re == org.bukkit.World.Environment.NETHER) {
                if (lastRunnerOverworldLocation != null && lastRunnerOverworldLocation.getWorld() != null) {
                    target = lastRunnerOverworldLocation.clone();
                } else {
                    target = hw.getSpawnLocation();
                }
            } else if (he == org.bukkit.World.Environment.NETHER && re == org.bukkit.World.Environment.NORMAL) {
                target = new org.bukkit.Location(hw, rloc.getX() / 8.0, rloc.getY(), rloc.getZ() / 8.0);
            } else {
                target = hw.getSpawnLocation();
            }
        }

        // Update every compass in inventory (incl. offhand)
        org.bukkit.inventory.PlayerInventory inv = hunter.getInventory();
        org.bukkit.inventory.ItemStack off = inv.getItemInOffHand();

        java.util.function.Consumer<org.bukkit.inventory.ItemStack> updateCompass = item -> {
            if (item == null || item.getType() != org.bukkit.Material.COMPASS) return;
            org.bukkit.inventory.meta.ItemMeta im = item.getItemMeta();
            if (!(im instanceof org.bukkit.inventory.meta.CompassMeta cm)) return;
            cm.setLodestone(target);
            cm.setLodestoneTracked(false);
            item.setItemMeta(cm);
        };

        updateCompass.accept(off);
        for (org.bukkit.inventory.ItemStack it : inv.getContents()) updateCompass.accept(it);
    }

    /**
     * Update compasses for all hunters
     */
    public void updateAllHunterCompasses() {
        Player activeRunner = plugin.getGameManager().getActiveRunner();
        if (activeRunner != null && activeRunner.isOnline()) {
            if (!hasOnlineHunters()) {
                stopTracking();
                return;
            }
            for (Player hunter : plugin.getGameManager().getHunters()) {
                if (hunter.isOnline()) {
                    updateHunterCompass(hunter, activeRunner);
                }
            }
        }
    }

    private void cacheRunnerPositions(Player runner) {
        if (runner == null) return;
        org.bukkit.Location loc = runner.getLocation();
        if (loc == null || loc.getWorld() == null) return;
        org.bukkit.World.Environment env = loc.getWorld().getEnvironment();
        if (env == org.bukkit.World.Environment.NORMAL) {
            lastRunnerOverworldLocation = loc.clone();
        }
    }

    public void setLastRunnerOverworldLocation(org.bukkit.Location location) {
        if (location == null || location.getWorld() == null) return;
        if (location.getWorld().getEnvironment() != org.bukkit.World.Environment.NORMAL) return;
        lastRunnerOverworldLocation = location.clone();
    }

    public void setLastRunnerEndPortalLocation(org.bukkit.Location location) {
        if (location == null || location.getWorld() == null) return;
        lastRunnerEndPortalLocation = location.clone();
        endHintNotifiedOnce.clear();
    }

    public void jamCompasses(long durationTicks) {
        isJammed = true;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            isJammed = false;
        }, durationTicks);
    }

    private boolean hasCompass(Player player) {
        if (player == null) return false;
        var inv = player.getInventory();
        if (inv.contains(Material.COMPASS, 1)) return true;
        if (inv.getItemInOffHand() != null && inv.getItemInOffHand().getType() == Material.COMPASS) return true;
        for (ItemStack stack : inv.getContents()) {
            if (stack != null && stack.getType() == Material.COMPASS) return true;
        }
        for (ItemStack stack : player.getEnderChest().getContents()) {
            if (stack != null && stack.getType() == Material.COMPASS) return true;
        }
        return false;
    }

    private boolean hasOnlineHunters() {
        java.util.List<Player> hunters = plugin.getGameManager().getHunters();
        if (hunters == null || hunters.isEmpty()) return false;
        for (Player hunter : hunters) {
            if (hunter != null && hunter.isOnline()) {
                return true;
            }
        }
        return false;
    }
}
