/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 */
package com.example.speedrunnerswap.game;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class StatsManager {
    private final SpeedrunnerSwap plugin;
    private final Map<UUID, PlayerStats> playerStats;
    private long gameStartTime;
    private BukkitTask periodicTask;

    public StatsManager(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
        this.playerStats = new HashMap<UUID, PlayerStats>();
    }

    public void startTracking() {
        this.gameStartTime = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            this.playerStats.put(player.getUniqueId(), new PlayerStats());
        }
        if (this.plugin.getConfig().getBoolean("stats.periodic_display", false)) {
            int seconds = Math.max(60, this.plugin.getConfig().getInt("stats.periodic_display_interval", 300));
            long ticks = (long)seconds * 20L;
            if (this.periodicTask != null) {
                this.periodicTask.cancel();
            }
            this.periodicTask = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, this::displayStats, ticks, ticks);
        }
    }

    public void stopTracking() {
        this.displayStats();
        this.playerStats.clear();
        if (this.periodicTask != null) {
            this.periodicTask.cancel();
            this.periodicTask = null;
        }
    }

    public void recordKill(Player killer, Player victim) {
        if (killer != null) {
            this.getStats(killer).incrementKills();
        }
        this.getStats(victim).incrementDeaths();
    }

    public void recordActiveTime(Player player, long duration) {
        this.getStats(player).addActiveTime(duration);
    }

    public void recordDistanceTraveled(Player player, double distance) {
        this.getStats(player).addDistanceTraveled(distance);
    }

    private PlayerStats getStats(Player player) {
        return this.playerStats.computeIfAbsent(player.getUniqueId(), k -> new PlayerStats());
    }

    public void displayStats() {
        long gameDuration = System.currentTimeMillis() - this.gameStartTime;
        Bukkit.broadcast((Component)Component.text((String)"\n\u00a76=== Game Statistics ==="));
        Bukkit.broadcast((Component)Component.text((String)("\u00a77Total Game Time: \u00a7f" + this.formatTime(gameDuration))));
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerStats stats = this.getStats(player);
            String role = this.plugin.getGameManager().isRunner(player) ? "Runner" : "Hunter";
            Bukkit.broadcast((Component)Component.text((String)("\n\u00a7e" + player.getName() + " \u00a77(" + role + ")")));
            Bukkit.broadcast((Component)Component.text((String)("\u00a77Time as Active: \u00a7f" + this.formatTime(stats.getActiveTime()))));
            Bukkit.broadcast((Component)Component.text((String)("\u00a77Distance Traveled: \u00a7f" + String.format("%.1f", stats.getDistanceTraveled()) + " blocks")));
            Bukkit.broadcast((Component)Component.text((String)("\u00a77Kills: \u00a7f" + stats.getKills())));
            Bukkit.broadcast((Component)Component.text((String)("\u00a77Deaths: \u00a7f" + stats.getDeaths())));
        }
    }

    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000L;
        long minutes = seconds / 60L;
        long hours = minutes / 60L;
        return String.format("%02d:%02d:%02d", hours, minutes %= 60L, seconds %= 60L);
    }

    private static class PlayerStats {
        private int kills;
        private int deaths;
        private long activeTime;
        private double distanceTraveled;

        private PlayerStats() {
        }

        public void incrementKills() {
            ++this.kills;
        }

        public void incrementDeaths() {
            ++this.deaths;
        }

        public void addActiveTime(long duration) {
            this.activeTime += duration;
        }

        public void addDistanceTraveled(double distance) {
            this.distanceTraveled += distance;
        }

        public int getKills() {
            return this.kills;
        }

        public int getDeaths() {
            return this.deaths;
        }

        public long getActiveTime() {
            return this.activeTime;
        }

        public double getDistanceTraveled() {
            return this.distanceTraveled;
        }
    }
}

