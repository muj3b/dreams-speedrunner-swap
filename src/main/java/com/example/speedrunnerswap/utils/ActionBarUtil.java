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

        // Fallback to Spigot API
        try {
            Object spigot = player.getClass().getMethod("spigot").invoke(player);
            Class<?> chatMessageType = Class.forName("net.md_5.bungee.api.ChatMessageType");
            Class<?> textComponent = Class.forName("net.md_5.bungee.api.chat.TextComponent");
            Object type = chatMessageType.getField("ACTION_BAR").get(null);
            Object tc = textComponent.getConstructor(String.class).newInstance(message);
            spigot.getClass().getMethod("sendMessage", chatMessageType, Class.forName("net.md_5.bungee.api.chat.BaseComponent")).invoke(spigot, type, tc);
        } catch (Throwable ignored) {
            // As a last resort, send as chat (not ideal but visible)
            player.sendMessage(message);
        }
    }
}
