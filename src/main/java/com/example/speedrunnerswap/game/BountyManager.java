package com.example.speedrunnerswap.game;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Random;
import java.util.UUID;

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
        if (!plugin.getConfig().getBoolean("bounty.enabled", false)) return;
        int cooldownSec = Math.max(0, plugin.getConfig().getInt("bounty.cooldown", 300));
        if ((System.currentTimeMillis() - lastAssignedAt) < cooldownSec * 1000L) {
            return; // still on cooldown
        }
        List<Player> runners = plugin.getGameManager().getRunners();
        if (runners.isEmpty()) return;

        // Randomly select a runner as the bounty target
        Player target = runners.get(random.nextInt(runners.size()));
        bountyTarget = target.getUniqueId();
        isBountyActive = true;

        // Apply glowing effect to make the target more visible (duration from config in seconds)
        int glowSec = Math.max(10, plugin.getConfig().getInt("bounty.glow_duration", 300));
        target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, glowSec * 20, 0));

        // Announce the new bounty
        announceBounty(target);
        lastAssignedAt = System.currentTimeMillis();
    }

    private void announceBounty(Player target) {
        Bukkit.broadcast(net.kyori.adventure.text.Component.text("\n§4§l=== BOUNTY ANNOUNCED ==="));
        Bukkit.broadcast(net.kyori.adventure.text.Component.text("§c" + target.getName() + " §ehas been marked for elimination!"));
        Bukkit.broadcast(net.kyori.adventure.text.Component.text("§eThe hunter who eliminates them will receive special rewards!"));

        // Play sound to all players
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);
        }
    }

    public void claimBounty(Player hunter) {
        if (!isBountyActive) return;
        isBountyActive = false;

        // Give rewards to the hunter
        giveRewards(hunter);

        // Announce bounty claimed
        Bukkit.broadcast(net.kyori.adventure.text.Component.text("\n§4§l=== BOUNTY CLAIMED ==="));
        Bukkit.broadcast(net.kyori.adventure.text.Component.text("§e" + hunter.getName() + " §ahas eliminated the bounty target!"));

        // Play victory sound
        hunter.playSound(hunter.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    }

    private void giveRewards(Player hunter) {
        // Reward durations in seconds, configurable
        int speedSec = Math.max(10, plugin.getConfig().getInt("bounty.rewards.speed_duration", 300));
        int strengthSec = Math.max(10, plugin.getConfig().getInt("bounty.rewards.strength_duration", 300));
        hunter.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, speedSec * 20, 0));
        hunter.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, strengthSec * 20, 0));
        
        // Heal the hunter
        hunter.setHealth(20.0);
        hunter.setFoodLevel(20);

        hunter.sendMessage(net.kyori.adventure.text.Component.text("§aYou have received bounty hunter rewards!"));
    }

    public boolean isBountyTarget(Player player) {
        return isBountyActive && player.getUniqueId().equals(bountyTarget);
    }

    public boolean isActive() {
        return isBountyActive;
    }

    public void clearBounty() {
        isBountyActive = false;
        bountyTarget = null;
    }
}
