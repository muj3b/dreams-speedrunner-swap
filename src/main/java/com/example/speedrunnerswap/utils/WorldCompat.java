package com.example.speedrunnerswap.utils;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;

/** World identity, independent of the on-disk dimension layout introduced in 26.1. */
public final class WorldCompat {
    private WorldCompat() {}

    public static World resolve(String key, String legacyName) {
        if (key != null && !key.isBlank()) {
            NamespacedKey parsed = NamespacedKey.fromString(key);
            if (parsed != null) {
                // Iterate to avoid relying on a newer getWorld overload.
                for (World world : Bukkit.getWorlds()) if (world.getKey().equals(parsed)) return world;
            }
        }
        if (legacyName == null || legacyName.isBlank()) return null;
        World named = Bukkit.getWorld(legacyName);
        if (named != null) return named;
        // Also permit a key directly in old config fields without rewriting the config.
        if (legacyName.contains(":")) return resolve(legacyName, null);
        return null;
    }
}
