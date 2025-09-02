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

public class SuddenDeathManager {
    private final SpeedrunnerSwap plugin;
    private boolean isActive;
    private final long activationDelay;

    public SuddenDeathManager(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
        this.isActive = false;
        this.activationDelay = plugin.getConfig().getLong("sudden_death.activation_delay", 1200) * 20; // Convert to ticks
    }

    public void scheduleSuddenDeath() {
        if (isActive) return;

        // Schedule sudden death activation
        Bukkit.getScheduler().runTaskLater(plugin, this::activateSuddenDeath, activationDelay);

        // Announce scheduled activation
        long minutes = activationDelay / (20 * 60);
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

        // Schedule periodic lightning for dramatic effect
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!isActive) return;
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getWorld().getEnvironment() == World.Environment.THE_END) {
                    endWorld.strikeLightningEffect(player.getLocation().add(
                        Math.random() * 20 - 10,
                        0,
                        Math.random() * 20 - 10
                    ));
                }
            }
        }, 0L, 60L); // Every 3 seconds
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
    }

    public boolean isActive() {
        return isActive;
    }
}
