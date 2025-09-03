package com.example.speedrunnerswap.tasks;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Task for handling scheduled swaps
 */
public class SwapTask extends BukkitRunnable {
    
    private final SpeedrunnerSwap plugin;
    
    public SwapTask(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void run() {
        // Trigger an immediate swap in the game manager
        if (plugin.getGameManager().isGameRunning() && !plugin.getGameManager().isGamePaused()) {
            plugin.getGameManager().triggerImmediateSwap();
        }
    }
}
