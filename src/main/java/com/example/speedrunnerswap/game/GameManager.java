package com.example.speedrunnerswap.game;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import com.example.speedrunnerswap.models.PlayerState;
import com.example.speedrunnerswap.models.Team;
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
import org.bukkit.GameMode;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;

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
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("§cGame cannot start: At least one runner and one hunter are required."), Server.BROADCAST_CHANNEL_USERS);
            return false;
        }
        
        gameRunning = true;
        gamePaused = false;
        activeRunnerIndex = 0;
        activeRunner = runners.get(activeRunnerIndex);
        
        saveAllPlayerStates();
        
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
        
        if (plugin.getConfigManager().isTrackerEnabled()) {
            plugin.getTrackerManager().startTracking();
            for (Player hunter : hunters) {
                if (hunter.isOnline()) {
                    plugin.getTrackerManager().giveTrackingCompass(hunter);
                }
            }
        }

        if (plugin.getConfigManager().isFreezeMechanicEnabled()) {
            startFreezeChecking();
        }
        
        return true;
    }
    
    public void endGame(Team winner) {
        if (!gameRunning) {
            return;
        }
        
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
        
        plugin.getTrackerManager().stopTracking();
        
        if (freezeCheckTask != null) {
            freezeCheckTask.cancel();
            freezeCheckTask = null;
        }
        
        restoreAllPlayerStates();
        
        gameRunning = false;
        gamePaused = false;
        activeRunner = null;
        
        if (plugin.getConfigManager().isBroadcastGameEvents()) {
            String winnerMessage = (winner != null) ? winner.name() + " team won!" : "Game ended!";
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("§a[SpeedrunnerSwap] Game ended! " + winnerMessage), Server.BROADCAST_CHANNEL_USERS);
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

        if (gameRunning && (runners.isEmpty() || hunters.isEmpty())) {
            endGame(null);
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
            
            player.removePotionEffect(PotionEffectType.BLINDNESS);
            player.removePotionEffect(PotionEffectType.DARKNESS);
            player.removePotionEffect(PotionEffectType.WEAKNESS);
            player.removePotionEffect(PotionEffectType.SLOW_FALLING);
            player.removePotionEffect(PotionEffectType.SLOWNESS);
            player.removePotionEffect(PotionEffectType.JUMP_BOOST);
            
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
                
                player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
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
        if (plugin.getConfigManager().isRandomizeSwap()) {
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
    
    private void updateActionBar() {
        if (!gameRunning || gamePaused) {
            return;
        }
    
        int timeLeft = getTimeUntilNextSwap();
        String timeMessage = String.format("§eTime until next swap: §c%ds", timeLeft);
    
        for (Player player : Bukkit.getOnlinePlayers()) {
            String visibility;
            
            if (player.equals(activeRunner)) {
                visibility = plugin.getConfigManager().getRunnerTimerVisibility();
            } else if (runners.contains(player)) {
                visibility = plugin.getConfigManager().getWaitingTimerVisibility();
            } else {
                visibility = plugin.getConfigManager().getHunterTimerVisibility();
            }
    
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
    
    private void applyInactiveEffects() {
        String freezeMode = plugin.getConfigManager().getFreezeMode();
        
        for (Player runner : runners) {
            if (runner.equals(activeRunner)) {
                runner.removePotionEffect(PotionEffectType.BLINDNESS);
                runner.removePotionEffect(PotionEffectType.DARKNESS);
                runner.removePotionEffect(PotionEffectType.SLOWNESS);
                runner.removePotionEffect(PotionEffectType.SLOW_FALLING);
                runner.setGameMode(GameMode.SURVIVAL);
                
                for (Player viewer : Bukkit.getOnlinePlayers()) {
                    viewer.showPlayer(plugin, runner);
                }
            } else {
                if (freezeMode.equalsIgnoreCase("EFFECTS")) {
                    runner.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, false));
                    runner.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, Integer.MAX_VALUE, 1, false, false));
                    runner.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 255, false, false));
                    runner.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, Integer.MAX_VALUE, 128, false, false));
                } else if (freezeMode.equalsIgnoreCase("SPECTATOR")) {
                    runner.setGameMode(GameMode.SPECTATOR);
                } else if (freezeMode.equalsIgnoreCase("LIMBO")) {
                    Location limboLocation = plugin.getConfigManager().getLimboLocation();
                    runner.teleport(limboLocation);
                    runner.setGameMode(GameMode.ADVENTURE);
                    runner.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, false));
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
            Entity target = activeRunner.getTargetEntity(maxDistance, false);

            if (target instanceof Player hunter && isHunter(hunter)) {
                int duration = plugin.getConfigManager().getFreezeDurationTicks();
                hunter.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 255, false, false));
                hunter.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, duration, 128, false, false));
                
                if (plugin.getConfigManager().isBroadcastGameEvents()) {
                    hunter.sendMessage(net.kyori.adventure.text.Component.text("§cYou have been frozen by the runner!"));
                }
            }
        }, 0L, interval);
    }
    
    private void performSwap() {
        if (!gameRunning || gamePaused || runners.size() < 1) {
            return;
        }
        
        if (activeRunner != null && activeRunner.isOnline()) {
            savePlayerState(activeRunner);
        }
        
        // Find next online runner
        int attempts = 0;
        // removed unused variable originalIndex
        do {
            activeRunnerIndex = (activeRunnerIndex + 1) % runners.size();
            attempts++;
            
            // Prevent infinite loop if no runners are online
            if (attempts >= runners.size()) {
                plugin.getLogger().warning("No online runners found during swap - pausing game");
                pauseGame();
                return;
            }
        } while (!runners.get(activeRunnerIndex).isOnline());
        
        Player nextRunner = runners.get(activeRunnerIndex);
        
        if (plugin.getConfigManager().isSafeSwapEnabled()) {
            Location safeLocation = SafeLocationFinder.findSafeLocation(nextRunner.getLocation(), 
                    plugin.getConfigManager().getSafeSwapHorizontalRadius(),
                    plugin.getConfigManager().getSafeSwapVerticalDistance(),
                    plugin.getConfigManager().getDangerousBlocks());
            
            if (safeLocation != null) {
                nextRunner.teleport(safeLocation);
            }
        }
        
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
        
        Player previousRunner = activeRunner;
        activeRunner = nextRunner;
        
        if (previousRunner != null && previousRunner.isOnline()) {
            activeRunner.teleport(previousRunner.getLocation());
            ItemStack[] invContents = previousRunner.getInventory().getContents();
            ItemStack[] armorContents = previousRunner.getInventory().getArmorContents();
            ItemStack offHand = previousRunner.getInventory().getItemInOffHand();
            
            activeRunner.getInventory().setContents(invContents);
            activeRunner.getInventory().setArmorContents(armorContents);
            activeRunner.getInventory().setItemInOffHand(offHand);
            
            activeRunner.setHealth(Math.min(previousRunner.getHealth(), previousRunner.getAttribute(Attribute.MAX_HEALTH).getValue()));
            activeRunner.setFoodLevel(previousRunner.getFoodLevel());
            activeRunner.setSaturation(previousRunner.getSaturation());
            
            activeRunner.setTotalExperience(previousRunner.getTotalExperience());
            activeRunner.setLevel(previousRunner.getLevel());
            activeRunner.setExp(previousRunner.getExp());
            
            previousRunner.getInventory().clear();
            previousRunner.getInventory().setArmorContents(new ItemStack[]{});
            previousRunner.getInventory().setItemInOffHand(null);
        } else {
            // Only clear/give kit if kits are enabled; otherwise leave inventory unchanged
            if (plugin.getConfigManager().isKitsEnabled()) {
                activeRunner.getInventory().clear();
                plugin.getKitManager().giveKit(activeRunner, "runner");
            }
        }
        
        applyInactiveEffects();
        scheduleNextSwap();
        
        if (plugin.getConfigManager().isBroadcastsEnabled() && previousRunner != null) {
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("§6[SpeedrunnerSwap] Swapped from " + previousRunner.getName() + " to " + activeRunner.getName() + "!"), Server.BROADCAST_CHANNEL_USERS);
        }

        if (plugin.getConfigManager().isPowerUpsEnabled()) {
            applyRandomPowerUp(activeRunner);
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
            goodTypes = java.util.Arrays.asList(
                PotionEffectType.SPEED,
                PotionEffectType.REGENERATION,
                PotionEffectType.RESISTANCE,
                PotionEffectType.NIGHT_VISION,
                PotionEffectType.DOLPHINS_GRACE
            );
        }
        if (badTypes.isEmpty()) {
            badTypes = java.util.Arrays.asList(
                PotionEffectType.SLOWNESS,
                PotionEffectType.WEAKNESS,
                PotionEffectType.HUNGER,
                PotionEffectType.DARKNESS,
                PotionEffectType.GLOWING
            );
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
        if (id == null) return null;
        String key = id.toLowerCase(java.util.Locale.ROOT);

        // Handle common legacy aliases -> modern keys
        key = switch (key) {
            case "increase_damage" -> "strength";
            case "damage_resistance" -> "resistance";
            case "slow" -> "slowness";
            case "jump" -> "jump_boost";
            case "slow_digging" -> "mining_fatigue";
            case "confusion" -> "nausea";
            default -> key;
        };

        PotionEffectType type = Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft(key));
        return type;
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
        return true;
    }

    /** Returns whether the game is currently paused */
    public boolean isGamePaused() {
        return gamePaused;
    }

    /** Replace runners list and update config team names */
    public void setRunners(java.util.List<Player> players) {
        // Clear existing names in config
        for (String name : new java.util.ArrayList<>(plugin.getConfigManager().getRunnerNames())) {
            Player p = Bukkit.getPlayerExact(name);
            if (p != null) plugin.getConfigManager().removeRunner(p);
        }
        // Add all provided
        for (Player p : players) plugin.getConfigManager().addRunner(p);
        plugin.getConfigManager().saveConfig();
        // Update runtime list
        this.runners = new java.util.ArrayList<>(players);
    }

    /** Replace hunters list and update config team names */
    public void setHunters(java.util.List<Player> players) {
        for (String name : new java.util.ArrayList<>(plugin.getConfigManager().getHunterNames())) {
            Player p = Bukkit.getPlayerExact(name);
            if (p != null) plugin.getConfigManager().removeHunter(p);
        }
        for (Player p : players) plugin.getConfigManager().addHunter(p);
        plugin.getConfigManager().saveConfig();
        this.hunters = new java.util.ArrayList<>(players);
    }
}
