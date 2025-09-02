package com.example.speedrunnerswap.powerups;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

public class PowerUpManager {
    private final SpeedrunnerSwap plugin;
    private final Random random = new Random();

    private final PotionEffectType[] POSITIVE_EFFECTS = {
        PotionEffectType.SPEED,
        PotionEffectType.DOLPHINS_GRACE,
        PotionEffectType.ABSORPTION,
        PotionEffectType.NIGHT_VISION,
        PotionEffectType.REGENERATION
    };

    private final PotionEffectType[] NEGATIVE_EFFECTS = {
        PotionEffectType.DARKNESS,
        PotionEffectType.WEAKNESS,
        PotionEffectType.HUNGER,
        PotionEffectType.POISON,
        PotionEffectType.GLOWING
    };

    public PowerUpManager(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
    }

    public void applyRandomEffect(Player player) {
        if (!plugin.getConfigManager().isPowerUpsEnabled()) {
            return;
        }

        boolean isPositive = random.nextBoolean();
        PotionEffectType[] effectPool = isPositive ? POSITIVE_EFFECTS : NEGATIVE_EFFECTS;
        PotionEffectType selectedEffect = effectPool[random.nextInt(effectPool.length)];
        
        // Duration: 15-30 seconds
        int duration = (random.nextInt(16) + 15) * 20; // Convert to ticks
        // Amplifier: 0-1 (Level I or II)
        int amplifier = random.nextInt(2);

        player.addPotionEffect(new PotionEffect(selectedEffect, duration, amplifier));
        
        String effectType = isPositive ? "§a§lPositive" : "§c§lNegative";
        player.sendMessage("§6You received a " + effectType + " §6swap effect: §f" + 
            formatEffectName(selectedEffect.getKey().getKey()) + " " + (amplifier + 1));
    }

    private String formatEffectName(String name) {
        return name.substring(0, 1).toUpperCase() + 
               name.substring(1).toLowerCase().replace("_", " ");
    }
}
