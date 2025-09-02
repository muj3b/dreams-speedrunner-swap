package com.example.speedrunnerswap.game;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import com.example.speedrunnerswap.models.PlayerState;
import com.example.speedrunnerswap.utils.PlayerStateUtil;
import com.example.speedrunnerswap.utils.SafeLocationFinder;
import org.bukkit.Location;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.Server;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.ItemStack;

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
    private BukkitTask hunterSwapTask;
    private BukkitTask actionBarTask;
    private BukkitTask freezeCheckTask;
    private long nextSwapTime;
    private final Map<UUID, PlayerState> playerStates;
    private final LastStandManager lastStandManager;
    private final KitManager kitManager;
    
    public GameManager(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
        this.gameRunning = false;
        this.gamePaused = false;
        this.activeRunnerIndex = 0;
        this.runners = new ArrayList<>();
        this.hunters = new ArrayList<>();
        this.playerStates = new HashMap<>();
        this.lastStandManager = new LastStandManager(plugin);
        this.kitManager = new KitManager(plugin);
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
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("§cGame cannot start: At least one runner and one hunter are required."), Server.BROADCAST_CHANNEL_USERS);
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
            Bukkit.getPlayer(Bukkit.getOnlinePlayers().iterator().next().getUniqueId()).sendMessage(net.kyori.adventure.text.Component.text("§cYou cannot start the game alone. At least two players are required."));
            return false;
        }
        
        // Initialize game state
        gameRunning = true;
        gamePaused = false;
        activeRunnerIndex = 0;
        activeRunner = runners.get(activeRunnerIndex);
        
        // Save initial player states
        saveAllPlayerStates();

        // Apply kits if enabled
        if (plugin.getConfig().getBoolean("kits.enabled", true)) {
            for (Player player : runners) {
                kitManager.applyRunnerKit(player);
            }
            for (Player player : hunters) {
                kitManager.applyHunterKit(player);
            }
        }
        
        // Initialize last stand system if enabled
        if (plugin.getConfig().getBoolean("last_stand.enabled", true)) {
            lastStandManager.setup();
        }
        if (plugin.getConfigManager().isKitsEnabled()) {
            applyKits();
        }
        
        // Apply effects to inactive runners
        applyInactiveEffects();
        
        // Start the swap timer
        scheduleNextSwap();
        scheduleNextHunterSwap();
        
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

        // Distribute kits if enabled
        if (plugin.getConfig().getBoolean("kits.enabled", true)) {
            for (Player runner : runners) {
                plugin.getKitManager().giveKit(runner, "runner");
            }
            for (Player hunter : hunters) {
                plugin.getKitManager().giveKit(hunter, "hunter");
            }
        }

        // Start stats tracking
        if (plugin.getConfig().getBoolean("stats.enabled", true)) {
            plugin.getStatsManager().startTracking();
        }

        // Start world border shrinking
        if (plugin.getConfig().getBoolean("world_border.enabled", true)) {
            plugin.getWorldBorderManager().startBorderShrinking();
        }

        // Assign initial bounty
        if (plugin.getConfig().getBoolean("bounty.enabled", true)) {
            plugin.getBountyManager().assignNewBounty();
        }

        // Schedule sudden death
        if (plugin.getConfig().getBoolean("sudden_death.enabled", true)) {
            plugin.getSuddenDeathManager().scheduleSuddenDeath();
        }

        // Start compass tracking with jamming support
        if (plugin.getConfig().getBoolean("tracker.compass_jamming.enabled", true)) {
            plugin.getCompassManager().startTracking();
        }
        
        // Broadcast game start if enabled
        if (plugin.getConfigManager().isBroadcastGameEvents()) {
            // Game title and welcome
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("\n=== Speedrunner Swap Game Started ===")
                .color(net.kyori.adventure.text.format.NamedTextColor.GREEN).decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("Welcome to Speedrunner Swap!")
                .color(net.kyori.adventure.text.format.NamedTextColor.AQUA).decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
            
            // Core objective
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("\nObjective:")
                .color(net.kyori.adventure.text.format.NamedTextColor.YELLOW).decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("• Runners must defeat the Ender Dragon to win")
                .color(net.kyori.adventure.text.format.NamedTextColor.WHITE));
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("• Hunters must stop the runners before they succeed")
                .color(net.kyori.adventure.text.format.NamedTextColor.WHITE));
            
            // Team roles
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("\nTeam Roles:")
                .color(net.kyori.adventure.text.format.NamedTextColor.YELLOW).decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("Runners:")
                .color(net.kyori.adventure.text.format.NamedTextColor.GREEN).decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("• Work together as a team to complete the game")
                .color(net.kyori.adventure.text.format.NamedTextColor.WHITE));
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("• Only the active runner can move and interact")
                .color(net.kyori.adventure.text.format.NamedTextColor.WHITE));
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("• Inactive runners are frozen in place")
                .color(net.kyori.adventure.text.format.NamedTextColor.WHITE));
            
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("\nHunters:")
                .color(net.kyori.adventure.text.format.NamedTextColor.RED).decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("• Track and eliminate runners before they reach The End")
                .color(net.kyori.adventure.text.format.NamedTextColor.WHITE));
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("• Use your tracking compass to locate the active runner")
                .color(net.kyori.adventure.text.format.NamedTextColor.WHITE));
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("• Work together to guard important locations")
                .color(net.kyori.adventure.text.format.NamedTextColor.WHITE));
            
            // Game mechanics
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("\nKey Game Mechanics:")
                .color(net.kyori.adventure.text.format.NamedTextColor.YELLOW).decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("• Active runner swaps every " + plugin.getConfigManager().getSwapInterval() + " seconds")
                .color(net.kyori.adventure.text.format.NamedTextColor.WHITE));
            if (plugin.getConfigManager().isTrackerEnabled()) {
                Bukkit.broadcast(net.kyori.adventure.text.Component.text("• Hunters receive a compass pointing to the active runner")
                    .color(net.kyori.adventure.text.format.NamedTextColor.WHITE));
            }
            if (plugin.getConfigManager().isFreezeMechanicEnabled()) {
                Bukkit.broadcast(net.kyori.adventure.text.Component.text("• Inactive runners cannot move or interact with the world")
                    .color(net.kyori.adventure.text.format.NamedTextColor.WHITE));
            }
            if (plugin.getConfigManager().isSafeSwapEnabled()) {
                Bukkit.broadcast(net.kyori.adventure.text.Component.text("§f• Safe swap system prevents unfair deaths during swaps"), Server.BROADCAST_CHANNEL_USERS);
            }
            
            // Victory conditions
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("\n§e§lVictory Conditions:"), Server.BROADCAST_CHANNEL_USERS);
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("§a• Runners Win: §fDefeat the Ender Dragon"), Server.BROADCAST_CHANNEL_USERS);
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("§c• Hunters Win: §fEliminate all runners"), Server.BROADCAST_CHANNEL_USERS);
            
            // Current game status
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("\n§e§lCurrent Game Status:"), Server.BROADCAST_CHANNEL_USERS);
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("§f• Active Runner: §a" + activeRunner.getName()), Server.BROADCAST_CHANNEL_USERS);
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("§f• Total Runners: §a" + runners.size()), Server.BROADCAST_CHANNEL_USERS);
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("§f• Total Hunters: §c" + hunters.size()), Server.BROADCAST_CHANNEL_USERS);
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("§f• Next Swap: §b" + plugin.getConfigManager().getSwapInterval() + " seconds"), Server.BROADCAST_CHANNEL_USERS);
            
            // Game commands
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("\n§e§lUseful Commands:"), Server.BROADCAST_CHANNEL_USERS);
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("§f• /swap status §7- Check game status"), Server.BROADCAST_CHANNEL_USERS);
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("§f• /swap gui §7- Open game control menu"), Server.BROADCAST_CHANNEL_USERS);
            
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("\n§a§l=== Good luck and have fun! ===\n"), Server.BROADCAST_CHANNEL_USERS);
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
        if (hunterSwapTask != null) {
            hunterSwapTask.cancel();
            hunterSwapTask = null;
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
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("§a[SpeedrunnerSwap] Game ended! " + winnerMessage), Server.BROADCAST_CHANNEL_USERS);
        }

        // Teleport all players to spawn and clear inventory/effects
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.teleport(plugin.getConfigManager().getSpawnLocation());
            p.getInventory().clear();
            for (PotionEffect effect : p.getActivePotionEffects()) {
                p.removePotionEffect(effect.getType());
            }
            p.setHealth(p.getAttribute(Attribute.MAX_HEALTH).getValue());
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
        if (hunterSwapTask != null) {
            hunterSwapTask.cancel();
            hunterSwapTask = null;
        }

        // Cancel freeze task
        if (freezeCheckTask != null) {
            freezeCheckTask.cancel();
            freezeCheckTask = null;
        }
        
        gamePaused = true;
        
        // Broadcast game pause if enabled
        if (plugin.getConfigManager().isBroadcastGameEvents()) {
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("§e[SpeedrunnerSwap] Game paused!"), Server.BROADCAST_CHANNEL_USERS);
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
        scheduleNextHunterSwap();

        // Restart freeze checking if enabled
        if (plugin.getConfigManager().isFreezeMechanicEnabled()) {
            startFreezeChecking();
        }
        
        // Broadcast game resume if enabled
        if (plugin.getConfigManager().isBroadcastGameEvents()) {
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("§a[SpeedrunnerSwap] Game resumed!"), Server.BROADCAST_CHANNEL_USERS);
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
        
        // Update active runner and transfer state from previous to new runner
        Player previousRunner = activeRunner;
        activeRunner = nextRunner;

        // Teleport the new active runner to the previous runner's location
        if (previousRunner != null && previousRunner.isOnline()) {
            activeRunner.teleport(previousRunner.getLocation());
        }

        // Transfer inventory, armor, off-hand, health, food, and experience
        if (previousRunner != null && previousRunner.isOnline()) {
            // Inventory & equipment
            ItemStack[] invContents = previousRunner.getInventory().getContents();
            ItemStack[] armorContents = previousRunner.getInventory().getArmorContents();
            ItemStack offHand = previousRunner.getInventory().getItemInOffHand();

            activeRunner.getInventory().setContents(invContents);
            activeRunner.getInventory().setArmorContents(armorContents);
            activeRunner.getInventory().setItemInOffHand(offHand);

            // Health & food
            if (activeRunner.getLocation() != null && !plugin.getConfigManager().isLocationSafe(activeRunner.getLocation())) {
                activeRunner.setHealth(Math.min(previousRunner.getHealth(), previousRunner.getAttribute(Attribute.MAX_HEALTH).getValue()));
            } else {
                plugin.getLogger().warning("Safe swap prevented: Dangerous location detected for " + activeRunner.getName());
            }
            activeRunner.setFoodLevel(previousRunner.getFoodLevel());
            activeRunner.setSaturation(previousRunner.getSaturation());

            // Experience
            activeRunner.setTotalExperience(previousRunner.getTotalExperience());
            activeRunner.setLevel(previousRunner.getLevel());
            activeRunner.setExp(previousRunner.getExp());

            // Clear previous runner inventory to avoid duplication
            previousRunner.getInventory().clear();
            previousRunner.getInventory().setArmorContents(new ItemStack[]{});
            previousRunner.getInventory().setItemInOffHand(null);
        }
        
        // Apply effects to inactive runners
        applyInactiveEffects();
        
        // Schedule next swap
        scheduleNextSwap();
        
        // Broadcast swap if enabled
        if (plugin.getConfigManager().isBroadcastsEnabled() && previousRunner != null) {
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("§6[SpeedrunnerSwap] Swapped from " + previousRunner.getName() + " to " + activeRunner.getName() + "!"), Server.BROADCAST_CHANNEL_USERS);
        }

        // Apply power-up/power-down if enabled
        if (plugin.getConfigManager().isPowerUpsEnabled()) {
            applyRandomPowerUp(activeRunner);
        }

        // Jam compasses if enabled
        if (plugin.getConfigManager().isCompassJammingEnabled()) {
            plugin.getTrackerManager().jamCompasses(plugin.getConfigManager().getCompassJamDuration());
        }
    }

    public void performHunterSwap() {
        if (!gameRunning || gamePaused || hunters.size() < 2) {
            return;
        }

        // Simple swap for now, just shuffle the list
        Collections.shuffle(hunters);
        plugin.getTrackerManager().updateAllHunterCompasses();

        if (plugin.getConfigManager().isBroadcastsEnabled()) {
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("§c[SpeedrunnerSwap] Hunters have been swapped!"), Server.BROADCAST_CHANNEL_USERS);
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
            updateActionBar();
        }, 0L, 20L); // Update every second
    }
    
    /**
     * Update the action bar for a player
     * @param player The player to update
     */
    private void updateActionBar() {
        if (!gameRunning || gamePaused) {
            return;
        }
    
        int timeLeft = getTimeUntilNextSwap();
        String timeMessage = String.format("§eTime until next swap: §c%ds", timeLeft);
    
        // Send action bar message to all online players based on their role
        for (Player player : Bukkit.getOnlinePlayers()) {
            String visibility;
            
            if (player.equals(activeRunner)) {
                visibility = plugin.getConfigManager().getRunnerTimerVisibility();
            } else if (runners.contains(player)) {
                visibility = plugin.getConfigManager().getWaitingTimerVisibility();
            } else {
                visibility = plugin.getConfigManager().getHunterTimerVisibility();
            }
    
            // Determine if we should show the timer based on visibility setting
            boolean showTimer = switch (visibility) {
                case "always" -> true;
                case "last_10" -> timeLeft <= 10;
                case "never" -> false;
                default -> false;
            };
    
            if (showTimer) {
                player.sendActionBar(net.kyori.adventure.text.Component.text(timeMessage));
            } else {
                player.sendActionBar(net.kyori.adventure.text.Component.text(""));
            }
        }
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
                
                // Ensure the active runner is visible to everyone
                for (Player viewer : Bukkit.getOnlinePlayers()) {
                    viewer.showPlayer(plugin, runner);
                }
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
                } else if (freezeMode.equalsIgnoreCase("LIMBO")) {
                    // Teleport to limbo and set to adventure mode
                    Location limboLocation = plugin.getConfigManager().getLimboLocation();
                    runner.teleport(limboLocation);
                    runner.setGameMode(GameMode.ADVENTURE);
                    runner.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, false));
                }
                
                // Hide this inactive runner from all other players (including other runners and hunters)
                for (Player viewer : Bukkit.getOnlinePlayers()) {
                    if (!viewer.equals(runner)) {
                        viewer.hidePlayer(plugin, runner);
                    }
                }
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
        plugin.getConfigManager().initializeDangerousBlocks();
        
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
                player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
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
                player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
                player.setFoodLevel(20);
            }
        } else {
            // No saved state, reset to safe defaults
            player.teleport(plugin.getConfigManager().getSpawnLocation());
            player.setGameMode(GameMode.SURVIVAL);
            player.getInventory().clear();
            player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
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
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("§a[SpeedrunnerSwap] Runners set: " + runnerNames), Server.BROADCAST_CHANNEL_USERS);
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
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("§c[SpeedrunnerSwap] Hunters set: " + hunterNames), Server.BROADCAST_CHANNEL_USERS);
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
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("§cAll hunters have been eliminated! Runners win!"), Server.BROADCAST_CHANNEL_USERS);
        } else if (runners.isEmpty() && isGameRunning()) {
            endGame(PlayerState.Team.HUNTER);
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("§cAll runners have been eliminated! Hunters win!"), Server.BROADCAST_CHANNEL_USERS);
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
                    Bukkit.broadcast(net.kyori.adventure.text.Component.text("§cThe active runner has disconnected! Game paused."), Server.BROADCAST_CHANNEL_USERS);
                } else {
                    // Force a swap to another runner if available
                    removeRunner(player);
                    if (!runners.isEmpty()) {
                        performSwap();
                        Bukkit.broadcast(net.kyori.adventure.text.Component.text("§eRunner " + player.getName() + " has disconnected. Swapping to next runner."), Server.BROADCAST_CHANNEL_USERS);
                    } else {
                        // End game if no runners left
                        endGame(PlayerState.Team.HUNTER);
                        Bukkit.broadcast(net.kyori.adventure.text.Component.text("§cAll runners have disconnected! Hunters win!"), Server.BROADCAST_CHANNEL_USERS);
                    }
                }
            } else {
                removeRunner(player);
                Bukkit.broadcast(net.kyori.adventure.text.Component.text("§cRunner " + player.getName() + " has disconnected and been removed from the game."), Server.BROADCAST_CHANNEL_USERS);
                if (runners.isEmpty() || hunters.isEmpty()) {
                    endGame(PlayerState.Team.HUNTER);
                    Bukkit.broadcast(net.kyori.adventure.text.Component.text("§cAll runners have disconnected! Hunters win!"), Server.BROADCAST_CHANNEL_USERS);
                } else {
                    Bukkit.broadcast(net.kyori.adventure.text.Component.text("§aThe game continues with the remaining runners."), Server.BROADCAST_CHANNEL_USERS);
                }
            }
        } else if (isHunter(player)) {
            removeHunter(player);
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("§cHunter " + player.getName() + " has disconnected."), Server.BROADCAST_CHANNEL_USERS);
            if (hunters.isEmpty()) {
                endGame(PlayerState.Team.RUNNER);
                Bukkit.broadcast(net.kyori.adventure.text.Component.text("§cAll hunters have disconnected! Runners win!"), Server.BROADCAST_CHANNEL_USERS);
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
                    hunter.sendMessage(net.kyori.adventure.text.Component.text("§cYou have been frozen by the runner!"));
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
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("§a[SpeedrunnerSwap] Hunters win! All runners have been eliminated!"), Server.BROADCAST_CHANNEL_USERS);
        } else if (runners.size() == 1 && plugin.getConfigManager().isLastStandEnabled()) {
            // Last runner standing, apply buffs
            Player lastRunner = runners.get(0);
            lastRunner.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, plugin.getConfigManager().getLastStandDuration(), plugin.getConfigManager().getLastStandStrengthAmplifier()));
            lastRunner.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, plugin.getConfigManager().getLastStandDuration(), plugin.getConfigManager().getLastStandSpeedAmplifier()));
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("§6[SpeedrunnerSwap] " + lastRunner.getName() + " is the last runner standing! They gain a last stand buff!"), Server.BROADCAST_CHANNEL_USERS);
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
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("§c[SpeedrunnerSwap] Game stopped!"), Server.BROADCAST_CHANNEL_USERS);
        }
    }

    public void updateTeams() {
        loadTeams();
    }

    private void applyKits() {
        for (Player runner : runners) {
            runner.getInventory().clear();
            for (String itemString : plugin.getConfigManager().getRunnerKitItems()) {
                ItemStack item = parseItemStack(itemString);
                if (item != null) {
                    runner.getInventory().addItem(item);
                }
            }
        }

        for (Player hunter : hunters) {
            hunter.getInventory().clear();
            for (String itemString : plugin.getConfigManager().getHunterKitItems()) {
                ItemStack item = parseItemStack(itemString);
                if (item != null) {
                    hunter.getInventory().addItem(item);
                }
            }
        }
    }

    private ItemStack parseItemStack(String itemString) {
        String[] parts = itemString.split(" ");
        if (parts.length < 1) {
            return null;
        }
        Material material = Material.getMaterial(parts[0].toUpperCase());
        if (material == null) {
            plugin.getLogger().warning("Invalid material in kit: " + parts[0]);
            return null;
        }
        int amount = 1;
        if (parts.length > 1) {
            try {
                amount = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid amount in kit item: " + itemString);
            }
        }
        return new ItemStack(material, amount);
    }

    private void applyRandomPowerUp(Player player) {
        // Define static effect lists
        PotionEffectType[] goodEffects = {
            PotionEffectType.SPEED,
            PotionEffectType.REGENERATION,
            PotionEffectType.RESISTANCE,
            PotionEffectType.NIGHT_VISION,
            PotionEffectType.DOLPHINS_GRACE
        };

        PotionEffectType[] badEffects = {
            PotionEffectType.SLOWNESS,
            PotionEffectType.WEAKNESS,
            PotionEffectType.HUNGER,
            PotionEffectType.DARKNESS,
            PotionEffectType.GLOWING
        };

        // Decide whether to apply a good or bad effect (50/50 chance)
        boolean isGoodEffect = ThreadLocalRandom.current().nextBoolean();
        PotionEffectType[] effectPool = isGoodEffect ? goodEffects : badEffects;
        
        PotionEffectType effectType = effectPool[ThreadLocalRandom.current().nextInt(effectPool.length)];
        
        // Apply the effect for 10-20 seconds with level 1-2
        int duration = ThreadLocalRandom.current().nextInt(10, 21) * 20; // Convert to ticks
        int amplifier = ThreadLocalRandom.current().nextInt(2); // 0 or 1 for level I or II
        
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
}