/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.entity.Player
 */
package com.example.speedrunnerswap.utils;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class ActionBarUtil {
    public static void sendActionBar(Player player, String message) {
        player.sendActionBar((Component)Component.text((String)message));
    }
}

