package com.example.speedrunnerswap.game;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import com.example.speedrunnerswap.utils.ActionBarUtil;
import com.example.speedrunnerswap.utils.PlayerStateUtil;
import com.example.speedrunnerswap.utils.SafeLocationFinder;
import com.example.speedrunnerswap.models.PlayerState;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.entity.Entity;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class GameManager {
    
    private final SpeedrunnerSwap plugin;
    private boolean gameRunning;
    private boolean gamePaused;
    private Player activeRunner;
    private int activeRunnerIndex;
    private List<Player> runners;
    private List<Player> hunters;
    private BukkitTask swapTask;
    private BukkitTask actionBarTask;
    private BukkitTask freezeCheckTask;
    private long nextSwapTime;
    private final Map<UUID, PlayerState> playerStates;
    
    public GameManager(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
        this.gameRunning = false;
        this.gamePaused = false;
        this.activeRunnerIndex = 0;
        this.runners = new ArrayList<>();
        this.hunters = new ArrayList<>();
        this.playerStates = new HashMap<>();
    }
    
    /**
     * Start the game
     * @return True if the game was started successfully
     */
    public boolean startGame() {
        if (gameRunning) {
            return false;
        }
        
        // Load teams from config
        loadTeams();
        
        // Check if we have enough players
        // Need at least one runner and one hunter to begin a valid game
        if (runners.isEmpty() || hunters.isEmpty()) {
            Bukkit.broadcastMessage("§cGame cannot start: At least one runner and one hunter are required.");
            return false;
        }

        // Prevent a single player from being the only runner or only hunter
        if (runners.size() == 1 && hunters.isEmpty()) {
            Bukkit.getPlayer(runners.get(0).getUniqueId()).sendMessage("§cYou cannot start the game as the only runner without any hunters.");
            return false;
        }
        if (hunters.size() == 1 && runners.isEmpty()) {
            Bukkit.getPlayer(hunters.get(0).getUniqueId()).sendMessage("§cYou cannot start the game as the only hunter without any runners.");
            return false;
        }

        // If there's only one player in total, they cannot start the game alone
        if (Bukkit.getOnlinePlayers().size() == 1) {
            Bukkit.getPlayer(Bukkit.getOnlinePlayers().iterator().next().getUniqueId()).sendMessage("§cYou cannot start the game alone. At least two players are required.");
            return false;
        }
        
        // Initialize game state
        gameRunning = true;
        gamePaused = false;
        activeRunnerIndex = 0;
        activeRunner = runners.get(activeRunnerIndex);
        
        // Save initial player states
        saveAllPlayerStates();
        
        // Apply effects to inactive runners
        applyInactiveEffects();
        
        // Start the swap timer
        scheduleNextSwap();
        
        // Start the action bar timer
        startActionBarUpdates();
        
        // Start the tracker updates
        if (plugin.getConfigManager().isTrackerEnabled()) {
            plugin.getTrackerManager().startTracking();
            // Give every hunter an initial tracking compass so they can start right away
            for (Player hunter : hunters) {
                if (hunter.isOnline()) {
                    plugin.getTrackerManager().giveTrackingCompass(hunter);
                }
            }
        }

        // Start freeze checking if enabled
        if (plugin.getConfigManager().isFreezeMechanicEnabled()) {
            startFreezeChecking();
        }
        
        // Broadcast game start if enabled
        if (plugin.getConfigManager().isBroadcastGameEvents()) {
            // Game title and welcome
            Bukkit.broadcastMessage("\n§a§l=== Speedrunner Swap Game Started ===");
            Bukkit.broadcastMessage("§b§lWelcome to Speedrunner Swap!");
            
            // Core objective
            Bukkit.broadcastMessage("\n§e§lObjective:");
            Bukkit.broadcastMessage("§f• Runners must defeat the Ender Dragon to win");
            Bukkit.broadcastMessage("§f• Hunters must stop the runners before they succeed");
            
            // Team roles
            Bukkit.broadcastMessage("\n§e§lTeam Roles:");
            Bukkit.broadcastMessage("§a§lRunners:");
            Bukkit.broadcastMessage("§f• Work together as a team to complete the game");
            Bukkit.broadcastMessage("§f• Only the active runner can move and interact");
            Bukkit.broadcastMessage("§f• Inactive runners are frozen in place");
            
            Bukkit.broadcastMessage("\n§c§lHunters:");
            Bukkit.broadcastMessage("§f• Track and eliminate runners before they reach The End");
            Bukkit.broadcastMessage("§f• Use your tracking compass to locate the active runner");
            Bukkit.broadcastMessage("§f• Work together to guard important locations");
            
            // Game mechanics
            Bukkit.broadcastMessage("\n§e§lKey Game Mechanics:");
            Bukkit.broadcastMessage("§f• Active runner swaps every §b" + plugin.getConfigManager().getSwapInterval() + " seconds");
            if (plugin.getConfigManager().isTrackerEnabled()) {
                Bukkit.broadcastMessage("§f• Hunters receive a compass pointing to the active runner");
            }
            if (plugin.getConfigManager().isFreezeMechanicEnabled()) {
                Bukkit.broadcastMessage("§f• Inactive runners cannot move or interact with the world");
            }
            if (plugin.getConfigManager().isSafeSwapEnabled()) {
                Bukkit.broadcastMessage("§f• Safe swap system prevents unfair deaths during swaps");
            }
            
            // Victory conditions
            Bukkit.broadcastMessage("\n§e§lVictory Conditions:");
            Bukkit.broadcastMessage("§a• Runners Win: §fDefeat the Ender Dragon");
            Bukkit.broadcastMessage("§c• Hunters Win: §fEliminate all runners");
            
            // Current game status
            Bukkit.broadcastMessage("\n§e§lCurrent Game Status:");
            Bukkit.broadcastMessage("§f• Active Runner: §a" + activeRunner.getName());
            Bukkit.broadcastMessage("§f• Total Runners: §a" + runners.size());
            Bukkit.broadcastMessage("§f• Total Hunters: §c" + hunters.size());
            Bukkit.broadcastMessage("§f• Next Swap: §b" + plugin.getConfigManager().getSwapInterval() + " seconds");
            
            // Game commands
            Bukkit.broadcastMessage("\n§e§lUseful Commands:");
            Bukkit.broadcastMessage("§f• /swap status §7- Check game status");
            Bukkit.broadcastMessage("§f• /swap gui §7- Open game control menu");
            
            Bukkit.broadcastMessage("\n§a§l=== Good luck and have fun! ===\n");
        }
        
        return true;
    }
    
    public void endGame(PlayerState.Team winner) {
        if (!gameRunning) {
            return;
        }

        // Only end the game if all runners are eliminated (if hunters win)
        // Or if the dragon is killed (if runners win)
        if (winner == PlayerState.Team.HUNTER && runners.stream().anyMatch(Player::isOnline)) {
            // If hunters are declared winners but there are still online runners, don't end the game prematurely
            return;
        }
        
        // Cancel tasks
        if (swapTask != null) {
            swapTask.cancel();
            swapTask = null;
        }
        
        if (actionBarTask != null) {
            actionBarTask.cancel();
            actionBarTask = null;
        }
        
        // Stop tracker
        plugin.getTrackerManager().stopTracking();

        // Cancel freeze task
        if (freezeCheckTask != null) {
            freezeCheckTask.cancel();
            freezeCheckTask = null;
        }
        
        // Restore player states
        restoreAllPlayerStates();
        
        // Reset game state
        gameRunning = false;
        gamePaused = false;
        activeRunner = null;
        
        // Broadcast winner
        if (plugin.getConfigManager().isBroadcastGameEvents()) {
            String winnerMessage = (winner != null) ? winner.name() + " team won!" : "Game ended!";
            Bukkit.broadcastMessage("§a[SpeedrunnerSwap] Game ended! " + winnerMessage);
        }

        // Teleport all players to spawn and clear inventory/effects
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.teleport(plugin.getConfigManager().getSpawnLocation());
            p.getInventory().clear();
            for (PotionEffect effect : p.getActivePotionEffects()) {
                p.removePotionEffect(effect.getType());
            }
            p.setHealth(p.getMaxHealth());
            p.setFoodLevel(20);
            p.setGameMode(GameMode.SURVIVAL);
        }
    }
    
    /**
     * Pause the game
     * @return True if the game was paused successfully
     */
    public boolean pauseGame() {
        if (!gameRunning || gamePaused) {
            return false;
        }
        
        // Cancel swap task
        if (swapTask != null) {
            swapTask.cancel();
            swapTask = null;
        }

        // Cancel freeze task
        if (freezeCheckTask != null) {
            freezeCheckTask.cancel();
            freezeCheckTask = null;
        }
        
        gamePaused = true;
        
        // Broadcast game pause if enabled
        if (plugin.getConfigManager().isBroadcastGameEvents()) {
            Bukkit.broadcastMessage("§e[SpeedrunnerSwap] Game paused!");
        }
        
        return true;
    }
    
    /**
     * Resume the game
     * @return True if the game was resumed successfully
     */
    public boolean resumeGame() {
        if (!gameRunning || !gamePaused) {
            return false;
        }
        
        gamePaused = false;
        
        // Schedule next swap
        scheduleNextSwap();

        // Restart freeze checking if enabled
        if (plugin.getConfigManager().isFreezeMechanicEnabled()) {
            startFreezeChecking();
        }
        
        // Broadcast game resume if enabled
        if (plugin.getConfigManager().isBroadcastGameEvents()) {
            Bukkit.broadcastMessage("§a[SpeedrunnerSwap] Game resumed!");
        }
        
        return true;
    }
    
    /**
     * Perform a swap to the next runner
     */
    public void performSwap() {
        if (!gameRunning || gamePaused || runners.size() < 1) {
            return;
        }
        
        // Save current active runner state
        savePlayerState(activeRunner);
        
        // Find next active runner
        activeRunnerIndex = (activeRunnerIndex + 1) % runners.size();
        Player nextRunner = runners.get(activeRunnerIndex);
        
        // Check if the next runner is online
        if (!nextRunner.isOnline()) {
            // Skip to the next runner
            activeRunnerIndex = (activeRunnerIndex + 1) % runners.size();
            nextRunner = runners.get(activeRunnerIndex);
            
            // If we've gone through all runners and none are online, pause the game
            if (!nextRunner.isOnline()) {
                pauseGame();
                return;
            }
        }
        
        // Check for safe location if enabled
        if (plugin.getConfigManager().isSafeSwapEnabled()) {
            Location safeLocation = SafeLocationFinder.findSafeLocation(nextRunner.getLocation(), 
                    plugin.getConfigManager().getSafeSwapHorizontalRadius(),
                    plugin.getConfigManager().getSafeSwapVerticalDistance(),
                    plugin.getConfigManager().getDangerousBlocks());
            
            if (safeLocation != null) {
                nextRunner.teleport(safeLocation);
            }
        }
        
        // Apply grace period
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
        
        // Update active runner
        Player previousRunner = activeRunner;
        activeRunner = nextRunner;
        
        // Restore state to new active runner
        restorePlayerState(activeRunner);
        
        // Apply effects to inactive runners
        applyInactiveEffects();
        
        // Schedule next swap
        scheduleNextSwap();
        
        // Broadcast swap if enabled
        if (plugin.getConfigManager().isBroadcastsEnabled()) {
            Bukkit.broadcastMessage("§6[SpeedrunnerSwap] Swapped from " + previousRunner.getName() + " to " + activeRunner.getName() + "!");
        }
    }
    
    /**
     * Schedule the next swap
     */
    private void scheduleNextSwap() {
        if (swapTask != null) {
            swapTask.cancel();
        }
        
        // Calculate next swap time
        long intervalSeconds;
        if (plugin.getConfigManager().isRandomizeSwap()) {
            // Use randomized interval with Gaussian jitter
            double mean = plugin.getConfigManager().getSwapInterval();
            double stdDev = plugin.getConfigManager().getJitterStdDev();
            double jitteredInterval = ThreadLocalRandom.current().nextGaussian() * stdDev + mean;
            
            // Clamp within min/max if enabled
            if (plugin.getConfigManager().isClampJitter()) {
                int min = plugin.getConfigManager().getMinSwapInterval();
                int max = plugin.getConfigManager().getMaxSwapInterval();
                jitteredInterval = Math.max(min, Math.min(max, jitteredInterval));
            }
            
            intervalSeconds = Math.round(jitteredInterval);
        } else {
            // Use fixed interval
            intervalSeconds = plugin.getConfigManager().getSwapInterval();
        }
        
        // Convert to ticks (20 ticks = 1 second)
        long intervalTicks = intervalSeconds * 20;
        
        // Set next swap time
        nextSwapTime = System.currentTimeMillis() + (intervalSeconds * 1000);
        
        // Schedule the swap
        swapTask = Bukkit.getScheduler().runTaskLater(plugin, this::performSwap, intervalTicks);
    }
    
    /**
     * Start the action bar updates
     */
    private void startActionBarUpdates() {
        if (actionBarTask != null) {
            actionBarTask.cancel();
        }
        
        actionBarTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!gameRunning) {
                return;
            }
            
            // Update action bar only for participants
            List<Player> participants = new ArrayList<>();
            participants.addAll(runners);
            participants.addAll(hunters);
            for (Player player : participants) {
                if (player.isOnline()) {
                    updateActionBar(player);
                }
            }
        }, 0L, 20L); // Update every 20 ticks (1 second) for better performance
    }
    
    /**
     * Update the action bar for a player
     * @param player The player to update
     */
    private void updateActionBar(Player player) {
        if (!gameRunning) {
            return;
        }
        
        // Calculate time until next swap
        long timeUntilSwap = nextSwapTime - System.currentTimeMillis();
        int secondsUntilSwap = Math.max(0, (int) (timeUntilSwap / 1000));
        
        String message;
        
        if (gamePaused) {
            message = "§e§lGAME PAUSED";
        } else if (player.equals(activeRunner)) {
            // Active runner sees time and status
            String status = "";
            if (player.isSneaking()) {
                status = " §7[Sneaking]";
            } else if (player.isSprinting()) {
                status = " §b[Sprinting]";
            }
            
            message = "§a§lACTIVE §f| §eNext swap: §f" + secondsUntilSwap + "s" + status;
        } else if (runners.contains(player)) {
            // Inactive runner sees time only
            message = "§c§lINACTIVE §f| §eNext swap: §f" + secondsUntilSwap + "s";
        } else if (hunters.contains(player) && activeRunner != null) {
            // Hunter sees only the target player's name (coordinates hidden)
            message = "§6§lHUNTER §f| §eTarget: §f" + activeRunner.getName();
        } else {
            // Spectator sees basic info
            message = "§7§lSPECTATOR §f| §eActive: §f" + (activeRunner != null ? activeRunner.getName() : "None");
        }
        
        ActionBarUtil.sendActionBar(player, message);
    }
    
    /**
     * Apply effects to inactive runners
     */
    private void applyInactiveEffects() {
        String freezeMode = plugin.getConfigManager().getFreezeMode();
        
        for (Player runner : runners) {
            if (runner.equals(activeRunner)) {
                // Clear effects for active runner
                runner.removePotionEffect(PotionEffectType.BLINDNESS);
                runner.removePotionEffect(PotionEffectType.DARKNESS);
                runner.removePotionEffect(PotionEffectType.SLOWNESS);
                runner.removePotionEffect(PotionEffectType.SLOW_FALLING);
                
                // Set game mode to survival
                runner.setGameMode(GameMode.SURVIVAL);
            } else {
                // Apply effects to inactive runner
                if (freezeMode.equalsIgnoreCase("EFFECTS")) {
                    // Apply blindness and immobilization
                    runner.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, false));
                    runner.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, Integer.MAX_VALUE, 1, false, false));
                    runner.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 255, false, false));
                    runner.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, Integer.MAX_VALUE, 128, false, false));
                } else if (freezeMode.equalsIgnoreCase("SPECTATOR")) {
                    // Set game mode to spectator
                    runner.setGameMode(GameMode.SPECTATOR);
                }
                
                // Voice chat integration is disabled in this version
            }
        }
        
        // Voice chat integration is disabled in this version
        // if (activeRunner != null && plugin.getConfigManager().isVoiceChatEnabled() && 
        //        plugin.getConfigManager().isMuteInactiveRunners()) {
        //    plugin.getVoiceChatIntegration().unmutePlayer(activeRunner);
        // }
    }
    
    /**
     * Load teams from config
     */
    private void loadTeams() {
        runners.clear();
        hunters.clear();
        
        // Load runners
        for (String name : plugin.getConfigManager().getRunnerNames()) {
            Player player = Bukkit.getPlayerExact(name);
            if (player != null && player.isOnline()) {
                runners.add(player);
            }
        }
        
        // Load hunters
        for (String name : plugin.getConfigManager().getHunterNames()) {
            Player player = Bukkit.getPlayerExact(name);
            if (player != null && player.isOnline()) {
                hunters.add(player);
            }
        }
    }
    
    /**
     * Save the state of all players
     */
    private void saveAllPlayerStates() {
        for (Player runner : runners) {
            savePlayerState(runner);
        }
    }
    
    /**
     * Save the state of a player
     * @param player The player to save
     */
    public void savePlayerState(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        
        PlayerState state = PlayerStateUtil.capturePlayerState(player);
        playerStates.put(player.getUniqueId(), state);
    }
    
    /**
     * Restore the state of all players
     */
    private void restoreAllPlayerStates() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (playerStates.containsKey(player.getUniqueId())) {
                restorePlayerState(player);
            }
            
            // Remove effects
                player.removePotionEffect(PotionEffectType.BLINDNESS);
                player.removePotionEffect(PotionEffectType.DARKNESS);
                player.removePotionEffect(PotionEffectType.WEAKNESS);
                player.removePotionEffect(PotionEffectType.SLOW_FALLING);
                player.removePotionEffect(PotionEffectType.SLOWNESS);
                player.removePotionEffect(PotionEffectType.JUMP_BOOST);
            
            // Reset game mode
            if (player.getGameMode() == GameMode.SPECTATOR && runners.contains(player)) {
                player.setGameMode(GameMode.SURVIVAL);
            }
            
            // Voice chat integration is disabled in this version
        }
    }
    
    /**
     * Restore the state of a player
     * @param player The player to restore
     */
    private void restorePlayerState(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        
        PlayerState state = playerStates.get(player.getUniqueId());
        if (state != null) {
            try {
                // Clear current effects and inventory first
                player.getInventory().clear();
                for (PotionEffect effect : player.getActivePotionEffects()) {
                    player.removePotionEffect(effect.getType());
                }
                
                // Reset basic stats
                player.setHealth(player.getMaxHealth());
                player.setFoodLevel(20);
                player.setFireTicks(0);
                player.setFallDistance(0);
                player.setInvulnerable(false);
                
                // Apply saved state
                PlayerStateUtil.applyPlayerState(player, state);
                
                // Ensure player is not stuck
                Location loc = state.getLocation();
                if (loc != null && !loc.getBlock().getType().isSolid()) {
                    player.teleport(loc);
                } else {
                    player.teleport(plugin.getConfigManager().getSpawnLocation());
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to restore state for player " + player.getName() + ": " + e.getMessage());
                // Fallback to safe state
                player.teleport(plugin.getConfigManager().getSpawnLocation());
                player.setGameMode(GameMode.SURVIVAL);
                player.getInventory().clear();
                player.setHealth(player.getMaxHealth());
                player.setFoodLevel(20);
            }
        } else {
            // No saved state, reset to safe defaults
            player.teleport(plugin.getConfigManager().getSpawnLocation());
            player.setGameMode(GameMode.SURVIVAL);
            player.getInventory().clear();
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
        }
    }
    
    /**
     * Set the runners
     * @param players The players to set as runners
     */
    public void setRunners(List<Player> players) {
        // Update config
        for (Player player : players) {
            plugin.getConfigManager().addRunner(player);
        }
        
        // Update runners list
        runners.clear();
        runners.addAll(players);
        
        // Remove from hunters if present
        hunters.removeAll(players);
        
        // Save config
        plugin.getConfigManager().saveConfig();
        
        // Broadcast team changes if enabled
        if (plugin.getConfigManager().isBroadcastTeamChanges()) {
            String runnerNames = runners.stream()
                    .map(Player::getName)
                    .collect(Collectors.joining(", "));
            Bukkit.broadcastMessage("§a[SpeedrunnerSwap] Runners set: " + runnerNames);
        }
    }
    
    /**
     * Set the hunters
     * @param players The players to set as hunters
     */
    public void setHunters(List<Player> players) {
        // Update config
        for (Player player : players) {
            plugin.getConfigManager().addHunter(player);
        }
        
        // Update hunters list
        hunters.clear();
        hunters.addAll(players);
        
        // Remove from runners if present
        runners.removeAll(players);
        
        // Save config
        plugin.getConfigManager().saveConfig();
        
        // Broadcast team changes if enabled
        if (plugin.getConfigManager().isBroadcastTeamChanges()) {
            String hunterNames = hunters.stream()
                    .map(Player::getName)
                    .collect(Collectors.joining(", "));
            Bukkit.broadcastMessage("§c[SpeedrunnerSwap] Hunters set: " + hunterNames);
        }
    }
    
    /**
     * Check if a player is the active runner
     * @param player The player to check
     * @return True if the player is the active runner
     */
    public boolean isActiveRunner(Player player) {
        return gameRunning && activeRunner != null && activeRunner.equals(player);
    }
    
    /**
     * Check if a player is a runner
     * @param player The player to check
     * @return True if the player is a runner
     */
    public boolean isRunner(Player player) {
        return runners.contains(player);
    }
    
    /**
     * Check if a player is a hunter
     * @param player The player to check
     * @return True if the player is a hunter
     */
    public boolean isHunter(Player player) {
        return hunters.contains(player);
    }
    
    /**
     * Get the active runner
     * @return The active runner
     */
    public Player getActiveRunner() {
        return activeRunner;
    }
    
    /**
     * Get the list of runners
     * @return The list of runners
     */
    public List<Player> getRunners() {
        return new ArrayList<>(runners);
    }
    
    /**
     * Get the list of hunters
     * @return The list of hunters
     */
    public List<Player> getHunters() {
        return new ArrayList<>(hunters);
    }

    /**
     * Check if a player was part of the game (either a runner or a hunter).
     * @param player The player to check.
     * @return True if the player was in the game, false otherwise.
     */
    public boolean wasPlayerInGame(Player player) {
        return runners.contains(player) || hunters.contains(player);
    }
    
    /**
     * Check if the game is running
     * @return True if the game is running
     */
    public boolean isGameRunning() {
        return gameRunning;
    }
    
    /**
     * Check if the game is paused
     * @return True if the game is paused
     */
    public boolean isGamePaused() {
        return gamePaused;
    }
    
    /**
     * Add a player to the runners team
     * @param player The player to add
     */
    public void addRunner(Player player) {
        if (!runners.contains(player)) {
            runners.add(player);
            hunters.remove(player);
            plugin.getConfigManager().addRunner(player);
        }
    }
    
    /**
     * Add a player to the hunters team
     * @param player The player to add
     */
    public void addHunter(Player player) {
        if (!hunters.contains(player)) {
            hunters.add(player);
            runners.remove(player);
            plugin.getConfigManager().addHunter(player);
        }
    }

    /**
     * Remove a player from the hunters team
     * @param player The player to remove
     */
    public void removeHunter(Player player) {
        hunters.remove(player);
        if (hunters.isEmpty() && isGameRunning()) {
            endGame(PlayerState.Team.RUNNER);
            Bukkit.broadcastMessage("§cAll hunters have been eliminated! Runners win!");
        }
    }

    // Note: getTargetEntity requires Minecraft 1.21 or later

    
    /**
     * Get the time until the next swap in seconds
     * @return The time until the next swap
     */
    public int getTimeUntilNextSwap() {
        if (!gameRunning || gamePaused) {
            return 0;
        }
        
        long timeUntilSwap = nextSwapTime - System.currentTimeMillis();
        return Math.max(0, (int) (timeUntilSwap / 1000));
    }

    public boolean canStartGame() {
        // Check if game is already running
        if (gameRunning) {
            return false;
        }

        // Check if teams are empty
        if (runners.isEmpty() || hunters.isEmpty()) {
            return false;
        }

        // Check if there are enough online players
        long onlineRunners = runners.stream().filter(Player::isOnline).count();
        long onlineHunters = hunters.stream().filter(Player::isOnline).count();

        // Require at least one online runner and one online hunter
        if (onlineRunners == 0 || onlineHunters == 0) {
            return false;
        }

        // Prevent a single player from being both runner and hunter
        Set<UUID> runnerIds = runners.stream().map(Player::getUniqueId).collect(Collectors.toSet());
        Set<UUID> hunterIds = hunters.stream().map(Player::getUniqueId).collect(Collectors.toSet());
        if (!Collections.disjoint(runnerIds, hunterIds)) {
            return false;
        }

        // Prevent starting with only one player total
        Set<UUID> allPlayerIds = new HashSet<>(runnerIds);
        allPlayerIds.addAll(hunterIds);
        if (allPlayerIds.size() < 2) {
            return false;
        }

        return true;
    }

    public void handlePlayerQuit(Player player) {
        if (!isGameRunning()) {
            return;
        }

        if (isRunner(player)) {
            if (player.equals(getActiveRunner())) {
                if (plugin.getConfigManager().isPauseOnDisconnect()) {
                    pauseGame();
                    Bukkit.broadcastMessage("§cThe active runner has disconnected! Game paused.");
                } else {
                    // Force a swap to another runner if available
                    removeRunner(player);
                    if (!runners.isEmpty()) {
                        performSwap();
                        Bukkit.broadcastMessage("§eRunner " + player.getName() + " has disconnected. Swapping to next runner.");
                    } else {
                        // End game if no runners left
                        endGame(PlayerState.Team.HUNTER);
                        Bukkit.broadcastMessage("§cAll runners have disconnected! Hunters win!");
                    }
                }
            } else {
                removeRunner(player);
                Bukkit.broadcastMessage("§cRunner " + player.getName() + " has disconnected and been removed from the game.");
                if (runners.isEmpty()) {
                    endGame(PlayerState.Team.HUNTER);
                    Bukkit.broadcastMessage("§cAll runners have disconnected! Hunters win!");
                } else {
                    Bukkit.broadcastMessage("§aThe game continues with the remaining runners.");
                }
            }
        } else if (isHunter(player)) {
            removeHunter(player);
            Bukkit.broadcastMessage("§cHunter " + player.getName() + " has disconnected.");
            if (hunters.isEmpty()) {
                endGame(PlayerState.Team.RUNNER);
                Bukkit.broadcastMessage("§cAll hunters have disconnected! Runners win!");
            }
        }
    }

    public void setActiveRunner(Player player) {
        if (!runners.contains(player)) {
            throw new IllegalArgumentException("Player must be a runner to be set as active runner");
        }
        this.activeRunner = player;
        plugin.getTrackerManager().updateAllHunterCompasses();
    }

    private void startFreezeChecking() {
        int interval = plugin.getConfigManager().getFreezeCheckIntervalTicks();
        freezeCheckTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!gameRunning || gamePaused || activeRunner == null) {
                return;
            }

            int maxDistance = (int) plugin.getConfigManager().getFreezeMaxDistance();
            Entity target = activeRunner.getTargetEntity(maxDistance, false);

            if (target instanceof Player hunter && isHunter(hunter)) {
                int duration = plugin.getConfigManager().getFreezeDurationTicks();
                hunter.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 255, false, false));
                hunter.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, duration, 128, false, false)); // Negative jump to prevent jumping
                // Optional: Send message to hunter
                if (plugin.getConfigManager().isBroadcastGameEvents()) {
                    hunter.sendMessage("§cYou have been frozen by the runner!");
                }
            }
        }, 0L, interval);
    }

    /**
     * Remove a runner from the game and check win conditions
     * @param player The runner to remove
     */
    public void removeRunner(Player player) {
        runners.remove(player);
        if (runners.isEmpty()) {
            endGame(com.example.speedrunnerswap.models.PlayerState.Team.HUNTER);
            Bukkit.broadcastMessage("§a[SpeedrunnerSwap] Hunters win! All runners have been eliminated!");
        } else if (player.equals(activeRunner)) {
            performSwap();
        }
    }

    /**
     * Get the PlayerState for a given player, creating one if it doesn't exist.
     * @param player The player to get the state for.
     * @return The PlayerState object for the player.
     */
    public PlayerState getPlayerState(Player player) {
        return playerStates.computeIfAbsent(player.getUniqueId(), uuid -> PlayerStateUtil.capturePlayerState(player));
    }

    /**
     * Stops the game and resets all game states.
     */
    public void stopGame() {
        if (!gameRunning) {
            return;
        }

        // Cancel tasks
        if (swapTask != null) {
            swapTask.cancel();
            swapTask = null;
        }

        if (actionBarTask != null) {
            actionBarTask.cancel();
            actionBarTask = null;
        }

        // Stop tracker
        plugin.getTrackerManager().stopTracking();

        // Cancel freeze task
        if (freezeCheckTask != null) {
            freezeCheckTask.cancel();
            freezeCheckTask = null;
        }

        // Restore player states
        restoreAllPlayerStates();

        // Reset game state
        gameRunning = false;
        gamePaused = false;
        activeRunner = null;

        // Broadcast game stop if enabled
        if (plugin.getConfigManager().isBroadcastGameEvents()) {
            Bukkit.broadcastMessage("§c[SpeedrunnerSwap] Game stopped!");
        }
    }
}