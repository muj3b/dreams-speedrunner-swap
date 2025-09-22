package com.example.speedrunnerswap.utils;

/**
 * Lightweight text helpers that avoid deprecated Bukkit ChatColor API.
 */
public final class TextUtil {
    private TextUtil() {}

    /**
     * Strip Minecraft legacy color codes from a string.
     * Supports both section sign (§) and ampersand (&) variants.
     */
    public static String stripColors(String input) {
        if (input == null || input.isEmpty()) return input;
        // Remove §-based color/format codes (e.g., §a, §l, §r)
        String out = input.replaceAll("(?i)\u00A7[0-9A-FK-OR]", "");
        // Remove &-based color/format codes if present (e.g., &a)
        out = out.replaceAll("(?i)&[0-9A-FK-OR]", "");
        return out;
    }
}
