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
    private volatile BukkitTask particleTask;
    private boolean isJammed = false;
    private final Object taskLock = new Object();
    // Cache last known compass slot to avoid full scans each update
    private final java.util.Map<java.util.UUID, Integer> compassSlotCache = new java.util.HashMap<>();
    // Track hunters we've already notified about End portal hint this game
    private final java.util.Set<java.util.UUID> endHintNotifiedOnce = new java.util.HashSet<>();
    
    public TrackerManager(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Start tracking the active runner
     */
    public void startTracking() {
        synchronized (taskLock) {
            if (trackerTask != null) {
                trackerTask.cancel();
            }
            
            // Respect configured update rate with a sane minimum
            int updateTicks = Math.max(10, plugin.getConfigManager().getTrackerUpdateTicks());
            
            trackerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                Player activeRunner = plugin.getGameManager().getActiveRunner();
                if (activeRunner == null || !activeRunner.isOnline() || !plugin.getGameManager().isGameRunning()) {
                    return;
                }

                // Update compass for all hunters regardless of dimension
                for (Player hunter : plugin.getGameManager().getHunters()) {
                    if (hunter.isOnline()) {
                        updateHunterCompass(hunter, activeRunner);
                    }
                }
            }, 0L, updateTicks);
        }

    // Particle trails have been removed intentionally. Hunters receive only the tracking compass.
    // If a future toggle is desired, reintroduce scheduled particleTask logic here guarded by config.
    }
    
    /**
     * Stop tracking
     */
    public void stopTracking() {
        synchronized (taskLock) {
            if (trackerTask != null) {
                trackerTask.cancel();
                trackerTask = null;
            }
            if (particleTask != null) {
                particleTask.cancel();
                particleTask = null;
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
        if (!hunter.getInventory().contains(Material.COMPASS)) {
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

            if (he == org.bukkit.World.Environment.NETHER && re == org.bukkit.World.Environment.NORMAL) {
                target = new org.bukkit.Location(hw, rloc.getX() / 8.0, rloc.getY(), rloc.getZ() / 8.0);
            } else if (he == org.bukkit.World.Environment.NORMAL && re == org.bukkit.World.Environment.NETHER) {
                target = new org.bukkit.Location(hw, rloc.getX() * 8.0, rloc.getY(), rloc.getZ() * 8.0);
            } else if (re == org.bukkit.World.Environment.THE_END) {
                if (endHintNotifiedOnce.add(hunter.getUniqueId())) {
                    hunter.sendMessage("§eTracker: Runner is in §5The End§e. Compass shows a fallback until you enter The End.");
                }
                target = hw.getSpawnLocation();
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
            for (Player hunter : plugin.getGameManager().getHunters()) {
                if (hunter.isOnline()) {
                    updateHunterCompass(hunter, activeRunner);
                }
            }
        }
    }

    public void jamCompasses(long durationTicks) {
        isJammed = true;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            isJammed = false;
        }, durationTicks);
    }
}
