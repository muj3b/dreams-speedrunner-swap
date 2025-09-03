package com.example.speedrunnerswap.powerups;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;

import java.util.Random;

public class PowerUpManager {
    private final SpeedrunnerSwap plugin;
    private final Random random = new Random();

    // Fallback pools used if config lists are empty
    private final PotionEffectType[] POSITIVE_FALLBACK = {
        PotionEffectType.SPEED,
        PotionEffectType.REGENERATION,
        PotionEffectType.RESISTANCE,
        PotionEffectType.NIGHT_VISION
    };

    private final PotionEffectType[] NEGATIVE_FALLBACK = {
        PotionEffectType.SLOWNESS,
        PotionEffectType.WEAKNESS,
        PotionEffectType.HUNGER,
        PotionEffectType.POISON
    };

    public PowerUpManager(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
    }

    public void applyRandomEffect(Player player) {
        if (!plugin.getConfigManager().isPowerUpsEnabled()) {
            return;
        }

        boolean isPositive = random.nextBoolean();
        PotionEffectType[] effectPool = getConfiguredPool(isPositive);
        PotionEffectType selectedEffect = effectPool[random.nextInt(effectPool.length)];

        // Duration and level from config ranges
        int minSec = plugin.getConfigManager().getPowerUpsMinSeconds();
        int maxSec = plugin.getConfigManager().getPowerUpsMaxSeconds();
        if (maxSec < minSec) maxSec = minSec;
        int durationSeconds = minSec + (maxSec > minSec ? random.nextInt(maxSec - minSec + 1) : 0);
        int duration = Math.max(1, durationSeconds) * 20; // ticks

        int minLvl = Math.max(1, plugin.getConfigManager().getPowerUpsMinLevel());
        int maxLvl = Math.max(minLvl, plugin.getConfigManager().getPowerUpsMaxLevel());
        int level = minLvl + (maxLvl > minLvl ? random.nextInt(maxLvl - minLvl + 1) : 0);
        int amplifier = Math.max(0, level - 1); // Amplifier is level-1

        player.addPotionEffect(new PotionEffect(selectedEffect, duration, amplifier));
        
        String effectType = isPositive ? "§a§lPositive" : "§c§lNegative";
        player.sendMessage("§6You received a " + effectType + " §6swap effect: §f" + 
            formatEffectName(selectedEffect.getKey().getKey()) + " " + (amplifier + 1));
    }

    private String formatEffectName(String name) {
        return name.substring(0, 1).toUpperCase() + 
               name.substring(1).toLowerCase().replace("_", " ");
    }

    private PotionEffectType[] getConfiguredPool(boolean positive) {
        java.util.List<String> ids = positive ? plugin.getConfigManager().getGoodPowerUps()
                                              : plugin.getConfigManager().getBadPowerUps();
        java.util.List<PotionEffectType> list = new java.util.ArrayList<>();
        for (String id : ids) {
            PotionEffectType t = resolveEffect(id);
            if (t != null) list.add(t);
        }
        if (list.isEmpty()) {
            return positive ? POSITIVE_FALLBACK : NEGATIVE_FALLBACK;
        }
        return list.toArray(new PotionEffectType[0]);
    }

    private PotionEffectType resolveEffect(String id) {
        if (id == null) return null;
        String key = id.toLowerCase(java.util.Locale.ROOT).trim();
        // Map common aliases
        key = switch (key) {
            case "increase_damage", "strength" -> "strength";
            case "damage_resistance", "resistance" -> "resistance";
            case "slow", "slowness" -> "slowness";
            case "jump", "jump_boost" -> "jump_boost";
            case "slow_digging", "mining_fatigue" -> "mining_fatigue";
            case "confusion", "nausea" -> "nausea";
            default -> key;
        };
        return Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft(key));
    }
}
