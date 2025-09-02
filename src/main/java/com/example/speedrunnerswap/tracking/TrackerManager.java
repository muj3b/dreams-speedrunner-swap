package com.example.speedrunnerswap.tracking;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.Particle;
import org.bukkit.Color;
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
            
            // Force more frequent updates (5 ticks = 0.25 seconds) for smoother tracking
            int updateTicks = Math.min(5, plugin.getConfigManager().getTrackerUpdateTicks());
            
            trackerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                Player activeRunner = plugin.getGameManager().getActiveRunner();
                if (activeRunner == null || !activeRunner.isOnline() || !plugin.getGameManager().isGameRunning()) {
                    return;
                }
                
                // Update compass for all hunters
                for (Player hunter : plugin.getGameManager().getHunters()) {
                    if (hunter.isOnline() && hunter.getWorld().equals(activeRunner.getWorld())) {
                        updateHunterCompass(hunter, activeRunner);
                    }
                }
            }, 0L, updateTicks);
        }

        if (plugin.getConfigManager().isShowParticles()) {
            synchronized (taskLock) {
                if (particleTask != null) {
                    particleTask.cancel();
                }
                int spawnInterval = plugin.getConfigManager().getParticleSpawnInterval();
                particleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                    Player activeRunner = plugin.getGameManager().getActiveRunner();
                    if (activeRunner == null || !activeRunner.isOnline() || !plugin.getGameManager().isGameRunning()) {
                        return;
                    }

                    String particleTypeStr = plugin.getConfigManager().getParticleTrailType();
                    Particle particleType;
                    try {
                        particleType = Particle.valueOf(particleTypeStr);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid particle type: " + particleTypeStr);
                        return;
                    }

                    Object data = null;
                    if (particleType == Particle.DUST) {
                        int[] rgb = plugin.getConfigManager().getParticleTrailColor();
                        data = new Particle.DustOptions(Color.fromRGB(rgb[0], rgb[1], rgb[2]), 1.0f);
                    }

                    for (Player hunter : plugin.getGameManager().getHunters()) {
                        if (hunter.isOnline() && hunter.getWorld().equals(activeRunner.getWorld())) {
                            hunter.spawnParticle(particleType, activeRunner.getLocation(), 5, 0.5, 0.5, 0.5, 0, data);
                        }
                    }
                }, 0L, spawnInterval);
            }
        }
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
        
        // Create compass item
        ItemStack compass = new ItemStack(Material.COMPASS);
        
        // Add to inventory
        hunter.getInventory().addItem(compass);
        
        // Update the compass
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
            // Check if hunter has a compass
            ItemStack compass = null;
            int slot = -1;
            
            // Check main inventory for compass
            for (int i = 0; i < hunter.getInventory().getSize(); i++) {
                ItemStack item = hunter.getInventory().getItem(i);
                if (item != null && item.getType() == Material.COMPASS) {
                    compass = item;
                    slot = i;
                    break;
                }
            }
            
            // If no compass found, give one
            if (compass == null) {
                giveTrackingCompass(hunter);
                return;
            }
            
            // Update compass target
            CompassMeta meta = null;
            if (compass.getItemMeta() instanceof CompassMeta) {
                meta = (CompassMeta) compass.getItemMeta();
            }

            if (meta != null) {
                if (isJammed) {
                    meta.setLodestone(new Location(hunter.getWorld(), Math.random() * 1000 - 500, 64, Math.random() * 1000 - 500));
                    meta.setLodestoneTracked(true);
                } else if (target.getLocation() != null) {
                    Location adjustedLoc;
                    World.Environment hunterEnv = hunter.getWorld().getEnvironment();
                    World.Environment targetEnv = target.getWorld().getEnvironment();
                    
                    if (hunterEnv == targetEnv) {
                        // Same dimension - direct tracking
                        adjustedLoc = target.getLocation();
                    } else if (hunterEnv == World.Environment.NORMAL && targetEnv == World.Environment.NETHER) {
                        // Overworld to Nether conversion (8:1)
                        adjustedLoc = new Location(hunter.getWorld(),
                            target.getLocation().getX() / 8,
                            Math.min(Math.max(target.getLocation().getY(), 0), 128), // Clamp Y to valid range
                            target.getLocation().getZ() / 8
                        );
                    } else if (hunterEnv == World.Environment.NETHER && targetEnv == World.Environment.NORMAL) {
                        // Nether to Overworld conversion (1:8)
                        adjustedLoc = new Location(hunter.getWorld(),
                            target.getLocation().getX() * 8,
                            Math.min(Math.max(target.getLocation().getY(), 0), 256), // Clamp Y to valid range
                            target.getLocation().getZ() * 8
                        );
                    } else if (targetEnv == World.Environment.THE_END) {
                        // Target in End - point to end portal
                        adjustedLoc = new Location(hunter.getWorld(), 100, 50, 0); // Approximate end portal location
                        hunter.sendMessage("§eTarget is in The End! Compass points to the End Portal.");
                    } else {
                        // Fallback - point to world spawn
                        adjustedLoc = hunter.getWorld().getSpawnLocation();
                        hunter.sendMessage(net.kyori.adventure.text.Component.text("§cTarget is in " + targetEnv + ", compass points to spawn."));
                    }
                    
                    // Update compass
                    hunter.setCompassTarget(adjustedLoc);
                    meta.setLodestone(null);
                    meta.setLodestoneTracked(false);
                    compass.setItemMeta(meta);
                    
                    // Update inventory
                    if (slot != -1) {
                        hunter.getInventory().setItem(slot, compass);
                    }
                    
                    // Send distance message if enabled
                    if (hunterEnv == targetEnv && plugin.getConfigManager().isShowDistance() && System.currentTimeMillis() % 5000 < 50) {
                        int distance = (int) hunter.getLocation().distance(target.getLocation());
                        hunter.sendActionBar(net.kyori.adventure.text.Component.text()
                            .append(net.kyori.adventure.text.Component.text("Distance to target: ", net.kyori.adventure.text.format.NamedTextColor.YELLOW))
                            .append(net.kyori.adventure.text.Component.text(distance + " blocks", net.kyori.adventure.text.format.NamedTextColor.WHITE))
                            .build());
                    }
                }
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
}