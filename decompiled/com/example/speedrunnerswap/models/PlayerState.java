/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.GameMode
 *  org.bukkit.Location
 *  org.bukkit.entity.Entity
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.potion.PotionEffect
 */
package com.example.speedrunnerswap.models;

import com.example.speedrunnerswap.models.Team;
import java.util.Collection;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

public class PlayerState {
    private final ItemStack[] inventory;
    private final ItemStack[] armor;
    private final ItemStack offhand;
    private final Location location;
    private final double health;
    private final int foodLevel;
    private final float saturation;
    private final float exhaustion;
    private final int totalExperience;
    private final float exp;
    private final int level;
    private final int fireTicks;
    private final int remainingAir;
    private final int maximumAir;
    private final GameMode gameMode;
    private final float fallDistance;
    private final boolean allowFlight;
    private final boolean flying;
    private final Collection<PotionEffect> activePotionEffects;
    private final double absorptionAmount;
    private final Entity vehicle;
    private final boolean inVehicle;
    private final int ticksLived;
    private final double lastDamage;
    private final int noDamageTicks;
    private final boolean gliding;
    private final float walkSpeed;
    private final float flySpeed;
    private final int portalCooldown;
    private Team selectedTeam;

    public int getFireTicks() {
        return this.fireTicks;
    }

    public int getRemainingAir() {
        return this.remainingAir;
    }

    public int getMaximumAir() {
        return this.maximumAir;
    }

    public Entity getVehicle() {
        return this.vehicle;
    }

    public boolean isInVehicle() {
        return this.inVehicle;
    }

    public int getTicksLived() {
        return this.ticksLived;
    }

    public double getLastDamage() {
        return this.lastDamage;
    }

    public int getNoDamageTicks() {
        return this.noDamageTicks;
    }

    public boolean isGliding() {
        return this.gliding;
    }

    public float getWalkSpeed() {
        return this.walkSpeed;
    }

    public float getFlySpeed() {
        return this.flySpeed;
    }

    public int getPortalCooldown() {
        return this.portalCooldown;
    }

    public GameMode getGameMode() {
        return this.gameMode;
    }

    public float getFallDistance() {
        return this.fallDistance;
    }

    public boolean isAllowFlight() {
        return this.allowFlight;
    }

    public boolean isFlying() {
        return this.flying;
    }

    public Collection<PotionEffect> getActivePotionEffects() {
        return this.activePotionEffects;
    }

    public double getAbsorptionAmount() {
        return this.absorptionAmount;
    }

    public PlayerState(ItemStack[] inventory, ItemStack[] armor, ItemStack offhand, Location location, double health, int foodLevel, float saturation, float exhaustion, int totalExperience, float exp, int level, int fireTicks, int remainingAir, int maximumAir, GameMode gameMode, float fallDistance, boolean allowFlight, boolean flying, Collection<PotionEffect> activePotionEffects, double absorptionAmount, Entity vehicle, boolean inVehicle, int ticksLived, double lastDamage, int noDamageTicks, boolean gliding, float walkSpeed, float flySpeed, int portalCooldown) {
        this.inventory = inventory;
        this.armor = armor;
        this.offhand = offhand;
        this.location = location;
        this.health = health;
        this.foodLevel = foodLevel;
        this.saturation = saturation;
        this.exhaustion = exhaustion;
        this.totalExperience = totalExperience;
        this.exp = exp;
        this.level = level;
        this.fireTicks = fireTicks;
        this.remainingAir = remainingAir;
        this.maximumAir = maximumAir;
        this.gameMode = gameMode;
        this.fallDistance = fallDistance;
        this.allowFlight = allowFlight;
        this.flying = flying;
        this.activePotionEffects = activePotionEffects;
        this.absorptionAmount = absorptionAmount;
        this.vehicle = vehicle;
        this.inVehicle = inVehicle;
        this.ticksLived = ticksLived;
        this.lastDamage = lastDamage;
        this.noDamageTicks = noDamageTicks;
        this.gliding = gliding;
        this.walkSpeed = walkSpeed;
        this.flySpeed = flySpeed;
        this.portalCooldown = portalCooldown;
        this.selectedTeam = Team.NONE;
    }

    public ItemStack[] getInventory() {
        return this.inventory;
    }

    public ItemStack[] getArmor() {
        return this.armor;
    }

    public ItemStack getOffhand() {
        return this.offhand;
    }

    public Location getLocation() {
        return this.location;
    }

    public double getHealth() {
        return this.health;
    }

    public int getFoodLevel() {
        return this.foodLevel;
    }

    public float getSaturation() {
        return this.saturation;
    }

    public float getExhaustion() {
        return this.exhaustion;
    }

    public int getTotalExperience() {
        return this.totalExperience;
    }

    public float getExp() {
        return this.exp;
    }

    public int getLevel() {
        return this.level;
    }

    public Team getSelectedTeam() {
        return this.selectedTeam;
    }

    public void setSelectedTeam(Team selectedTeam) {
        this.selectedTeam = selectedTeam;
    }
}

