package com.example.speedrunnerswap.game;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import com.example.speedrunnerswap.utils.Msg;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.scheduler.BukkitTask;

public class WorldBorderManager {
    private final SpeedrunnerSwap plugin;
    // Values are read from config at runtime to reflect GUI changes
    private boolean isActive;
    private BukkitTask borderWarningTask;

    public WorldBorderManager(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
        this.isActive = false;
        this.borderWarningTask = null;
    }

    public void startBorderShrinking() {
        if (isActive) return;
        isActive = true;

        // Set up world border for all worlds
        int initialSize = plugin.getConfig().getInt("world_border.initial_size", 2000);
        int finalSize = plugin.getConfig().getInt("world_border.final_size", 100);
        long shrinkDuration = plugin.getConfig().getLong("world_border.shrink_duration", 1800);

        for (World world : Bukkit.getWorlds()) {
            WorldBorder border = world.getWorldBorder();
            border.setCenter(0, 0);
            border.setSize(initialSize);
            
            // Start shrinking
            border.setSize(finalSize, shrinkDuration);

            // Broadcast border start message
            Msg.broadcast("§c§lWorld Border will shrink from " +
                initialSize + " blocks to " + finalSize + " blocks over " +
                (shrinkDuration / 60) + " minutes!");
        }

        // Schedule warning messages
        scheduleBorderWarnings();
    }

    public void stopBorderShrinking() {
        if (!isActive) return;
        isActive = false;

        int initialSize = plugin.getConfig().getInt("world_border.initial_size", 2000);
        for (World world : Bukkit.getWorlds()) {
            WorldBorder border = world.getWorldBorder();
            border.setSize(initialSize);
        }

        // Cancel warnings task if running
        if (borderWarningTask != null) {
            borderWarningTask.cancel();
            borderWarningTask = null;
        }
    }

    private void scheduleBorderWarnings() {
        // Cancel any previous task
        if (borderWarningTask != null) {
            borderWarningTask.cancel();
            borderWarningTask = null;
        }

        // Schedule periodic warnings about border size with configurable interval
        int warnEverySec = Math.max(30, plugin.getConfig().getInt("world_border.warning_interval", 120));
        long period = warnEverySec * 20L;
        borderWarningTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!isActive) return;
            try {
                for (World world : Bukkit.getWorlds()) {
                    double size = world.getWorldBorder().getSize();
                    Msg.broadcast("§eWorld Border current size: §f" + (int) size + "§e blocks.");
                }
            } catch (Throwable ignored) {}
        }, period, period);
    }

    public boolean isActive() {
        return isActive;
    }
}
