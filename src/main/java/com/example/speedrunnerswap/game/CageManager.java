package com.example.speedrunnerswap.game;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public class CageManager {

    private final SpeedrunnerSwap plugin;
    private BukkitTask cageTask;

    // Shared cage management per-world (one cage in each world)
    private final Map<World, List<BlockState>> sharedCageBlocks = new HashMap<>();
    private final Map<World, Location> sharedCageCenters = new HashMap<>();
    private final Set<UUID> cagedPlayers = new HashSet<>();

    public CageManager(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
    }

    public void createOrEnsureSharedCage(World world) {
        if (world == null) world = Bukkit.getWorlds().get(0);
        Location base = plugin.getConfigManager().getLimboLocation();
        int y = world.getMaxHeight() - 10;
        int cx = (int) Math.round(base.getX());
        int cz = (int) Math.round(base.getZ());
        Location center = new Location(world, cx + 0.5, y, cz + 0.5);
        Location existing = sharedCageCenters.get(world);
        if (existing != null && Math.abs(existing.getX() - center.getX()) < 0.1 && Math.abs(existing.getY() - center.getY()) < 0.1 && Math.abs(existing.getZ() - center.getZ()) < 0.1) {
            return;
        }
        // Cleanup old cage in this world
        List<BlockState> old = sharedCageBlocks.remove(world);
        if (old != null) {
            for (BlockState s : old) { try { s.update(true, false); } catch (Exception ignored) {} }
        }
        try { center.getChunk().load(true); } catch (Throwable ignored) {}
        List<BlockState> changed = new ArrayList<>();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    Block block = world.getBlockAt(cx + dx, y + dy, cz + dz);
                    boolean isShell = (dx == -2 || dx == 2 || dz == -2 || dz == 2 || dy == -1 || dy == 2);
                    changed.add(block.getState());
                    block.setType(isShell ? Material.BEDROCK : Material.AIR, false);
                }
            }
        }
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                Block floor = world.getBlockAt(cx + dx, y - 1, cz + dz);
                changed.add(floor.getState());
                floor.setType(Material.BEDROCK, false);
            }
        }
        sharedCageBlocks.put(world, changed);
        sharedCageCenters.put(world, center.clone());
    }

    public void teleportToSharedCage(Player p) {
        if (p == null || !p.isOnline()) return;
        createOrEnsureSharedCage(p.getWorld());
        Location center = sharedCageCenters.get(p.getWorld());
        if (center != null) {
            // Teleport player to the center of the cage floor
            p.teleport(center);
            cagedPlayers.add(p.getUniqueId());
            try { p.setAllowFlight(true); } catch (Exception ignored) {}
            try { p.setFlying(false); } catch (Exception ignored) {}
        }
    }

    public void cleanupAllCages() {
        for (Map.Entry<World, List<BlockState>> e : sharedCageBlocks.entrySet()) {
            List<BlockState> list = e.getValue();
            if (list != null) for (BlockState s : list) { try { s.update(true, false); } catch (Exception ignored) {} }
        }
        sharedCageBlocks.clear();
        sharedCageCenters.clear();
        cagedPlayers.clear();
    }

    public void startCageEnforcement() {
        if (cageTask != null) { cageTask.cancel(); cageTask = null; }
        cageTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            GameManager gameManager = plugin.getGameManager();
            if (!gameManager.isGameRunning() || gameManager.isGamePaused()) return;
            if (!"CAGE".equalsIgnoreCase(plugin.getConfigManager().getFreezeMode())) return;
            // Ensure a cage exists in each runner's current world and enforce
            for (Player r : gameManager.getRunners()) {
                if (r.equals(gameManager.getActiveRunner())) continue;
                if (!r.isOnline()) continue;
                createOrEnsureSharedCage(r.getWorld());
                teleportToSharedCage(r);
                Location center = sharedCageCenters.get(r.getWorld());
                if (center == null) continue;
                Location loc = r.getLocation();
                double dx = Math.abs(loc.getX() - center.getX());
                double dy = loc.getY() - center.getY();
                double dz = Math.abs(loc.getZ() - center.getZ());
                boolean outside = dx > 1.2 || dz > 1.2 || dy < -0.2 || dy > 0.8;
                if (outside) {
                    r.teleport(center);
                    r.setVelocity(new Vector(0, 0, 0));
                    r.setFallDistance(0f);
                    r.setNoDamageTicks(Math.max(10, r.getNoDamageTicks()));
                } else {
                    try { r.setAllowFlight(true); } catch (Exception ignored) {}
                    try { r.setFlying(false); } catch (Exception ignored) {}
                }
            }
        }, 0L, 5L); // enforce every 0.25s
    }

    public boolean areBothPlayersInSharedCage(Player a, Player b) {
        if (a == null || b == null) return false;
        if (!cagedPlayers.contains(a.getUniqueId()) || !cagedPlayers.contains(b.getUniqueId())) return false;
        return a.getWorld().equals(b.getWorld()) && sharedCageCenters.containsKey(a.getWorld());
    }

    public void stopCageTask() {
        if (cageTask != null) {
            cageTask.cancel();
            cageTask = null;
        }
    }

    public void addCagedPlayer(UUID uuid) {
        cagedPlayers.add(uuid);
    }

    public void removeCagedPlayer(UUID uuid) {
        cagedPlayers.remove(uuid);
    }

    public boolean isCaged(Player player) {
        return cagedPlayers.contains(player.getUniqueId());
    }
}