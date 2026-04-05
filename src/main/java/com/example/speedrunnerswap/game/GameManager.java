package com.example.speedrunnerswap.game;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import com.example.speedrunnerswap.models.PlayerState;
import com.example.speedrunnerswap.models.Team;
import com.example.speedrunnerswap.utils.PlayerStateUtil;
import com.example.speedrunnerswap.utils.SafeLocationFinder;
import org.bukkit.Location;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.example.speedrunnerswap.utils.Msg;
import org.bukkit.GameMode;
import com.example.speedrunnerswap.utils.BukkitCompat;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class GameManager {
    private final SpeedrunnerSwap plugin;
    private boolean gameRunning;
    private boolean gamePaused;
    private Player activeRunner;
    private Player activeHunter;
    private int activeRunnerIndex;
    private int activeHunterIndex;
    private List<Player> runners;
    private List<Player> hunters;
    private BukkitTask swapTask;
    private BukkitTask hunterSwapTask;
    private BukkitTask actionBarTask;
    private BukkitTask titleTask;
    private BukkitTask freezeCheckTask;
    private BukkitTask cageTask;
    private BukkitTask runnerTimeoutTask;
    private BukkitTask taskMaxDurationTask;
    private long nextSwapTime;
    private long nextHunterSwapTime;
    private long taskGameDeadlineMs;
    private long taskGameRemainingMs;
    private final Map<UUID, PlayerState> playerStates;
    private final Set<UUID> restorableParticipantIds = new HashSet<>();
    private final Map<UUID, Long> runnerDisconnectAt = new HashMap<>();
    // Shared cage management per-world (one cage in each world)
    private final java.util.Map<org.bukkit.World, java.util.List<org.bukkit.block.BlockState>> sharedCageBlocks = new java.util.HashMap<>();
    private final java.util.Map<org.bukkit.World, org.bukkit.Location> sharedCageCenters = new java.util.HashMap<>();
    private final java.util.Set<java.util.UUID> cagedPlayers = new java.util.HashSet<>();
    private final java.util.Map<java.util.UUID, Integer> portalSwapRetries = new java.util.HashMap<>();
    private boolean swapInProgress = false;
    private boolean hunterSwapInProgress = false;
    private boolean pausedByDisconnect = false;
    private Location sharedRunnerSpawn;
    private String sessionWorldName;
    private boolean spawnSyncInFlight;

    private static final EnumSet<InventoryType> RETURN_CONTAINERS = EnumSet.of(
            InventoryType.CRAFTING,
            InventoryType.WORKBENCH,
            InventoryType.SMITHING,
            InventoryType.CARTOGRAPHY,
            InventoryType.GRINDSTONE,
            InventoryType.STONECUTTER,
            InventoryType.LOOM,
            InventoryType.ANVIL,
            InventoryType.ENCHANTING,
            InventoryType.MERCHANT,
            InventoryType.BEACON);

    private static final long TASK_INTRO_DELAY_TICKS = 8L * 20L;
    private static final int RESPAWN_SEARCH_RADIUS = 6;
    private static final int RESPAWN_VERTICAL_RANGE = 8;

    public GameManager(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
        this.gameRunning = false;
        this.gamePaused = false;
        this.activeRunnerIndex = 0;
        this.activeHunterIndex = 0;
        this.runners = new ArrayList<>();
        this.hunters = new ArrayList<>();
        this.playerStates = new HashMap<>();
    }

    private List<Player> getOnlineGameParticipants() {
        LinkedHashMap<UUID, Player> participants = new LinkedHashMap<>();
        for (Player runner : runners) {
            if (runner != null && runner.isOnline()) {
                participants.put(runner.getUniqueId(), runner);
            }
        }
        for (Player hunter : hunters) {
            if (hunter != null && hunter.isOnline()) {
                participants.put(hunter.getUniqueId(), hunter);
            }
        }
        return new ArrayList<>(participants.values());
    }

    private List<Player> getTaskCompetitionParticipants() {
        LinkedHashMap<UUID, Player> participants = new LinkedHashMap<>();
        for (Player runner : runners) {
            if (runner != null) {
                participants.put(runner.getUniqueId(), runner);
            }
        }
        if (plugin.isDualBodyTaskMode()) {
            for (Player hunter : hunters) {
                if (hunter != null) {
                    participants.put(hunter.getUniqueId(), hunter);
                }
            }
        }
        return new ArrayList<>(participants.values());
    }

    private boolean isTaskCompetitionParticipant(Player player) {
        if (player == null || !plugin.isTaskCompetitionMode()) {
            return false;
        }
        return isRunner(player) || (plugin.isDualBodyTaskMode() && isHunter(player));
    }

    public boolean isActiveRunner(Player player) {
        return player != null && activeRunner != null && player.getUniqueId().equals(activeRunner.getUniqueId());
    }

    public boolean isActiveHunter(Player player) {
        return player != null && activeHunter != null && player.getUniqueId().equals(activeHunter.getUniqueId());
    }

    // shuffleQueue() is implemented later in this class

    public boolean startGame() {
        if (gameRunning) {
            return false;
        }

        if (!canStartGame()) {
            SpeedrunnerSwap.SwapMode mode = plugin.getCurrentMode();
            boolean hasRunner = !runners.isEmpty();
            boolean hasHunter = !hunters.isEmpty();

            String failureMessage = switch (mode) {
                case DREAM -> hasRunner
                        ? "§cGame cannot start: Assign at least one hunter for Dream mode."
                        : "§cGame cannot start: Assign at least one speedrunner.";
                case SAPNAP -> hasRunner
                        ? (hasHunter
                                ? "§cGame cannot start: Sapnap mode does not allow hunters. Clear them and keep only speedrunners."
                                : "§cGame cannot start: Assign at least one speedrunner.")
                        : "§cGame cannot start: Assign at least one speedrunner.";
                case TASK -> hasRunner
                        ? (hasHunter
                                ? "§cGame cannot start: Task Master mode uses only speedrunners. Remove any hunters before starting."
                                : "§cGame cannot start: Assign at least one speedrunner.")
                        : "§cGame cannot start: Assign at least one speedrunner.";
                case TASK_DUEL -> hasRunner
                        ? (hasHunter
                                ? "§cGame cannot start: Assign at least one player to each shared body for Task Master Duo."
                                : "§cGame cannot start: Task Master Duo needs a second shared body. Assign at least one hunter slot player.")
                        : "§cGame cannot start: Task Master Duo needs at least one speedrunner.";
                case TASK_RACE -> hasRunner
                        ? (hasHunter
                                ? "§cGame cannot start: Task Race uses only speedrunners. Remove any hunters before starting."
                                : "§cGame cannot start: Task Race needs at least two speedrunners.")
                        : "§cGame cannot start: Task Race needs at least two speedrunners.";
            };

            Msg.broadcast(failureMessage);
            return false;
        }

        // Capture mode for countdown presentation
        final SpeedrunnerSwap.SwapMode countdownMode = plugin.getCurrentMode();

        // Countdown
        new BukkitRunnable() {
            int count = 3;

            @Override
            public void run() {
                if (count > 0) {
                    String title = switch (countdownMode) {
                        case DREAM -> "§b§lDream Swap starting in " + count;
                        case SAPNAP -> "§d§lSapnap speedrunner swap in " + count;
                        case TASK -> "§6§lTaskmaster starting in " + count;
                        case TASK_DUEL -> "§6§lTaskmaster Duo starting in " + count;
                        case TASK_RACE -> "§6§lTask Race starting in " + count;
                    };
                    String subtitle = "§7Made by muj4b";
                    for (Player player : getOnlineGameParticipants()) {
                        BukkitCompat.showTitle(player, title, subtitle, 10, 70, 10);
                    }
                    count--;
                } else {
                    String goTitle = switch (countdownMode) {
                        case DREAM -> "§b§lDream Swap GO!";
                        case SAPNAP -> "§d§lSapnap swap GO!";
                        case TASK -> "§6§lTaskmaster GO!";
                        case TASK_DUEL -> "§6§lTaskmaster Duo GO!";
                        case TASK_RACE -> "§6§lTask Race GO!";
                    };
                    for (Player player : getOnlineGameParticipants()) {
                        BukkitCompat.showTitle(player, goTitle, "§7Made by muj4b", 10, 60, 10);
                    }
                    this.cancel();
                    gameRunning = true;
                    gamePaused = false;
                    activeRunnerIndex = 0;
                    activeHunterIndex = 0;
                    activeRunner = runners.get(activeRunnerIndex);
                    activeHunter = plugin.usesSharedSecondBody() && !hunters.isEmpty()
                            ? hunters.get(activeHunterIndex)
                            : null;
                    initializeSessionWorld();
                    playerStates.clear();
                    restorableParticipantIds.clear();
                    saveAllPlayerStates();
                    portalSwapRetries.clear();
                    swapInProgress = false;
                    pausedByDisconnect = false;

                    if (plugin.getConfigManager().isKitsEnabled()) {
                        for (Player player : runners) {
                            plugin.getKitManager().giveKit(player, "runner");
                        }
                        for (Player hunter : hunters) {
                            plugin.getKitManager().giveKit(hunter, "hunter");
                        }
                    }

                    if (plugin.usesSharedRunnerControl()) {
                        scheduleNextSwap();
                    }
                    if (plugin.getCurrentMode() == com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.DREAM
                            || plugin.isDualBodyTaskMode()) {
                        scheduleNextHunterSwap();
                    }

                    Runnable postStart = () -> {
                        if (!gameRunning)
                            return;
                        if (plugin.usesSharedRunnerControl()) {
                            applyInactiveEffects();
                            startActionBarUpdates();
                            startTitleUpdates();
                            startCageEnforcement();
                            if (plugin.getConfigManager().isFreezeMechanicEnabled()) {
                                startFreezeChecking();
                            }
                        }
                    };

                    if (plugin.isTaskCompetitionMode()) {
                        try {
                            List<Player> participants = getTaskCompetitionParticipants();
                            plugin.getTaskManagerMode().beginRound(participants);
                            plugin.getTaskManagerMode().assignAndAnnounceTasks(participants);
                        } catch (Throwable t) {
                            plugin.getLogger().warning("Task assignment failed: " + t.getMessage());
                        }
                        startRunnerTimeoutWatcher();
                        startTaskMaxDurationWatcher(getConfiguredTaskDurationMs());
                        Bukkit.getScheduler().runTaskLater(plugin, postStart, TASK_INTRO_DELAY_TICKS);
                    } else {
                        postStart.run();
                    }

                    if (plugin.getCurrentMode() == com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.DREAM
                            && plugin.getConfigManager().isTrackerEnabled()) {
                        plugin.getTrackerManager().startTracking();
                        if (plugin.usesSharedHunterControl()) {
                            if (activeHunter != null && activeHunter.isOnline()) {
                                plugin.getTrackerManager().giveTrackingCompass(activeHunter);
                            }
                        } else {
                            for (Player hunter : hunters) {
                                if (hunter.isOnline()) {
                                    plugin.getTrackerManager().giveTrackingCompass(hunter);
                                }
                            }
                        }
                    }

                    try {
                        if (plugin.getVoiceChatIntegration() != null) {
                            plugin.getVoiceChatIntegration().updateRunnerMuteStatus();
                        }
                    } catch (Throwable ignored) {
                    }

                    // Optionally start stats tracking
                    if (plugin.getConfig().getBoolean("stats.enabled", true)) {
                        plugin.getStatsManager().startTracking();
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);

        return true;
    }

    public void endGame(Team winner) {
        if (!gameRunning) {
            return;
        }

        String titleStr;
        String runnerSubtitle = "";
        String hunterSubtitle = "";
        com.example.speedrunnerswap.config.ConfigManager cfg = plugin.getConfigManager();

        if (winner == Team.RUNNER) {
            titleStr = cfg.getEndGameRunnerWinTitle();
            runnerSubtitle = cfg.getEndGameRunnerWinRunnerSubtitle();
            hunterSubtitle = cfg.getEndGameRunnerWinHunterSubtitle();
        } else if (winner == Team.HUNTER) {
            titleStr = cfg.getEndGameHunterWinTitle();
            runnerSubtitle = cfg.getEndGameHunterWinRunnerSubtitle();
            hunterSubtitle = cfg.getEndGameHunterWinHunterSubtitle();
        } else {
            titleStr = cfg.getEndGameNoWinnerTitle();
            runnerSubtitle = cfg.getEndGameNoWinnerRunnerSubtitle();
            hunterSubtitle = cfg.getEndGameNoWinnerHunterSubtitle();
        }

        for (Player player : getOnlineGameParticipants()) {
            String sub = isRunner(player) ? runnerSubtitle : hunterSubtitle;
            BukkitCompat.showTitle(player, titleStr, sub, 10, 100, 10);
        }

        finalizeGameEnd(formatEndGameBroadcast(winner));
    }

    public void endTaskCompetitionRound(Player winner, String taskDescription) {
        if (!gameRunning) {
            return;
        }

        String winnerName = winner != null ? winner.getName() : "Unknown";
        String description = taskDescription != null && !taskDescription.isBlank() ? taskDescription : "their secret task";

        for (Player participant : getOnlineGameParticipants()) {
            BukkitCompat.showTitle(participant, "§a§lTASK COMPLETE!",
                    "§e" + winnerName + " §7completed: §f" + description, 10, 80, 16);
            participant.sendMessage("§a[Task Manager] Winner: §f" + winnerName);
        }

        finalizeGameEnd("§a[Task Manager] Winner: §f" + winnerName);
    }

    private void finalizeGameEnd(String endMessage) {
        if (swapTask != null)
            swapTask.cancel();
        if (hunterSwapTask != null)
            hunterSwapTask.cancel();
        if (actionBarTask != null)
            actionBarTask.cancel();
        if (titleTask != null)
            titleTask.cancel();
        if (freezeCheckTask != null)
            freezeCheckTask.cancel();
        if (cageTask != null) {
            cageTask.cancel();
            cageTask = null;
        }
        if (runnerTimeoutTask != null) {
            runnerTimeoutTask.cancel();
            runnerTimeoutTask = null;
        }
        if (taskMaxDurationTask != null) {
            taskMaxDurationTask.cancel();
            taskMaxDurationTask = null;
        }
        plugin.getTrackerManager().stopTracking();
        portalSwapRetries.clear();
        swapInProgress = false;
        pausedByDisconnect = false;
        taskGameDeadlineMs = 0L;
        taskGameRemainingMs = 0L;
        try {
            plugin.getStatsManager().stopTracking();
        } catch (Exception ignored) {
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                // Optionally preserve final runner progress for all runners (configurable)
                if (plugin.getConfig().getBoolean("swap.preserve_runner_progress_on_end", false)) {
                    try {
                        if (activeRunner != null && activeRunner.isOnline() && !runners.isEmpty()) {
                            com.example.speedrunnerswap.models.PlayerState finalState = PlayerStateUtil
                                    .capturePlayerState(activeRunner);
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

                // Reset voice chat mute status so all players can talk again
                try {
                    if (plugin.getVoiceChatIntegration() != null) {
                        plugin.getVoiceChatIntegration().resetAllPlayerMuteStatus();
                    }
                } catch (Throwable ignored) {
                }

                gameRunning = false;
                gamePaused = false;
                activeRunner = null;
                activeHunter = null;
                activeRunnerIndex = 0;
                activeHunterIndex = 0;
                nextSwapTime = 0L;
                nextHunterSwapTime = 0L;
                sessionWorldName = null;

                if (plugin.getConfigManager().isBroadcastGameEvents() && endMessage != null && !endMessage.isBlank()) {
                    for (Player participant : getOnlineGameParticipants()) {
                        participant.sendMessage(endMessage);
                    }
                }

                broadcastDonationMessage(getOnlineGameParticipants());
            }
        }.runTaskLater(plugin, 200L);
    }

    private String formatEndGameBroadcast(Team winner) {
        String winnerLabel = "NONE";
        String winnerMessage = "Game ended!";
        if (winner == Team.RUNNER) {
            winnerLabel = "RUNNER";
            winnerMessage = "RUNNER team won!";
        } else if (winner == Team.HUNTER) {
            winnerLabel = "HUNTER";
            winnerMessage = "HUNTER team won!";
        }
        String template = plugin.getConfigManager().getEndGameBroadcastMessage();
        return template
                .replace("%winner%", winnerMessage)
                .replace("%winner_team%", winnerLabel);
    }

    public void sendDonationMessage(Player recipient) {
        if (recipient != null) {
            deliverDonationMessage(recipient, SpeedrunnerSwap.DONATION_URL);
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            deliverDonationMessage(player, SpeedrunnerSwap.DONATION_URL);
        }
    }

    private void deliverDonationMessage(Player player, String donateUrl) {
        if (player == null)
            return;
        player.sendMessage("");
        player.sendMessage("§6§l=== Support the Creator ===");
        player.sendMessage("§eEnjoyed the game? Help keep updates coming!");
        player.sendMessage("§d❤ Donate to support development");
        player.sendMessage("§b" + donateUrl);
        player.sendMessage("");
    }

    private void broadcastDonationMessage(List<Player> recipients) {
        if (recipients == null || recipients.isEmpty()) {
            return;
        }
        for (Player player : recipients) {
            deliverDonationMessage(player, SpeedrunnerSwap.DONATION_URL);
        }
    }

    /** Stop the game without declaring a winner */
    public void stopGame() {
        endGame(null);
    }

    /**
     * Get whether the player is a hunter
     * 
     * @param player The player to check
     * @return true if the player is a hunter
     */
    public boolean isHunter(Player player) {
        return player != null && containsPlayerByUuid(hunters, player.getUniqueId());
    }

    /**
     * Get whether the player is a runner
     * 
     * @param player The player to check
     * @return true if the player is a runner
     */
    public boolean isRunner(Player player) {
        return player != null && containsPlayerByUuid(runners, player.getUniqueId());
    }

    /**
     * Get whether the game is running
     * 
     * @return true if the game is running
     */
    public boolean isGameRunning() {
        return gameRunning;
    }

    /**
     * Get the current active runner
     * 
     * @return The currently active runner
     */
    public Player getActiveRunner() {
        return activeRunner;
    }

    public Player getActiveHunter() {
        return activeHunter;
    }

    /**
     * Get all runners
     * 
     * @return List of all runners
     */
    public List<Player> getRunners() {
        return runners;
    }

    /**
     * Get all hunters
     * 
     * @return List of all hunters
     */
    public List<Player> getHunters() {
        return hunters;
    }

    /**
     * Refresh the swap schedule timer
     */
    public void refreshSwapSchedule() {
        if (gameRunning && !gamePaused && plugin.usesSharedRunnerControl()) {
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
     * 
     * @param player The player to get state for
     * @return The player's game state or null if not found
     */
    public PlayerState getPlayerState(Player player) {
        if (player == null)
            return null;
        return playerStates.computeIfAbsent(player.getUniqueId(), id -> PlayerStateUtil.capturePlayerState(player));
    }

    /**
     * Check if the game can be started
     * 
     * @return true if the game can be started, false otherwise
     */
    public boolean canStartGame() {
        if (gameRunning) {
            return false;
        }
        loadTeams();
        SpeedrunnerSwap.SwapMode mode = plugin.getCurrentMode();
        boolean hasRunner = !runners.isEmpty();
        boolean hasHunter = !hunters.isEmpty();

        if (!hasRunner) {
            return false;
        }

        if (huntersRequired(mode)) {
            return hasHunter;
        }

        if (!huntersAllowed(mode) && hasHunter) {
            return false;
        }

        return runners.size() >= minimumRequiredRunners(mode);
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
        boolean huntersRequired = huntersRequired(plugin.getCurrentMode());
        boolean teamEmpty = runners.isEmpty() || (huntersRequired && hunters.isEmpty());
        if (gameRunning && teamEmpty) {
            boolean pauseOnDc = plugin.isTaskCompetitionMode()
                    ? plugin.getConfig().getBoolean("task_manager.pause_on_disconnect",
                            plugin.getConfigManager().isPauseOnDisconnect())
                    : plugin.getConfigManager().isPauseOnDisconnect();
            if (pauseOnDc) {
                if (pauseGame()) {
                    pausedByDisconnect = true;
                }
                if (plugin.getConfigManager().isBroadcastGameEvents()) {
                    Msg.broadcast("§e[SpeedrunnerSwap] Game paused: waiting for players to return.");
                }
            } else {
                // Keep running but log a warning for admins
                plugin.getLogger().warning("A team is empty; game continues (pause_on_disconnect=false)");
            }
        }

        // End-when-one-left behavior (Task mode)
        if (gameRunning && plugin.isTaskCompetitionMode()
                && plugin.getConfig().getBoolean("task_manager.end_when_one_left", false)) {
            int online = 0;
            for (Player participant : getTaskCompetitionParticipants())
                if (participant.isOnline())
                    online++;
            if (online <= 1) {
                Msg.broadcast("§e[Task Manager] Ending: only one participant remains.");
                stopGame();
                return;
            }
        }
    }

    /**
     * Handle a player quitting
     * 
     * @param player The player who quit
     */
    public void handlePlayerQuit(Player player) {
        if (!gameRunning) {
            return;
        }

        if (isRunner(player) || isTaskCompetitionParticipant(player)) {
            runnerDisconnectAt.put(player.getUniqueId(), System.currentTimeMillis());
            // Persist runtime disconnect time for Task mode
            if (plugin.isTaskCompetitionMode()) {
                setRuntimeDisconnectTime(player.getUniqueId(), System.currentTimeMillis());
            }
        }

        boolean pauseOnDc = plugin.isTaskCompetitionMode()
                ? plugin.getConfig().getBoolean("task_manager.pause_on_disconnect",
                        plugin.getConfigManager().isPauseOnDisconnect())
                : plugin.getConfigManager().isPauseOnDisconnect();

        if (plugin.isTaskCompetitionMode() && isTaskCompetitionParticipant(player)) {
            if (pauseOnDc) {
                if (pauseGame()) {
                    pausedByDisconnect = true;
                }
            } else {
                if (plugin.usesSharedRunnerControl() && isRunner(player) && player.equals(activeRunner)) {
                    performSwap();
                } else if (plugin.usesSharedSecondBody() && isHunter(player) && isActiveHunter(player)) {
                    performHunterSwap();
                } else {
                    reselectSessionLeader();
                }
            }
        } else if (player.equals(activeRunner)) {
            if (pauseOnDc) {
                if (pauseGame()) {
                    pausedByDisconnect = true;
                }
            } else {
                performSwap();
            }
        } else if (plugin.usesSharedSecondBody() && isHunter(player) && isActiveHunter(player)) {
            if (pauseOnDc) {
                if (pauseGame()) {
                    pausedByDisconnect = true;
                }
            } else {
                performHunterSwap();
            }
        }

        savePlayerState(player);
        portalSwapRetries.remove(player.getUniqueId());
        ensureRunnerQueueCoherence();
        ensureHunterQueueCoherence();
    }

    /** Handle player rejoin */
    public void handlePlayerJoin(Player player) {
        if (!gameRunning)
            return;
        synchronizeRejoinedPlayer(player);
        runnerDisconnectAt.remove(player.getUniqueId());
        clearRuntimeDisconnectTime(player.getUniqueId());
        if (plugin.isTaskCompetitionMode()) {
            // If they had an assignment, ensure they're in runners
            var tmm = plugin.getTaskManagerMode();
            if (tmm != null && tmm.getAssignedTask(player) != null) {
                if (!isRunner(player) && !isHunter(player)) {
                    runners.add(player);
                }
                // Remind their task
                var def = tmm.getTask(tmm.getAssignedTask(player));
                if (def != null) {
                    player.sendMessage("§6[Task Manager] Your task:");
                    player.sendMessage("§e → " + def.description());
                }
            } else if (plugin.getConfig().getBoolean("task_manager.allow_late_joiners", false)) {
                // Late joiner allowed: add as runner and optionally assign a task if pool
                // available
                if (!isRunner(player)) {
                    runners.add(player);
                }
                if (tmm != null && tmm.getAssignedTask(player) == null) {
                    tmm.assignAdditionalTasks(java.util.List.of(player));
                }
            }

        }
        reselectSessionLeader();
        if (isGamePaused() && pausedByDisconnect && canResumeAfterDisconnectPause()) {
            resumeGame();
        }
        if (plugin.usesSharedRunnerControl() && !gamePaused) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!gameRunning || !player.isOnline()) {
                    return;
                }
                applyInactiveEffects();
                refreshActiveTrackerTargets();
            });
        }
        if (plugin.usesSharedSecondBody() && isHunter(player)) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!gameRunning || !player.isOnline()) {
                    return;
                }
                applyInactiveEffects();
                if (plugin.getCurrentMode() == com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.DREAM
                        && isActiveHunter(player)) {
                    plugin.getTrackerManager().giveTrackingCompass(player);
                }
            });
        }
        portalSwapRetries.remove(player.getUniqueId());
        ensureRunnerQueueCoherence();
        ensureHunterQueueCoherence();
        if (plugin.getConfigManager().isTrackerEnabled()) {
            plugin.getTrackerManager().startTracking();
        }
    }

    private void startRunnerTimeoutWatcher() {
        if (runnerTimeoutTask != null)
            runnerTimeoutTask.cancel();
        runnerTimeoutTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!gameRunning)
                    return;
                long now = System.currentTimeMillis();
                int graceSec = plugin.getConfig().getInt("task_manager.rejoin_grace_seconds", 180);
                boolean remove = plugin.getConfig().getBoolean("task_manager.remove_on_timeout", true);
                boolean pauseOnDc = plugin.getConfig().getBoolean("task_manager.pause_on_disconnect", true);

                java.util.List<UUID> toRemove = new java.util.ArrayList<>();
                for (var e : runnerDisconnectAt.entrySet()) {
                    long elapsed = now - e.getValue();
                    if (elapsed >= graceSec * 1000L) {
                        UUID uuid = e.getKey();
                        if (remove) {
                            // Remove from active task participant lists
                            runners.removeIf(r -> r.getUniqueId().equals(uuid));
                            hunters.removeIf(h -> h.getUniqueId().equals(uuid));
                            toRemove.add(uuid);
                        }
                        // If active runner, move on
                        if (activeRunner != null && activeRunner.getUniqueId().equals(uuid)) {
                            if (plugin.usesSharedRunnerControl()) {
                                performSwap();
                            } else {
                                reselectSessionLeader();
                            }
                        } else if (plugin.usesSharedSecondBody()
                                && activeHunter != null
                                && activeHunter.getUniqueId().equals(uuid)) {
                            performHunterSwap();
                        }
                    }
                }
                for (UUID id : toRemove) {
                    runnerDisconnectAt.remove(id);
                    clearRuntimeDisconnectTime(id);
                }

                // Enforce end-when-one-left if enabled
                if (gameRunning && plugin.getConfig().getBoolean("task_manager.end_when_one_left", false)
                        && plugin.isTaskCompetitionMode()) {
                    int online = 0;
                    for (Player participant : getTaskCompetitionParticipants())
                        if (participant.isOnline()) {
                            online++;
                        }
                    if (online <= 1) {
                        Msg.broadcast("§e[Task Manager] Ending: only one participant remains.");
                        stopGame();
                        return;
                    }
                }

                // If paused due to disconnect and we still have online runners, resume
                if (gamePaused && pauseOnDc) {
                    if (pausedByDisconnect && canResumeAfterDisconnectPause())
                        resumeGame();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L * 5);
    }

    private long getConfiguredTaskDurationMs() {
        int minutes = plugin.getConfig().getInt("task_manager.max_game_duration", 0);
        if (minutes <= 0) {
            return 0L;
        }
        return minutes * 60_000L;
    }

    private void startTaskMaxDurationWatcher(long durationMs) {
        if (taskMaxDurationTask != null) {
            taskMaxDurationTask.cancel();
            taskMaxDurationTask = null;
        }
        taskGameDeadlineMs = 0L;
        taskGameRemainingMs = 0L;
        if (!plugin.isTaskCompetitionMode() || durationMs <= 0L) {
            return;
        }
        scheduleTaskMaxDurationWatcher(durationMs);
    }

    private void scheduleTaskMaxDurationWatcher(long durationMs) {
        if (!plugin.isTaskCompetitionMode() || durationMs <= 0L) {
            taskGameDeadlineMs = 0L;
            taskGameRemainingMs = 0L;
            return;
        }
        if (taskMaxDurationTask != null) {
            taskMaxDurationTask.cancel();
        }
        long delayTicks = Math.max(1L, (long) Math.ceil(durationMs / 50.0D));
        taskGameDeadlineMs = System.currentTimeMillis() + durationMs;
        taskGameRemainingMs = durationMs;
        taskMaxDurationTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            taskMaxDurationTask = null;
            taskGameDeadlineMs = 0L;
            taskGameRemainingMs = 0L;
            if (!gameRunning) {
                return;
            }
            Msg.broadcast("§e[Task Manager] Maximum game duration reached. Ending the round.");
            stopGame();
        }, delayTicks);
    }

    private void setRuntimeDisconnectTime(UUID uuid, long when) {
        try {
            String path = "task_manager.runtime.disconnect_times." + uuid;
            plugin.getConfig().set(path, when);
            plugin.saveConfig();
        } catch (Throwable ignored) {
        }
    }

    private void clearRuntimeDisconnectTime(UUID uuid) {
        try {
            String path = "task_manager.runtime.disconnect_times." + uuid;
            plugin.getConfig().set(path, null);
            plugin.saveConfig();
        } catch (Throwable ignored) {
        }
    }

    /**
     * Get time until next swap in seconds
     */
    public int getTimeUntilNextSwap() {
        if (!plugin.usesSharedRunnerControl() || !gameRunning) {
            return 0;
        }
        return (int) ((nextSwapTime - System.currentTimeMillis()) / 1000);
    }

    public int getTimeUntilNextHunterSwap() {
        if (!plugin.usesSharedSecondBody() || !gameRunning) {
            return 0;
        }
        return (int) ((nextHunterSwapTime - System.currentTimeMillis()) / 1000);
    }

    private void saveAllPlayerStates() {
        LinkedHashMap<UUID, Player> participants = new LinkedHashMap<>();
        for (Player runner : runners) {
            if (runner != null) {
                participants.put(runner.getUniqueId(), runner);
            }
        }
        for (Player hunter : hunters) {
            if (hunter != null) {
                participants.put(hunter.getUniqueId(), hunter);
            }
        }
        for (Player participant : participants.values()) {
            savePlayerState(participant);
        }
    }

    private void savePlayerState(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        reclaimOpenContainerItems(player);
        PlayerState state = PlayerStateUtil.capturePlayerState(player);
        playerStates.put(player.getUniqueId(), state);
        restorableParticipantIds.add(player.getUniqueId());
    }

    private void restoreAllPlayerStates() {
        Set<UUID> restored = new HashSet<>();
        for (UUID participantId : new HashSet<>(restorableParticipantIds)) {
            Player player = Bukkit.getPlayer(participantId);
            if (player == null || !player.isOnline()) {
                continue;
            }

            restoreTrackedParticipant(player);
            restored.add(participantId);
        }

        for (UUID restoredId : restored) {
            restorableParticipantIds.remove(restoredId);
            playerStates.remove(restoredId);
        }
    }

    private void restoreTrackedParticipant(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        restorePlayerState(player);

        PotionEffectType eff;
        if ((eff = BukkitCompat.resolvePotionEffect("blindness")) != null)
            player.removePotionEffect(eff);
        if ((eff = BukkitCompat.resolvePotionEffect("darkness")) != null)
            player.removePotionEffect(eff);
        if ((eff = BukkitCompat.resolvePotionEffect("weakness")) != null)
            player.removePotionEffect(eff);
        if ((eff = BukkitCompat.resolvePotionEffect("slow_falling")) != null)
            player.removePotionEffect(eff);
        if ((eff = BukkitCompat.resolvePotionEffect("slowness")) != null)
            player.removePotionEffect(eff);
        if ((eff = BukkitCompat.resolvePotionEffect("jump_boost")) != null)
            player.removePotionEffect(eff);

        if (player.getGameMode() == GameMode.SPECTATOR && isRunner(player)) {
            player.setGameMode(GameMode.SURVIVAL);
        }
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            try {
                viewer.showPlayer(plugin, player);
            } catch (Throwable ignored) {
            }
        }
    }

    public void restorePendingStateIfNeeded(Player player) {
        if (player == null || gameRunning) {
            return;
        }
        UUID uuid = player.getUniqueId();
        if (!restorableParticipantIds.contains(uuid) || !playerStates.containsKey(uuid)) {
            return;
        }
        restoreTrackedParticipant(player);
        restorableParticipantIds.remove(uuid);
        playerStates.remove(uuid);
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

                Location target = null;
                boolean forceGlobalSpawn = plugin.getConfigManager().isForceGlobalSpawn();
                if (!forceGlobalSpawn) {
                    Location saved = state.getLocation();
                    if (saved != null && !saved.getBlock().getType().isSolid()) {
                        target = saved;
                    }
                }

                if (target == null) {
                    target = plugin.getConfigManager().getSpawnLocation();
                }

                player.teleport(target);
                if (target.equals(plugin.getConfigManager().getSpawnLocation())) {
                    player.setGameMode(GameMode.SURVIVAL);
                    player.getInventory().clear();
                }
            } catch (Exception e) {
                plugin.getLogger()
                        .warning("Failed to restore state for player " + player.getName() + ": " + e.getMessage());
                player.teleport(plugin.getConfigManager().getSpawnLocation());
                player.setGameMode(GameMode.SURVIVAL);
                player.getInventory().clear();
            }
        }
    }

    private void reclaimOpenContainerItems(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        try {
            InventoryView view = player.getOpenInventory();
            if (view == null) {
                return;
            }

            InventoryType type = view.getType();
            if (!RETURN_CONTAINERS.contains(type)) {
                return;
            }

            // Handle item on cursor first
            ItemStack cursor = view.getCursor();
            if (cursor != null && !cursor.getType().isAir()) {
                ItemStack cursorClone = cursor.clone();
                view.setCursor(null);
                Map<Integer, ItemStack> overflow = player.getInventory().addItem(cursorClone);
                if (!overflow.isEmpty()) {
                    overflow.values()
                            .forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
                }
            }

            org.bukkit.inventory.Inventory top = view.getTopInventory();
            for (int i = 0; i < top.getSize(); i++) {
                ItemStack stack = top.getItem(i);
                if (stack == null || stack.getType().isAir()) {
                    continue;
                }
                ItemStack clone = stack.clone();
                top.setItem(i, null);
                Map<Integer, ItemStack> overflow = player.getInventory().addItem(clone);
                if (!overflow.isEmpty()) {
                    overflow.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
                }
            }
            player.updateInventory();
        } catch (Throwable ignored) {
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

    private long resolvePrimarySwapIntervalSeconds() {
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

        return Math.max(1, intervalSeconds);
    }

    private void scheduleNextSwap() {
        if (swapTask != null) {
            swapTask.cancel();
        }
        if (!plugin.usesSharedRunnerControl()) {
            nextSwapTime = System.currentTimeMillis();
            return;
        }

        long intervalSeconds = resolvePrimarySwapIntervalSeconds();

        long intervalTicks = intervalSeconds * 20;
        nextSwapTime = System.currentTimeMillis() + (intervalSeconds * 1000);
        swapTask = Bukkit.getScheduler().runTaskLater(plugin, this::performSwap, intervalTicks);
    }

    private void scheduleNextHunterSwap() {
        if (hunterSwapTask != null) {
            hunterSwapTask.cancel();
        }

        if (plugin.getCurrentMode() != com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.DREAM
                && !plugin.isDualBodyTaskMode()) {
            nextHunterSwapTime = System.currentTimeMillis();
            return;
        }

        long intervalSeconds;
        if (plugin.isDualBodyTaskMode()) {
            intervalSeconds = resolvePrimarySwapIntervalSeconds();
        } else if (plugin.usesSharedHunterControl()) {
            intervalSeconds = Math.max(1, plugin.getConfigManager().getSharedHunterControlInterval());
        } else if (plugin.getConfigManager().isHunterSwapEnabled()) {
            intervalSeconds = Math.max(1, plugin.getConfigManager().getHunterSwapInterval());
        } else {
            nextHunterSwapTime = System.currentTimeMillis();
            return;
        }

        long intervalTicks = intervalSeconds * 20L;
        nextHunterSwapTime = System.currentTimeMillis() + (intervalSeconds * 1000L);
        hunterSwapTask = Bukkit.getScheduler().runTaskLater(plugin, this::performHunterSwap, intervalTicks);
    }

    private void startActionBarUpdates() {
        if (actionBarTask != null) {
            actionBarTask.cancel();
        }
        if (!plugin.usesSharedRunnerControl()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                com.example.speedrunnerswap.utils.ActionBarUtil.sendActionBar(player, "");
            }
            return;
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
        if (titleTask != null)
            titleTask.cancel();
        if (!plugin.usesSharedRunnerControl())
            return;
        int period = Math.max(1, plugin.getConfigManager().getTitleUpdateTicks());
        titleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!gameRunning || gamePaused)
                return;
            updateTitles();
        }, 0L, period);
    }

    private void updateActionBar() {
        if (!gameRunning || gamePaused || !plugin.usesSharedRunnerControl()) {
            return;
        }

        int timeLeft = getTimeUntilNextSwap();
        int hunterTimeLeft = getTimeUntilNextHunterSwap();
        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean isRunner = isRunner(player);
            boolean isHunter = isHunter(player);
            boolean isActive = isActiveRunner(player);
            boolean isActiveHunter = isActiveHunter(player);
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
                case "last_10" -> show = (isHunter && plugin.usesSharedSecondBody() ? hunterTimeLeft : timeLeft) <= 10;
                default -> show = false;
            }

            if (show) {
                // For caged players, show queue position instead of timer
                if (isCaged && ((isRunner && !isActive) || (isHunter && plugin.usesSharedSecondBody() && !isActiveHunter))) {
                    int queuePosition = isRunner
                            ? getQueuePosition(runners, activeRunner, player)
                            : getQueuePosition(hunters, activeHunter, player);
                    String msg = String.format("§6Queued (%d) - You're up next", queuePosition);
                    com.example.speedrunnerswap.utils.ActionBarUtil.sendActionBar(player, msg);
                } else {
                    int displayTime = isHunter && plugin.usesSharedSecondBody() ? hunterTimeLeft : timeLeft;
                    String label = isHunter && plugin.usesSharedSecondBody() ? "Second body in" : "Swap in";
                    String msg = String.format("§e%s: §c%ds", label, Math.max(0, displayTime));
                    com.example.speedrunnerswap.utils.ActionBarUtil.sendActionBar(player, msg);
                }
            } else {
                com.example.speedrunnerswap.utils.ActionBarUtil.sendActionBar(player, "");
            }
        }
    }

    private int getQueuePosition(List<Player> group, Player active, Player player) {
        if (!isRunner(player))
            if (!isHunter(player))
                return 0;

        int position = 1;
        for (Player participant : group) {
            if (participant.equals(player))
                break;
            if (participant.isOnline() && !participant.equals(active)) {
                position++;
            }
        }
        return position;
    }

    private Location prepareSafeSpawn(Location base, World fallbackWorld) {
        if (base == null) {
            return null;
        }
        World world = base.getWorld() != null ? base.getWorld() : fallbackWorld;
        if (world == null) {
            return null;
        }

        Location centered = base.clone();
        centered.setWorld(world);
        centered.setX(centered.getBlockX() + 0.5);
        centered.setZ(centered.getBlockZ() + 0.5);

        Location safe = SafeLocationFinder.findSafeLocation(
                centered,
                Math.max(RESPAWN_SEARCH_RADIUS, plugin.getConfigManager().getSafeSwapHorizontalRadius()),
                Math.max(RESPAWN_VERTICAL_RANGE, plugin.getConfigManager().getSafeSwapVerticalDistance()),
                plugin.getConfigManager().getDangerousBlocks());
        if (safe != null) {
            safe.setX(safe.getBlockX() + 0.5);
            safe.setZ(safe.getBlockZ() + 0.5);
            ensureChunkLoaded(safe);
            return safe;
        }
        ensureChunkLoaded(centered);
        return centered;
    }

    private void ensureChunkLoaded(Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        try {
            location.getWorld().getChunkAt(location);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Update hostile mob targeting when players swap
     * This ensures mobs that were targeting the previous runner now target the new
     * runner
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

                    // Endermen don't have a direct getTarget() method, but we can check if they're
                    // angry
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
        ensureRunnerQueueCoherence();
        ensureHunterQueueCoherence();
        String freezeMode = plugin.getConfigManager().getFreezeMode();

        applySharedControlEffects(runners, activeRunner, freezeMode);
        if (plugin.usesSharedSecondBody()) {
            applySharedControlEffects(hunters, activeHunter, freezeMode);
        }
    }

    private void applySharedControlEffects(List<Player> group, Player activePlayer, String freezeMode) {
        if (group == null || group.isEmpty()) {
            return;
        }

        for (Player participant : group) {
            if (participant == null || !participant.isOnline()) {
                continue;
            }
            if (participant.equals(activePlayer)) {
                // Remove player from shared cage set
                cagedPlayers.remove(participant.getUniqueId());
                PotionEffectType eff;
                if ((eff = BukkitCompat.resolvePotionEffect("blindness")) != null)
                    participant.removePotionEffect(eff);
                if ((eff = BukkitCompat.resolvePotionEffect("darkness")) != null)
                    participant.removePotionEffect(eff);
                if ((eff = BukkitCompat.resolvePotionEffect("slowness")) != null)
                    participant.removePotionEffect(eff);
                if ((eff = BukkitCompat.resolvePotionEffect("slow_falling")) != null)
                    participant.removePotionEffect(eff);
                if ((eff = BukkitCompat.resolvePotionEffect("invisibility")) != null)
                    participant.removePotionEffect(eff);
                participant.setGameMode(GameMode.SURVIVAL);
                try {
                    participant.setAllowFlight(false);
                } catch (Exception ignored) {
                }
                try {
                    participant.setFlying(false);
                } catch (Exception ignored) {
                }

                for (Player viewer : Bukkit.getOnlinePlayers()) {
                    viewer.showPlayer(plugin, participant);
                }
            } else {
                if (freezeMode.equalsIgnoreCase("EFFECTS")) {
                    PotionEffectType blindness = BukkitCompat.resolvePotionEffect("blindness");
                    if (blindness != null)
                        participant.addPotionEffect(new PotionEffect(blindness, Integer.MAX_VALUE, 1, false, false));
                    PotionEffectType darkness = BukkitCompat.resolvePotionEffect("darkness");
                    if (darkness != null)
                        participant.addPotionEffect(new PotionEffect(darkness, Integer.MAX_VALUE, 1, false, false));
                    PotionEffectType slowness = BukkitCompat.resolvePotionEffect("slowness");
                    if (slowness != null)
                        participant.addPotionEffect(new PotionEffect(slowness, Integer.MAX_VALUE, 255, false, false));
                    PotionEffectType slowFalling = BukkitCompat.resolvePotionEffect("slow_falling");
                    if (slowFalling != null)
                        participant.addPotionEffect(new PotionEffect(slowFalling, Integer.MAX_VALUE, 128, false, false));
                } else if (freezeMode.equalsIgnoreCase("SPECTATOR")) {
                    participant.setGameMode(GameMode.SPECTATOR);
                } else if (freezeMode.equalsIgnoreCase("LIMBO")) {
                    Location limboLocation = plugin.getConfigManager().getLimboLocation();
                    Location safe = SafeLocationFinder.findSafeLocation(
                            limboLocation,
                            plugin.getConfigManager().getSafeSwapHorizontalRadius(),
                            plugin.getConfigManager().getSafeSwapVerticalDistance(),
                            plugin.getConfigManager().getDangerousBlocks());
                    participant.teleport(safe != null ? safe : limboLocation);
                    participant.setGameMode(GameMode.ADVENTURE);
                    PotionEffectType blindness2 = BukkitCompat.resolvePotionEffect("blindness");
                    if (blindness2 != null)
                        participant.addPotionEffect(new PotionEffect(blindness2, Integer.MAX_VALUE, 1, false, false));
                } else if (freezeMode.equalsIgnoreCase("CAGE")) {
                    createOrEnsureSharedCage(participant.getWorld());
                    teleportToSharedCage(participant);
                    PotionEffectType blindness = BukkitCompat.resolvePotionEffect("blindness");
                    if (blindness != null)
                        participant.addPotionEffect(new PotionEffect(blindness, Integer.MAX_VALUE, 1, false, false));
                    PotionEffectType invis = BukkitCompat.resolvePotionEffect("invisibility");
                    if (invis != null)
                        participant.addPotionEffect(new PotionEffect(invis, Integer.MAX_VALUE, 1, false, false));
                    participant.setGameMode(GameMode.ADVENTURE);
                    try {
                        participant.setAllowFlight(true);
                    } catch (Exception ignored) {
                    }
                    try {
                        participant.setFlying(false);
                    } catch (Exception ignored) {
                    }
                }

                try {
                    participant.getInventory().clear();
                    participant.getInventory().setArmorContents(new ItemStack[] {});
                    participant.getInventory().setItemInOffHand(null);
                    participant.updateInventory();
                } catch (Exception ignored) {
                }

                if (!plugin.getConfigManager().isVoiceChatIntegrationEnabled()) {
                    for (Player viewer : Bukkit.getOnlinePlayers()) {
                        if (!viewer.equals(participant)) {
                            viewer.hidePlayer(plugin, participant);
                        }
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
        if (gameRunning && plugin.usesSharedRunnerControl() && plugin.getConfigManager().isFreezeMechanicEnabled()) {
            startFreezeChecking();
        }
        if (gameRunning && plugin.usesSharedRunnerControl()) {
            applyInactiveEffects();
            // If CAGE mode is not selected anymore, ensure cages are removed
            if (!"CAGE".equalsIgnoreCase(plugin.getConfigManager().getFreezeMode())) {
                cleanupAllCages();
            }
        } else if (!plugin.usesSharedRunnerControl()) {
            cleanupAllCages();
        }
    }

    /** Public hook for GUI/actions to re-apply active/inactive effects and cages */
    public void reapplyStates() {
        refreshFreezeMechanic();
    }

    private void performSwap() {
        if (!plugin.usesSharedRunnerControl()) {
            return;
        }
        // End-when-one-left early check
        if (gameRunning && plugin.isTaskCompetitionMode()
                && plugin.getConfig().getBoolean("task_manager.end_when_one_left", false)) {
            int online = 0;
            for (Player participant : getTaskCompetitionParticipants())
                if (participant.isOnline())
                    online++;
            if (online <= 1) {
                Msg.broadcast("§e[Task Manager] Ending: only one participant remains.");
                stopGame();
                return;
            }
        }
        if (!gameRunning || gamePaused || runners.isEmpty()) {
            return;
        }

        if (activeRunner != null && deferSwapForPortal(activeRunner)) {
            return;
        }

        if (swapInProgress) {
            return;
        }
        swapInProgress = true;

        try {
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
            boolean sameRunner = previousRunner != null && previousRunner.equals(nextRunner);

            if (plugin.isTaskCompetitionMode() && !plugin.isParallelTaskMode() && previousRunner != null && !sameRunner) {
                try {
                    plugin.getTaskManagerMode().markFirstTurnCompleted(previousRunner);
                } catch (Throwable ignored) {
                }
            }

            activeRunner = nextRunner;
            portalSwapRetries.remove(nextRunner.getUniqueId());
            ensureRunnerQueueCoherence();

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

            if (!sameRunner && previousRunner != null && previousRunner.isOnline()) {
                // Capture full state (includes potion effects, XP, etc.) from the previous
                // runner
                com.example.speedrunnerswap.models.PlayerState prevState = PlayerStateUtil
                        .capturePlayerState(previousRunner);

                // Apply to the next runner
                PlayerStateUtil.applyPlayerState(nextRunner, prevState);
                try {
                    nextRunner.updateInventory();
                } catch (Throwable ignored) {
                }

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
                previousRunner.getInventory().setArmorContents(new ItemStack[] {});
                previousRunner.getInventory().setItemInOffHand(null);
                previousRunner.updateInventory();
            } else if (previousRunner == null || !previousRunner.isOnline()) {
                // First-time activation (no previous runner). If kits enabled, give runner kit.
                if (plugin.getConfigManager().isKitsEnabled()) {
                    nextRunner.getInventory().clear();
                    plugin.getKitManager().giveKit(nextRunner, "runner");
                }
            }

            applyInactiveEffects();
            refreshActiveTrackerTargets();
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

            // Update voice chat mute status after swap
            try {
                if (plugin.getVoiceChatIntegration() != null) {
                    plugin.getVoiceChatIntegration().updateRunnerMuteStatus();
                }
            } catch (Throwable ignored) {
            }
        } finally {
            swapInProgress = false;
        }
    }

    private void refreshActiveTrackerTargets() {
        if (!plugin.getConfigManager().isTrackerEnabled()) {
            return;
        }
        Player current = activeRunner;
        if (current == null || !current.isOnline()) {
            return;
        }
        try {
            plugin.getTrackerManager().updateAllHunterCompasses();
            org.bukkit.Location loc = current.getLocation();
            if (loc != null && loc.getWorld() != null
                    && loc.getWorld().getEnvironment() == org.bukkit.World.Environment.NORMAL) {
                plugin.getTrackerManager().setLastRunnerOverworldLocation(loc);
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to refresh tracker targets: " + t.getMessage());
        }
    }

    private boolean deferSwapForPortal(Player runner) {
        if (runner == null || !runner.isOnline()) {
            return false;
        }
        if (runner.getGameMode() == GameMode.SPECTATOR) {
            portalSwapRetries.remove(runner.getUniqueId());
            return false;
        }

        org.bukkit.Location loc = runner.getLocation();
        if (loc == null) {
            return false;
        }
        org.bukkit.Material type = loc.getBlock().getType();
        boolean inPortal = type == org.bukkit.Material.NETHER_PORTAL || type == org.bukkit.Material.END_PORTAL
                || type == org.bukkit.Material.END_GATEWAY;
        if (!inPortal) {
            portalSwapRetries.remove(runner.getUniqueId());
            return false;
        }

        int attempts = portalSwapRetries.getOrDefault(runner.getUniqueId(), 0);
        int maxAttempts = plugin.getConfig().getInt("tracker.portal_retry_attempts", 5);
        if (attempts >= maxAttempts) {
            portalSwapRetries.remove(runner.getUniqueId());
            return false;
        }

        portalSwapRetries.put(runner.getUniqueId(), attempts + 1);
        long delay = plugin.getConfig().getLong("tracker.portal_retry_delay_ticks", 20L);
        long normalizedDelay = Math.max(5L, delay);
        if (plugin.getConfigManager().isBroadcastGameEvents()) {
            Msg.broadcast("§e[SpeedrunnerSwap] Swap deferred: runner is mid-portal. Retrying in "
                    + (normalizedDelay / 20.0) + "s (attempt " + (attempts + 1) + "/" + maxAttempts + ").");
        } else {
            plugin.getLogger().info("Deferred runner swap while player is in portal. Retry " + (attempts + 1) + "/"
                    + maxAttempts + " in " + normalizedDelay + " ticks.");
        }
        Bukkit.getScheduler().runTaskLater(plugin, this::performSwap, normalizedDelay);
        return true;
    }

    private void ensureRunnerQueueCoherence() {
        List<Player> cleaned = new ArrayList<>();
        java.util.Set<java.util.UUID> seen = new java.util.HashSet<>();

        for (Player candidate : runners) {
            if (candidate == null || !candidate.isOnline()) {
                continue;
            }
            if (seen.add(candidate.getUniqueId())) {
                cleaned.add(candidate);
            }
        }

        runners = cleaned;

        if (runners.isEmpty()) {
            activeRunner = null;
            activeRunnerIndex = 0;
            return;
        }

        if (activeRunner != null && activeRunner.isOnline()) {
            int idx = indexOfPlayerByUuid(runners, activeRunner.getUniqueId());
            if (idx >= 0) {
                activeRunner = runners.get(idx);
                activeRunnerIndex = idx;
            } else {
                runners.add(0, activeRunner);
                activeRunnerIndex = 0;
                seen.add(activeRunner.getUniqueId());
            }
        } else {
            activeRunner = runners.get(0);
            activeRunnerIndex = 0;
            seen.add(activeRunner.getUniqueId());
        }

        java.util.Set<java.util.UUID> allowed = new java.util.HashSet<>(seen);
        if (activeRunner != null) {
            allowed.add(activeRunner.getUniqueId());
        }
        portalSwapRetries.keySet().retainAll(allowed);
        swapInProgress = false;
    }

    private void ensureHunterQueueCoherence() {
        List<Player> cleaned = new ArrayList<>();
        java.util.Set<java.util.UUID> seen = new java.util.HashSet<>();

        for (Player candidate : hunters) {
            if (candidate == null || !candidate.isOnline()) {
                continue;
            }
            if (seen.add(candidate.getUniqueId())) {
                cleaned.add(candidate);
            }
        }

        hunters = cleaned;

        if (hunters.isEmpty()) {
            activeHunter = null;
            activeHunterIndex = 0;
            return;
        }

        if (activeHunter != null && activeHunter.isOnline()) {
            int idx = indexOfPlayerByUuid(hunters, activeHunter.getUniqueId());
            if (idx >= 0) {
                activeHunter = hunters.get(idx);
                activeHunterIndex = idx;
            } else {
                hunters.add(0, activeHunter);
                activeHunterIndex = 0;
            }
        } else {
            activeHunter = hunters.get(0);
            activeHunterIndex = 0;
        }

        hunterSwapInProgress = false;
    }

    /** Trigger an immediate runner swap (admin action) */
    public void triggerImmediateSwap() {
        if (!gameRunning || gamePaused || !plugin.usesSharedRunnerControl())
            return;
        Bukkit.getScheduler().runTask(plugin, this::performSwap);
    }

    /** Trigger an immediate hunter shuffle (admin action) */
    public void triggerImmediateHunterSwap() {
        if (!gameRunning || gamePaused)
            return;
        Bukkit.getScheduler().runTask(plugin, this::performHunterSwap);
    }

    private void performHunterSwap() {
        if (!gameRunning || gamePaused || hunters.size() < 2) {
            return;
        }

        if (plugin.getCurrentMode() == com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.DREAM
                && !plugin.usesSharedHunterControl()) {
            Collections.shuffle(hunters);
            plugin.getTrackerManager().updateAllHunterCompasses();

            if (plugin.getConfigManager().isBroadcastsEnabled()) {
                Msg.broadcast("§c[SpeedrunnerSwap] Hunters have been swapped!");
            }
            return;
        }

        if (hunterSwapInProgress) {
            return;
        }
        hunterSwapInProgress = true;

        try {
            ensureHunterQueueCoherence();
            if (hunters.isEmpty()) {
                return;
            }

            if (activeHunter != null && activeHunter.isOnline()) {
                savePlayerState(activeHunter);
            }

            int attempts = 0;
            do {
                activeHunterIndex = (activeHunterIndex + 1) % hunters.size();
                attempts++;
                if (attempts >= hunters.size()) {
                    plugin.getLogger().warning("No online hunters found during hunter swap - pausing game");
                    pauseGame();
                    return;
                }
            } while (!hunters.get(activeHunterIndex).isOnline());

            Player nextHunter = hunters.get(activeHunterIndex);
            Player previousHunter = activeHunter;
            boolean sameHunter = previousHunter != null && previousHunter.equals(nextHunter);

            if (plugin.isDualBodyTaskMode() && previousHunter != null && !sameHunter) {
                try {
                    plugin.getTaskManagerMode().markFirstTurnCompleted(previousHunter);
                } catch (Throwable ignored) {
                }
            }

            activeHunter = nextHunter;
            ensureHunterQueueCoherence();

            if (!sameHunter && previousHunter != null && previousHunter.isOnline()) {
                PlayerState previousState = PlayerStateUtil.capturePlayerState(previousHunter);
                PlayerStateUtil.applyPlayerState(nextHunter, previousState);
                try {
                    nextHunter.updateInventory();
                } catch (Throwable ignored) {
                }

                for (PotionEffect effect : previousHunter.getActivePotionEffects()) {
                    previousHunter.removePotionEffect(effect.getType());
                }

                previousHunter.getInventory().clear();
                previousHunter.getInventory().setArmorContents(new ItemStack[] {});
                previousHunter.getInventory().setItemInOffHand(null);
                previousHunter.updateInventory();
            }

            int gracePeriodTicks = plugin.getConfigManager().getGracePeriodTicks();
            if (gracePeriodTicks > 0) {
                nextHunter.setInvulnerable(true);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (nextHunter.isOnline()) {
                        nextHunter.setInvulnerable(false);
                    }
                }, gracePeriodTicks);
            }

            applyInactiveEffects();
            if (plugin.getCurrentMode() == com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.DREAM) {
                plugin.getTrackerManager().giveTrackingCompass(nextHunter);
            }
            scheduleNextHunterSwap();

            try {
                if (plugin.getVoiceChatIntegration() != null) {
                    plugin.getVoiceChatIntegration().updateRunnerMuteStatus();
                }
            } catch (Throwable ignored) {
            }
        } finally {
            hunterSwapInProgress = false;
        }
    }

    private void applyRandomPowerUp(Player player) {
        java.util.List<String> good = plugin.getConfigManager().getGoodPowerUps();
        java.util.List<String> bad = plugin.getConfigManager().getBadPowerUps();

        java.util.List<PotionEffectType> goodTypes = new java.util.ArrayList<>();
        java.util.List<PotionEffectType> badTypes = new java.util.ArrayList<>();

        for (String id : good) {
            PotionEffectType t = resolveEffect(id);
            if (t != null)
                goodTypes.add(t);
        }
        for (String id : bad) {
            PotionEffectType t = resolveEffect(id);
            if (t != null)
                badTypes.add(t);
        }

        // Fallbacks if config lists are empty or invalid
        if (goodTypes.isEmpty()) {
            String[] defaults = { "speed", "regeneration", "resistance", "night_vision", "dolphins_grace" };
            for (String k : defaults) {
                PotionEffectType t = BukkitCompat.resolvePotionEffect(k);
                if (t != null)
                    goodTypes.add(t);
            }
            if (goodTypes.isEmpty()) {
                // Ultra-safe baseline
                PotionEffectType t = BukkitCompat.resolvePotionEffect("speed");
                if (t != null)
                    goodTypes.add(t);
            }
        }
        if (badTypes.isEmpty()) {
            String[] defaults = { "slowness", "weakness", "hunger", "darkness", "glowing" };
            for (String k : defaults) {
                PotionEffectType t = BukkitCompat.resolvePotionEffect(k);
                if (t != null)
                    badTypes.add(t);
            }
            if (badTypes.isEmpty()) {
                PotionEffectType t = BukkitCompat.resolvePotionEffect("slowness");
                if (t != null)
                    badTypes.add(t);
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
        pausedByDisconnect = false;
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
        if (taskMaxDurationTask != null) {
            taskMaxDurationTask.cancel();
            taskMaxDurationTask = null;
        }
        if (plugin.isTaskCompetitionMode() && taskGameDeadlineMs > 0L) {
            taskGameRemainingMs = Math.max(0L, taskGameDeadlineMs - System.currentTimeMillis());
            taskGameDeadlineMs = 0L;
        }
        // Apply a brief heavy slowness to current active runner as a visual pause cue
        Player ar = getActiveRunner();
        if (plugin.usesSharedRunnerControl() && ar != null) {
            try {
                ar.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 10, false, false));
            } catch (Throwable ignored) {
            }
        }
        Player ah = getActiveHunter();
        if (plugin.usesSharedSecondBody() && ah != null) {
            try {
                ah.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 10, false, false));
            } catch (Throwable ignored) {
            }
        }
        // Broadcast pause
        if (plugin.getConfigManager().isBroadcastGameEvents()) {
            Msg.broadcast("§e§lGame paused by admin.");
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
        pausedByDisconnect = false;
        reselectSessionLeader();
        if (plugin.usesSharedRunnerControl()) {
            scheduleNextSwap();
            startActionBarUpdates();
            startTitleUpdates();
            startCageEnforcement();
        }
        if (plugin.usesSharedSecondBody()) {
            scheduleNextHunterSwap();
        }
        if (plugin.isTaskCompetitionMode()) {
            long resumeDurationMs = taskGameRemainingMs > 0L ? taskGameRemainingMs : getConfiguredTaskDurationMs();
            scheduleTaskMaxDurationWatcher(resumeDurationMs);
        }
        if (plugin.getCurrentMode() == com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.DREAM
                && !plugin.usesSharedHunterControl()) {
            scheduleNextHunterSwap();
        }
        // Broadcast resume
        if (plugin.getConfigManager().isBroadcastGameEvents()) {
            Msg.broadcast("§a§lGame resumed.");
        }
        return true;
    }

    /** Returns whether the game is currently paused */
    public boolean isGamePaused() {
        return gamePaused;
    }

    /** Shuffle the runner queue while keeping the current active runner first */
    public boolean shuffleQueue() {
        if (!plugin.usesSharedRunnerControl())
            return false;
        if (runners == null || runners.size() < 2)
            return false;
        Player current = activeRunner;
        java.util.List<Player> rest = new java.util.ArrayList<>();
        for (Player p : runners) {
            if (!p.equals(current))
                rest.add(p);
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
        java.util.LinkedHashSet<Player> unique = new java.util.LinkedHashSet<>(players);
        java.util.List<String> names = new java.util.ArrayList<>();
        for (Player p : unique)
            names.add(p.getName());
        plugin.getConfigManager().setRunnerNames(names);

        java.util.List<String> currentHunters = plugin.getConfigManager().getHunterNames();
        currentHunters.removeAll(names);
        plugin.getConfigManager().setHunterNames(currentHunters);

        this.runners = new java.util.ArrayList<>(unique);
        refreshTeamSelections();
    }

    public void setSharedRunnerSpawn(Location location) {
        if (location == null || location.getWorld() == null) {
            this.sharedRunnerSpawn = null;
            return;
        }
        Location prepared = prepareSafeSpawn(location, location.getWorld());
        if (prepared == null) {
            prepared = location.clone();
        }
        ensureChunkLoaded(prepared);
        this.sharedRunnerSpawn = prepared;
        updateSessionWorld(prepared.getWorld());
        for (Player runner : runners) {
            if (runner != null && runner.isOnline()) {
                syncRunnerRespawn(runner, prepared);
            }
        }
    }

    public Location getSharedRunnerSpawn() {
        return sharedRunnerSpawn == null ? null : sharedRunnerSpawn.clone();
    }

    public boolean isSpawnSyncInFlight() {
        return spawnSyncInFlight;
    }

    public Location resolveRunnerRespawn(Player player) {
        if (player == null) {
            return null;
        }
        if (plugin.usesSharedRunnerControl()) {
            Location candidate = prepareSafeSpawn(sharedRunnerSpawn, null);
            if (candidate != null) {
                return candidate;
            }
        }

        World sessionWorld = resolveSessionWorld(player);
        if (sessionWorld != null && plugin.getConfigManager().isKeepRunnersInSessionWorldEnabled()) {
            Location sessionSpawn = sessionWorld.getSpawnLocation();
            Location safeSessionSpawn = prepareSafeSpawn(sessionSpawn, sessionWorld);
            if (safeSessionSpawn != null) {
                return safeSessionSpawn;
            }
            return sessionSpawn;
        }

        if (plugin.getConfigManager().isForceGlobalSpawn()) {
            Location configured = plugin.getConfigManager().getSpawnLocation();
            if (configured != null && configured.getWorld() != null) {
                World playerWorld = player.getWorld();
                if (playerWorld == null || configured.getWorld().equals(playerWorld)) {
                    Location safeConfigured = prepareSafeSpawn(configured, playerWorld);
                    if (safeConfigured != null) {
                        return safeConfigured;
                    }
                }
            }
        }

        // Always use the Overworld spawn as fallback (not the player's current world)
        // This prevents respawning in the Nether when dying there with no bed set
        World overworld = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        if (overworld == null) {
            overworld = player.getWorld(); // Absolute fallback
        }
        Location worldSpawn = overworld.getSpawnLocation();
        Location safeWorldSpawn = prepareSafeSpawn(worldSpawn, overworld);
        if (safeWorldSpawn != null) {
            return safeWorldSpawn;
        }

        return worldSpawn;
    }

    public void syncRunnerRespawn(Player player, Location target) {
        if (player == null || target == null) {
            return;
        }
        boolean previous = spawnSyncInFlight;
        spawnSyncInFlight = true;
        try {
            try {
                player.setRespawnLocation(target, true);
            } catch (Throwable ignored) {
                // Fallback handled below
            }
            plugin.getConfigManager().applyRespawnLocation(player, target);
        } finally {
            spawnSyncInFlight = previous;
        }
    }

    public void scheduleRunnerRespawnEnforcement(Player player, Location target) {
        if (player == null || target == null || target.getWorld() == null) {
            return;
        }
        if (!plugin.getConfigManager().isMultiworldCompatibilityEnabled()
                || !plugin.getConfigManager().isRunnerRespawnEnforcementEnabled()) {
            return;
        }

        int firstDelay = plugin.getConfigManager().getRunnerRespawnEnforcementDelayTicks();
        int secondDelay = firstDelay + 10;
        scheduleRunnerRespawnCheck(player.getUniqueId(), target.clone(), firstDelay);
        scheduleRunnerRespawnCheck(player.getUniqueId(), target.clone(), secondDelay);
    }

    private void scheduleRunnerRespawnCheck(UUID playerId, Location target, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player online = Bukkit.getPlayer(playerId);
            if (online == null || !online.isOnline() || target.getWorld() == null) {
                return;
            }
            if (!isRunner(online)) {
                return;
            }
            Location current = online.getLocation();
            if (current != null && current.getWorld() != null
                    && current.getWorld().equals(target.getWorld())
                    && current.distanceSquared(target) <= 1.0D) {
                return;
            }
            ensureChunkLoaded(target);
            syncRunnerRespawn(online, target);
            online.teleport(target);
        }, delayTicks);
    }

    /** Replace hunters list and update config team names */
    public void setHunters(java.util.List<Player> players) {
        java.util.LinkedHashSet<Player> unique = new java.util.LinkedHashSet<>(players);
        java.util.List<String> names = new java.util.ArrayList<>();
        for (Player p : unique)
            names.add(p.getName());
        plugin.getConfigManager().setHunterNames(names);

        java.util.List<String> currentRunners = plugin.getConfigManager().getRunnerNames();
        currentRunners.removeAll(names);
        plugin.getConfigManager().setRunnerNames(currentRunners);

        this.hunters = new java.util.ArrayList<>(unique);
        refreshTeamSelections();
    }

    public String getSessionWorldName() {
        return sessionWorldName;
    }

    public World getSessionWorld() {
        if (sessionWorldName != null) {
            World persisted = Bukkit.getWorld(sessionWorldName);
            if (persisted != null) {
                return persisted;
            }
        }
        return resolveSessionWorld(activeRunner);
    }

    public String getAssignmentRestrictionReason(Player target, Team team, World referenceWorld) {
        if (target == null) {
            return "No player selected.";
        }
        if (team == Team.NONE) {
            return null;
        }
        if (team == Team.HUNTER && !huntersAllowed(plugin.getCurrentMode())) {
            return "The second body can only be assigned in Dream mode or Task Master Duo.";
        }
        if (!plugin.getConfigManager().isMultiworldCompatibilityEnabled()
                || !plugin.getConfigManager().isAssignmentRestrictedToSessionWorld()) {
            return null;
        }

        World requiredWorld = referenceWorld != null ? referenceWorld : getSessionWorld();
        World targetWorld = target.getWorld();
        if (requiredWorld == null || targetWorld == null) {
            return null;
        }
        if (!requiredWorld.equals(targetWorld)) {
            return "Assignments are restricted to world '" + requiredWorld.getName()
                    + "'. " + target.getName() + " is in '" + targetWorld.getName() + "'.";
        }
        return null;
    }

    public void updateSessionWorldFromPlayer(Player player) {
        if (player == null || !plugin.getConfigManager().isMultiworldCompatibilityEnabled()
                || !plugin.getConfigManager().isSessionWorldUpdatesEnabled()) {
            return;
        }
        World world = player.getWorld();
        if (world != null && world.getEnvironment() == World.Environment.NORMAL) {
            updateSessionWorld(world);
        }
    }

    private void initializeSessionWorld() {
        if (!plugin.getConfigManager().isMultiworldCompatibilityEnabled()) {
            sessionWorldName = null;
            return;
        }
        World sessionWorld = resolveSessionWorld(activeRunner);
        if (sessionWorld != null) {
            sessionWorldName = sessionWorld.getName();
        }
    }

    public void establishSessionWorldFromAssignment(Player target, World referenceWorld) {
        if (!plugin.getConfigManager().isMultiworldCompatibilityEnabled()) {
            return;
        }
        if (getSessionWorld() != null) {
            return;
        }
        World selected = referenceWorld;
        if (selected == null && target != null) {
            selected = resolvePreferredNormalWorld(target);
        }
        updateSessionWorld(selected);
    }

    private void updateSessionWorld(World world) {
        if (world == null || !plugin.getConfigManager().isMultiworldCompatibilityEnabled()) {
            return;
        }
        if (world.getEnvironment() != World.Environment.NORMAL) {
            return;
        }
        sessionWorldName = world.getName();
    }

    private World resolveSessionWorld(Player player) {
        if (!plugin.getConfigManager().isMultiworldCompatibilityEnabled()) {
            return null;
        }
        if (sharedRunnerSpawn != null && sharedRunnerSpawn.getWorld() != null
                && sharedRunnerSpawn.getWorld().getEnvironment() == World.Environment.NORMAL) {
            return sharedRunnerSpawn.getWorld();
        }
        if (sessionWorldName != null) {
            World persisted = Bukkit.getWorld(sessionWorldName);
            if (persisted != null) {
                return persisted;
            }
        }
        World fromPlayer = resolvePreferredNormalWorld(player);
        if (fromPlayer != null) {
            sessionWorldName = fromPlayer.getName();
            return fromPlayer;
        }
        World fromActive = resolvePreferredNormalWorld(activeRunner);
        if (fromActive != null) {
            sessionWorldName = fromActive.getName();
            return fromActive;
        }
        for (Player runner : runners) {
            World runnerWorld = resolvePreferredNormalWorld(runner);
            if (runnerWorld != null) {
                sessionWorldName = runnerWorld.getName();
                return runnerWorld;
            }
        }
        for (Player hunter : hunters) {
            World hunterWorld = resolvePreferredNormalWorld(hunter);
            if (hunterWorld != null) {
                sessionWorldName = hunterWorld.getName();
                return hunterWorld;
            }
        }
        return null;
    }

    private World resolvePreferredNormalWorld(Player player) {
        if (player == null) {
            return null;
        }
        World world = player.getWorld();
        if (world != null && world.getEnvironment() == World.Environment.NORMAL) {
            return world;
        }
        return null;
    }

    /** Clear all team assignments */
    public void clearAllTeams() {
        this.runners = new java.util.ArrayList<>();
        this.hunters = new java.util.ArrayList<>();
        plugin.getConfigManager().setRunnerNames(java.util.Collections.emptyList());
        plugin.getConfigManager().setHunterNames(java.util.Collections.emptyList());
        refreshTeamSelections();
    }

    /**
     * Assign a single player to the requested team (removing them from the other).
     */
    public boolean assignPlayerToTeam(Player target, Team team) {
        return assignPlayerToTeam(target, team, null);
    }

    public boolean assignPlayerToTeam(Player target, Team team, World referenceWorld) {
        if (target == null)
            return false;

        String restriction = getAssignmentRestrictionReason(target, team, referenceWorld);
        if (restriction != null) {
            return false;
        }

        java.util.List<Player> newRunners = new java.util.ArrayList<>(runners);
        java.util.List<Player> newHunters = new java.util.ArrayList<>(hunters);
        boolean changed = false;

        if (newRunners.remove(target))
            changed = true;
        if (newHunters.remove(target))
            changed = true;

        if (team == Team.RUNNER) {
            if (!newRunners.contains(target)) {
                newRunners.add(target);
                changed = true;
            }
        } else if (team == Team.HUNTER) {
            if (!huntersAllowed(plugin.getCurrentMode())) {
                return false;
            }
            if (!newHunters.contains(target)) {
                newHunters.add(target);
                changed = true;
            }
        }

        if (!changed) {
            return false;
        }

        if (team == Team.RUNNER) {
            establishSessionWorldFromAssignment(target, referenceWorld);
        }

        setRunners(newRunners);
        setHunters(newHunters);
        return true;
    }

    private boolean huntersAllowed(SpeedrunnerSwap.SwapMode mode) {
        return mode == SpeedrunnerSwap.SwapMode.DREAM || mode == SpeedrunnerSwap.SwapMode.TASK_DUEL;
    }

    private boolean huntersRequired(SpeedrunnerSwap.SwapMode mode) {
        return mode == SpeedrunnerSwap.SwapMode.DREAM || mode == SpeedrunnerSwap.SwapMode.TASK_DUEL;
    }

    private int minimumRequiredRunners(SpeedrunnerSwap.SwapMode mode) {
        return mode == SpeedrunnerSwap.SwapMode.TASK_RACE ? 2 : 1;
    }

    private void reselectSessionLeader() {
        if (!plugin.isParallelTaskMode()) {
            return;
        }
        if (activeRunner != null && activeRunner.isOnline() && isRunner(activeRunner)) {
            return;
        }
        for (Player runner : runners) {
            if (runner != null && runner.isOnline()) {
                activeRunner = runner;
                activeRunnerIndex = Math.max(0, runners.indexOf(runner));
                return;
            }
        }
        activeRunner = null;
        activeRunnerIndex = 0;
    }

    private void refreshTeamSelections() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            try {
                PlayerState state = getPlayerState(online);
                if (state == null)
                    continue;
                if (isRunner(online)) {
                    state.setSelectedTeam(Team.RUNNER);
                } else if (isHunter(online)) {
                    state.setSelectedTeam(Team.HUNTER);
                } else {
                    state.setSelectedTeam(Team.NONE);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private void updateTitles() {
        if (!plugin.usesSharedRunnerControl() && !plugin.usesSharedSecondBody()) {
            return;
        }
        int timeLeft = getTimeUntilNextSwap();
        int hunterTimeLeft = getTimeUntilNextHunterSwap();
        Player current = activeRunner;
        Player currentHunter = activeHunter;
        boolean isSneak = current != null && current.isSneaking();
        boolean isSprint = current != null && current.isSprinting();

        String waitingVis = plugin.getConfigManager().getWaitingTimerVisibility();
        boolean waitingAlways = "always".equalsIgnoreCase(waitingVis);
        boolean waitingLast10 = "last_10".equalsIgnoreCase(waitingVis);

        for (Player p : Bukkit.getOnlinePlayers()) {
            boolean runnerGroup = isRunner(p);
            boolean secondBodyGroup = plugin.usesSharedSecondBody() && isHunter(p);
            if (!runnerGroup && !secondBodyGroup)
                continue;

            boolean isActive = runnerGroup ? p.equals(current) : p.equals(currentHunter);
            boolean isCaged = cagedPlayers.contains(p.getUniqueId());
            int displayTime = runnerGroup ? timeLeft : hunterTimeLeft;
            String label = runnerGroup ? "Swap in" : "Second body in";

            // For caged players, show large aesthetic title
            if (isCaged && !isActive) {
                String t = String.format("§6§l%s: %ds", label, Math.max(0, displayTime));
                String sub = String.format("§eSneaking: %s  §7|  §eRunning: %s", isSneak ? "Yes" : "No",
                        isSprint ? "Yes" : "No");
                BukkitCompat.showTitle(p, t, sub, 0, 20, 0);
                continue;
            }

            // For non-caged waiting runners, show smaller title
            boolean shouldShow = !isActive && !isCaged && (waitingAlways || (waitingLast10 && displayTime <= 10));
            if (!shouldShow)
                continue;

            String t = String.format("§6§l%s: %ds", label, Math.max(0, displayTime));
            String sub = String.format("§eSneaking: %s  §7|  §eRunning: %s", isSneak ? "Yes" : "No",
                    isSprint ? "Yes" : "No");
            BukkitCompat.showTitle(p, t, sub, 0, 12, 0);
        }
    }

    private void createOrEnsureSharedCage(World world) {
        World requestedWorld = world;
        world = resolveCageWorld(world);
        if (world == null) {
            return;
        }
        if (requestedWorld != null && !requestedWorld.equals(world)) {
            clearSharedCageForWorld(requestedWorld);
        }
        Location center = resolveSharedCageCenter(world);
        int cx = center.getBlockX();
        int y = center.getBlockY();
        int cz = center.getBlockZ();
        Location existing = sharedCageCenters.get(world);
        if (existing != null && Math.abs(existing.getX() - center.getX()) < 0.1
                && Math.abs(existing.getY() - center.getY()) < 0.1 && Math.abs(existing.getZ() - center.getZ()) < 0.1) {
            return;
        }
        // Cleanup old cage in this world
        java.util.List<org.bukkit.block.BlockState> old = sharedCageBlocks.remove(world);
        if (old != null) {
            for (org.bukkit.block.BlockState s : old) {
                try {
                    s.update(true, false);
                } catch (Exception ignored) {
                }
            }
        }
        try {
            center.getChunk().load(true);
        } catch (Throwable ignored) {
        }
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

    private void clearSharedCageForWorld(World world) {
        if (world == null) {
            return;
        }
        java.util.List<org.bukkit.block.BlockState> old = sharedCageBlocks.remove(world);
        if (old != null) {
            for (org.bukkit.block.BlockState s : old) {
                try {
                    s.update(true, false);
                } catch (Exception ignored) {
                }
            }
        }
        sharedCageCenters.remove(world);
    }

    private World resolveCageWorld(World currentWorld) {
        if (currentWorld == null) {
            if (!Bukkit.getWorlds().isEmpty()) {
                return Bukkit.getWorlds().get(0);
            }
            return null;
        }
        if (currentWorld.getEnvironment() != World.Environment.THE_END) {
            return currentWorld;
        }
        if (!plugin.getConfigManager().isAvoidCageInEndEnabled()) {
            return currentWorld;
        }
        Location limbo = plugin.getConfigManager().getLimboLocation();
        if (limbo != null && limbo.getWorld() != null
                && limbo.getWorld().getEnvironment() != World.Environment.THE_END) {
            return limbo.getWorld();
        }
        for (World candidate : Bukkit.getWorlds()) {
            if (candidate != null && candidate.getEnvironment() != World.Environment.THE_END) {
                return candidate;
            }
        }
        if (!Bukkit.getWorlds().isEmpty()) {
            return Bukkit.getWorlds().get(0);
        }
        return currentWorld;
    }

    private Location resolveSharedCageCenter(World world) {
        Location base = plugin.getConfigManager().getLimboLocation();
        int cx = (int) Math.round(base.getX());
        int cz = (int) Math.round(base.getZ());
        int y = Math.max(world.getMinHeight() + 5, world.getMaxHeight() - 10);

        if (world.getEnvironment() == World.Environment.THE_END) {
            int minDistance = plugin.getConfigManager().getEndCageMinDistanceFromOrigin();
            long distanceSquared = (long) cx * cx + (long) cz * cz;
            long minDistanceSquared = (long) minDistance * minDistance;
            if (distanceSquared < minDistanceSquared) {
                int safeCoord = plugin.getConfigManager().getEndCageSafeCoordinate();
                cx = cx < 0 ? -safeCoord : safeCoord;
                cz = cz < 0 ? -safeCoord : safeCoord;
            }
        }

        Location center = new Location(world, cx + 0.5, y, cz + 0.5);
        return pushEndCageAwayFromDragon(center);
    }

    private Location pushEndCageAwayFromDragon(Location center) {
        if (center == null || center.getWorld() == null) {
            return center;
        }
        World world = center.getWorld();
        if (world.getEnvironment() != World.Environment.THE_END) {
            return center;
        }

        double minDistance = plugin.getConfigManager().getEndCageMinDistanceFromDragon();
        if (minDistance <= 0) {
            return center;
        }
        double minDistanceSq = minDistance * minDistance;
        double x = center.getX();
        double z = center.getZ();

        for (EnderDragon dragon : world.getEntitiesByClass(EnderDragon.class)) {
            if (dragon == null || dragon.isDead()) {
                continue;
            }
            Location dragonLoc = dragon.getLocation();
            double dx = x - dragonLoc.getX();
            double dz = z - dragonLoc.getZ();
            double horizontalSq = dx * dx + dz * dz;
            if (horizontalSq >= minDistanceSq) {
                continue;
            }
            double length = Math.sqrt(horizontalSq);
            if (length < 0.001D) {
                dx = x >= 0 ? 1.0D : -1.0D;
                dz = z >= 0 ? 1.0D : -1.0D;
                length = Math.sqrt(2.0D);
            }
            double scale = minDistance / length;
            x = dragonLoc.getX() + dx * scale;
            z = dragonLoc.getZ() + dz * scale;
        }

        return new Location(world, Math.round(x) + 0.5, center.getY(), Math.round(z) + 0.5);
    }

    private void teleportToSharedCage(Player p) {
        if (p == null || !p.isOnline())
            return;
        World cageWorld = resolveCageWorld(p.getWorld());
        createOrEnsureSharedCage(cageWorld);
        org.bukkit.Location center = sharedCageCenters.get(cageWorld);
        if (center != null) {
            // Teleport player to the center of the cage floor
            p.teleport(center);
            cagedPlayers.add(p.getUniqueId());
            try {
                p.setAllowFlight(true);
            } catch (Exception ignored) {
            }
            try {
                p.setFlying(false);
            } catch (Exception ignored) {
            }
        }
    }

    private void cleanupAllCages() {
        for (java.util.Map.Entry<org.bukkit.World, java.util.List<org.bukkit.block.BlockState>> e : sharedCageBlocks
                .entrySet()) {
            java.util.List<org.bukkit.block.BlockState> list = e.getValue();
            if (list != null)
                for (org.bukkit.block.BlockState s : list) {
                    try {
                        s.update(true, false);
                    } catch (Exception ignored) {
                    }
                }
        }
        sharedCageBlocks.clear();
        sharedCageCenters.clear();
        cagedPlayers.clear();
    }

    private void startCageEnforcement() {
        if (cageTask != null) {
            cageTask.cancel();
            cageTask = null;
        }
        cageTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!gameRunning || gamePaused)
                return;
            if (!"CAGE".equalsIgnoreCase(plugin.getConfigManager().getFreezeMode()))
                return;
            // Ensure a cage exists in each runner's current world and enforce
            for (Player r : runners) {
                if (r.equals(activeRunner))
                    continue;
                if (!r.isOnline())
                    continue;
                World cageWorld = resolveCageWorld(r.getWorld());
                createOrEnsureSharedCage(cageWorld);
                teleportToSharedCage(r);
                org.bukkit.Location center = sharedCageCenters.get(cageWorld);
                if (center == null)
                    continue;
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
                    try {
                        r.setAllowFlight(true);
                    } catch (Exception ignored) {
                    }
                    try {
                        r.setFlying(false);
                    } catch (Exception ignored) {
                    }
                }
            }
        }, 0L, 5L); // enforce every 0.25s
    }

    public boolean areBothPlayersInSharedCage(Player a, Player b) {
        if (a == null || b == null)
            return false;
        if (!cagedPlayers.contains(a.getUniqueId()) || !cagedPlayers.contains(b.getUniqueId()))
            return false;
        return a.getWorld().equals(b.getWorld()) && sharedCageCenters.containsKey(a.getWorld());
    }

    private void synchronizeRejoinedPlayer(Player player) {
        if (player == null) {
            return;
        }

        UUID uuid = player.getUniqueId();
        replaceTeamReferenceByUuid(runners, uuid, player);
        replaceTeamReferenceByUuid(hunters, uuid, player);

        String playerName = player.getName();
        if (plugin.getConfigManager().getRunnerNames().contains(playerName)
                && !containsPlayerByUuid(runners, uuid)) {
            runners.add(player);
        }
        if (plugin.getConfigManager().getHunterNames().contains(playerName)
                && !containsPlayerByUuid(hunters, uuid)) {
            hunters.add(player);
        }

        if (activeRunner != null && uuid.equals(activeRunner.getUniqueId())) {
            activeRunner = player;
            cagedPlayers.remove(uuid);
        }
        if (activeHunter != null && uuid.equals(activeHunter.getUniqueId())) {
            activeHunter = player;
            cagedPlayers.remove(uuid);
        }

        PlayerState state = getPlayerState(player);
        if (state != null) {
            if (isRunner(player)) {
                state.setSelectedTeam(Team.RUNNER);
            } else if (isHunter(player)) {
                state.setSelectedTeam(Team.HUNTER);
            }
        }
    }

    private boolean canResumeAfterDisconnectPause() {
        if (!gameRunning || !gamePaused) {
            return false;
        }
        if (!hasOnlinePlayer(runners)) {
            return false;
        }
        if (plugin.isTaskCompetitionMode()) {
            if (plugin.isDualBodyTaskMode() && !hasOnlinePlayer(hunters)) {
                return false;
            }
            return runnerDisconnectAt.isEmpty();
        }
        if (huntersRequired(plugin.getCurrentMode())) {
            return hasOnlinePlayer(hunters);
        }
        return true;
    }

    private boolean hasOnlinePlayer(List<Player> players) {
        for (Player player : players) {
            if (player != null && player.isOnline()) {
                return true;
            }
        }
        return false;
    }

    private boolean containsPlayerByUuid(List<Player> players, UUID uuid) {
        return indexOfPlayerByUuid(players, uuid) >= 0;
    }

    private int indexOfPlayerByUuid(List<Player> players, UUID uuid) {
        if (uuid == null) {
            return -1;
        }
        for (int i = 0; i < players.size(); i++) {
            Player candidate = players.get(i);
            if (candidate != null && uuid.equals(candidate.getUniqueId())) {
                return i;
            }
        }
        return -1;
    }

    private void replaceTeamReferenceByUuid(List<Player> team, UUID uuid, Player updatedPlayer) {
        int index = indexOfPlayerByUuid(team, uuid);
        if (index >= 0) {
            team.set(index, updatedPlayer);
        }
    }
}
