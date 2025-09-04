/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.attribute.Attribute
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.potion.PotionEffect
 */
package com.example.speedrunnerswap.utils;

import com.example.speedrunnerswap.models.PlayerState;
import java.util.ArrayList;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

public class PlayerStateUtil {
    public static PlayerState capturePlayerState(Player player) {
        return new PlayerState((ItemStack[])player.getInventory().getContents().clone(), (ItemStack[])player.getInventory().getArmorContents().clone(), player.getInventory().getItemInOffHand().clone(), player.getLocation().clone(), player.getHealth(), player.getFoodLevel(), player.getSaturation(), player.getExhaustion(), player.getTotalExperience(), player.getExp(), player.getLevel(), player.getFireTicks(), player.getRemainingAir(), player.getMaximumAir(), player.getGameMode(), player.getFallDistance(), player.getAllowFlight(), player.isFlying(), new ArrayList<PotionEffect>(player.getActivePotionEffects()), player.getAbsorptionAmount(), player.getVehicle(), player.isInsideVehicle(), player.getTicksLived(), player.getLastDamage(), player.getNoDamageTicks(), player.isGliding(), player.getWalkSpeed(), player.getFlySpeed(), player.getPortalCooldown());
    }

    public static void applyPlayerState(Player player, PlayerState state) {
        player.getInventory().clear();
        player.getInventory().setContents(state.getInventory());
        player.getInventory().setArmorContents(state.getArmor());
        player.getInventory().setItemInOffHand(state.getOffhand());
        player.teleport(state.getLocation());
        player.setHealth(Math.min(state.getHealth(), player.getAttribute(Attribute.MAX_HEALTH).getValue()));
        player.setFoodLevel(state.getFoodLevel());
        player.setSaturation(state.getSaturation());
        player.setExhaustion(state.getExhaustion());
        player.setTotalExperience(state.getTotalExperience());
        player.setExp(state.getExp());
        player.setLevel(state.getLevel());
        player.setFireTicks(state.getFireTicks());
        player.setRemainingAir(state.getRemainingAir());
        player.setMaximumAir(state.getMaximumAir());
        player.setGameMode(state.getGameMode());
        player.setFallDistance(state.getFallDistance());
        player.setAllowFlight(state.isAllowFlight());
        player.setFlying(state.isFlying());
        player.setAbsorptionAmount(state.getAbsorptionAmount());
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        for (PotionEffect effect : state.getActivePotionEffects()) {
            player.addPotionEffect(effect);
        }
        if (player.isInsideVehicle() && player.getVehicle() != null) {
            player.getVehicle().eject();
        }
        if (state.isInVehicle() && state.getVehicle() != null) {
            state.getVehicle().addPassenger((Entity)player);
        }
        player.setWalkSpeed(state.getWalkSpeed());
        player.setFlySpeed(state.getFlySpeed());
        player.setGliding(state.isGliding());
        player.setLastDamage(state.getLastDamage());
        player.setNoDamageTicks(state.getNoDamageTicks());
        player.setPortalCooldown(state.getPortalCooldown());
        player.updateInventory();
    }
}

