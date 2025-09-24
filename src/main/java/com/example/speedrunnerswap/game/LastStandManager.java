package com.example.speedrunnerswap.game;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class LastStandManager {
    private final SpeedrunnerSwap plugin;

    public LastStandManager(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
    }

    /**
     * Sets up the LastStand system at game start
     */
    public void setup() {
        if (!plugin.getConfigManager().isLastStandEnabled()) {
            return;
        }

        FileConfiguration config = plugin.getConfig();
        boolean updated = false;

        // Migrate legacy second-based duration to tick-based configuration
        if (config.contains("last_stand.duration") && !config.contains("last_stand.duration_ticks")) {
            int seconds = Math.max(1, config.getInt("last_stand.duration", 300));
            config.set("last_stand.duration_ticks", seconds * 20);
            updated = true;
        }

        if (!config.isInt("last_stand.duration_ticks")) {
            config.set("last_stand.duration_ticks", 600); // 30 seconds
            updated = true;
        }
        if (!config.isInt("last_stand.strength_amplifier")) {
            config.set("last_stand.strength_amplifier", 1);
            updated = true;
        }
        if (!config.isInt("last_stand.speed_amplifier")) {
            config.set("last_stand.speed_amplifier", 1);
            updated = true;
        }

        if (updated) {
            plugin.saveConfig();
        }
    }

    /**
     * Activates Last Stand mode for the final runner
     * @param lastRunner The final remaining runner
     */
    public void activateLastStand(Player lastRunner) {
        if (!plugin.getConfigManager().isLastStandEnabled()) {
            return;
        }

        int durationTicks = plugin.getConfigManager().getLastStandDuration();
        int strengthAmplifier = plugin.getConfigManager().getLastStandStrengthAmplifier();
        int speedAmplifier = plugin.getConfigManager().getLastStandSpeedAmplifier();

        // Add Strength effect
        lastRunner.addPotionEffect(new PotionEffect(
            PotionEffectType.STRENGTH,
            durationTicks,
            strengthAmplifier
        ));

        // Add Speed effect
        lastRunner.addPotionEffect(new PotionEffect(
            PotionEffectType.SPEED,
            durationTicks,
            speedAmplifier
        ));

        // Notify the player
        lastRunner.sendMessage("§6§lLAST STAND ACTIVATED! §eYou've received Strength and Speed boosts!");
    }
}
