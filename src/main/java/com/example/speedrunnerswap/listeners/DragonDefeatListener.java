package com.example.speedrunnerswap.listeners;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import com.example.speedrunnerswap.models.Team;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

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
        plugin.getGameManager().endGame(Team.RUNNER);

        // Create celebration title for all players
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Show title
            player.showTitle(net.kyori.adventure.title.Title.title(
                net.kyori.adventure.text.Component.text("CONGRATULATIONS!", net.kyori.adventure.text.format.NamedTextColor.GOLD, net.kyori.adventure.text.format.TextDecoration.BOLD),
                net.kyori.adventure.text.Component.text("Runners have defeated the Ender Dragon!", net.kyori.adventure.text.format.NamedTextColor.GREEN, net.kyori.adventure.text.format.TextDecoration.BOLD),
                net.kyori.adventure.title.Title.Times.times(
                    java.time.Duration.ofMillis(10 * 50),
                    java.time.Duration.ofMillis(100 * 50),
                    java.time.Duration.ofMillis(20 * 50)
                )
            ));

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
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("\n=== Support the Creator ===")
                .color(net.kyori.adventure.text.format.NamedTextColor.GOLD)
                .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD), Server.BROADCAST_CHANNEL_USERS);
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("Enjoy the plugin? Consider supporting the creator (muj3b)!")
                .color(net.kyori.adventure.text.format.NamedTextColor.YELLOW), Server.BROADCAST_CHANNEL_USERS);
            
            // Create clickable donation link using Adventure API
            net.kyori.adventure.text.Component donateMessage = net.kyori.adventure.text.Component.text("[Click here to donate]")
                .color(net.kyori.adventure.text.format.NamedTextColor.GREEN)
                .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD)
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.openUrl("https://donate.stripe.com/cNicN5gG3f8ocU4cjN0Ba00"))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                    net.kyori.adventure.text.Component.text("Click to support the creator!")
                        .color(net.kyori.adventure.text.format.NamedTextColor.YELLOW)
                ));
            
            // Send to all players
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(donateMessage);
            }
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("Thank you for playing SpeedrunnerSwap!")
                .color(net.kyori.adventure.text.format.NamedTextColor.GOLD), Server.BROADCAST_CHANNEL_USERS);
        }, 100L); // 5 seconds after victory message
    }
}