/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Bukkit
 *  org.bukkit.Sound
 *  org.bukkit.entity.Player
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 */
package com.example.speedrunnerswap.game;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class BountyManager {
    private final SpeedrunnerSwap plugin;
    private final Random random;
    private UUID bountyTarget;
    private boolean isBountyActive;
    private long lastAssignedAt = 0L;

    public BountyManager(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
        this.random = new Random();
        this.isBountyActive = false;
    }

    public void assignNewBounty() {
        if (!this.plugin.getConfig().getBoolean("bounty.enabled", false)) {
            return;
        }
        int cooldownSec = Math.max(0, this.plugin.getConfig().getInt("bounty.cooldown", 300));
        if (System.currentTimeMillis() - this.lastAssignedAt < (long)cooldownSec * 1000L) {
            return;
        }
        List<Player> runners = this.plugin.getGameManager().getRunners();
        if (runners.isEmpty()) {
            return;
        }
        Player target = runners.get(this.random.nextInt(runners.size()));
        this.bountyTarget = target.getUniqueId();
        this.isBountyActive = true;
        int glowSec = Math.max(10, this.plugin.getConfig().getInt("bounty.glow_duration", 300));
        target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, glowSec * 20, 0));
        this.announceBounty(target);
        this.lastAssignedAt = System.currentTimeMillis();
    }

    private void announceBounty(Player target) {
        Bukkit.broadcast((Component)Component.text((String)"\n\u00a74\u00a7l=== BOUNTY ANNOUNCED ==="));
        Bukkit.broadcast((Component)Component.text((String)("\u00a7c" + target.getName() + " \u00a7ehas been marked for elimination!")));
        Bukkit.broadcast((Component)Component.text((String)"\u00a7eThe hunter who eliminates them will receive special rewards!"));
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);
        }
    }

    public void claimBounty(Player hunter) {
        if (!this.isBountyActive) {
            return;
        }
        this.isBountyActive = false;
        this.giveRewards(hunter);
        Bukkit.broadcast((Component)Component.text((String)"\n\u00a74\u00a7l=== BOUNTY CLAIMED ==="));
        Bukkit.broadcast((Component)Component.text((String)("\u00a7e" + hunter.getName() + " \u00a7ahas eliminated the bounty target!")));
        hunter.playSound(hunter.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    }

    private void giveRewards(Player hunter) {
        int speedSec = Math.max(10, this.plugin.getConfig().getInt("bounty.rewards.speed_duration", 300));
        int strengthSec = Math.max(10, this.plugin.getConfig().getInt("bounty.rewards.strength_duration", 300));
        hunter.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, speedSec * 20, 0));
        hunter.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, strengthSec * 20, 0));
        hunter.setHealth(20.0);
        hunter.setFoodLevel(20);
        hunter.sendMessage((Component)Component.text((String)"\u00a7aYou have received bounty hunter rewards!"));
    }

    public boolean isBountyTarget(Player player) {
        return this.isBountyActive && player.getUniqueId().equals(this.bountyTarget);
    }

    public boolean isActive() {
        return this.isBountyActive;
    }

    public void clearBounty() {
        this.isBountyActive = false;
        this.bountyTarget = null;
    }
}

