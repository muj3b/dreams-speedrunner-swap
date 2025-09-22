package com.example.speedrunnerswap.utils;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.Location;
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
        // Use reflection to access enum fields directly, avoiding deprecated
        // helper methods like values() or name() present in newer APIs.
        for (String name : names) {
            try {
                java.lang.reflect.Field f = Attribute.class.getField(name);
                Object v = f.get(null);
                if (v instanceof Attribute) return (Attribute) v;
            } catch (Throwable ignored) {
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
            case "damage_resistance", "resistance" -> "resistance";
            case "slow", "slowness" -> "slowness";
            case "jump" -> "jump_boost";
            case "slow_digging" -> "mining_fatigue";
            case "confusion", "nausea" -> "nausea";
            case "glow", "glowing" -> "glowing";
            case "dolphin", "dolphins_grace" -> "dolphins_grace";
            case "hero_of_the_village", "hov" -> "hero_of_the_village";
            default -> key;
        };

        // Prefer modern getByKey via NamespacedKey (handled below).
        // Then, as a legacy fallback, try getByName via reflection to avoid
        // compile-time deprecation warnings on 1.21+.

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

        // Legacy fallback via reflection call to getByName
        try {
            java.lang.reflect.Method getByName = PotionEffectType.class.getMethod("getByName", String.class);
            Object type = getByName.invoke(null, key.toUpperCase(java.util.Locale.ROOT));
            if (type instanceof PotionEffectType) return (PotionEffectType) type;
        } catch (Throwable ignored) {
        }

        return null;
    }

    /**
     * Get server TPS in a cross-platform way. Returns the 1-minute TPS value if available.
     * Uses reflection to avoid hard dependency on Paper's Bukkit#getTPS().
     *
     * @return TPS value as Double, or null if not available on this platform
     */
    public static Double getServerTPS() {
        try {
            // Paper exposes static double[] Bukkit#getTPS()
            java.lang.reflect.Method m = org.bukkit.Bukkit.class.getMethod("getTPS");
            Object tpsObj = m.invoke(null);
            if (tpsObj instanceof double[] arr && arr.length > 0) {
                return arr[0];
            }
        } catch (Throwable ignored) {
            // Not Paper or method not available
        }
        return null;
    }

    /**
     * Show a title to a player in a cross-platform way. Uses Adventure Title API when available (Paper),
     * otherwise falls back to Player#sendTitle on Spigot/Bukkit.
     */
    public static void showTitle(org.bukkit.entity.Player player, String title, String subtitle,
                                 int fadeInTicks, int stayTicks, int fadeOutTicks) {
        if (player == null) return;
        // Try Adventure (Paper)
        try {
            Class<?> compCls = Class.forName("net.kyori.adventure.text.Component");
            Class<?> titleCls = Class.forName("net.kyori.adventure.title.Title");
            Class<?> timesCls = Class.forName("net.kyori.adventure.title.Title$Times");

            java.lang.reflect.Method text = compCls.getMethod("text", String.class);
            Object main = text.invoke(null, title);
            Object sub = text.invoke(null, subtitle);

            Class<?> durationCls = Class.forName("java.time.Duration");
            java.lang.reflect.Method ofMillis = durationCls.getMethod("ofMillis", long.class);
            Object fadeIn = ofMillis.invoke(null, fadeInTicks * 50L);
            Object stay = ofMillis.invoke(null, stayTicks * 50L);
            Object fadeOut = ofMillis.invoke(null, fadeOutTicks * 50L);

            java.lang.reflect.Method timesFactory = timesCls.getMethod("times", durationCls, durationCls, durationCls);
            Object times = timesFactory.invoke(null, fadeIn, stay, fadeOut);

            java.lang.reflect.Method titleFactory = titleCls.getMethod("title", compCls, compCls, timesCls);
            Object advTitle = titleFactory.invoke(null, main, sub, times);

            java.lang.reflect.Method showTitle = player.getClass().getMethod("showTitle", titleCls);
            showTitle.invoke(player, advTitle);
            return;
        } catch (Throwable ignored) {
            // Adventure not present; fall through
        }

        // Fallback to legacy Spigot API
        try {
            // Player#sendTitle(String, String, int, int, int)
            java.lang.reflect.Method send = player.getClass().getMethod("sendTitle", String.class, String.class, int.class, int.class, int.class);
            send.invoke(player, title, subtitle, fadeInTicks, stayTicks, fadeOutTicks);
        } catch (Throwable ignored) {
            // As ultimate fallback, send chat messages
            try {
                player.sendMessage("§6§l" + title);
                if (subtitle != null && !subtitle.isEmpty()) player.sendMessage("§7" + subtitle);
            } catch (Throwable ignored2) {}
        }
    }

    /**
     * Resolve targeted entity for a player across Paper/Bukkit versions.
     * Tries Player#getTargetEntity(int, boolean), then Player#getTargetEntity(int),
     * finally falls back to a ray trace against entities.
     */
    public static Entity getTargetEntity(Player player, int maxDistance) {
        if (player == null) return null;
        try {
            // Paper 1.19+: getTargetEntity(int maxDistance, boolean ignoreBlocks)
            java.lang.reflect.Method m = Player.class.getMethod("getTargetEntity", int.class, boolean.class);
            Object ent = m.invoke(player, maxDistance, false);
            return (Entity) ent;
        } catch (NoSuchMethodException ignored) {
        } catch (Throwable t) {
            // continue
        }
        try {
            // Older Paper: getTargetEntity(int maxDistance)
            java.lang.reflect.Method m = Player.class.getMethod("getTargetEntity", int.class);
            Object ent = m.invoke(player, maxDistance);
            return (Entity) ent;
        } catch (NoSuchMethodException ignored) {
        } catch (Throwable t) {
        }

        // Vanilla Bukkit fallback using ray trace
        try {
            Location eye = player.getEyeLocation();
            org.bukkit.util.Vector dir = eye.getDirection();
            org.bukkit.util.RayTraceResult res = player.getWorld().rayTraceEntities(eye, dir, maxDistance, e -> e != player);
            if (res != null) {
                return res.getHitEntity();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
