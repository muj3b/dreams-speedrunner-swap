package com.example.speedrunnerswap.config;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConfigManager {
    
    private final SpeedrunnerSwap plugin;
    private FileConfiguration config;
    private List<String> runnerNames;
    private List<String> hunterNames;
    private Set<Material> dangerousBlocks;
    
    public ConfigManager(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    /**
     * Load or reload the configuration
     */
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
        
        // Load team lists
        runnerNames = config.getStringList("teams.runners");
        hunterNames = config.getStringList("teams.hunters");
        
        // Load dangerous blocks
        dangerousBlocks = new HashSet<>();
        for (String blockName : config.getStringList("safe_swap.dangerous_blocks")) {
            try {
                Material material = Material.valueOf(blockName);
                dangerousBlocks.add(material);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material in dangerous_blocks: " + blockName);
            }
        }
    }
    
    /**
     * Save the configuration
     */
    public void saveConfig() {
        // Update team lists in config
        config.set("teams.runners", runnerNames);
        config.set("teams.hunters", hunterNames);
        
        plugin.saveConfig();
    }
    
    /**
     * Add a player to the runners list
     * @param player The player to add
     */
    public void addRunner(Player player) {
        String name = player.getName();
        if (!runnerNames.contains(name)) {
            runnerNames.add(name);
            // Remove from hunters if present
            hunterNames.remove(name);
        }
    }
    
    /**
     * Remove a player from the runners list
     * @param player The player to remove
     */
    public void removeRunner(Player player) {
        runnerNames.remove(player.getName());
    }
    
    /**
     * Add a player to the hunters list
     * @param player The player to add
     */
    public void addHunter(Player player) {
        String name = player.getName();
        if (!hunterNames.contains(name)) {
            hunterNames.add(name);
            // Remove from runners if present
            runnerNames.remove(name);
        }
    }
    
    /**
     * Remove a player from the hunters list
     * @param player The player to remove
     */
    public void removeHunter(Player player) {
        hunterNames.remove(player.getName());
    }
    
    /**
     * Get the list of runner names
     * @return The list of runner names
     */
    public List<String> getRunnerNames() {
        return new ArrayList<>(runnerNames);
    }
    
    /**
     * Get the list of hunter names
     * @return The list of hunter names
     */
    public List<String> getHunterNames() {
        return new ArrayList<>(hunterNames);
    }
    
    /**
     * Check if a player is a runner
     * @param player The player to check
     * @return True if the player is a runner
     */
    public boolean isRunner(Player player) {
        return runnerNames.contains(player.getName());
    }
    
    /**
     * Check if a player is a hunter
     * @param player The player to check
     * @return True if the player is a hunter
     */
    public boolean isHunter(Player player) {
        return hunterNames.contains(player.getName());
    }
    
    /**
     * Get whether the swap system should use randomized intervals
     * @return True if randomized intervals should be used
     */
    public boolean isRandomizeSwap() {
        return config.getBoolean("swap.randomize", true);
    }
    
    /**
     * Get the base swap interval in seconds
     * @return The base swap interval
     */
    public int getSwapInterval() {
        return config.getInt("swap.interval", 60);
    }
    
    /**
     * Get the minimum swap interval in seconds
     * @return The minimum swap interval
     */
    public int getMinSwapInterval() {
        return config.getInt("swap.min_interval", 30);
    }
    
    /**
     * Get the maximum swap interval in seconds
     * @return The maximum swap interval
     */
    public int getMaxSwapInterval() {
        return config.getInt("swap.max_interval", 90);
    }
    
    /**
     * Get the jitter standard deviation in seconds
     * @return The jitter standard deviation
     */
    public double getJitterStdDev() {
        return config.getDouble("swap.jitter.stddev", 15);
    }
    
    /**
     * Get whether to clamp jittered intervals within min/max limits
     * @return True if jittered intervals should be clamped
     */
    public boolean isClampJitter() {
        return config.getBoolean("swap.jitter.clamp", true);
    }
    
    /**
     * Get the grace period after swaps in ticks
     * @return The grace period in ticks
     */
    public int getGracePeriodTicks() {
        return config.getInt("swap.grace_period_ticks", 40);
    }
    
    /**
     * Get whether to pause the game when a runner disconnects
     * @return True if the game should pause on disconnect
     */
    public boolean isPauseOnDisconnect() {
        return config.getBoolean("swap.pause_on_disconnect", true);
    }

    /**
     * Get the spawn location for players after the game ends.
     * @return The spawn location.
     */
    public org.bukkit.Location getSpawnLocation() {
        double x = config.getDouble("spawn.x", 0);
        double y = config.getDouble("spawn.y", 0);
        double z = config.getDouble("spawn.z", 0);
        String worldName = config.getString("spawn.world", "world");
        org.bukkit.World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            world = plugin.getServer().getWorlds().get(0); // Fallback to default world
            plugin.getLogger().warning("Spawn world '" + worldName + "' not found. Using default world: " + world.getName());
        }
        return new org.bukkit.Location(world, x, y, z);
    }

    /**
     * Get whether the swap should be randomized.
     * @return True if the swap should be randomized.
     */
    public boolean isSwapRandomized() {
        return config.getBoolean("swap.randomized", false);
    }

    public void setRandomizeSwap(boolean randomizeSwap) {
        config.set("swap.randomize", randomizeSwap);
        plugin.saveConfig();
    }

    public void setSwapInterval(int interval) {
        config.set("swap.interval", interval);
        plugin.saveConfig();
    }



    public void setSafeSwapEnabled(boolean safeSwapEnabled) {
        config.set("safe_swap.enabled", safeSwapEnabled);
        plugin.saveConfig();
    }



    public void setBroadcastsEnabled(boolean broadcastsEnabled) {
        config.set("broadcasts.enabled", broadcastsEnabled);
        plugin.saveConfig();
    }

    public boolean isVoiceChatIntegrationEnabled() {
        return config.getBoolean("voice_chat.enabled", false);
    }

    public void setVoiceChatIntegrationEnabled(boolean enabled) {
        config.set("voice_chat.enabled", enabled);
        plugin.saveConfig();
    }

    public String getFreezeMode() {
        return config.getString("freeze.mode", "SPECTATOR");
    }

    public void setFreezeMode(String mode) {
        config.set("freeze.mode", mode);
        plugin.saveConfig();
    }

    public boolean isTrackerEnabled() {
        return config.getBoolean("tracker.enabled", true);
    }

    public void setTrackerEnabled(boolean enabled) {
        config.set("tracker.enabled", enabled);
        plugin.saveConfig();
    }

    public int getTrackerUpdateTicks() {
        return config.getInt("tracker.update_ticks", 20);
    }

    public boolean isParticleTrailEnabled() {
        return config.getBoolean("particle_trail.enabled", true);
    }

    public int getParticleSpawnInterval() {
        return config.getInt("particle_trail.spawn_interval", 5);
    }

    public String getParticleTrailType() {
        return config.getString("particle_trail.type", "DUST");
    }

    public int[] getParticleTrailColor() {
        List<Integer> rgb = config.getIntegerList("particle_trail.color");
        return new int[]{
            rgb.size() > 0 ? rgb.get(0) : 255,
            rgb.size() > 1 ? rgb.get(1) : 0,
            rgb.size() > 2 ? rgb.get(2) : 0
        };
    }

    public boolean isSafeSwapEnabled() {
        return config.getBoolean("safe_swap.enabled", true);
    }

    public String getGuiMainMenuTitle() {
        return config.getString("gui.main_menu.title", "§6SpeedrunnerSwap - Main Menu");
    }

    public String getGuiTeamSelectorTitle() {
        return config.getString("gui.team_selector.title", "§6SpeedrunnerSwap - Team Selector");
    }

    public String getGuiSettingsTitle() {
        return config.getString("gui.settings.title", "§6SpeedrunnerSwap - Settings");
    }



    /**
     * Get the horizontal scan radius for safe swaps
     * @return The horizontal scan radius
     */
    public int getSafeSwapHorizontalRadius() {
        return config.getInt("safe_swap.horizontal_radius", 5);
    }
    
    /**
     * Get the vertical scan distance for safe swaps
     * @return The vertical scan distance
     */
    public int getSafeSwapVerticalDistance() {
        return config.getInt("safe_swap.vertical_distance", 10);
    }
    
    /**
     * Get the set of dangerous block materials
     * @return The set of dangerous block materials
     */
    public Set<Material> getDangerousBlocks() {
        return dangerousBlocks;
    }
    
    /**
     * Get whether to cancel movement for inactive runners
     * @return True if movement should be canceled
     */
    public boolean isCancelMovement() {
        return config.getBoolean("cancel.movement", true);
    }
    
    /**
     * Get whether to cancel interactions for inactive runners
     * @return True if interactions should be canceled
     */
    public boolean isCancelInteractions() {
        return config.getBoolean("cancel.interactions", true);
    }
    
    public int getGuiMainMenuRows() {
        return config.getInt("gui.main_menu_rows", 3);
    }
    
    public int getGuiTeamSelectorRows() {
        return config.getInt("gui.team_selector_rows", 4);
    }

    public int getGuiSettingsRows() {
        return config.getInt("gui.settings_rows", 5);
    }

    public boolean isBroadcastGameEvents() {
        return config.getBoolean("broadcasts.game_events", true);
    }

    public boolean isBroadcastsEnabled() {
        return config.getBoolean("broadcasts.enabled", true);
    }

    public boolean isBroadcastTeamChanges() {
        return config.getBoolean("broadcasts.team_changes", true);
    }

    public boolean isMuteInactiveRunners() {
        return config.getBoolean("voice_chat.mute_inactive_runners", true);
    }

    /**
     * Get whether the freeze mechanic is enabled
     * @return True if enabled
     */
    public boolean isFreezeMechanicEnabled() {
        return config.getBoolean("freeze_mechanic.enabled", true);
    }

    /**
     * Get the freeze duration in ticks
     * @return The duration in ticks
     */
    public int getFreezeDurationTicks() {
        return config.getInt("freeze_mechanic.duration_ticks", 100);
    }

    /**
     * Get the interval to check for freezing in ticks
     * @return The check interval in ticks
     */
    public int getFreezeCheckIntervalTicks() {
        return config.getInt("freeze_mechanic.check_interval_ticks", 10);
    }

    /**
     * Get the maximum distance for freezing
     * @return The max distance
     */
    public double getFreezeMaxDistance() {
        return config.getDouble("freeze_mechanic.max_distance", 50.0);
    }

    /**
     * Get the timer visibility setting for active runners
     * @return The visibility setting ("always", "last_10", or "never")
     */
    public String getRunnerTimerVisibility() {
        return config.getString("timer_visibility.runner_visibility", "last_10");
    }

    /**
     * Get the timer visibility setting for waiting runners
     * @return The visibility setting ("always", "last_10", or "never")
     */
    public String getWaitingTimerVisibility() {
        return config.getString("timer_visibility.waiting_visibility", "always");
    }

    /**
     * Get the timer visibility setting for hunters
     * @return The visibility setting ("always", "last_10", or "never")
     */
    public String getHunterTimerVisibility() {
        return config.getString("timer_visibility.hunter_visibility", "never");
    }

    /**
     * Set the timer visibility setting for active runners
     * @param visibility The visibility setting ("always", "last_10", or "never")
     */
    public void setRunnerTimerVisibility(String visibility) {
        config.set("timer_visibility.runner_visibility", visibility);
        plugin.saveConfig();
    }

    /**
     * Set the timer visibility setting for waiting runners
     * @param visibility The visibility setting ("always", "last_10", or "never")
     */
    public void setWaitingTimerVisibility(String visibility) {
        config.set("timer_visibility.waiting_visibility", visibility);
        plugin.saveConfig();
    }

    /**
     * Set the timer visibility setting for hunters
     * @param visibility The visibility setting ("always", "last_10", or "never")
     */
    public void setHunterTimerVisibility(String visibility) {
        config.set("timer_visibility.hunter_visibility", visibility);
        plugin.saveConfig();
    }
}