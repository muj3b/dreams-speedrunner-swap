package com.example.speedrunnerswap.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Simple broadcasting helpers that avoid deprecated Bukkit#broadcastMessage.
 * Uses Adventure components in 1.21.8, with legacy-section deserialization for
 * existing colored string messages.
 */
public final class Msg {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private Msg() {}

    public static void broadcast(String legacyText) {
        broadcast(LEGACY.deserialize(legacyText));
    }

    public static void broadcast(Component component) {
        // Send to all online players
        for (Player p : Bukkit.getOnlinePlayers()) {
            try {
                p.sendMessage(component);
            } catch (Throwable ignored) {
                // extremely old implementations might not support Component
                p.sendMessage(LEGACY.serialize(component));
            }
        }
        // Also echo to console so operators see it
        try {
            CommandSender console = Bukkit.getConsoleSender();
            console.sendMessage(component);
        } catch (Throwable ignored) {
            Bukkit.getLogger().info(LEGACY.serialize(component));
        }
    }

    public static void send(Player player, String legacyText) {
        if (player == null) return;
        send(player, LEGACY.deserialize(legacyText));
    }

    public static void send(CommandSender sender, String legacyText) {
        if (sender == null) return;
        send(sender, LEGACY.deserialize(legacyText));
    }

    public static void send(Player player, Component component) {
        if (player == null) return;
        try {
            player.sendMessage(component);
        } catch (Throwable ignored) {
            player.sendMessage(LEGACY.serialize(component));
        }
    }

    public static void send(CommandSender sender, Component component) {
        if (sender == null) return;
        try {
            sender.sendMessage(component);
        } catch (Throwable ignored) {
            sender.sendMessage(LEGACY.serialize(component));
        }
    }
}
