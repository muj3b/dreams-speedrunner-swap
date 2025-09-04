/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.World
 *  org.bukkit.block.Block
 */
package com.example.speedrunnerswap.utils;

import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public class SafeLocationFinder {
    public static Location findSafeLocation(Location location, int horizontalRadius, int verticalDistance, Set<Material> dangerousBlocks) {
        World world = location.getWorld();
        int startX = location.getBlockX();
        int startY = location.getBlockY();
        int startZ = location.getBlockZ();
        if (SafeLocationFinder.isSafeLocation(location, dangerousBlocks)) {
            return location;
        }
        for (int r = 1; r <= horizontalRadius; ++r) {
            for (int x = -r; x <= r; ++x) {
                for (int z = -r; z <= r; ++z) {
                    if (Math.abs(x) != r && Math.abs(z) != r) continue;
                    for (int y = 0; y <= verticalDistance; ++y) {
                        Location checkLoc = new Location(world, (double)(startX + x), (double)(startY - y), (double)(startZ + z));
                        if (SafeLocationFinder.isSafeLocation(checkLoc, dangerousBlocks)) {
                            return checkLoc;
                        }
                        checkLoc = new Location(world, (double)(startX + x), (double)(startY + y), (double)(startZ + z));
                        if (!SafeLocationFinder.isSafeLocation(checkLoc, dangerousBlocks)) continue;
                        return checkLoc;
                    }
                }
            }
        }
        return null;
    }

    private static boolean isSafeLocation(Location location, Set<Material> dangerousBlocks) {
        World world = location.getWorld();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        if (y < 0 || y >= world.getMaxHeight()) {
            return false;
        }
        Block feet = world.getBlockAt(x, y, z);
        Block head = world.getBlockAt(x, y + 1, z);
        Block ground = world.getBlockAt(x, y - 1, z);
        if (feet.getType().isSolid() || head.getType().isSolid()) {
            return false;
        }
        if (!ground.getType().isSolid()) {
            return false;
        }
        if (dangerousBlocks.contains(ground.getType())) {
            return false;
        }
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                Block surroundingBlock = world.getBlockAt(x + dx, y, z + dz);
                if (!dangerousBlocks.contains(surroundingBlock.getType())) continue;
                return false;
            }
        }
        return true;
    }
}

