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
        String out = input;
        // Remove legacy hex sequences first: §x§1§2§3§4§5§6 or &x&1&2&3&4&5&6
        out = out.replaceAll("(?i)\u00A7x(\u00A7[0-9A-F]){6}", "");
        out = out.replaceAll("(?i)&x(&[0-9A-F]){6}", "");
        // Then remove standard single legacy codes (section sign and ampersand variants)
        out = out.replaceAll("(?i)\u00A7[0-9A-FK-OR]", "");
        out = out.replaceAll("(?i)&[0-9A-FK-OR]", "");
        return out;
    }
}
