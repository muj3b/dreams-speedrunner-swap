package com.example.speedrunnerswap.game;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import com.example.speedrunnerswap.utils.Msg;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StatsManager {
    private final SpeedrunnerSwap plugin;
    private final Map<UUID, PlayerStats> playerStats;
    private long gameStartTime;
    private BukkitTask periodicTask;

    public StatsManager(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
        this.playerStats = new HashMap<>();
    }

    public void startTracking() {
        gameStartTime = System.currentTimeMillis();
        // Initialize stats for all players
        for (Player player : Bukkit.getOnlinePlayers()) {
            playerStats.put(player.getUniqueId(), new PlayerStats());
        }
        // Optional periodic display
        if (plugin.getConfig().getBoolean("stats.periodic_display", false)) {
            int seconds = Math.max(60, plugin.getConfig().getInt("stats.periodic_display_interval", 300));
            long ticks = seconds * 20L;
            if (periodicTask != null) {
                periodicTask.cancel();
            }
            periodicTask = Bukkit.getScheduler().runTaskTimer(plugin, this::displayStats, ticks, ticks);
        }
    }

    public void stopTracking() {
        displayStats();
        playerStats.clear();
        if (periodicTask != null) {
            periodicTask.cancel();
            periodicTask = null;
        }
    }

    public void recordKill(Player killer, Player victim) {
        if (killer != null) {
            getStats(killer).incrementKills();
        }
        getStats(victim).incrementDeaths();
    }

    public void recordActiveTime(Player player, long duration) {
        getStats(player).addActiveTime(duration);
    }

    public void recordDistanceTraveled(Player player, double distance) {
        getStats(player).addDistanceTraveled(distance);
    }

    private PlayerStats getStats(Player player) {
        return playerStats.computeIfAbsent(player.getUniqueId(), k -> new PlayerStats());
    }

    /**
     * Initialize stats for a player (useful for late joiners)
     * @param player The player to initialize stats for
     */
    public void initializePlayerStats(Player player) {
        getStats(player); // This will create the stats entry if it doesn't exist
    }

    public void displayStats() {
        long gameDuration = System.currentTimeMillis() - gameStartTime;
        Msg.broadcast("\n§6=== Game Statistics ===");
        Msg.broadcast("§7Total Game Time: §f" + formatTime(gameDuration));
        for (java.util.Map.Entry<java.util.UUID, PlayerStats> e : playerStats.entrySet()) {
            Player p = Bukkit.getPlayer(e.getKey());
            if (p == null) continue;
            PlayerStats s = e.getValue();
            Msg.broadcast("§e" + p.getName() + " §7- Kills: §f" + s.getKills() +
                    " §7Deaths: §f" + s.getDeaths() +
                    " §7Active: §f" + formatTime(s.getActiveTime()) +
                    " §7Distance: §f" + String.format(java.util.Locale.ROOT, "%.1f", s.getDistanceTraveled()) + "m");
        }
    }

    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        seconds %= 60;
        minutes %= 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private static class PlayerStats {
        private int kills;
        private int deaths;
        private long activeTime;
        private double distanceTraveled;

        public void incrementKills() {
            kills++;
        }

        public void incrementDeaths() {
            deaths++;
        }

        public void addActiveTime(long duration) {
            activeTime += duration;
        }

        public void addDistanceTraveled(double distance) {
            distanceTraveled += distance;
        }

        public int getKills() {
            return kills;
        }

        public int getDeaths() {
            return deaths;
        }

        public long getActiveTime() {
            return activeTime;
        }

        public double getDistanceTraveled() {
            return distanceTraveled;
        }
    }
}
