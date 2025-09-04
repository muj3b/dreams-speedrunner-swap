/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.EnderDragon
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.EntityDeathEvent
 */
package com.example.speedrunnerswap.listeners;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import com.example.speedrunnerswap.models.Team;
import org.bukkit.entity.EnderDragon;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class DragonDefeatListener
implements Listener {
    private final SpeedrunnerSwap plugin;

    public DragonDefeatListener(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEnderDragonDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof EnderDragon) || !this.plugin.getGameManager().isGameRunning()) {
            return;
        }
        this.plugin.getGameManager().endGame(Team.RUNNER);
    }
}

