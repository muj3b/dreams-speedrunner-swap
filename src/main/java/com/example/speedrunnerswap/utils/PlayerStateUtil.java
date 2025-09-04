package com.example.speedrunnerswap.utils;

import com.example.speedrunnerswap.models.PlayerState;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import com.example.speedrunnerswap.utils.BukkitCompat;

import java.util.ArrayList;

/**
 * Utility class for capturing and applying player states
 */
public class PlayerStateUtil {

    /**
     * Capture a player's current state
     * @param player The player to capture state from
     * @return The captured player state
     */
    public static com.example.speedrunnerswap.models.PlayerState capturePlayerState(Player player) {
        return new PlayerState(
                player.getInventory().getContents().clone(),
                player.getInventory().getArmorContents().clone(),
                player.getInventory().getItemInOffHand().clone(),
                player.getLocation().clone(),
                player.getHealth(),
                player.getFoodLevel(),
                player.getSaturation(),
                player.getExhaustion(),
                player.getTotalExperience(),
                player.getExp(),
                player.getLevel(),
                player.getFireTicks(),
                player.getRemainingAir(),
                player.getMaximumAir(),
                player.getGameMode(),
                player.getFallDistance(),
                player.getAllowFlight(),
                player.isFlying(),
                new ArrayList<>(player.getActivePotionEffects()),
                player.getAbsorptionAmount(),
                player.getVehicle(),
                player.isInsideVehicle(),
                player.getTicksLived(),
                player.getLastDamage(),
                player.getNoDamageTicks(),
                player.isGliding(),
                player.getWalkSpeed(),
                player.getFlySpeed(),
                player.getPortalCooldown()
        );
    }

    /**
     * Apply a saved state to a player
     * @param player The player to apply state to
     * @param state The state to apply
     */
    public static void applyPlayerState(Player player, com.example.speedrunnerswap.models.PlayerState state) {
        // Clear inventory and apply saved inventory
        player.getInventory().clear();
        player.getInventory().setContents(state.getInventory());
        player.getInventory().setArmorContents(state.getArmor());
        player.getInventory().setItemInOffHand(state.getOffhand());
        
        // Teleport to saved location
        player.teleport(state.getLocation());
        
        // Apply health and food (handle API changes across versions)
        player.setHealth(Math.min(state.getHealth(), BukkitCompat.getMaxHealthValue(player)));
        player.setFoodLevel(state.getFoodLevel());
        player.setSaturation(state.getSaturation());
        player.setExhaustion(state.getExhaustion());
        
        // Apply XP
        player.setTotalExperience(state.getTotalExperience());
        player.setExp(state.getExp());
        player.setLevel(state.getLevel());
        
        // Apply other attributes
        player.setFireTicks(state.getFireTicks());
        player.setRemainingAir(state.getRemainingAir());
        player.setMaximumAir(state.getMaximumAir());
        player.setGameMode(state.getGameMode());
        player.setFallDistance(state.getFallDistance());
        player.setAllowFlight(state.isAllowFlight());
        player.setFlying(state.isFlying());
        player.setAbsorptionAmount(state.getAbsorptionAmount());
        
        // Clear and apply potion effects
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        
        for (PotionEffect effect : state.getActivePotionEffects()) {
            player.addPotionEffect(effect);
        }
        
        // Handle vehicle state
        if (player.isInsideVehicle() && player.getVehicle() != null) {
            player.getVehicle().eject();
        }
        if (state.isInVehicle() && state.getVehicle() != null) {
            state.getVehicle().addPassenger(player);
        }

        // Apply speed settings
        player.setWalkSpeed(state.getWalkSpeed());
        player.setFlySpeed(state.getFlySpeed());

        // Apply gliding state
        player.setGliding(state.isGliding());

        // Apply damage-related stats
        player.setLastDamage(state.getLastDamage());
        player.setNoDamageTicks(state.getNoDamageTicks());

        // Apply portal cooldown
        player.setPortalCooldown(state.getPortalCooldown());

        // Update inventory
        player.updateInventory();
    }
}
