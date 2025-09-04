/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.World
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.entity.Player
 */
package com.example.speedrunnerswap.config;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class ConfigManager {
    private final SpeedrunnerSwap plugin;
    private FileConfiguration config;
    private List<String> runnerNames;
    private List<String> hunterNames;
    private Set<Material> dangerousBlocks;
    private boolean powerUpsEnabled;

    public ConfigManager(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
        this.loadConfig();
    }

    public boolean isSafeSwapEnabled() {
        return this.config.getBoolean("safe_swap.enabled", false);
    }

    public void setSafeSwapEnabled(boolean enabled) {
        this.config.set("safe_swap.enabled", (Object)enabled);
        this.saveConfig();
    }

    public int getSwapInterval() {
        return this.config.getInt("swap.interval", 60);
    }

    public void setSwapInterval(int interval) {
        this.config.set("swap.interval", (Object)Math.max(30, interval));
        this.saveConfig();
    }

    public boolean isSwapRandomized() {
        return this.config.getBoolean("swap.randomize", false);
    }

    @Deprecated
    public boolean isRandomizeSwap() {
        return this.isSwapRandomized();
    }

    public void setSwapRandomized(boolean randomized) {
        this.config.set("swap.randomize", (Object)randomized);
        this.saveConfig();
    }

    public void loadConfig() {
        this.plugin.saveDefaultConfig();
        this.plugin.reloadConfig();
        this.config = this.plugin.getConfig();
        this.runnerNames = this.config.getStringList("teams.runners");
        this.hunterNames = this.config.getStringList("teams.hunters");
        this.dangerousBlocks = new HashSet<Material>();
        for (String blockName : this.config.getStringList("safe_swap.dangerous_blocks")) {
            try {
                Material material = Material.valueOf((String)blockName);
                this.dangerousBlocks.add(material);
            }
            catch (IllegalArgumentException e) {
                this.plugin.getLogger().warning("Invalid material in dangerous_blocks: " + blockName);
            }
        }
        this.powerUpsEnabled = this.config.getBoolean("power_ups.enabled", false);
        this.config.set("power_ups.enabled", (Object)this.powerUpsEnabled);
    }

    public void saveConfig() {
        this.config.set("teams.runners", this.runnerNames);
        this.config.set("teams.hunters", this.hunterNames);
        this.plugin.saveConfig();
    }

    public void addRunner(Player player) {
        String name = player.getName();
        if (!this.runnerNames.contains(name)) {
            this.runnerNames.add(name);
            this.hunterNames.remove(name);
        }
    }

    public void removeRunner(Player player) {
        this.runnerNames.remove(player.getName());
    }

    public boolean isPowerUpsEnabled() {
        return this.powerUpsEnabled;
    }

    public void setPowerUpsEnabled(boolean enabled) {
        this.powerUpsEnabled = enabled;
        this.config.set("power_ups.enabled", (Object)enabled);
        this.saveConfig();
    }

    public void addHunter(Player player) {
        String name = player.getName();
        if (!this.hunterNames.contains(name)) {
            this.hunterNames.add(name);
            this.runnerNames.remove(name);
        }
    }

    public void removeHunter(Player player) {
        this.hunterNames.remove(player.getName());
    }

    public List<String> getRunnerNames() {
        return new ArrayList<String>(this.runnerNames);
    }

    public List<String> getHunterNames() {
        return new ArrayList<String>(this.hunterNames);
    }

    public void setRunnerNames(List<String> names) {
        if (names == null) {
            names = Collections.emptyList();
        }
        this.runnerNames.clear();
        this.runnerNames.addAll(names);
        this.saveConfig();
    }

    public void setHunterNames(List<String> names) {
        if (names == null) {
            names = Collections.emptyList();
        }
        this.hunterNames.clear();
        this.hunterNames.addAll(names);
        this.saveConfig();
    }

    public boolean isRunner(Player player) {
        return this.runnerNames.contains(player.getName());
    }

    public boolean isHunter(Player player) {
        return this.hunterNames.contains(player.getName());
    }

    public int getMinSwapInterval() {
        return this.config.getInt("swap.min_interval", 30);
    }

    public int getMaxSwapInterval() {
        return this.config.getInt("swap.max_interval", 90);
    }

    public double getJitterStdDev() {
        return this.config.getDouble("swap.jitter.stddev", 15.0);
    }

    public boolean isClampJitter() {
        return this.config.getBoolean("swap.jitter.clamp", true);
    }

    public int getGracePeriodTicks() {
        return this.config.getInt("swap.grace_period_ticks", 40);
    }

    public boolean isPauseOnDisconnect() {
        return this.config.getBoolean("swap.pause_on_disconnect", true);
    }

    public Location getSpawnLocation() {
        double x = this.config.getDouble("spawn.x", 0.0);
        double y = this.config.getDouble("spawn.y", 0.0);
        double z = this.config.getDouble("spawn.z", 0.0);
        String worldName = this.config.getString("spawn.world", "world");
        World world = this.plugin.getServer().getWorld(worldName);
        if (world == null) {
            world = (World)this.plugin.getServer().getWorlds().get(0);
            this.plugin.getLogger().warning("Spawn world '" + worldName + "' not found. Using default world: " + world.getName());
        }
        return new Location(world, x, y, z);
    }

    public void setBroadcastsEnabled(boolean broadcastsEnabled) {
        this.config.set("broadcasts.enabled", (Object)broadcastsEnabled);
        this.plugin.saveConfig();
    }

    public boolean isVoiceChatIntegrationEnabled() {
        return this.config.getBoolean("voice_chat.enabled", false);
    }

    public void setVoiceChatIntegrationEnabled(boolean enabled) {
        this.config.set("voice_chat.enabled", (Object)enabled);
        this.plugin.saveConfig();
    }

    public String getFreezeMode() {
        return this.config.getString("freeze_mode", "LIMBO");
    }

    public Location getLimboLocation() {
        double x = this.config.getDouble("limbo.x", 0.5);
        double y = this.config.getDouble("limbo.y", 200.0);
        double z = this.config.getDouble("limbo.z", 0.5);
        String worldName = this.config.getString("limbo.world", "world");
        World world = this.plugin.getServer().getWorld(worldName);
        if (world == null) {
            world = (World)this.plugin.getServer().getWorlds().get(0);
            this.plugin.getLogger().warning("Limbo world '" + worldName + "' not found. Using default world: " + world.getName());
        }
        return new Location(world, x, y, z);
    }

    public void setFreezeMode(String mode) {
        this.config.set("freeze_mode", (Object)mode);
        this.plugin.saveConfig();
    }

    public boolean isTrackerEnabled() {
        return this.config.getBoolean("tracker.enabled", true);
    }

    public void setTrackerEnabled(boolean enabled) {
        this.config.set("tracker.enabled", (Object)enabled);
        this.plugin.saveConfig();
    }

    public int getTrackerUpdateTicks() {
        return this.config.getInt("tracker.update_ticks", 20);
    }

    public boolean isParticleTrailEnabled() {
        return this.config.getBoolean("particle_trail.enabled", false);
    }

    public int getParticleSpawnInterval() {
        return this.config.getInt("particle_trail.spawn_interval", 5);
    }

    public String getParticleTrailType() {
        return this.config.getString("particle_trail.type", "DUST");
    }

    public int[] getParticleTrailColor() {
        List rgb = this.config.getIntegerList("particle_trail.color");
        return new int[]{rgb.size() > 0 ? (Integer)rgb.get(0) : 255, rgb.size() > 1 ? (Integer)rgb.get(1) : 0, rgb.size() > 2 ? (Integer)rgb.get(2) : 0};
    }

    public String getGuiMainMenuTitle() {
        return this.config.getString("gui.main_menu.title", "\u00a76SpeedrunnerSwap - Main Menu");
    }

    public String getGuiTeamSelectorTitle() {
        return this.config.getString("gui.team_selector.title", "\u00a76SpeedrunnerSwap - Team Selector");
    }

    public String getGuiSettingsTitle() {
        return this.config.getString("gui.settings.title", "\u00a76SpeedrunnerSwap - Settings");
    }

    public int getSafeSwapHorizontalRadius() {
        return this.config.getInt("safe_swap.horizontal_radius", 5);
    }

    public int getSafeSwapVerticalDistance() {
        return this.config.getInt("safe_swap.vertical_distance", 10);
    }

    public Set<Material> getDangerousBlocks() {
        return this.dangerousBlocks;
    }

    public boolean isCancelMovement() {
        return this.config.getBoolean("cancel.movement", true);
    }

    public boolean isCancelInteractions() {
        return this.config.getBoolean("cancel.interactions", true);
    }

    public int getGuiMainMenuRows() {
        return this.config.getInt("gui.main_menu.rows", this.config.getInt("gui.main_menu_rows", 3));
    }

    public int getGuiTeamSelectorRows() {
        return this.config.getInt("gui.team_selector.rows", this.config.getInt("gui.team_selector_rows", 4));
    }

    public int getGuiSettingsRows() {
        return this.config.getInt("gui.settings.rows", this.config.getInt("gui.settings_rows", 5));
    }

    public boolean isBroadcastGameEvents() {
        return this.config.getBoolean("broadcasts.game_events", true);
    }

    public boolean isBroadcastsEnabled() {
        return this.config.getBoolean("broadcasts.enabled", true);
    }

    public boolean isBroadcastTeamChanges() {
        return this.config.getBoolean("broadcasts.team_changes", true);
    }

    public boolean isMuteInactiveRunners() {
        return this.config.getBoolean("voice_chat.mute_inactive_runners", true);
    }

    public boolean isFreezeMechanicEnabled() {
        return this.config.getBoolean("freeze_mechanic.enabled", false);
    }

    public int getFreezeDurationTicks() {
        return this.config.getInt("freeze_mechanic.duration_ticks", 100);
    }

    public int getFreezeCheckIntervalTicks() {
        return this.config.getInt("freeze_mechanic.check_interval_ticks", 10);
    }

    public double getFreezeMaxDistance() {
        return this.config.getDouble("freeze_mechanic.max_distance", 50.0);
    }

    public String getRunnerTimerVisibility() {
        return this.config.getString("timer_visibility.runner_visibility", "last_10");
    }

    public String getWaitingTimerVisibility() {
        return this.config.getString("timer_visibility.waiting_visibility", "always");
    }

    public String getHunterTimerVisibility() {
        return this.config.getString("timer_visibility.hunter_visibility", "never");
    }

    public boolean isCompassJammingEnabled() {
        return this.config.getBoolean("tracker.compass_jamming.enabled", false);
    }

    public int getCompassJamDuration() {
        return this.config.getInt("tracker.compass_jamming.duration_ticks", 100);
    }

    public Location getEndPortalHint(World world) {
        if (world == null) {
            return null;
        }
        String base = "tracker.end_portal_hint." + world.getName();
        if (!this.config.contains(base + ".x")) {
            return null;
        }
        double x = this.config.getDouble(base + ".x", world.getSpawnLocation().getX());
        double y = this.config.getDouble(base + ".y", world.getSpawnLocation().getY());
        double z = this.config.getDouble(base + ".z", world.getSpawnLocation().getZ());
        return new Location(world, x, y, z);
    }

    public void setEndPortalHint(World world, Location loc) {
        if (world == null || loc == null) {
            return;
        }
        String base = "tracker.end_portal_hint." + world.getName();
        this.config.set(base + ".x", (Object)loc.getX());
        this.config.set(base + ".y", (Object)loc.getY());
        this.config.set(base + ".z", (Object)loc.getZ());
        this.plugin.saveConfig();
    }

    public void clearEndPortalHint(World world) {
        if (world == null) {
            return;
        }
        String base = "tracker.end_portal_hint." + world.getName();
        this.config.set(base, null);
        this.plugin.saveConfig();
    }

    public boolean isHunterSwapEnabled() {
        return this.config.getBoolean("swap.hunter_swap.enabled", false);
    }

    public int getHunterSwapInterval() {
        return this.config.getInt("swap.hunter_swap.interval", 60);
    }

    public List<String> getGoodPowerUps() {
        return this.config.getStringList("power_ups.good_effects");
    }

    public List<String> getBadPowerUps() {
        return this.config.getStringList("power_ups.bad_effects");
    }

    public int getPowerUpsMinSeconds() {
        return Math.max(1, this.config.getInt("power_ups.duration.min_seconds", 10));
    }

    public int getPowerUpsMaxSeconds() {
        int min = this.getPowerUpsMinSeconds();
        int max = this.config.getInt("power_ups.duration.max_seconds", 20);
        return Math.max(min, max);
    }

    public void setPowerUpsMinSeconds(int seconds) {
        int max;
        if ((seconds = Math.max(1, seconds)) > (max = this.getPowerUpsMaxSeconds())) {
            this.config.set("power_ups.duration.max_seconds", (Object)seconds);
        }
        this.config.set("power_ups.duration.min_seconds", (Object)seconds);
        this.plugin.saveConfig();
    }

    public void setPowerUpsMaxSeconds(int seconds) {
        seconds = Math.max(this.getPowerUpsMinSeconds(), seconds);
        this.config.set("power_ups.duration.max_seconds", (Object)seconds);
        this.plugin.saveConfig();
    }

    public int getPowerUpsMinLevel() {
        return Math.max(1, this.config.getInt("power_ups.level.min", 1));
    }

    public int getPowerUpsMaxLevel() {
        int min = this.getPowerUpsMinLevel();
        int max = this.config.getInt("power_ups.level.max", 2);
        return Math.max(min, max);
    }

    public void setPowerUpsMinLevel(int level) {
        int max;
        if ((level = Math.max(1, level)) > (max = this.getPowerUpsMaxLevel())) {
            this.config.set("power_ups.level.max", (Object)level);
        }
        this.config.set("power_ups.level.min", (Object)level);
        this.plugin.saveConfig();
    }

    public void setPowerUpsMaxLevel(int level) {
        level = Math.max(this.getPowerUpsMinLevel(), level);
        this.config.set("power_ups.level.max", (Object)level);
        this.plugin.saveConfig();
    }

    public boolean isLastStandEnabled() {
        return this.config.getBoolean("last_stand.enabled", false);
    }

    public int getLastStandDuration() {
        return this.config.getInt("last_stand.duration_ticks", 600);
    }

    public int getLastStandStrengthAmplifier() {
        return this.config.getInt("last_stand.strength_amplifier", 1);
    }

    public int getLastStandSpeedAmplifier() {
        return this.config.getInt("last_stand.speed_amplifier", 1);
    }

    public boolean isKitsEnabled() {
        return this.config.getBoolean("kits.enabled", false);
    }

    public void setKitsEnabled(boolean enabled) {
        this.config.set("kits.enabled", (Object)enabled);
        this.plugin.saveConfig();
        try {
            FileConfiguration kits = this.plugin.getKitConfigManager().getConfig();
            kits.set("kits.enabled", (Object)enabled);
            this.plugin.getKitConfigManager().saveConfig();
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public List<String> getRunnerKitItems() {
        return this.config.getStringList("kits.runner_kit");
    }

    public List<String> getHunterKitItems() {
        return this.config.getStringList("kits.hunter_kit");
    }

    public boolean isHotPotatoModeEnabled() {
        return this.config.getBoolean("swap.hot_potato_mode.enabled", false);
    }

    public void setRunnerTimerVisibility(String visibility) {
        this.config.set("timer_visibility.runner_visibility", (Object)visibility);
        this.plugin.saveConfig();
    }

    public void setWaitingTimerVisibility(String visibility) {
        this.config.set("timer_visibility.waiting_visibility", (Object)visibility);
        this.plugin.saveConfig();
    }

    public void setHunterTimerVisibility(String visibility) {
        this.config.set("timer_visibility.hunter_visibility", (Object)visibility);
        this.plugin.saveConfig();
    }
}

