package com.example.speedrunnerswap.utils;


import org.bukkit.entity.Player;

/**
 * Utility class for sending action bar messages
 */
public class ActionBarUtil {

    /**
     * Send an action bar message to a player
     * @param player The player to send the message to
     * @param message The message to send
     */
    public static void sendActionBar(Player player, String message) {
        player.sendActionBar(net.kyori.adventure.text.Component.text(message));
    }
}