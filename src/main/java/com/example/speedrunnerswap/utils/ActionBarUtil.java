package com.example.speedrunnerswap.utils;

import org.bukkit.entity.Player;

/**
 * Utility for sending action bar messages with compatibility fallbacks.
 *
 * - Primary: Paper/Adventure Player#sendActionBar(Component)
 * - Fallback: Spigot ChatMessageType.ACTION_BAR path via reflection
 */
public class ActionBarUtil {

    public static void sendActionBar(Player player, String message) {
        if (player == null) return;

        // Try Paper/Adventure API first using reflection to avoid hard dependency at runtime
        try {
            Class<?> compClass = Class.forName("net.kyori.adventure.text.Component");
            java.lang.reflect.Method text = compClass.getMethod("text", String.class);
            Object component = text.invoke(null, message);
            java.lang.reflect.Method send = player.getClass().getMethod("sendActionBar", compClass);
            send.invoke(player, component);
            return;
        } catch (Throwable ignored) {
        }

        // NOTE: Fallback deliberately avoids ChatMessageType.ACTION_BAR to preserve cross-compat with Spigot/Paper 1.21 changes
        // Fallback: send as plain chat (keeps compatibility, avoids legacy bungee API)
        player.sendMessage(message);
    }
}
