package com.example.speedrunnerswap.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.time.Duration;

public final class ChatTitleCompat {
    private ChatTitleCompat() {}

    public static void showTitle(Player player, String title, String subtitle, long fadeInMs, long stayMs, long fadeOutMs) {
        try {
            Title t = Title.title(
                    Component.text(title).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
                    Component.text(subtitle).color(NamedTextColor.YELLOW),
                    Title.Times.times(Duration.ofMillis(fadeInMs), Duration.ofMillis(stayMs), Duration.ofMillis(fadeOutMs))
            );
            player.showTitle(t);
        } catch (Throwable ignored) {
            sendDeprecatedTitle(player, title, subtitle, fadeInMs, stayMs, fadeOutMs);
        }
    }

    @SuppressWarnings("deprecation")
    private static void sendDeprecatedTitle(Player player, String title, String subtitle, long fadeInMs, long stayMs, long fadeOutMs) {
        player.sendTitle(title, subtitle, (int) (fadeInMs / 50), (int) (stayMs / 50), (int) (fadeOutMs / 50));
    }

    public static void sendMessage(Player player, String message) {
        try {
            Component parsed = LegacyComponentSerializer.legacySection().deserialize(message);
            player.sendMessage(parsed);
        } catch (Throwable ignored) {
            player.sendMessage(message);
        }
    }

    public static void sendClickableUrl(Player player, String prefix, String url) {
        try {
            Component msg = Component.text(prefix)
                    .append(Component.text(url, NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)
                            .hoverEvent(HoverEvent.showText(Component.text("Open donation page", NamedTextColor.GOLD)))
                            .clickEvent(ClickEvent.openUrl(url)));
            player.sendMessage(msg);
        } catch (Throwable ignored) {
            player.sendMessage(prefix + url);
        }
    }
}
