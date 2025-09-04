package com.example.speedrunnerswap.utils;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;

/**
 * Small compatibility helpers for API changes across Bukkit/Spigot/Paper versions.
 */
public final class BukkitCompat {
    private BukkitCompat() {}

    /**
     * Resolve the max health attribute across versions.
     *
     * Newer APIs use GENERIC_MAX_HEALTH, older builds used MAX_HEALTH.
     * We resolve by name at runtime to avoid compile-time breakage and
     * provide a safe numeric fallback if the attribute instance is missing.
     */
    public static double getMaxHealthValue(LivingEntity entity) {
        Attribute attr = resolveAttribute("GENERIC_MAX_HEALTH", "MAX_HEALTH");
        if (attr != null) {
            AttributeInstance inst = entity.getAttribute(attr);
            if (inst != null) {
                return inst.getValue();
            }
        }
        // Final fallback: assume vanilla default 20.0 hearts if attribute missing
        // (still bounded by current health usages where applied).
        return 20.0D;
    }

    /**
     * Try to resolve an Attribute constant by name without referencing
     * potentially-removed fields directly. Tries names in order.
     */
    public static Attribute resolveAttribute(String... names) {
        for (String name : names) {
            try {
                return Attribute.valueOf(name);
            } catch (IllegalArgumentException ignored) {
                // try next
            }
        }
        return null;
    }

    /**
     * Resolve a PotionEffectType by id with common alias handling, compatible with older/newer APIs.
     * Prefers modern keys (e.g., "slowness", "jump_boost"), but maps legacy names as needed.
     */
    public static PotionEffectType resolvePotionEffect(String id) {
        if (id == null) return null;
        String key = id.toLowerCase(java.util.Locale.ROOT).trim();
        // Normalize common legacy aliases to modern names
        key = switch (key) {
            case "increase_damage" -> "strength";
            case "damage_resistance" -> "resistance";
            case "slow" -> "slowness";
            case "jump" -> "jump_boost";
            case "slow_digging" -> "mining_fatigue";
            case "confusion" -> "nausea";
            default -> key;
        };

        // Use the classic resolver which exists across many versions
        PotionEffectType byName = PotionEffectType.getByName(key.toUpperCase(java.util.Locale.ROOT));
        if (byName != null) return byName;

        // Try namespaced-key resolver if available at runtime
        try {
            Class<?> nsk = Class.forName("org.bukkit.NamespacedKey");
            java.lang.reflect.Method minecraft = nsk.getMethod("minecraft", String.class);
            Object namespacedKey = minecraft.invoke(null, key);
            java.lang.reflect.Method getByKey = PotionEffectType.class.getMethod("getByKey", nsk);
            Object type = getByKey.invoke(null, namespacedKey);
            if (type instanceof PotionEffectType) return (PotionEffectType) type;
        } catch (Throwable ignored) {
        }

        return null;
    }
}
