package com.example.speedrunnerswap.game;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class CompassManager {
    private final SpeedrunnerSwap plugin;
    private final Map<UUID, Boolean> jammedCompasses;
    private final Random random;
    private BukkitTask updateTask;

    public CompassManager(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
        this.jammedCompasses = new HashMap<>();
        this.random = new Random();
    }

    public void startTracking() {
        if (updateTask != null) {
            updateTask.cancel();
        }

        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateCompasses, 
            0L, plugin.getConfig().getInt("tracker.update_ticks", 20));
    }

    public void stopTracking() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        jammedCompasses.clear();
    }

    public void jamCompass(Player hunter) {
        if (!plugin.getConfig().getBoolean("tracker.compass_jamming.enabled", true)) {
            return;
        }

        jammedCompasses.put(hunter.getUniqueId(), true);
        hunter.sendMessage(net.kyori.adventure.text.Component.text("§c§lYour compass has been jammed!"));

        // Schedule unjam
        int jamDuration = plugin.getConfig().getInt("tracker.compass_jamming.duration_ticks", 100);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            jammedCompasses.remove(hunter.getUniqueId());
            hunter.sendMessage(net.kyori.adventure.text.Component.text("§a§lYour compass has been unjammed!"));
        }, jamDuration);
    }

    private void updateCompasses() {
        Player activeRunner = plugin.getGameManager().getActiveRunner();
        if (activeRunner == null) return;

        for (Player hunter : plugin.getGameManager().getHunters()) {
            if (!hunter.isOnline()) continue;

            ItemStack compass = hunter.getInventory().getItemInMainHand();
            if (compass.getType() != org.bukkit.Material.COMPASS) {
                compass = hunter.getInventory().getItemInOffHand();
                if (compass.getType() != org.bukkit.Material.COMPASS) {
                    continue;
                }
            }

            Location targetLocation;
            if (jammedCompasses.getOrDefault(hunter.getUniqueId(), false)) {
                // Generate random location when jammed
                targetLocation = getRandomLocation(hunter.getWorld(), hunter.getLocation(), 100);
            } else {
                targetLocation = activeRunner.getLocation();
            }

            CompassMeta meta = (CompassMeta) compass.getItemMeta();
            meta.setLodestone(targetLocation);
            meta.setLodestoneTracked(false);
            compass.setItemMeta(meta);
        }
    }

    private Location getRandomLocation(World world, Location center, int radius) {
        double angle = random.nextDouble() * 2 * Math.PI;
        double distance = random.nextDouble() * radius;
        double x = center.getX() + distance * Math.cos(angle);
        double z = center.getZ() + distance * Math.sin(angle);
        return new Location(world, x, center.getY(), z);
    }

    public void clearJamming() {
        jammedCompasses.clear();
    }

    public boolean isJammed(Player hunter) {
        return jammedCompasses.getOrDefault(hunter.getUniqueId(), false);
    }
}
