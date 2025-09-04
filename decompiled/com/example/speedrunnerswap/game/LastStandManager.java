/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 */
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

    public void setup() {
        if (!this.plugin.getConfig().getBoolean("last_stand.enabled", true)) {
            return;
        }
        this.plugin.getConfig().addDefault("last_stand.strength_amplifier", (Object)1);
        this.plugin.getConfig().addDefault("last_stand.speed_amplifier", (Object)1);
        this.plugin.getConfig().addDefault("last_stand.duration", (Object)300);
        this.plugin.saveConfig();
    }

    public void activateLastStand(Player lastRunner) {
        if (!this.plugin.getConfigManager().isLastStandEnabled()) {
            return;
        }
        int duration = this.plugin.getConfigManager().getLastStandDuration();
        lastRunner.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, this.plugin.getConfig().getInt("last_stand.strength_amplifier", 1)));
        lastRunner.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, this.plugin.getConfig().getInt("last_stand.speed_amplifier", 1)));
        lastRunner.sendMessage("\u00a76\u00a7lLAST STAND ACTIVATED! \u00a7eYou've received Strength and Speed boosts!");
    }
}

