/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.Sound
 *  org.bukkit.World
 *  org.bukkit.World$Environment
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 *  org.bukkit.scheduler.BukkitTask
 */
package com.example.speedrunnerswap.game;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import com.example.speedrunnerswap.utils.SafeLocationFinder;
import java.util.Collection;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
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
        if (this.isActive) {
            return;
        }
        if (this.scheduledTask != null) {
            this.scheduledTask.cancel();
            this.scheduledTask = null;
        }
        long seconds = this.plugin.getConfig().getLong("sudden_death.activation_delay", 1200L);
        long minutes = Math.max(1L, seconds) / 60L;
        long activationDelayTicks = Math.max(1L, seconds) * 20L;
        this.scheduledTask = Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, this::activateSuddenDeath, activationDelayTicks);
        Bukkit.broadcast((Component)Component.text((String)"\n\u00a74\u00a7l=== SUDDEN DEATH SCHEDULED ==="));
        Bukkit.broadcast((Component)Component.text((String)("\u00a7cSudden Death will begin in " + minutes + " minutes!")));
    }

    public void activateSuddenDeath() {
        World endWorld;
        if (this.isActive) {
            return;
        }
        this.isActive = true;
        if (this.scheduledTask != null) {
            this.scheduledTask.cancel();
            this.scheduledTask = null;
        }
        if ((endWorld = (World)Bukkit.getWorlds().stream().filter(world -> world.getEnvironment() == World.Environment.THE_END).findFirst().orElse(null)) == null) {
            this.plugin.getLogger().warning("Could not find The End dimension for Sudden Death!");
            return;
        }
        this.announceSuddenDeath();
        double ax = this.plugin.getConfig().getDouble("sudden_death.arena.x", 100.0);
        double ay = this.plugin.getConfig().getDouble("sudden_death.arena.y", 50.0);
        double az = this.plugin.getConfig().getDouble("sudden_death.arena.z", 0.0);
        Location arena = new Location(endWorld, ax, ay, az);
        Location spawnLocation = SafeLocationFinder.findSafeLocation(arena, this.plugin.getConfigManager().getSafeSwapHorizontalRadius(), this.plugin.getConfigManager().getSafeSwapVerticalDistance(), this.plugin.getConfigManager().getDangerousBlocks());
        if (spawnLocation == null) {
            spawnLocation = arena;
        }
        Collection players = Bukkit.getOnlinePlayers();
        for (Player player : players) {
            int resTicks = this.plugin.getConfig().getInt("sudden_death.effects.resistance_duration", 200);
            int regenTicks = this.plugin.getConfig().getInt("sudden_death.effects.regeneration_duration", 200);
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Math.max(20, resTicks), 4));
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Math.max(20, regenTicks), 2));
            player.teleport(spawnLocation);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().getEnvironment() != World.Environment.THE_END) continue;
            endWorld.strikeLightningEffect(player.getLocation().add(Math.random() * 6.0 - 3.0, 0.0, Math.random() * 6.0 - 3.0));
        }
    }

    private void announceSuddenDeath() {
        Bukkit.broadcast((Component)Component.text((String)"\n\u00a74\u00a7l=== SUDDEN DEATH ACTIVATED ==="));
        Bukkit.broadcast((Component)Component.text((String)"\u00a7cAll players have been teleported to The End!"));
        Bukkit.broadcast((Component)Component.text((String)"\u00a7cFight to the death!"));
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
        }
    }

    public void deactivate() {
        this.isActive = false;
        if (this.scheduledTask != null) {
            this.scheduledTask.cancel();
            this.scheduledTask = null;
        }
    }

    public boolean isActive() {
        return this.isActive;
    }

    public void cancelSchedule() {
        if (this.scheduledTask != null) {
            this.scheduledTask.cancel();
            this.scheduledTask = null;
            Bukkit.broadcast((Component)Component.text((String)"\u00a7eSudden Death schedule cancelled."));
        }
    }

    public boolean isScheduled() {
        return this.scheduledTask != null && !this.isActive;
    }
}

