package com.example.speedrunnerswap.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Set;

/**
 * Utility class for finding safe locations for swaps
 */
public class SafeLocationFinder {

    /**
     * Find a safe location near the given location
     * @param location The starting location
     * @param horizontalRadius The horizontal radius to search
     * @param verticalDistance The vertical distance to search
     * @param dangerousBlocks Set of dangerous block materials to avoid
     * @return A safe location, or null if none found
     */
    public static Location findSafeLocation(Location location, int horizontalRadius, int verticalDistance, Set<Material> dangerousBlocks) {
        World world = location.getWorld();
        int startX = location.getBlockX();
        int startY = location.getBlockY();
        int startZ = location.getBlockZ();
        
        // First check if current location is safe
        if (isSafeLocation(location, dangerousBlocks)) {
            return location;
        }
        
        // Search in a spiral pattern outward from the starting location
        for (int r = 1; r <= horizontalRadius; r++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    // Only check blocks on the edge of the current radius
                    if (Math.abs(x) != r && Math.abs(z) != r) {
                        continue;
                    }
                    
                    // Check vertical column
                    for (int y = 0; y <= verticalDistance; y++) {
                        // Check below first
                        Location checkLoc = new Location(world, startX + x, startY - y, startZ + z);
                        if (isSafeLocation(checkLoc, dangerousBlocks)) {
                            return checkLoc;
                        }
                        
                        // Then check above
                        checkLoc = new Location(world, startX + x, startY + y, startZ + z);
                        if (isSafeLocation(checkLoc, dangerousBlocks)) {
                            return checkLoc;
                        }
                    }
                }
            }
        }
        
        // No safe location found
        return null;
    }
    
    /**
     * Check if a location is safe
     * @param location The location to check
     * @param dangerousBlocks Set of dangerous block materials to avoid
     * @return True if the location is safe
     */
    private static boolean isSafeLocation(Location location, Set<Material> dangerousBlocks) {
        World world = location.getWorld();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        
        // Check if the location is in the void
        if (y < world.getMinHeight() || y >= world.getMaxHeight()) {
            return false;
        }
        
        // Get blocks at and around the location
        Block feet = world.getBlockAt(x, y, z);
        Block head = world.getBlockAt(x, y + 1, z);
        Block ground = world.getBlockAt(x, y - 1, z);
        
        // Check if the player has room to stand (feet and head must be non-solid)
        if (feet.getType().isSolid() || head.getType().isSolid()) {
            return false;
        }
        
        // Check if the ground is solid
        if (!ground.getType().isSolid()) {
            return false;
        }
        
        // Check if the ground is a dangerous block
        if (dangerousBlocks.contains(ground.getType())) {
            return false;
        }
        
        // Check surrounding blocks for dangerous materials
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Block surroundingBlock = world.getBlockAt(x + dx, y, z + dz);
                if (dangerousBlocks.contains(surroundingBlock.getType())) {
                    return false;
                }
            }
        }
        
        return true;
    }
}
