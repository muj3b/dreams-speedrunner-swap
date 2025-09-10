package com.example.speedrunnerswap.game;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EnderDragonChangePhaseEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages Ender Dragon health to prevent restoration during swaps
 * while allowing natural healing from End Crystals
 */
public class DragonManager implements Listener {
    private final Map<UUID, Double> dragonHealthCache = new HashMap<>();
    private final Map<UUID, Long> lastHealthUpdate = new HashMap<>();
    private boolean isSwapInProgress = false;
    
    public DragonManager(SpeedrunnerSwap plugin) {
        // Plugin reference not currently used but kept for future extensibility
    }
    
    /**
     * Called when a swap is about to happen - save current dragon health
     */
    public void onSwapStart() {
        isSwapInProgress = true;
        saveDragonHealth();
    }
    
    /**
     * Called when a swap is complete - restore dragon health
     */
    public void onSwapComplete() {
        restoreDragonHealth();
        isSwapInProgress = false;
    }
    
    private void saveDragonHealth() {
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() == World.Environment.THE_END) {
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof EnderDragon dragon) {
                        dragonHealthCache.put(dragon.getUniqueId(), dragon.getHealth());
                        lastHealthUpdate.put(dragon.getUniqueId(), System.currentTimeMillis());
                    }
                }
            }
        }
    }
    
    private void restoreDragonHealth() {
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() == World.Environment.THE_END) {
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof EnderDragon dragon) {
                        Double savedHealth = dragonHealthCache.get(dragon.getUniqueId());
                        if (savedHealth != null) {
                            // Only restore if the dragon hasn't been naturally damaged/healed recently
                            Long lastUpdate = lastHealthUpdate.get(dragon.getUniqueId());
                            if (lastUpdate != null && System.currentTimeMillis() - lastUpdate < 1000) {
                                dragon.setHealth(savedHealth);
                            }
                        }
                    }
                }
            }
        }
    }
    
    @EventHandler
    public void onDragonDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof EnderDragon)) return;
        
        EnderDragon dragon = (EnderDragon) event.getEntity();
        dragonHealthCache.put(dragon.getUniqueId(), dragon.getHealth() - event.getFinalDamage());
        lastHealthUpdate.put(dragon.getUniqueId(), System.currentTimeMillis());
    }
    
    @EventHandler
    public void onDragonHeal(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof EnderDragon)) return;
        
        EnderDragon dragon = (EnderDragon) event.getEntity();
        
        // Allow natural healing from End Crystals (regeneration)
        if (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.REGEN) {
            dragonHealthCache.put(dragon.getUniqueId(), dragon.getHealth() + event.getAmount());
            lastHealthUpdate.put(dragon.getUniqueId(), System.currentTimeMillis());
        }
        // Block other types of healing during swaps
        else if (isSwapInProgress) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onDragonPhaseChange(EnderDragonChangePhaseEvent event) {
        EnderDragon dragon = event.getEntity();
        dragonHealthCache.put(dragon.getUniqueId(), dragon.getHealth());
        lastHealthUpdate.put(dragon.getUniqueId(), System.currentTimeMillis());
    }
    
    /**
     * Get the current health of all dragons in The End
     */
    public Map<UUID, Double> getDragonHealths() {
        Map<UUID, Double> currentHealths = new HashMap<>();
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() == World.Environment.THE_END) {
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof EnderDragon dragon) {
                        currentHealths.put(dragon.getUniqueId(), dragon.getHealth());
                    }
                }
            }
        }
        return currentHealths;
    }
    
    /**
     * Check if there are any dragons in The End
     */
    public boolean hasDragons() {
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() == World.Environment.THE_END) {
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof EnderDragon) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Clear all cached dragon health data
     */
    public void clearCache() {
        dragonHealthCache.clear();
        lastHealthUpdate.clear();
    }
}
