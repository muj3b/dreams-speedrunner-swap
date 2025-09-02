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

    public BountyManager(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
        this.random = new Random();
        this.isBountyActive = false;
    }

    public void assignNewBounty() {
        List<Player> runners = plugin.getGameManager().getRunners();
        if (runners.isEmpty()) return;

        // Randomly select a runner as the bounty target
        Player target = runners.get(random.nextInt(runners.size()));
        bountyTarget = target.getUniqueId();
        isBountyActive = true;

        // Apply glowing effect to make the target more visible
        target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 60 * 5, 0)); // 5 minutes

        // Announce the new bounty
        announceBounty(target);
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
        // Give permanent effects
        hunter.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 60 * 5, 0)); // Speed I for 5 minutes
        hunter.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 60 * 5, 0)); // Strength I for 5 minutes
        
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
