package com.example.speedrunnerswap.game;

import com.example.speedrunnerswap.SpeedrunnerSwap;
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
        if (!plugin.getConfig().getBoolean("last_stand.enabled", true)) {
            return;
        }
        
        // Load and validate configuration
        plugin.getConfig().addDefault("last_stand.strength_amplifier", 1);
        plugin.getConfig().addDefault("last_stand.speed_amplifier", 1);
        plugin.getConfig().addDefault("last_stand.duration", 300); // 5 minutes
        plugin.saveConfig();
    }

    /**
     * Activates Last Stand mode for the final runner
     * @param lastRunner The final remaining runner
     */
    public void activateLastStand(Player lastRunner) {
        if (!plugin.getConfigManager().isLastStandEnabled()) {
            return;
        }

        int duration = plugin.getConfigManager().getLastStandDuration();
        
        // Add Strength effect
        lastRunner.addPotionEffect(new PotionEffect(
            PotionEffectType.STRENGTH, 
            duration,
            plugin.getConfig().getInt("last_stand.strength_amplifier", 1)
        ));
        
        // Add Speed effect
        lastRunner.addPotionEffect(new PotionEffect(
            PotionEffectType.SPEED,
            duration,
            plugin.getConfig().getInt("last_stand.speed_amplifier", 1)
        ));

        // Notify the player
        lastRunner.sendMessage("§6§lLAST STAND ACTIVATED! §eYou've received Strength and Speed boosts!");
    }
}
