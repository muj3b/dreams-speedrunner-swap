package com.example.speedrunnerswap.game;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import com.example.speedrunnerswap.models.PlayerState;
import com.example.speedrunnerswap.models.Team;
import com.example.speedrunnerswap.utils.PlayerStateUtil;
import com.example.speedrunnerswap.utils.SafeLocationFinder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.example.speedrunnerswap.utils.BukkitCompat;
import com.example.speedrunnerswap.utils.Msg;
import org.bukkit.inventory.ItemStack;
import org.bukkit.GameMode;
import java.time.Duration;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class GameManager {
    private final SpeedrunnerSwap plugin;
    private boolean gameRunning;
    private boolean gamePaused;
    private Player activeRunner;
    private int activeRunnerIndex;
    private List<Player> runners;
    private List<Player> hunters;
    private BukkitTask swapTask;
    private BukkitTask hunterSwapTask;
    private BukkitTask actionBarTask;
    private BukkitTask titleTask;
    private BukkitTask freezeCheckTask;
    private BukkitTask cageTask;
    private BukkitTask runnerTimeoutTask;
    private long nextSwapTime;
    private final Map<UUID, PlayerState> playerStates;
    private final Map<UUID, Long> runnerDisconnectAt = new HashMap<>();
    // Shared cage management per-world (one cage in each world)
    private final java.util.Map<org.bukkit.World, java.util.List<org.bukkit.block.BlockState>> sharedCageBlocks = new java.util.HashMap<>();
    private final java.util.Map<org.bukkit.World, org.bukkit.Location> sharedCageCenters = new java.util.HashMap<>();
    private final java.util.Set<java.util.UUID> cagedPlayers = new java.util.HashSet<>();
    
    public GameManager(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
        this.gameRunning = false;
        this.gamePaused = false;
        this.activeRunnerIndex = 0;
        this.runners = new ArrayList<>();
        this.hunters = new ArrayList<>();
        this.playerStates = new HashMap<>();
    }
    
    public boolean startGame() {
        if (gameRunning) {
            return false;
        }
        
        if (!canStartGame()) {
            if (plugin.getCurrentMode() == com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.SAPNAP) {
                Msg.broadcast("§cGame cannot start: No online players available.");
            } else {
                Msg.broadcast("§cGame cannot start: At least one runner and one hunter are required.");
            }
            return false;
        }
        
        // Countdown
        new BukkitRunnable() {
            int count = 3;

            @Override
            public void run() {
                if (count > 0) {
                    Title title = Title.title(
                        Component.text("Starting in " + count).color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD),
                        Component.text("Made by muj3b").color(NamedTextColor.GRAY),
                        Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3500), Duration.ofMillis(500))
                    );
                    
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.showTitle(title);
                    }
                    count--;
                } else {
                    this.cancel();
                    gameRunning = true;
                    gamePaused = false;
                    activeRunnerIndex = 0;
                    activeRunner = runners.get(activeRunnerIndex);                saveAllPlayerStates();
                
                if (plugin.getConfigManager().isKitsEnabled()) {
                    for (Player player : runners) {
                        plugin.getKitManager().giveKit(player, "runner");
                    }
                    for (Player hunter : hunters) {
                        plugin.getKitManager().giveKit(hunter, "hunter");
                    }
                }
                
                applyInactiveEffects();
                scheduleNextSwap();
                if (plugin.getCurrentMode() == com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.DREAM) {
                    scheduleNextHunterSwap();
                }
                startActionBarUpdates();

                // If Task Manager mode, assign tasks and announce to each runner
                if (plugin.getCurrentMode() == com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.TASK) {
                    try { plugin.getTaskManagerMode().assignAndAnnounceTasks(runners); } catch (Throwable t) {
                        plugin.getLogger().warning("Task assignment failed: " + t.getMessage());
                    }
                }
                startTitleUpdates();
                startCageEnforcement();
                if (plugin.getCurrentMode() == com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.TASK) {
                    startRunnerTimeoutWatcher();
                }
                
                if (plugin.getCurrentMode() != com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.SAPNAP && plugin.getConfigManager().isTrackerEnabled()) {
                    plugin.getTrackerManager().startTracking();
                    for (Player hunter : hunters) {
                        if (hunter.isOnline()) {
                            plugin.getTrackerManager().giveTrackingCompass(hunter);
                        }
                    }
                }

                // Optionally start stats tracking
                if (plugin.getConfig().getBoolean("stats.enabled", true)) {
                    plugin.getStatsManager().startTracking();
                }

                if (plugin.getConfigManager().isFreezeMechanicEnabled()) {
                    startFreezeChecking();
                }
            }
        }
    }.runTaskTimer(plugin, 0L, 20L);
    
    return true;
}    public void endGame(Team winner) {
        if (!gameRunning) {
            return;
        }

        Component titleText;
        String runnerSubtitle = "";
        String hunterSubtitle = "";

        if (winner == Team.RUNNER) {
            titleText = Component.text("RUNNERS WIN!", NamedTextColor.GREEN, TextDecoration.BOLD);
            runnerSubtitle = "bro y'all are locked in, good stuff";
            hunterSubtitle = "bro y'all are locked in, good stuff";
        } else if (winner == Team.HUNTER) {
            titleText = Component.text("HUNTERS WIN!", NamedTextColor.RED, TextDecoration.BOLD);
            runnerSubtitle = "you ain't the main character unc";
            hunterSubtitle = "bro those speedrunners are trash asf";
        } else {
            titleText = Component.text("GAME OVER", NamedTextColor.RED, TextDecoration.BOLD);
            runnerSubtitle = "No winner declared.";
            hunterSubtitle = "No winner declared.";
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            Component subtitleText;
            if (isRunner(player)) {
                subtitleText = Component.text(runnerSubtitle, NamedTextColor.YELLOW);
            } else {
                subtitleText = Component.text(hunterSubtitle, NamedTextColor.YELLOW);
            }

            Title endTitle = Title.title(
                titleText,
                subtitleText,
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(5000), Duration.ofMillis(500))
            );
            player.showTitle(endTitle);
        }

        if (swapTask != null) swapTask.cancel();
        if (hunterSwapTask != null) hunterSwapTask.cancel();
        if (actionBarTask != null) actionBarTask.cancel();
        if (titleTask != null) titleTask.cancel();
        if (freezeCheckTask != null) freezeCheckTask.cancel();
        if (cageTask != null) { cageTask.cancel(); cageTask = null; }
        if (runnerTimeoutTask != null) { runnerTimeoutTask.cancel(); runnerTimeoutTask = null; }
        plugin.getTrackerManager().stopTracking();
        try { plugin.getStatsManager().stopTracking(); } catch (Exception ignored) {}

        new BukkitRunnable() {
            @Override
            public void run() {
                // Optionally preserve final runner progress for all runners (configurable)
                if (plugin.getConfig().getBoolean("swap.preserve_runner_progress_on_end", false)) {
                    try {
                        if (activeRunner != null && activeRunner.isOnline() && !runners.isEmpty()) {
                            com.example.speedrunnerswap.models.PlayerState finalState = PlayerStateUtil.capturePlayerState(activeRunner);
                            for (Player r : runners) {
                                playerStates.put(r.getUniqueId(), finalState);
                            }
                        }
                    } catch (Exception ex) {
                        plugin.getLogger().warning("Failed to capture/apply final runner state: " + ex.getMessage());
                    }
                }

                cleanupAllCages();
                restoreAllPlayerStates();
                
                gameRunning = false;
                gamePaused = false;
                activeRunner = null;
                
                if (plugin.getConfigManager().isBroadcastGameEvents()) {
                    String winnerMessage = (winner != null) ? winner.name() + " team won!" : "Game ended!";
                    Msg.broadcast("§a[SpeedrunnerSwap] Game ended! " + winnerMessage);
                }

                broadcastDonationMessage();
            }
        }.runTaskLater(plugin, 200L);
    }

    private void broadcastDonationMessage() {
        final String donateUrl = plugin.getConfig().getString(
            "donation.url",
            "https://donate.stripe.com/8x29AT0H58K03judnR0Ba01"
        );

        Component spacer = Component.text("");
        Component header = Component.text("=== Support the Creator ===")
            .color(NamedTextColor.GOLD)
            .decorate(TextDecoration.BOLD);
        Component desc = Component.text("Enjoyed the game? Help keep updates coming!")
            .color(NamedTextColor.YELLOW);
        Component donate = Component.text("❤ Click to Donate")
            .color(NamedTextColor.LIGHT_PURPLE)
            .decorate(TextDecoration.BOLD)
            .hoverEvent(HoverEvent.showText(Component.text("Open donation page", NamedTextColor.GOLD)))
            .clickEvent(ClickEvent.openUrl(donateUrl));

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(spacer);
            player.sendMessage(header);
            player.sendMessage(desc);
            player.sendMessage(donate);
            player.sendMessage(spacer);
        }
    }
    /** Stop the game without declaring a winner */
    public void stopGame() {
        endGame(null);
    }

    /**
     * Get whether the player is a hunter
     * @param player The player to check
     * @return true if the player is a hunter
     */
    public boolean isHunter(Player player) {
        return hunters.contains(player);
    }

    /**
     * Get whether the player is a runner
     * @param player The player to check
     * @return true if the player is a runner
     */
    public boolean isRunner(Player player) {
        return runners.contains(player);
    }

    /**
     * Get whether the game is running
     * @return true if the game is running
     */
    public boolean isGameRunning() {
        return gameRunning;
    }

    /**
     * Get the current active runner
     * @return The currently active runner
     */
    public Player getActiveRunner() {
        return activeRunner;
    }

    /**
     * Get all runners
     * @return List of all runners
     */
    public List<Player> getRunners() {
        return runners;
    }

    /**
     * Get all hunters
     * @return List of all hunters
     */
    public List<Player> getHunters() {
        return hunters;
    }
    
    /**
     * Refresh the swap schedule timer
     */
    public void refreshSwapSchedule() {
        if (gameRunning && !gamePaused) {
            scheduleNextSwap();
        }
    }

    /**
     * Refresh the action bar display for all players
     */
    public void refreshActionBar() {
        if (gameRunning && !gamePaused) {
            updateActionBar();
        }
    }

    /**
     * Get the game state for a specific player
     * @param player The player to get state for
     * @return The player's game state or null if not found
     */
    public PlayerState getPlayerState(Player player) {
        if (player == null) return null;
        return playerStates.computeIfAbsent(player.getUniqueId(), id -> PlayerStateUtil.capturePlayerState(player));
    }

    /**
     * Check if the game can be started
     * @return true if the game can be started, false otherwise
     */
    public boolean canStartGame() {
        if (gameRunning) {
            return false;
        }
        loadTeams();
        if (!runners.isEmpty()) {
            if (plugin.getCurrentMode() == com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.SAPNAP ||
                plugin.getCurrentMode() == com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.TASK) return true;
            return !hunters.isEmpty();
        }
        return false;
    }

    /**
     * Update teams after player join/leave
     */
    public void updateTeams() {
        List<Player> newRunners = new ArrayList<>();
        List<Player> newHunters = new ArrayList<>();

        for (Player runner : runners) {
            if (runner.isOnline()) {
                newRunners.add(runner);
            }
        }
        
        for (Player hunter : hunters) {
            if (hunter.isOnline()) {
                newHunters.add(hunter);
            }
        }

        runners = newRunners;
        hunters = newHunters;

        // If a team becomes empty due to disconnects, pause instead of ending the game.
        // In runner-only mode, ignore hunters list being empty.
        boolean teamEmpty = runners.isEmpty() || (plugin.getCurrentMode() != com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.SAPNAP && hunters.isEmpty());
        if (gameRunning && teamEmpty) {
            if (plugin.getConfigManager().isPauseOnDisconnect()) {
                pauseGame();
                if (plugin.getConfigManager().isBroadcastGameEvents()) {
                    Msg.broadcast("§e[SpeedrunnerSwap] Game paused: waiting for players to return.");
                }
            } else {
                // Keep running but log a warning for admins
                plugin.getLogger().warning("A team is empty; game continues (pause_on_disconnect=false)");
            }
        }
    }

    /**
     * Handle a player quitting
     * @param player The player who quit
     */
    public void handlePlayerQuit(Player player) {
        if (!gameRunning) {
            return;
        }

        if (isRunner(player)) {
            runnerDisconnectAt.put(player.getUniqueId(), System.currentTimeMillis());
            // Persist runtime disconnect time for Task mode
            if (plugin.getCurrentMode() == com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.TASK) {
                setRuntimeDisconnectTime(player.getUniqueId(), System.currentTimeMillis());
            }
        }

        boolean pauseOnDc = plugin.getCurrentMode() == com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.TASK
                ? plugin.getConfig().getBoolean("task_manager.pause_on_disconnect", plugin.getConfigManager().isPauseOnDisconnect())
                : plugin.getConfigManager().isPauseOnDisconnect();

        if (player.equals(activeRunner)) {
            if (pauseOnDc) {
                pauseGame();
            } else {
                performSwap();
            }
        }
        
        savePlayerState(player);
    }

    /** Handle player rejoin */
    public void handlePlayerJoin(Player player) {
        if (!gameRunning) return;
        if (plugin.getCurrentMode() == com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.TASK) {
            // If they had an assignment, ensure they're in runners
            var tmm = plugin.getTaskManagerMode();
            if (tmm != null && tmm.getAssignedTask(player) != null) {
                if (!isRunner(player)) {
                    // Add back into runners at end of queue
                    runners.add(player);
                }
                // Remind their task
                var def = tmm.getTask(tmm.getAssignedTask(player));
                if (def != null) {
                    player.sendMessage(Component.text("[Task Manager] Your task:").color(NamedTextColor.GOLD));
                    player.sendMessage(Component.text(" → " + def.description()).color(NamedTextColor.YELLOW));
                }
            } else if (plugin.getConfig().getBoolean("task_manager.allow_late_joiners", false)) {
                // Late joiner allowed: add as runner and optionally assign a task if pool available
                if (!isRunner(player)) {
                    runners.add(player);
                }
                if (tmm != null && tmm.getAssignedTask(player) == null) {
                    tmm.assignAndAnnounceTasks(java.util.List.of(player));
                }
            }

            // Clear disconnect record
            runnerDisconnectAt.remove(player.getUniqueId());
            clearRuntimeDisconnectTime(player.getUniqueId());

            // If paused and we have at least one online runner, resume automatically
            if (isGamePaused()) {
                boolean anyOnline = false;
                for (Player r : runners) if (r.isOnline()) { anyOnline = true; break; }
                if (anyOnline) {
                    resumeGame();
                }
            }
        }
    }

    private void startRunnerTimeoutWatcher() {
        if (runnerTimeoutTask != null) runnerTimeoutTask.cancel();
        runnerTimeoutTask = new BukkitRunnable() {
            @Override public void run() {
                if (!gameRunning) return;
                long now = System.currentTimeMillis();
                int graceSec = plugin.getConfig().getInt("task_manager.rejoin_grace_seconds", 180);
                boolean remove = plugin.getConfig().getBoolean("task_manager.remove_on_timeout", true);
                boolean pauseOnDc = plugin.getConfig().getBoolean("task_manager.pause_on_disconnect", true);

                java.util.List<UUID> toRemove = new java.util.ArrayList<>();
                for (var e : runnerDisconnectAt.entrySet()) {
                    long elapsed = now - e.getValue();
                    if (elapsed >= graceSec * 1000L) {
                        UUID uuid = e.getKey();
                        Player p = Bukkit.getPlayer(uuid);
                        if (remove) {
                            // Remove from runners
                            runners.removeIf(r -> r.getUniqueId().equals(uuid));
                            toRemove.add(uuid);
                        }
                        // If active runner, move on
                        if (activeRunner != null && activeRunner.getUniqueId().equals(uuid)) {
                            performSwap();
                        }
                    }
                }
                for (UUID id : toRemove) {
                    runnerDisconnectAt.remove(id);
                    clearRuntimeDisconnectTime(id);
                }

                // If paused due to disconnect and we still have online runners, resume
                if (gamePaused && pauseOnDc) {
                    boolean anyOnline = false;
                    for (Player r : runners) if (r.isOnline()) { anyOnline = true; break; }
                    if (anyOnline) resumeGame();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L * 5);
    }

    private void setRuntimeDisconnectTime(UUID uuid, long when) {
        try {
            String path = "task_manager.runtime.disconnect_times." + uuid;
            plugin.getConfig().set(path, when);
            plugin.saveConfig();
        } catch (Throwable ignored) {}
    }
    private void clearRuntimeDisconnectTime(UUID uuid) {
        try {
            String path = "task_manager.runtime.disconnect_times." + uuid;
            plugin.getConfig().set(path, null);
            plugin.saveConfig();
        } catch (Throwable ignored) {}
    }

    /**
     * Get time until next swap in seconds
     */
    public int getTimeUntilNextSwap() {
        return (int) ((nextSwapTime - System.currentTimeMillis()) / 1000);
    }
    
    private void saveAllPlayerStates() {
        for (Player runner : runners) {
            savePlayerState(runner);
        }
    }
    
    private void savePlayerState(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        
        PlayerState state = PlayerStateUtil.capturePlayerState(player);
        playerStates.put(player.getUniqueId(), state);
    }
    
    private void restoreAllPlayerStates() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (playerStates.containsKey(player.getUniqueId())) {
                restorePlayerState(player);
            }
            
            // Remove effects using compat lookups for cross-version support
            PotionEffectType eff;
            if ((eff = BukkitCompat.resolvePotionEffect("blindness")) != null) player.removePotionEffect(eff);
            if ((eff = BukkitCompat.resolvePotionEffect("darkness")) != null) player.removePotionEffect(eff);
            if ((eff = BukkitCompat.resolvePotionEffect("weakness")) != null) player.removePotionEffect(eff);
            if ((eff = BukkitCompat.resolvePotionEffect("slow_falling")) != null) player.removePotionEffect(eff);
            if ((eff = BukkitCompat.resolvePotionEffect("slowness")) != null) player.removePotionEffect(eff);
            if ((eff = BukkitCompat.resolvePotionEffect("jump_boost")) != null) player.removePotionEffect(eff);
            
            if (player.getGameMode() == GameMode.SPECTATOR && runners.contains(player)) {
                player.setGameMode(GameMode.SURVIVAL);
            }
            // Ensure everyone can see everyone again after cleanup
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                try { viewer.showPlayer(plugin, player); } catch (Throwable ignored) {}
            }
        }
    }
    
    private void restorePlayerState(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        
        PlayerState state = playerStates.get(player.getUniqueId());
        if (state != null) {
            try {
                player.getInventory().clear();
                for (PotionEffect effect : player.getActivePotionEffects()) {
                    player.removePotionEffect(effect.getType());
                }
                // Reset to server-defined max health using version-safe attribute access
                player.setHealth(BukkitCompat.getMaxHealthValue(player));
                player.setFoodLevel(20);
                player.setFireTicks(0);
                player.setFallDistance(0);
                player.setInvulnerable(false);
                
                PlayerStateUtil.applyPlayerState(player, state);
                
                Location loc = state.getLocation();
                if (loc != null && !loc.getBlock().getType().isSolid()) {
                    player.teleport(loc);
                } else {
                    player.teleport(plugin.getConfigManager().getSpawnLocation());
                    player.setGameMode(GameMode.SURVIVAL);
                    player.getInventory().clear();
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to restore state for player " + player.getName() + ": " + e.getMessage());
                player.teleport(plugin.getConfigManager().getSpawnLocation());
                player.setGameMode(GameMode.SURVIVAL);
                player.getInventory().clear();
            }
        }
    }
    
    private void loadTeams() {
        runners.clear();
        hunters.clear();
        
        for (String name : plugin.getConfigManager().getRunnerNames()) {
            Player player = Bukkit.getPlayerExact(name);
            if (player != null && player.isOnline()) {
                runners.add(player);
            }
        }
        
        for (String name : plugin.getConfigManager().getHunterNames()) {
            Player player = Bukkit.getPlayerExact(name);
            if (player != null && player.isOnline()) {
                hunters.add(player);
            }
        }
    }
    
    private void scheduleNextSwap() {
        if (swapTask != null) {
            swapTask.cancel();
        }
        
        long intervalSeconds;
        if (plugin.getConfigManager().isSwapRandomized()) {
            double mean = plugin.getConfigManager().getSwapInterval();
            double stdDev = plugin.getConfigManager().getJitterStdDev();
            double jitteredInterval = ThreadLocalRandom.current().nextGaussian() * stdDev + mean;
            
            if (plugin.getConfigManager().isClampJitter()) {
                int min = plugin.getConfigManager().getMinSwapInterval();
                int max = plugin.getConfigManager().getMaxSwapInterval();
                jitteredInterval = Math.max(min, Math.min(max, jitteredInterval));
            }
            
            intervalSeconds = Math.round(jitteredInterval);
        } else {
            intervalSeconds = plugin.getConfigManager().getSwapInterval();
        }
        
        long intervalTicks = intervalSeconds * 20;
        nextSwapTime = System.currentTimeMillis() + (intervalSeconds * 1000);
        swapTask = Bukkit.getScheduler().runTaskLater(plugin, this::performSwap, intervalTicks);
    }
    
    private void scheduleNextHunterSwap() {
        if (hunterSwapTask != null) {
            hunterSwapTask.cancel();
        }

        if (!plugin.getConfigManager().isHunterSwapEnabled()) {
            return;
        }

        long intervalTicks = plugin.getConfigManager().getHunterSwapInterval() * 20L;
        hunterSwapTask = Bukkit.getScheduler().runTaskLater(plugin, this::performHunterSwap, intervalTicks);
    }
    
    private void startActionBarUpdates() {
        if (actionBarTask != null) {
            actionBarTask.cancel();
        }
    
        int period = Math.max(1, plugin.getConfigManager().getActionBarUpdateTicks());
        actionBarTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!gameRunning) {
                return;
            }
            updateActionBar();
        }, 0L, period);
    }

    private void startTitleUpdates() {
        if (titleTask != null) titleTask.cancel();
        int period = Math.max(1, plugin.getConfigManager().getTitleUpdateTicks());
        titleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!gameRunning || gamePaused) return;
            updateTitles();
        }, 0L, period);
    }
    
    private void updateActionBar() {
        if (!gameRunning || gamePaused) {
            return;
        }

        int timeLeft = getTimeUntilNextSwap();
        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean isRunner = runners.contains(player);
            boolean isHunter = hunters.contains(player);
            boolean isActive = player.equals(activeRunner);
            boolean isCaged = cagedPlayers.contains(player.getUniqueId());

            String vis;
            if (isRunner) {
                vis = isActive ? plugin.getConfigManager().getRunnerTimerVisibility()
                               : plugin.getConfigManager().getWaitingTimerVisibility();
            } else if (isHunter) {
                vis = plugin.getConfigManager().getHunterTimerVisibility();
            } else {
                vis = "never";
            }

            boolean show;
            switch (String.valueOf(vis).toLowerCase()) {
                case "always" -> show = true;
                case "last_10" -> show = timeLeft <= 10;
                default -> show = false;
            }

            if (show) {
                // For caged players, show queue position instead of timer
                if (isCaged && !isActive) {
                    int queuePosition = getQueuePosition(player);
                    String msg = String.format("§6Queued (%d) - You're up next", queuePosition);
                    com.example.speedrunnerswap.utils.ActionBarUtil.sendActionBar(player, msg);
                } else {
                    String msg = String.format("§eSwap in: §c%ds", Math.max(0, timeLeft));
                    com.example.speedrunnerswap.utils.ActionBarUtil.sendActionBar(player, msg);
                }
            } else {
                com.example.speedrunnerswap.utils.ActionBarUtil.sendActionBar(player, "");
            }
        }
    }
    
    private int getQueuePosition(Player player) {
        if (!runners.contains(player)) return 0;
        
        int position = 1;
        for (Player runner : runners) {
            if (runner.equals(player)) break;
            // Only count runners that are online and not the current active runner
            if (runner.isOnline() && !runner.equals(activeRunner)) {
                position++;
            }
        }
        return position;
    }
    
    /**
     * Update hostile mob targeting when players swap
     * This ensures mobs that were targeting the previous runner now target the new runner
     */
    private void updateHostileMobTargeting(Player previousRunner, Player newRunner) {
        if (previousRunner == null || newRunner == null || !previousRunner.isOnline() || !newRunner.isOnline()) {
            return;
        }
        
        // Get all worlds to check for hostile mobs
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                // Check for hostile mobs that can target players
                if (entity instanceof org.bukkit.entity.Mob) {
                    org.bukkit.entity.Mob mob = (org.bukkit.entity.Mob) entity;
                    
                    // Check if this mob was targeting the previous runner
                    if (mob.getTarget() != null && mob.getTarget().equals(previousRunner)) {
                        // Transfer the target to the new runner
                        mob.setTarget(newRunner);
                    }
                }
                
                // Special handling for Endermen (they have different targeting mechanics)
                if (entity instanceof org.bukkit.entity.Enderman) {
                    org.bukkit.entity.Enderman enderman = (org.bukkit.entity.Enderman) entity;
                    
                    // Endermen don't have a direct getTarget() method, but we can check if they're angry
                    // and if the previous runner is nearby, make them angry at the new runner
                    if (enderman.getLocation().distance(previousRunner.getLocation()) < 16) {
                        // Make the enderman angry at the new runner
                        enderman.setTarget(newRunner);
                    }
                }
            }
        }
    }
    
    private void applyInactiveEffects() {
        String freezeMode = plugin.getConfigManager().getFreezeMode();
        
        for (Player runner : runners) {
            if (runner.equals(activeRunner)) {
                // Remove player from shared cage set
                cagedPlayers.remove(runner.getUniqueId());
                PotionEffectType eff;
                if ((eff = BukkitCompat.resolvePotionEffect("blindness")) != null) runner.removePotionEffect(eff);
                if ((eff = BukkitCompat.resolvePotionEffect("darkness")) != null) runner.removePotionEffect(eff);
                if ((eff = BukkitCompat.resolvePotionEffect("slowness")) != null) runner.removePotionEffect(eff);
                if ((eff = BukkitCompat.resolvePotionEffect("slow_falling")) != null) runner.removePotionEffect(eff);
                if ((eff = BukkitCompat.resolvePotionEffect("invisibility")) != null) runner.removePotionEffect(eff);
                runner.setGameMode(GameMode.SURVIVAL);
                // Ensure flight is disabled for the active runner to avoid anti-cheat confusion
                try { runner.setAllowFlight(false); } catch (Exception ignored) {}
                try { runner.setFlying(false); } catch (Exception ignored) {}
                
                for (Player viewer : Bukkit.getOnlinePlayers()) {
                    viewer.showPlayer(plugin, runner);
                }
            } else {
                if (freezeMode.equalsIgnoreCase("EFFECTS")) {
                    PotionEffectType blindness = BukkitCompat.resolvePotionEffect("blindness");
                    if (blindness != null) runner.addPotionEffect(new PotionEffect(blindness, Integer.MAX_VALUE, 1, false, false));
                    PotionEffectType darkness = BukkitCompat.resolvePotionEffect("darkness");
                    if (darkness != null) runner.addPotionEffect(new PotionEffect(darkness, Integer.MAX_VALUE, 1, false, false));
                    PotionEffectType slowness = BukkitCompat.resolvePotionEffect("slowness");
                    if (slowness != null) runner.addPotionEffect(new PotionEffect(slowness, Integer.MAX_VALUE, 255, false, false));
                    PotionEffectType slowFalling = BukkitCompat.resolvePotionEffect("slow_falling");
                    if (slowFalling != null) runner.addPotionEffect(new PotionEffect(slowFalling, Integer.MAX_VALUE, 128, false, false));
                } else if (freezeMode.equalsIgnoreCase("SPECTATOR")) {
                    runner.setGameMode(GameMode.SPECTATOR);
                } else if (freezeMode.equalsIgnoreCase("LIMBO")) {
                    Location limboLocation = plugin.getConfigManager().getLimboLocation();
                    // Try to find a safe nearby spot instead of blindly teleporting
                    Location safe = SafeLocationFinder.findSafeLocation(
                            limboLocation,
                            plugin.getConfigManager().getSafeSwapHorizontalRadius(),
                            plugin.getConfigManager().getSafeSwapVerticalDistance(),
                            plugin.getConfigManager().getDangerousBlocks());
                    runner.teleport(safe != null ? safe : limboLocation);
                    runner.setGameMode(GameMode.ADVENTURE);
                    PotionEffectType blindness2 = BukkitCompat.resolvePotionEffect("blindness");
                    if (blindness2 != null) runner.addPotionEffect(new PotionEffect(blindness2, Integer.MAX_VALUE, 1, false, false));
                } else if (freezeMode.equalsIgnoreCase("CAGE")) {
                    // Teleport to shared bedrock cage and blind + invis
                    createOrEnsureSharedCage(runner.getWorld());
                    teleportToSharedCage(runner);
                    PotionEffectType blindness = BukkitCompat.resolvePotionEffect("blindness");
                    if (blindness != null) runner.addPotionEffect(new PotionEffect(blindness, Integer.MAX_VALUE, 1, false, false));
                    PotionEffectType invis = BukkitCompat.resolvePotionEffect("invisibility");
                    if (invis != null) runner.addPotionEffect(new PotionEffect(invis, Integer.MAX_VALUE, 1, false, false));
                    runner.setGameMode(GameMode.ADVENTURE);
                    // Allow flight while caged to prevent server kicking for "flying"
                    try { runner.setAllowFlight(true); } catch (Exception ignored) {}
                    try { runner.setFlying(false); } catch (Exception ignored) {}
                }

                // Ensure inactive runners have no visible/usable inventory
                // until they are swapped in. Their original inventories are
                // preserved via saveAllPlayerStates() and restored on endGame.
                try {
                    runner.getInventory().clear();
                    runner.getInventory().setArmorContents(new ItemStack[]{});
                    runner.getInventory().setItemInOffHand(null);
                    runner.updateInventory();
                } catch (Exception ignored) {}
                
                for (Player viewer : Bukkit.getOnlinePlayers()) {
                    if (!viewer.equals(runner)) {
                        viewer.hidePlayer(plugin, runner);
                    }
                }
            }
        }
    }
    
    private void startFreezeChecking() {
        int interval = plugin.getConfigManager().getFreezeCheckIntervalTicks();
        freezeCheckTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!gameRunning || gamePaused || activeRunner == null) {
                return;
            }

            int maxDistance = (int) plugin.getConfigManager().getFreezeMaxDistance();
            Entity target = com.example.speedrunnerswap.utils.BukkitCompat.getTargetEntity(activeRunner, maxDistance);

            if (target instanceof Player hunter && isHunter(hunter)) {
                int duration = plugin.getConfigManager().getFreezeDurationTicks();
                PotionEffectType slowness2 = BukkitCompat.resolvePotionEffect("slowness");
                if (slowness2 != null)
                    hunter.addPotionEffect(new PotionEffect(slowness2, duration, 255, false, false));
                PotionEffectType jumpBoost = BukkitCompat.resolvePotionEffect("jump_boost");
                if (jumpBoost != null)
                    hunter.addPotionEffect(new PotionEffect(jumpBoost, duration, 128, false, false));
                
                if (plugin.getConfigManager().isBroadcastGameEvents()) {
                    hunter.sendMessage("§cYou have been frozen by the runner!");
                }
            }
        }, 0L, interval);
    }

    /**
     * Refresh freeze mechanic task and reapply inactive effects/modes at runtime.
     * Safe to call while game is running.
     */
    public void refreshFreezeMechanic() {
        if (freezeCheckTask != null) {
            freezeCheckTask.cancel();
            freezeCheckTask = null;
        }
        if (gameRunning && plugin.getConfigManager().isFreezeMechanicEnabled()) {
            startFreezeChecking();
        }
        if (gameRunning) {
            applyInactiveEffects();
            // If CAGE mode is not selected anymore, ensure cages are removed
            if (!"CAGE".equalsIgnoreCase(plugin.getConfigManager().getFreezeMode())) {
                cleanupAllCages();
            }
        }
    }

    /** Public hook for GUI/actions to re-apply active/inactive effects and cages */
    public void reapplyStates() {
        refreshFreezeMechanic();
    }
    
    private void performSwap() {
        if (!gameRunning || gamePaused || runners.isEmpty()) {
            return;
        }

        // Save dragon health before swap
        plugin.getDragonManager().onSwapStart();

        // Persist the current active runner's state before swapping
        if (activeRunner != null && activeRunner.isOnline()) {
            savePlayerState(activeRunner);
        }

        // Advance to next online runner
        int attempts = 0;
        do {
            activeRunnerIndex = (activeRunnerIndex + 1) % runners.size();
            attempts++;
            if (attempts >= runners.size()) {
                plugin.getLogger().warning("No online runners found during swap - pausing game");
                pauseGame();
                return;
            }
        } while (!runners.get(activeRunnerIndex).isOnline());

        Player nextRunner = runners.get(activeRunnerIndex);
        Player previousRunner = activeRunner;

        // Handle single-runner scenario gracefully: just refresh timers/powerups
        if (previousRunner != null && previousRunner.equals(nextRunner)) {
            // Keep the same active runner; just reschedule next swap and apply optional power-up
            scheduleNextSwap();
            if (plugin.getConfigManager().isPowerUpsEnabled()) {
                applyRandomPowerUp(nextRunner);
            }
            return;
        }

        activeRunner = nextRunner;

        // Grace period for the new active runner
        int gracePeriodTicks = plugin.getConfigManager().getGracePeriodTicks();
        if (gracePeriodTicks > 0) {
            nextRunner.setInvulnerable(true);
            final Player finalNextRunner = nextRunner;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (finalNextRunner.isOnline()) {
                    finalNextRunner.setInvulnerable(false);
                }
            }, gracePeriodTicks);
        }

        if (previousRunner != null && previousRunner.isOnline()) {
            // Capture full state (includes potion effects, XP, etc.) from the previous runner
            com.example.speedrunnerswap.models.PlayerState prevState = PlayerStateUtil.capturePlayerState(previousRunner);

            // Apply to the next runner
            PlayerStateUtil.applyPlayerState(nextRunner, prevState);
            try { nextRunner.updateInventory(); } catch (Throwable ignored) {}

            // Teleport adjustment for safe swap near the previous runner's location
            if (plugin.getConfigManager().isSafeSwapEnabled()) {
                Location swapLocation = previousRunner.getLocation();
                Location safeLocation = SafeLocationFinder.findSafeLocation(
                        swapLocation,
                        plugin.getConfigManager().getSafeSwapHorizontalRadius(),
                        plugin.getConfigManager().getSafeSwapVerticalDistance(),
                        plugin.getConfigManager().getDangerousBlocks());
                if (safeLocation != null) {
                    nextRunner.teleport(safeLocation);
                }
            }

            // Remove all active potion effects from the previous runner
            for (PotionEffect effect : previousRunner.getActivePotionEffects()) {
                previousRunner.removePotionEffect(effect.getType());
            }

            // Clear previous runner's inventory to prevent duplication exploits
            previousRunner.getInventory().clear();
            previousRunner.getInventory().setArmorContents(new ItemStack[]{});
            previousRunner.getInventory().setItemInOffHand(null);
            previousRunner.updateInventory();
        } else {
            // First-time activation (no previous runner). If kits enabled, give runner kit.
            if (plugin.getConfigManager().isKitsEnabled()) {
                nextRunner.getInventory().clear();
                plugin.getKitManager().giveKit(nextRunner, "runner");
            }
        }

        applyInactiveEffects();
        scheduleNextSwap();

        // Restore dragon health after swap
        plugin.getDragonManager().onSwapComplete();
        
        // Update hostile mob targeting
        updateHostileMobTargeting(previousRunner, nextRunner);

        // Suppress public chat broadcast on swap per request

        if (plugin.getConfigManager().isPowerUpsEnabled()) {
            applyRandomPowerUp(nextRunner);
        }

        if (plugin.getConfigManager().isCompassJammingEnabled()) {
            long duration = plugin.getConfigManager().getCompassJamDuration();
            if (duration > 0) {
                plugin.getTrackerManager().jamCompasses(duration);
            }
        }
    }

    /** Trigger an immediate runner swap (admin action) */
    public void triggerImmediateSwap() {
        if (!gameRunning || gamePaused) return;
        Bukkit.getScheduler().runTask(plugin, this::performSwap);
    }

    /** Trigger an immediate hunter shuffle (admin action) */
    public void triggerImmediateHunterSwap() {
        if (!gameRunning || gamePaused) return;
        Bukkit.getScheduler().runTask(plugin, this::performHunterSwap);
    }
    
    private void performHunterSwap() {
        if (!gameRunning || gamePaused || hunters.size() < 2) {
            return;
        }

        Collections.shuffle(hunters);
        plugin.getTrackerManager().updateAllHunterCompasses();

        if (plugin.getConfigManager().isBroadcastsEnabled()) {
            Msg.broadcast("§c[SpeedrunnerSwap] Hunters have been swapped!");
        }
    }
    
    private void applyRandomPowerUp(Player player) {
        java.util.List<String> good = plugin.getConfigManager().getGoodPowerUps();
        java.util.List<String> bad = plugin.getConfigManager().getBadPowerUps();

        java.util.List<PotionEffectType> goodTypes = new java.util.ArrayList<>();
        java.util.List<PotionEffectType> badTypes = new java.util.ArrayList<>();

        for (String id : good) {
            PotionEffectType t = resolveEffect(id);
            if (t != null) goodTypes.add(t);
        }
        for (String id : bad) {
            PotionEffectType t = resolveEffect(id);
            if (t != null) badTypes.add(t);
        }

        // Fallbacks if config lists are empty or invalid
        if (goodTypes.isEmpty()) {
            String[] defaults = {"speed", "regeneration", "resistance", "night_vision", "dolphins_grace"};
            for (String k : defaults) {
                PotionEffectType t = BukkitCompat.resolvePotionEffect(k);
                if (t != null) goodTypes.add(t);
            }
            if (goodTypes.isEmpty()) {
                // Ultra-safe baseline
                PotionEffectType t = BukkitCompat.resolvePotionEffect("speed");
                if (t != null) goodTypes.add(t);
            }
        }
        if (badTypes.isEmpty()) {
            String[] defaults = {"slowness", "weakness", "hunger", "darkness", "glowing"};
            for (String k : defaults) {
                PotionEffectType t = BukkitCompat.resolvePotionEffect(k);
                if (t != null) badTypes.add(t);
            }
            if (badTypes.isEmpty()) {
                PotionEffectType t = BukkitCompat.resolvePotionEffect("slowness");
                if (t != null) badTypes.add(t);
            }
        }

        boolean isGoodEffect = ThreadLocalRandom.current().nextBoolean();
        java.util.List<PotionEffectType> effectPool = isGoodEffect ? goodTypes : badTypes;
        PotionEffectType effectType = effectPool.get(ThreadLocalRandom.current().nextInt(effectPool.size()));
        
        int duration = ThreadLocalRandom.current().nextInt(10, 21) * 20;
        int amplifier = ThreadLocalRandom.current().nextInt(2);
        
        player.addPotionEffect(new PotionEffect(effectType, duration, amplifier));
        
        String effectName = effectType.getKey().getKey().replace("_", " ").toLowerCase();
        String effectLevel = amplifier == 0 ? "I" : "II";
        
        player.sendMessage(String.format("§%sYou received a %s power-up: %s %s!",
                isGoodEffect ? "a" : "c",
                isGoodEffect ? "good" : "bad",
                effectName,
                effectLevel));
    }

    private PotionEffectType resolveEffect(String id) {
        return BukkitCompat.resolvePotionEffect(id);
    }

    /**
     * Pause the game
     */
    public boolean pauseGame() {
        if (!gameRunning || gamePaused) {
            return false;
        }
        gamePaused = true;
        if (swapTask != null) {
            swapTask.cancel();
        }
        if (hunterSwapTask != null) {
            hunterSwapTask.cancel();
        }
        if (actionBarTask != null) {
            actionBarTask.cancel();
        }
        if (titleTask != null) {
            titleTask.cancel();
        }
        return true;
    }

    /**
     * Resume the game
     */
    public boolean resumeGame() {
        if (!gameRunning || !gamePaused) {
            return false;
        }
        gamePaused = false;
        scheduleNextSwap();
        scheduleNextHunterSwap();
        startActionBarUpdates();
        startTitleUpdates();
        startCageEnforcement();
        return true;
    }

    /** Returns whether the game is currently paused */
    public boolean isGamePaused() {
        return gamePaused;
    }

    /** Shuffle the runner queue while keeping the current active runner first */
    public boolean shuffleQueue() {
        if (runners == null || runners.size() < 2) return false;
        Player current = activeRunner;
        java.util.List<Player> rest = new java.util.ArrayList<>();
        for (Player p : runners) {
            if (!p.equals(current)) rest.add(p);
        }
        java.util.Collections.shuffle(rest, new java.util.Random());
        java.util.List<Player> newOrder = new java.util.ArrayList<>();
        newOrder.add(current);
        newOrder.addAll(rest);
        runners = newOrder;
        applyInactiveEffects();
        refreshActionBar();
        return true;
    }

    /** Replace runners list and update config team names */
    public void setRunners(java.util.List<Player> players) {
        java.util.List<String> names = new java.util.ArrayList<>();
        for (Player p : players) names.add(p.getName());
        // Clear and set in config atomically
        plugin.getConfigManager().setRunnerNames(names);
        // Also ensure no overlap: remove these names from hunters in config
        java.util.List<String> currentHunters = plugin.getConfigManager().getHunterNames();
        currentHunters.removeAll(names);
        plugin.getConfigManager().setHunterNames(currentHunters);
        // Update runtime list
        this.runners = new java.util.ArrayList<>(players);
    }

    /** Replace hunters list and update config team names */
    public void setHunters(java.util.List<Player> players) {
        java.util.List<String> names = new java.util.ArrayList<>();
        for (Player p : players) names.add(p.getName());
        plugin.getConfigManager().setHunterNames(names);
        // Ensure no overlap: remove from runners
        java.util.List<String> currentRunners = plugin.getConfigManager().getRunnerNames();
        currentRunners.removeAll(names);
        plugin.getConfigManager().setRunnerNames(currentRunners);
        this.hunters = new java.util.ArrayList<>(players);
    }

    private void updateTitles() {
        int timeLeft = getTimeUntilNextSwap();
        Player current = activeRunner;
        boolean isSneak = current != null && current.isSneaking();
        boolean isSprint = current != null && current.isSprinting();

        String waitingVis = plugin.getConfigManager().getWaitingTimerVisibility();
        boolean waitingAlways = "always".equalsIgnoreCase(waitingVis);
        boolean waitingLast10 = "last_10".equalsIgnoreCase(waitingVis);

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!runners.contains(p)) continue; // Only runners get titles

            boolean isActive = p.equals(current);
            boolean isCaged = cagedPlayers.contains(p.getUniqueId());
            
            // For caged players, show large aesthetic title
            if (isCaged && !isActive) {
                net.kyori.adventure.text.Component titleText = net.kyori.adventure.text.Component.text(
                        String.format("Swap in: %ds", Math.max(0, timeLeft)))
                        .color(NamedTextColor.GOLD)
                        .decorate(TextDecoration.BOLD);

                net.kyori.adventure.text.Component sub = net.kyori.adventure.text.Component.text(
                        String.format("Sneaking: %s  |  Running: %s", isSneak ? "Yes" : "No", isSprint ? "Yes" : "No"))
                        .color(NamedTextColor.YELLOW);

                Title title = Title.title(
                        titleText,
                        sub,
                        Title.Times.times(Duration.ZERO, Duration.ofMillis(1000), Duration.ZERO)
                );
                p.showTitle(title);
                continue;
            }
            
            // For non-caged waiting runners, show smaller title
            boolean shouldShow = !isActive && !isCaged && (waitingAlways || (waitingLast10 && timeLeft <= 10));
            if (!shouldShow) continue;

            net.kyori.adventure.text.Component titleText = net.kyori.adventure.text.Component.text(
                    String.format("Swap in: %ds", Math.max(0, timeLeft)))
                    .color(NamedTextColor.GOLD)
                    .decorate(TextDecoration.BOLD);

            net.kyori.adventure.text.Component sub = net.kyori.adventure.text.Component.text(
                    String.format("Sneaking: %s  |  Running: %s", isSneak ? "Yes" : "No", isSprint ? "Yes" : "No"))
                    .color(NamedTextColor.YELLOW);

            Title title = Title.title(
                    titleText,
                    sub,
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(600), Duration.ZERO)
            );
            p.showTitle(title);
        }
    }

    private void createOrEnsureSharedCage(World world) {
        if (world == null) world = Bukkit.getWorlds().get(0);
        Location base = plugin.getConfigManager().getLimboLocation();
        int y = world.getMaxHeight() - 10;
        int cx = (int) Math.round(base.getX());
        int cz = (int) Math.round(base.getZ());
        Location center = new Location(world, cx + 0.5, y, cz + 0.5);
        Location existing = sharedCageCenters.get(world);
        if (existing != null && Math.abs(existing.getX() - center.getX()) < 0.1 && Math.abs(existing.getY() - center.getY()) < 0.1 && Math.abs(existing.getZ() - center.getZ()) < 0.1) {
            return;
        }
        // Cleanup old cage in this world
        java.util.List<org.bukkit.block.BlockState> old = sharedCageBlocks.remove(world);
        if (old != null) {
            for (org.bukkit.block.BlockState s : old) { try { s.update(true, false); } catch (Exception ignored) {} }
        }
        try { center.getChunk().load(true); } catch (Throwable ignored) {}
        java.util.List<org.bukkit.block.BlockState> changed = new java.util.ArrayList<>();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    org.bukkit.block.Block block = world.getBlockAt(cx + dx, y + dy, cz + dz);
                    boolean isShell = (dx == -2 || dx == 2 || dz == -2 || dz == 2 || dy == -1 || dy == 2);
                    changed.add(block.getState());
                    block.setType(isShell ? Material.BEDROCK : Material.AIR, false);
                }
            }
        }
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                org.bukkit.block.Block floor = world.getBlockAt(cx + dx, y - 1, cz + dz);
                changed.add(floor.getState());
                floor.setType(Material.BEDROCK, false);
            }
        }
        sharedCageBlocks.put(world, changed);
        sharedCageCenters.put(world, center.clone());
    }

    private void teleportToSharedCage(Player p) {
        if (p == null || !p.isOnline()) return;
        createOrEnsureSharedCage(p.getWorld());
        org.bukkit.Location center = sharedCageCenters.get(p.getWorld());
        if (center != null) {
            // Teleport player to the center of the cage floor
            p.teleport(center);
            cagedPlayers.add(p.getUniqueId());
            try { p.setAllowFlight(true); } catch (Exception ignored) {}
            try { p.setFlying(false); } catch (Exception ignored) {}
        }
    }

    private void cleanupAllCages() {
        for (java.util.Map.Entry<org.bukkit.World, java.util.List<org.bukkit.block.BlockState>> e : sharedCageBlocks.entrySet()) {
            java.util.List<org.bukkit.block.BlockState> list = e.getValue();
            if (list != null) for (org.bukkit.block.BlockState s : list) { try { s.update(true, false); } catch (Exception ignored) {} }
        }
        sharedCageBlocks.clear();
        sharedCageCenters.clear();
        cagedPlayers.clear();
    }

    private void startCageEnforcement() {
        if (cageTask != null) { cageTask.cancel(); cageTask = null; }
        cageTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!gameRunning || gamePaused) return;
            if (!"CAGE".equalsIgnoreCase(plugin.getConfigManager().getFreezeMode())) return;
            // Ensure a cage exists in each runner's current world and enforce
            for (Player r : runners) {
                if (r.equals(activeRunner)) continue;
                if (!r.isOnline()) continue;
                createOrEnsureSharedCage(r.getWorld());
                teleportToSharedCage(r);
                org.bukkit.Location center = sharedCageCenters.get(r.getWorld());
                if (center == null) continue;
                org.bukkit.Location loc = r.getLocation();
                double dx = Math.abs(loc.getX() - center.getX());
                double dy = loc.getY() - center.getY();
                double dz = Math.abs(loc.getZ() - center.getZ());
                boolean outside = dx > 1.2 || dz > 1.2 || dy < -0.2 || dy > 0.8;
                if (outside) {
                    r.teleport(center);
                    r.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
                    r.setFallDistance(0f);
                    r.setNoDamageTicks(Math.max(10, r.getNoDamageTicks()));
                } else {
                    try { r.setAllowFlight(true); } catch (Exception ignored) {}
                    try { r.setFlying(false); } catch (Exception ignored) {}
                }
            }
        }, 0L, 5L); // enforce every 0.25s
    }

    public boolean areBothPlayersInSharedCage(Player a, Player b) {
        if (a == null || b == null) return false;
        if (!cagedPlayers.contains(a.getUniqueId()) || !cagedPlayers.contains(b.getUniqueId())) return false;
        return a.getWorld().equals(b.getWorld()) && sharedCageCenters.containsKey(a.getWorld());
    }
}
