package com.example.speedrunnerswap.task;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Event listener that detects task completions for Task Manager mode.
 */
public class TaskEventListener implements Listener {
    private final SpeedrunnerSwap plugin;

    public TaskEventListener(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
    }

    private boolean isTaskMode() {
        return plugin.getCurrentMode() == SpeedrunnerSwap.SwapMode.TASK && plugin.getGameManager().isGameRunning();
    }

    @SuppressWarnings("unused")
    private boolean hasTask(Player p, String id) {
        return plugin.getTaskManagerMode().getAssignedTask(p) != null && plugin.getTaskManagerMode().getAssignedTask(p).equals(id);
    }

    private String getTask(Player p) { return plugin.getTaskManagerMode().getAssignedTask(p); }

    // Record bed exploder in Nether to attribute golem death
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBedInteract(PlayerInteractEvent event) {
        if (!isTaskMode()) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        Material m = event.getClickedBlock().getType();
        if (!m.name().endsWith("_BED")) return;
        World.Environment env = event.getPlayer().getWorld().getEnvironment();
        if (env == World.Environment.NETHER || env == World.Environment.THE_END) {
            // Beds explode in these dims
            plugin.getTaskManagerMode().markBedExploder(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!isTaskMode()) return;
        if (event.getEntity() instanceof IronGolem golem) {
            if (golem.getWorld().getEnvironment() == World.Environment.NETHER) {
                EntityDamageEvent cause = golem.getLastDamageCause();
                if (cause != null && (cause.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION || cause.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION)) {
                    UUID attr = plugin.getTaskManagerMode().getRecentBedExploder(golem.getWorld().getName());
                    if (attr != null) {
                        Player p = Bukkit.getPlayer(attr);
                        if (p != null && p.isOnline()) {
                            String id = getTask(p);
                            if ("kill_golem_nether_bed".equals(id)) {
                                plugin.getTaskManagerMode().complete(p);
                            }
                        }
                    }
                }
            }
        }
        // Kill entity generic
        if (event.getEntity().getKiller() != null) {
            Player killer = event.getEntity().getKiller();
            String id = getTask(killer);
            if (id != null && id.startsWith("kill_") && !id.equals("kill_golem_nether_bed") && !id.equals("kill_all_sheep_iron_shovel")) {
                // id format kill_<ENTITY>
                String target = id.substring("kill_".length()).toUpperCase();
                if (event.getEntityType().name().equals(target)) {
                    plugin.getTaskManagerMode().complete(killer);
                }
            }
            // Special: kill colored sheep with iron shovel (track progress)
            if (event.getEntityType() == EntityType.SHEEP && id != null && id.equals("kill_all_sheep_iron_shovel")) {
                ItemStack hand = killer.getInventory().getItemInMainHand();
                if (hand != null && hand.getType() == Material.IRON_SHOVEL) {
                    org.bukkit.entity.Sheep sheep = (org.bukkit.entity.Sheep) event.getEntity();
                    EnumSet<DyeColor> set = plugin.getTaskManagerMode().sheepKilledWithIronShovel.computeIfAbsent(killer.getUniqueId(), u -> EnumSet.noneOf(DyeColor.class));
                    set.add(sheep.getColor());
                    if (set.size() >= DyeColor.values().length) {
                        plugin.getTaskManagerMode().complete(killer);
                    } else {
                        killer.sendMessage("§eSheep colors killed (iron shovel): §a"+ set.size()+"/"+DyeColor.values().length);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    @SuppressWarnings("deprecation")
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!isTaskMode()) return;
        Player p = event.getEntity();
        String id = getTask(p);
        if (id == null) return;
        if (id.equals("die_on_bedrock_fall")) {
            if (event.getDeathMessage() != null) {
                if (event.getEntity().getLastDamageCause() != null && event.getEntity().getLastDamageCause().getCause() == EntityDamageEvent.DamageCause.FALL) {
                    Location loc = p.getLocation();
                    Location below = loc.clone().subtract(0,1,0);
                    if (below.getBlock().getType() == Material.BEDROCK) {
                        // require substantial fall distance (heuristic)
                        if (p.getFallDistance() >= 20f) {
                            plugin.getTaskManagerMode().complete(p);
                        }
                    }
                }
            }
        } else if (id.startsWith("die_")) {
            String cause = id.substring("die_".length()).toUpperCase();
            EntityDamageEvent last = p.getLastDamageCause();
            if (last != null) {
                switch (cause) {
                    case "LAVA" -> {
                        if (last.getCause() == EntityDamageEvent.DamageCause.LAVA) plugin.getTaskManagerMode().complete(p);
                    }
                    case "FIRE" -> {
                        if (last.getCause() == EntityDamageEvent.DamageCause.FIRE || last.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK) plugin.getTaskManagerMode().complete(p);
                    }
                    case "FALL" -> { if (last.getCause() == EntityDamageEvent.DamageCause.FALL) plugin.getTaskManagerMode().complete(p); }
                    case "VOID" -> { if (last.getCause() == EntityDamageEvent.DamageCause.VOID) plugin.getTaskManagerMode().complete(p); }
                    case "DROWNING" -> { if (last.getCause() == EntityDamageEvent.DamageCause.DROWNING) plugin.getTaskManagerMode().complete(p); }
                    case "EXPLOSION" -> { if (last.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION || last.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) plugin.getTaskManagerMode().complete(p); }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (!isTaskMode()) return;
        Player p = event.getPlayer();
        String id = getTask(p);
        if (id == null) return;
        World.Environment env = p.getWorld().getEnvironment();
        if (id.equals("enter_nether") && env == World.Environment.NETHER) plugin.getTaskManagerMode().complete(p);
        if (id.equals("enter_end") && env == World.Environment.THE_END) plugin.getTaskManagerMode().complete(p);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!isTaskMode()) return;
        if (!(event.getWhoClicked() instanceof Player p)) return;
        String id = getTask(p);
        if (id == null) return;
        if (!id.startsWith("craft_")) return;
        String matName = id.substring("craft_".length()).toUpperCase();
        Material expected = Material.matchMaterial(matName);
        if (expected == null) return;
        if (event.getRecipe() != null && event.getRecipe().getResult() != null && event.getRecipe().getResult().getType() == expected) {
            plugin.getTaskManagerMode().complete(p);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!isTaskMode()) return;
        Player p = event.getPlayer();
        String id = getTask(p);
        if (id == null) return;
        if (!id.startsWith("mine_")) return;
        String blockName = id.substring("mine_".length()).toUpperCase();
        if (event.getBlock().getType().name().equals(blockName)) {
            plugin.getTaskManagerMode().complete(p);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!isTaskMode()) return;
        Player p = event.getPlayer();
        String id = getTask(p);
        if (id == null) return;
        if (!id.startsWith("place_")) return;
        String blockName = id.substring("place_".length()).toUpperCase();
        if (event.getBlockPlaced().getType().name().equals(blockName)) {
            plugin.getTaskManagerMode().complete(p);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShear(org.bukkit.event.player.PlayerShearEntityEvent event) {
        if (!isTaskMode()) return;
        if (!(event.getEntity() instanceof org.bukkit.entity.Sheep sheep)) return;
        Player p = event.getPlayer();
        String id = getTask(p);
        if (id == null) return;
        if (!id.startsWith("shear_")) return;
        // Expected format: shear_<color>_sheep
        String expectedColor = id.replace("shear_","{}").replace("_sheep","").replace("{}","").toUpperCase();
        if (sheep.getColor().name().equals(expectedColor)) {
            plugin.getTaskManagerMode().complete(p);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTame(org.bukkit.event.entity.EntityTameEvent event) {
        if (!isTaskMode()) return;
        if (!(event.getOwner() instanceof Player p)) return;
        String id = getTask(p);
        if (id == null) return;
        if (!id.startsWith("tame_")) return;
        String type = id.substring("tame_".length()).toUpperCase();
        if (event.getEntityType().name().equals(type)) plugin.getTaskManagerMode().complete(p);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreed(org.bukkit.event.entity.EntityBreedEvent event) {
        if (!isTaskMode()) return;
        if (!(event.getBreeder() instanceof Player p)) return;
        String id = getTask(p);
        if (id == null) return;
        if (!id.startsWith("breed_")) return;
        String type = id.substring("breed_".length()).toUpperCase();
        if (event.getEntityType().name().equals(type)) plugin.getTaskManagerMode().complete(p);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (!isTaskMode()) return;
        Player p = event.getPlayer();
        String id = getTask(p);
        if (id == null) return;
        if (!id.startsWith("eat_")) return;
        String matName = id.substring("eat_".length()).toUpperCase();
        Material m = Material.matchMaterial(matName);
        if (m != null && event.getItem() != null && event.getItem().getType() == m) {
            plugin.getTaskManagerMode().complete(p);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (!isTaskMode()) return;
        if (!(event.getState() == PlayerFishEvent.State.CAUGHT_FISH)) return;
        Player p = event.getPlayer();
        String id = getTask(p);
        if ("fish_any".equals(id)) plugin.getTaskManagerMode().complete(p);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(org.bukkit.event.player.PlayerMoveEvent event) {
        if (!isTaskMode()) return;
        Player p = event.getPlayer();
        String id = getTask(p);
        if (id == null) return;
        if (id.startsWith("reach_y_")) {
            try {
                int target = Integer.parseInt(id.substring("reach_y_".length()));
                if ((int) Math.round(p.getLocation().getY()) == target) {
                    plugin.getTaskManagerMode().complete(p);
                }
            } catch (Exception ignored) {}
        }
    }
}
