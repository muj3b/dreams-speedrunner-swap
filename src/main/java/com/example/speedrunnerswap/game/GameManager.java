package com.example.speedrunnerswap.game;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import com.example.speedrunnerswap.models.PlayerState;
import com.example.speedrunnerswap.models.Team;
import com.example.speedrunnerswap.utils.PlayerStateUtil;
import com.example.speedrunnerswap.utils.SafeLocationFinder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.Server;
import com.example.speedrunnerswap.utils.BukkitCompat;
import org.bukkit.inventory.ItemStack;
import org.bukkit.GameMode;
import java.time.Duration;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
// Use compat resolver for cross-version effect lookups
import com.example.speedrunnerswap.utils.BukkitCompat;

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
    private long nextSwapTime;
    private final Map<UUID, PlayerState> playerStates;
    // Cage management for CAGE freeze mode
    private final Map<java.util.UUID, java.util.List<org.bukkit.block.BlockState>> builtCages = new java.util.HashMap<>();
    private final Map<java.util.UUID, org.bukkit.Location> cageCenters = new java.util.HashMap<>();
    
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
            Bukkit.broadcast(Component.text("§cGame cannot start: At least one runner and one hunter are required."), Server.BROADCAST_CHANNEL_USERS);
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
                scheduleNextHunterSwap();
                startActionBarUpdates();
                startTitleUpdates();
                startCageEnforcement();
                
                if (plugin.getConfigManager().isTrackerEnabled()) {
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
                    Bukkit.broadcast(net.kyori.adventure.text.Component.text("§a[SpeedrunnerSwap] Game ended! " + winnerMessage), Server.BROADCAST_CHANNEL_USERS);
                }

                broadcastDonationMessage();
            }
        }.runTaskLater(plugin, 200L);
    }

    private void broadcastDonationMessage() {
        // Add some spacing
        Bukkit.broadcast(Component.text("\n"), Server.BROADCAST_CHANNEL_USERS);
        
        // Header
        Bukkit.broadcast(Component.text("=== Support the Creator ===")
            .color(NamedTextColor.GOLD)
            .decorate(TextDecoration.BOLD), Server.BROADCAST_CHANNEL_USERS);
            
        // Message
        Bukkit.broadcast(Component.text("Enjoy the plugin? Consider supporting the creator (muj3b)!")
            .color(NamedTextColor.YELLOW), Server.BROADCAST_CHANNEL_USERS);
        
        // Clickable donation link
        Component donateMessage = Component.text("[Click here to donate]")
            .color(NamedTextColor.GREEN)
            .decorate(TextDecoration.BOLD)
            .clickEvent(net.kyori.adventure.text.event.ClickEvent.openUrl("https://donate.stripe.com/cNicN5gG3f8ocU4cjN0Ba00"))
            .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                Component.text("Click to support the creator!")
                    .color(NamedTextColor.YELLOW)
            ));
            
        Bukkit.broadcast(donateMessage, Server.BROADCAST_CHANNEL_USERS);
        
        // Add spacing after
        Bukkit.broadcast(Component.text("\n"), Server.BROADCAST_CHANNEL_USERS);
    }/** Stop the game without declaring a winner */
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
        return !runners.isEmpty() && !hunters.isEmpty();
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

        // If a team becomes empty due to disconnects, pause instead of ending the game
        if (gameRunning && (runners.isEmpty() || hunters.isEmpty())) {
            if (plugin.getConfigManager().isPauseOnDisconnect()) {
                pauseGame();
                if (plugin.getConfigManager().isBroadcastGameEvents()) {
                    Bukkit.broadcast(net.kyori.adventure.text.Component.text(
                            "§e[SpeedrunnerSwap] Game paused: waiting for players to return."),
                            Server.BROADCAST_CHANNEL_USERS);
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

        if (player.equals(activeRunner)) {
            if (plugin.getConfigManager().isPauseOnDisconnect()) {
                pauseGame();
            } else {
                performSwap();
            }
        }
        
        savePlayerState(player);
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
    
        actionBarTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!gameRunning) {
                return;
            }
            updateActionBar();
        }, 0L, 20L);
    }

    private void startTitleUpdates() {
        if (titleTask != null) titleTask.cancel();
        // Update titles a bit faster for near-immediate status feedback
        titleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!gameRunning || gamePaused) return;
            updateTitles();
        }, 0L, 5L); // 0.25s for snappier updates
    }
    
    private void updateActionBar() {
        if (!gameRunning || gamePaused) {
            return;
        }

        int timeLeft = getTimeUntilNextSwap();
        for (Player player : Bukkit.getOnlinePlayers()) {
            String visibility;
            boolean isWaitingRunner = false;

            if (player.equals(activeRunner)) {
                visibility = plugin.getConfigManager().getRunnerTimerVisibility();
            } else if (runners.contains(player)) {
                visibility = plugin.getConfigManager().getWaitingTimerVisibility();
                isWaitingRunner = true;
            } else {
                visibility = plugin.getConfigManager().getHunterTimerVisibility();
            }

            boolean showTimer = switch (visibility) {
                case "always" -> true;
                case "last_10" -> timeLeft <= 10;
                case "never" -> false;
                default -> false;
            };

            if (!showTimer) {
                com.example.speedrunnerswap.utils.ActionBarUtil.sendActionBar(player, "");
                continue;
            }

            String message;
            if (isWaitingRunner && activeRunner != null) {
                String dim = switch (activeRunner.getWorld().getEnvironment()) {
                    case NETHER -> "Nether";
                    case THE_END -> "End";
                    default -> "Overworld";
                };
                message = String.format("§eActive: §b%s §7in §d%s §7| §eSwap in: §c%ds",
                        activeRunner.getName(), dim, timeLeft);
            } else {
                message = String.format("§eTime until next swap: §c%ds", timeLeft);
            }

            com.example.speedrunnerswap.utils.ActionBarUtil.sendActionBar(player, message);
        }
    }
    
    private void applyInactiveEffects() {
        String freezeMode = plugin.getConfigManager().getFreezeMode();
        
        for (Player runner : runners) {
            if (runner.equals(activeRunner)) {
                // Remove cage if previously created
                removeCageFor(runner);
                PotionEffectType eff;
                if ((eff = BukkitCompat.resolvePotionEffect("blindness")) != null) runner.removePotionEffect(eff);
                if ((eff = BukkitCompat.resolvePotionEffect("darkness")) != null) runner.removePotionEffect(eff);
                if ((eff = BukkitCompat.resolvePotionEffect("slowness")) != null) runner.removePotionEffect(eff);
                if ((eff = BukkitCompat.resolvePotionEffect("slow_falling")) != null) runner.removePotionEffect(eff);
                runner.setGameMode(GameMode.SURVIVAL);
                
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
                    // Teleport to a high-altitude bedrock cage and blind
                    createCageFor(runner);
                    PotionEffectType blindness = BukkitCompat.resolvePotionEffect("blindness");
                    if (blindness != null) runner.addPotionEffect(new PotionEffect(blindness, Integer.MAX_VALUE, 1, false, false));
                    runner.setGameMode(GameMode.ADVENTURE);
                }
                
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
                    hunter.sendMessage(net.kyori.adventure.text.Component.text("§cYou have been frozen by the runner!"));
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
    
    private void performSwap() {
        if (!gameRunning || gamePaused || runners.isEmpty()) {
            return;
        }

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

        if (plugin.getConfigManager().isBroadcastsEnabled() && previousRunner != null) {
            Bukkit.broadcast(net.kyori.adventure.text.Component.text(
                    "§6[SpeedrunnerSwap] Swapped from " + previousRunner.getName() + " to " + nextRunner.getName() + "!"),
                    Server.BROADCAST_CHANNEL_USERS);
        }

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
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("§c[SpeedrunnerSwap] Hunters have been swapped!"), Server.BROADCAST_CHANNEL_USERS);
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
        
        player.sendMessage(net.kyori.adventure.text.Component.text(
            String.format("§%sYou received a %s power-up: %s %s!", 
                isGoodEffect ? "a" : "c",
                isGoodEffect ? "good" : "bad",
                effectName,
                effectLevel)));
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

        net.kyori.adventure.text.Component sub = net.kyori.adventure.text.Component.text(
                String.format("Sneaking: %s  |  Running: %s", isSneak ? "Yes" : "No", isSprint ? "Yes" : "No"))
                .color(NamedTextColor.YELLOW);

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!runners.contains(p)) continue; // Only runners get titles

            boolean isActive = p.equals(current);
            boolean shouldShow = !isActive || timeLeft <= 10; // waiting: always; active: last 10s only
            if (!shouldShow) continue;

            net.kyori.adventure.text.Component titleText = net.kyori.adventure.text.Component.text(
                    String.format("Swap in: %ds", Math.max(0, timeLeft)))
                    .color(isActive ? NamedTextColor.RED : NamedTextColor.GOLD)
                    .decorate(TextDecoration.BOLD);

            Title title = Title.title(
                    titleText,
                    sub,
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(600), Duration.ZERO)
            );
            p.showTitle(title);
        }
    }

    private void createCageFor(Player runner) {
        if (runner == null || !runner.isOnline()) return;
        if (builtCages.containsKey(runner.getUniqueId())) return;

        // Base world/location from limbo config world; center spaced by runner index
        Location base = plugin.getConfigManager().getLimboLocation();
        World world = base.getWorld() != null ? base.getWorld() : runner.getWorld();
        int y = world.getMaxHeight() - 10;
        int index = Math.max(0, runners.indexOf(runner));
        int spacing = 10;
        int cx = (int) Math.round(base.getX()) + index * spacing;
        int cz = (int) Math.round(base.getZ());
        Location center = new Location(world, cx + 0.5, y + 1, cz + 0.5);
        // Ensure chunk is loaded
        center.getChunk().load(true);

        java.util.List<org.bukkit.block.BlockState> changed = new java.util.ArrayList<>();
        // Build 5x5x5 cube of bedrock with 3x3x3 air cavity, plus extended floor to catch glitches
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy <= 3; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    org.bukkit.block.Block block = world.getBlockAt(cx + dx, y + dy, cz + dz);
                    boolean isShell = (dx == -2 || dx == 2 || dz == -2 || dz == 2 || dy == -1 || dy == 3);
                    if (isShell) {
                        changed.add(block.getState());
                        block.setType(Material.BEDROCK, false);
                    } else {
                        changed.add(block.getState());
                        block.setType(Material.AIR, false);
                    }
                }
            }
        }

        // Extended floor: 7x7 bedrock platform at y-1 to prevent falling off due to lag/glitch
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                org.bukkit.block.Block floor = world.getBlockAt(cx + dx, y - 1, cz + dz);
                changed.add(floor.getState());
                floor.setType(Material.BEDROCK, false);
            }
        }

        builtCages.put(runner.getUniqueId(), changed);
        cageCenters.put(runner.getUniqueId(), center.clone());
        // Teleport inside cage
        runner.teleport(center);
    }

    private void removeCageFor(Player runner) {
        if (runner == null) return;
        java.util.List<org.bukkit.block.BlockState> states = builtCages.remove(runner.getUniqueId());
        cageCenters.remove(runner.getUniqueId());
        if (states != null) {
            for (org.bukkit.block.BlockState s : states) {
                try { s.update(true, false); } catch (Exception ignored) {}
            }
        }
    }

    private void cleanupAllCages() {
        java.util.Set<java.util.UUID> ids = new java.util.HashSet<>(builtCages.keySet());
        for (java.util.UUID id : ids) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) removeCageFor(p);
        }
        builtCages.clear();
        cageCenters.clear();
    }

    private void startCageEnforcement() {
        if (cageTask != null) { cageTask.cancel(); cageTask = null; }
        cageTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!gameRunning || gamePaused) return;
            if (!"CAGE".equalsIgnoreCase(plugin.getConfigManager().getFreezeMode())) return;

            for (Player r : runners) {
                if (r.equals(activeRunner)) continue;
                if (!r.isOnline()) continue;
                // Ensure cage exists
                createCageFor(r);
                org.bukkit.Location center = cageCenters.get(r.getUniqueId());
                if (center == null) continue;

                org.bukkit.Location loc = r.getLocation();
                double dx = Math.abs(loc.getX() - center.getX());
                double dy = loc.getY() - center.getY();
                double dz = Math.abs(loc.getZ() - center.getZ());
                boolean outside = dx > 1.2 || dz > 1.2 || dy < -0.6 || dy > 2.6;
                if (outside) {
                    r.teleport(center);
                    r.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
                    r.setFallDistance(0f);
                    r.setNoDamageTicks(Math.max(10, r.getNoDamageTicks()));
                }
            }
        }, 0L, 5L); // enforce every 0.25s
    }
}
