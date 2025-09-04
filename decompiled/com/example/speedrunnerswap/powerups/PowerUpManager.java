/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.NamespacedKey
 *  org.bukkit.Registry
 *  org.bukkit.entity.Player
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 */
package com.example.speedrunnerswap.powerups;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PowerUpManager {
    private final SpeedrunnerSwap plugin;
    private final Random random = new Random();
    private final PotionEffectType[] POSITIVE_FALLBACK = new PotionEffectType[]{PotionEffectType.SPEED, PotionEffectType.REGENERATION, PotionEffectType.RESISTANCE, PotionEffectType.NIGHT_VISION};
    private final PotionEffectType[] NEGATIVE_FALLBACK = new PotionEffectType[]{PotionEffectType.SLOWNESS, PotionEffectType.WEAKNESS, PotionEffectType.HUNGER, PotionEffectType.POISON};

    public PowerUpManager(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
    }

    public void applyRandomEffect(Player player) {
        if (!this.plugin.getConfigManager().isPowerUpsEnabled()) {
            return;
        }
        boolean isPositive = this.random.nextBoolean();
        PotionEffectType[] effectPool = this.getConfiguredPool(isPositive);
        PotionEffectType selectedEffect = effectPool[this.random.nextInt(effectPool.length)];
        int minSec = this.plugin.getConfigManager().getPowerUpsMinSeconds();
        int maxSec = this.plugin.getConfigManager().getPowerUpsMaxSeconds();
        if (maxSec < minSec) {
            maxSec = minSec;
        }
        int durationSeconds = minSec + (maxSec > minSec ? this.random.nextInt(maxSec - minSec + 1) : 0);
        int duration = Math.max(1, durationSeconds) * 20;
        int minLvl = Math.max(1, this.plugin.getConfigManager().getPowerUpsMinLevel());
        int maxLvl = Math.max(minLvl, this.plugin.getConfigManager().getPowerUpsMaxLevel());
        int level = minLvl + (maxLvl > minLvl ? this.random.nextInt(maxLvl - minLvl + 1) : 0);
        int amplifier = Math.max(0, level - 1);
        player.addPotionEffect(new PotionEffect(selectedEffect, duration, amplifier));
        String effectType = isPositive ? "\u00a7a\u00a7lPositive" : "\u00a7c\u00a7lNegative";
        player.sendMessage("\u00a76You received a " + effectType + " \u00a76swap effect: \u00a7f" + this.formatEffectName(selectedEffect.getKey().getKey()) + " " + (amplifier + 1));
    }

    private String formatEffectName(String name) {
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase().replace("_", " ");
    }

    private PotionEffectType[] getConfiguredPool(boolean positive) {
        List<String> ids = positive ? this.plugin.getConfigManager().getGoodPowerUps() : this.plugin.getConfigManager().getBadPowerUps();
        ArrayList<PotionEffectType> list = new ArrayList<PotionEffectType>();
        for (String id : ids) {
            PotionEffectType t = this.resolveEffect(id);
            if (t == null) continue;
            list.add(t);
        }
        if (list.isEmpty()) {
            return positive ? this.POSITIVE_FALLBACK : this.NEGATIVE_FALLBACK;
        }
        return list.toArray(new PotionEffectType[0]);
    }

    private PotionEffectType resolveEffect(String id) {
        String key;
        if (id == null) {
            return null;
        }
        key = switch (key = id.toLowerCase(Locale.ROOT).trim()) {
            case "increase_damage", "strength" -> "strength";
            case "damage_resistance", "resistance" -> "resistance";
            case "slow", "slowness" -> "slowness";
            case "jump", "jump_boost" -> "jump_boost";
            case "slow_digging", "mining_fatigue" -> "mining_fatigue";
            case "confusion", "nausea" -> "nausea";
            default -> key;
        };
        return (PotionEffectType)Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft((String)key));
    }
}

