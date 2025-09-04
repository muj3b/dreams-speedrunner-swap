/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Bukkit
 *  org.bukkit.World
 *  org.bukkit.WorldBorder
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 */
package com.example.speedrunnerswap.game;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class WorldBorderManager {
    private final SpeedrunnerSwap plugin;
    private boolean isActive;
    private BukkitTask borderWarningTask;

    public WorldBorderManager(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
        this.isActive = false;
        this.borderWarningTask = null;
    }

    public void startBorderShrinking() {
        if (this.isActive) {
            return;
        }
        this.isActive = true;
        int initialSize = this.plugin.getConfig().getInt("world_border.initial_size", 2000);
        int finalSize = this.plugin.getConfig().getInt("world_border.final_size", 100);
        long shrinkDuration = this.plugin.getConfig().getLong("world_border.shrink_duration", 1800L);
        for (World world : Bukkit.getWorlds()) {
            WorldBorder border = world.getWorldBorder();
            border.setCenter(0.0, 0.0);
            border.setSize((double)initialSize);
            border.setSize((double)finalSize, shrinkDuration);
            Bukkit.broadcast((Component)Component.text((String)("\u00a7c\u00a7lWorld Border will shrink from " + initialSize + " blocks to " + finalSize + " blocks over " + shrinkDuration / 60L + " minutes!")));
        }
        this.scheduleBorderWarnings();
    }

    public void stopBorderShrinking() {
        if (!this.isActive) {
            return;
        }
        this.isActive = false;
        int initialSize = this.plugin.getConfig().getInt("world_border.initial_size", 2000);
        for (World world : Bukkit.getWorlds()) {
            WorldBorder border = world.getWorldBorder();
            border.setSize((double)initialSize);
        }
        if (this.borderWarningTask != null) {
            this.borderWarningTask.cancel();
            this.borderWarningTask = null;
        }
    }

    private void scheduleBorderWarnings() {
        if (this.borderWarningTask != null) {
            this.borderWarningTask.cancel();
            this.borderWarningTask = null;
        }
        this.borderWarningTask = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, () -> {
            int finalSize;
            if (!this.isActive) {
                return;
            }
            World overworld = (World)Bukkit.getWorlds().get(0);
            double currentSize = overworld.getWorldBorder().getSize();
            if (currentSize > (double)(finalSize = this.plugin.getConfig().getInt("world_border.final_size", 100))) {
                Bukkit.broadcast((Component)Component.text((String)String.format("\u00a7e\u00a7lWorld Border: \u00a7r\u00a7e%.0f blocks and shrinking!", currentSize)));
            }
        }, 6000L, 6000L);
    }

    public boolean isActive() {
        return this.isActive;
    }
}

