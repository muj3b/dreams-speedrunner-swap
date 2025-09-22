package com.example.speedrunnerswap.utils;

import com.example.speedrunnerswap.models.PlayerState;
import org.bukkit.entity.Player;
// Use fully-qualified reference for BukkitCompat to avoid IDE false positives

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
        if (player == null || state == null) return;

        // Inventory & offhand
        player.getInventory().clear();
        player.getInventory().setContents(state.getInventory());
        player.getInventory().setArmorContents(state.getArmor());
        player.getInventory().setItemInOffHand(state.getOffhand());

        // Location
        if (state.getLocation() != null) {
            try { player.teleport(state.getLocation()); } catch (Throwable ignored) {}
        }

        // Vital stats (clamp)
        double max = com.example.speedrunnerswap.utils.BukkitCompat.getMaxHealthValue(player);
        double clamped = Math.max(0.0D, Math.min(max, state.getHealth()));
        try { player.setHealth(clamped); } catch (Throwable ignored) {}

        player.setFoodLevel(state.getFoodLevel());
        player.setSaturation(state.getSaturation());
        player.setExhaustion(state.getExhaustion());

        // Experience
        player.setTotalExperience(state.getTotalExperience());
        player.setExp(state.getExp());
        player.setLevel(state.getLevel());

        // Environment
        player.setFireTicks(state.getFireTicks());
        try { player.setMaximumAir(state.getMaximumAir()); } catch (Throwable ignored) {}
        try { player.setRemainingAir(state.getRemainingAir()); } catch (Throwable ignored) {}

        // Mode & motion
        if (state.getGameMode() != null) player.setGameMode(state.getGameMode());
        player.setFallDistance(state.getFallDistance());
        try { player.setAllowFlight(state.isAllowFlight()); } catch (Throwable ignored) {}
        try { player.setFlying(state.isFlying()); } catch (Throwable ignored) {}

        // Effects
        try {
            for (org.bukkit.potion.PotionEffect e : player.getActivePotionEffects()) player.removePotionEffect(e.getType());
            if (state.getActivePotionEffects() != null) {
                for (org.bukkit.potion.PotionEffect e : state.getActivePotionEffects()) player.addPotionEffect(e);
            }
        } catch (Throwable ignored) {}

        // Misc
        try { player.setAbsorptionAmount(state.getAbsorptionAmount()); } catch (Throwable ignored) {}
        try { if (state.isGliding()) player.setGliding(true); } catch (Throwable ignored) {}
        try { player.setTicksLived(state.getTicksLived()); } catch (Throwable ignored) {}
        try { player.setNoDamageTicks(state.getNoDamageTicks()); } catch (Throwable ignored) {}
        try { player.setWalkSpeed(state.getWalkSpeed()); } catch (Throwable ignored) {}
        try { player.setFlySpeed(state.getFlySpeed()); } catch (Throwable ignored) {}
        try { player.setPortalCooldown(state.getPortalCooldown()); } catch (Throwable ignored) {}
    }
}
