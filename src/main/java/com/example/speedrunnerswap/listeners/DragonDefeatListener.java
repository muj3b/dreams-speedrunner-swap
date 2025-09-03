package com.example.speedrunnerswap.listeners;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import com.example.speedrunnerswap.models.Team;
import org.bukkit.entity.EnderDragon;
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
    }
}
