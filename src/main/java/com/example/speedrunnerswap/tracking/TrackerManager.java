package com.example.speedrunnerswap.tracking;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.scheduler.BukkitTask;
// Particle functionality removed - imports intentionally omitted
import org.bukkit.Location;
import org.bukkit.World;

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
    private void updateHunterCompass(Player hunter, Player target) {
        // Validate parameters
        if (hunter == null || target == null || !hunter.isOnline() || !target.isOnline()) {
            return;
        }
        
        try {
            // Find an existing compass (use cache first)
            int slot = findCompassSlot(hunter);
            if (slot == -1) {
                giveTrackingCompass(hunter);
                slot = findCompassSlot(hunter);
                if (slot == -1) return; // couldn't find/give
            }
            ItemStack compass = hunter.getInventory().getItem(slot);
            if (compass == null || compass.getType() != Material.COMPASS) return;

            // Compute target location or apply jamming
            Location adjustedLoc;
            World.Environment hunterEnv = hunter.getWorld().getEnvironment();
            World.Environment targetEnv = target.getWorld().getEnvironment();

            if (isJammed) {
                CompassMeta meta = (CompassMeta) compass.getItemMeta();
                meta.setLodestone(new Location(hunter.getWorld(), Math.random() * 1000 - 500, 64, Math.random() * 1000 - 500));
                meta.setLodestoneTracked(false);
                compass.setItemMeta(meta);
            } else if (hunterEnv == targetEnv) {
                // Same dimension: track exact target
                adjustedLoc = target.getLocation();
                hunter.setCompassTarget(adjustedLoc);
            } else if (hunterEnv == World.Environment.NORMAL && targetEnv == World.Environment.NETHER) {
                adjustedLoc = new Location(hunter.getWorld(), target.getLocation().getX() / 8,
                        Math.min(Math.max(target.getLocation().getY(), 0), 128), target.getLocation().getZ() / 8);
                hunter.setCompassTarget(adjustedLoc);
            } else if (hunterEnv == World.Environment.NETHER && targetEnv == World.Environment.NORMAL) {
                adjustedLoc = new Location(hunter.getWorld(), target.getLocation().getX() * 8,
                        Math.min(Math.max(target.getLocation().getY(), 0), 256), target.getLocation().getZ() * 8);
                hunter.setCompassTarget(adjustedLoc);
            } else if (targetEnv == World.Environment.THE_END) {
                // Point towards a configured hint if available (per hunter's current world)
                Location hint = plugin.getConfigManager().getEndPortalHint(hunter.getWorld());
                adjustedLoc = hint != null ? hint : hunter.getWorld().getSpawnLocation();
                hunter.setCompassTarget(adjustedLoc);
                hunter.sendMessage("§eTarget is in The End! Compass points to the End Portal hint.");
            } else {
                adjustedLoc = hunter.getWorld().getSpawnLocation();
                hunter.setCompassTarget(adjustedLoc);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error updating compass for hunter " + hunter.getName() + ": " + e.getMessage());
        }
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

    private int findCompassSlot(Player hunter) {
        Integer cached = compassSlotCache.get(hunter.getUniqueId());
        if (cached != null) {
            ItemStack cachedItem = hunter.getInventory().getItem(cached);
            if (cachedItem != null && cachedItem.getType() == Material.COMPASS) return cached;
        }
        for (int i = 0; i < hunter.getInventory().getSize(); i++) {
            ItemStack item = hunter.getInventory().getItem(i);
            if (item != null && item.getType() == Material.COMPASS) {
                compassSlotCache.put(hunter.getUniqueId(), i);
                return i;
            }
        }
        compassSlotCache.remove(hunter.getUniqueId());
        return -1;
    }
}
