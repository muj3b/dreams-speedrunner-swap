package com.example.speedrunnerswap.tasks;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import org.bukkit.Bukkit;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import com.example.speedrunnerswap.game.LastStandManager;

public class SwapTask extends BukkitRunnable {
    private final SpeedrunnerSwap plugin;
    private int timeUntilNextSwap;
    private final Random random = new Random();
    private final LastStandManager lastStandManager;

    public SwapTask(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
        this.timeUntilNextSwap = plugin.getConfigManager().getSwapInterval();
        this.lastStandManager = new LastStandManager(plugin);
    }

    @Override
    public void run() {
        if (timeUntilNextSwap <= 0) {
            performSwap();
            resetTimer();
        } else {
            if (timeUntilNextSwap <= 5) {
                Bukkit.broadcast(Component.text("§e§lSwap in " + timeUntilNextSwap + " seconds!"));
            }
            timeUntilNextSwap--;
        }
    }

    private void performSwap() {
        List<Player> onlineRunners = new ArrayList<>();
        for (Player runner : plugin.getGameManager().getRunners()) {
            if (runner.isOnline()) {
                onlineRunners.add(runner);
            }
        }

        if (onlineRunners.isEmpty()) {
            Bukkit.broadcast(Component.text("§cNo online runners to swap with!"));
            return;
        }

        Player currentRunner = plugin.getGameManager().getActiveRunner();
        
        // Apply powerup to the new runner if enabled
        if (plugin.getConfigManager().isPowerUpsEnabled()) {
            plugin.getPowerUpManager().applyRandomEffect(currentRunner);
        }
        Player nextRunner;
        
        // Check for Last Stand conditions
        if (onlineRunners.size() == 1 && plugin.getConfigManager().isLastStandEnabled()) {
            lastStandManager.activateLastStand(onlineRunners.get(0));
        }

        if (plugin.getConfigManager().isSwapRandomized()) {
            // Random selection
            do {
                nextRunner = onlineRunners.get(random.nextInt(onlineRunners.size()));
            } while (onlineRunners.size() > 1 && nextRunner.equals(currentRunner));
        } else {
            // Sequential selection
            int currentIndex = onlineRunners.indexOf(currentRunner);
            if (currentIndex == -1 || currentIndex == onlineRunners.size() - 1) {
                nextRunner = onlineRunners.get(0);
            } else {
                nextRunner = onlineRunners.get(currentIndex + 1);
            }
        }

        plugin.getGameManager().setActiveRunner(nextRunner);
        Bukkit.broadcast(Component.text("§a§lSwap! §fNew runner is §b" + nextRunner.getName()));
    }

    private void resetTimer() {
        if (plugin.getConfigManager().isSwapRandomized()) {
            int minInterval = plugin.getConfigManager().getMinSwapInterval();
            int maxInterval = plugin.getConfigManager().getMaxSwapInterval();
            timeUntilNextSwap = random.nextInt(maxInterval - minInterval + 1) + minInterval;
        } else {
            timeUntilNextSwap = plugin.getConfigManager().getSwapInterval();
        }
    }

    public void setTimeUntilNextSwap(int seconds) {
        this.timeUntilNextSwap = seconds;
    }

    public int getTimeUntilNextSwap() {
        return timeUntilNextSwap;
    }
}