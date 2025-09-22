package com.example.speedrunnerswap.game;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Sound;
import com.example.speedrunnerswap.utils.SafeLocationFinder;

import java.util.Collection;
import org.bukkit.scheduler.BukkitTask;

public class SuddenDeathManager {
    private final SpeedrunnerSwap plugin;
    private boolean isActive;
    private BukkitTask scheduledTask;

    public SuddenDeathManager(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
        this.isActive = false;
        this.scheduledTask = null;
    }

// Cross-platform broadcast of legacy strings (avoids Adventure dependency)
private void broadcastLegacy(String msg) {
    for (Player p : Bukkit.getOnlinePlayers()) {
        try { p.sendMessage(msg); } catch (Throwable ignored) {}
    }
}

    public void scheduleSuddenDeath() {
        if (isActive) return;

        // Cancel previous schedule if any
        if (scheduledTask != null) {
            scheduledTask.cancel();
            scheduledTask = null;
        }

        // Read seconds from config and convert to ticks; display minutes
        long seconds = plugin.getConfig().getLong("sudden_death.activation_delay", 1200);
        long minutes = Math.max(1, seconds) / 60L;
        long activationDelayTicks = Math.max(1L, seconds) * 20L;

        // Schedule sudden death activation
        scheduledTask = Bukkit.getScheduler().runTaskLater(plugin, this::activateSuddenDeath, activationDelayTicks);

        // Announce scheduled activation (cross-platform)
        broadcastLegacy("\n§4§l=== SUDDEN DEATH SCHEDULED ===");
        broadcastLegacy("§cSudden Death will begin in " + minutes + " minutes!");
    }

    public void activateSuddenDeath() {
        if (isActive) return;
        isActive = true;
        // Clear any pending schedule
        if (scheduledTask != null) {
            scheduledTask.cancel();
            scheduledTask = null;
        }

        // Find The End dimension
        World endWorld = Bukkit.getWorlds().stream()
            .filter(world -> world.getEnvironment() == World.Environment.THE_END)
            .findFirst()
            .orElse(null);

        if (endWorld == null) {
            plugin.getLogger().warning("Could not find The End dimension for Sudden Death!");
            return;
        }

        // Announce activation
        announceSuddenDeath();

        // Determine arena spawn from config, and try to find a safe nearby spot
        double ax = plugin.getConfig().getDouble("sudden_death.arena.x", 100.0);
        double ay = plugin.getConfig().getDouble("sudden_death.arena.y", 50.0);
        double az = plugin.getConfig().getDouble("sudden_death.arena.z", 0.0);
        Location arena = new Location(endWorld, ax, ay, az);
        Location spawnLocation = SafeLocationFinder.findSafeLocation(
                arena,
                plugin.getConfigManager().getSafeSwapHorizontalRadius(),
                plugin.getConfigManager().getSafeSwapVerticalDistance(),
                plugin.getConfigManager().getDangerousBlocks());
        if (spawnLocation == null) spawnLocation = arena;
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        
        for (Player player : players) {
            // Apply effects (durations from config, ticks)
            int resTicks = plugin.getConfig().getInt("sudden_death.effects.resistance_duration", 200);
            int regenTicks = plugin.getConfig().getInt("sudden_death.effects.regeneration_duration", 200);
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Math.max(20, resTicks), 4));
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Math.max(20, regenTicks), 2));
            
            // Teleport
            player.teleport(spawnLocation);
            
            // Play sound
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
        }

        // One-time lightning effect near each player in The End (no repeating)
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().getEnvironment() == World.Environment.THE_END) {
                endWorld.strikeLightningEffect(player.getLocation().add(
                    Math.random() * 6 - 3,
                    0,
                    Math.random() * 6 - 3
                ));
            }
        }
    }

    private void announceSuddenDeath() {
        broadcastLegacy("\n§4§l=== SUDDEN DEATH ACTIVATED ===");
        broadcastLegacy("§cAll players have been teleported to The End!");
        broadcastLegacy("§cFight to the death!");
        
        // Play dramatic sound for all players
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
        }
    }

    public void deactivate() {
        isActive = false;
        if (scheduledTask != null) {
            scheduledTask.cancel();
            scheduledTask = null;
        }
    }

    public boolean isActive() {
        return isActive;
    }

    /**
     * Cancel a pending scheduled activation, if any
     */
    public void cancelSchedule() {
        if (scheduledTask != null) {
            scheduledTask.cancel();
            scheduledTask = null;
            broadcastLegacy("§eSudden Death schedule cancelled.");
        }
    }

    /**
     * Whether a schedule is pending
     */
    public boolean isScheduled() {
        return scheduledTask != null && !isActive;
    }
}
