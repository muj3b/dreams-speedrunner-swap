package com.example.speedrunnerswap.tasks;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SwapTask extends BukkitRunnable {
    private final SpeedrunnerSwap plugin;
    private int timeUntilNextSwap;
    private final Random random = new Random();

    public SwapTask(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
        this.timeUntilNextSwap = plugin.getConfigManager().getSwapInterval();
    }

    @Override
    public void run() {
        if (timeUntilNextSwap <= 0) {
            performSwap();
            resetTimer();
        } else {
            if (timeUntilNextSwap <= 5) {
                Bukkit.broadcastMessage("§e§lSwap in " + timeUntilNextSwap + " seconds!");
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
            Bukkit.broadcastMessage("§cNo online runners to swap with!");
            return;
        }

        Player currentRunner = plugin.getGameManager().getActiveRunner();
        Player nextRunner;

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
        Bukkit.broadcastMessage("§a§lSwap! §fNew runner is §b" + nextRunner.getName());
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