package com.example.speedrunnerswap.game;

import com.example.speedrunnerswap.SpeedrunnerSwap;
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
            Bukkit.broadcastMessage("§c§lWorld Border will shrink from " +
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

        // Schedule periodic warnings about border size
        borderWarningTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!isActive) return;

            World overworld = Bukkit.getWorlds().get(0);
            double currentSize = overworld.getWorldBorder().getSize();
            int finalSize = plugin.getConfig().getInt("world_border.final_size", 100);
            
            if (currentSize > finalSize) {
                Bukkit.broadcastMessage(String.format("§e§lWorld Border: §r§e%.0f blocks and shrinking!", currentSize));
            }
        }, 20 * 60 * 5, 20 * 60 * 5); // Every 5 minutes
    }

    public boolean isActive() {
        return isActive;
    }
}
