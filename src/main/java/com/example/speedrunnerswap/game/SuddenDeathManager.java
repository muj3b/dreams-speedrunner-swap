package com.example.speedrunnerswap.game;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Sound;

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

        // Announce scheduled activation
        Bukkit.broadcast(
            net.kyori.adventure.text.Component.text("\n§4§l=== SUDDEN DEATH SCHEDULED ===")
        );
        Bukkit.broadcast(
            net.kyori.adventure.text.Component.text("§cSudden Death will begin in " + minutes + " minutes!")
        );
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

        // Teleport all players to The End
        Location spawnLocation = new Location(endWorld, 100, 50, 0); // Safe location away from the center
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        
        for (Player player : players) {
            // Apply effects
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 10, 4)); // 10 seconds of resistance
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 10, 2)); // 10 seconds of regeneration
            
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
        Bukkit.broadcast(net.kyori.adventure.text.Component.text("\n§4§l=== SUDDEN DEATH ACTIVATED ==="));
        Bukkit.broadcast(net.kyori.adventure.text.Component.text("§cAll players have been teleported to The End!"));
        Bukkit.broadcast(net.kyori.adventure.text.Component.text("§cFight to the death!"));
        
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
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("§eSudden Death schedule cancelled."));
        }
    }

    /**
     * Whether a schedule is pending
     */
    public boolean isScheduled() {
        return scheduledTask != null && !isActive;
    }
}
