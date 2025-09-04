/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.scheduler.BukkitRunnable
 */
package com.example.speedrunnerswap.tasks;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import org.bukkit.scheduler.BukkitRunnable;

public class SwapTask
extends BukkitRunnable {
    private final SpeedrunnerSwap plugin;

    public SwapTask(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
    }

    public void run() {
        if (this.plugin.getGameManager().isGameRunning() && !this.plugin.getGameManager().isGamePaused()) {
            this.plugin.getGameManager().triggerImmediateSwap();
        }
    }
}

