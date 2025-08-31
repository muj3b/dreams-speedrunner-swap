package com.example.speedrunnerswap.listeners;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import com.example.speedrunnerswap.models.PlayerState;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;

public class DragonDefeatListener implements Listener {
    private final SpeedrunnerSwap plugin;

    public DragonDefeatListener(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEnderDragonDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof EnderDragon) || !plugin.getGameManager().isGameRunning()) {
            return;
        }

        // End the game with runners as winners
        plugin.getGameManager().endGame(PlayerState.Team.RUNNER);

        // Create celebration title for all players
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Show title
            player.sendTitle(
                "§6§lCONGRATULATIONS!",
                "§a§lRunners have defeated the Ender Dragon!",
                10, 100, 20
            );

            // Different messages based on team
            if (plugin.getGameManager().isRunner(player)) {
                player.sendMessage("§6§l=== VICTORY! ===");
                player.sendMessage("§aYou've successfully defeated the Ender Dragon!");
                player.sendMessage("§eThe hunters couldn't stop you this time!");
            } else {
                player.sendMessage("§c§l=== GAME OVER ===");
                player.sendMessage("§cThe runners have succeeded in their mission.");
                player.sendMessage("§eBetter luck next time, hunters!");
            }
        }

        // Broadcast donation message
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.broadcastMessage("\n§6§l=== Support the Creator ===");
            Bukkit.broadcastMessage("§eEnjoy the plugin? Consider supporting the creator (muj3b)!");
            
            // Create clickable donation link
            TextComponent donateMessage = new TextComponent("§a§l[Click here to donate]");
            donateMessage.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://donate.stripe.com/cNicN5gG3f8ocU4cjN0Ba00"));
            donateMessage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                new ComponentBuilder("§eClick to support the creator!").create()));
            
            // Send to all players
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.spigot().sendMessage(donateMessage);
            }
            Bukkit.broadcastMessage("§6Thank you for playing SpeedrunnerSwap!");
        }, 100L); // 5 seconds after victory message
    }
}