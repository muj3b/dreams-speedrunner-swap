package com.example.speedrunnerswap.task;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles Task Manager mode: assigns tasks and tracks per-player state.
 */
public class TaskManagerMode {
    private final SpeedrunnerSwap plugin;

    // Per-player assigned task id
    private final Map<UUID, String> assignments = new HashMap<>();
    // Progress maps for complex tasks
    final Map<UUID, EnumSet<org.bukkit.DyeColor>> sheepKilledWithIronShovel = new HashMap<>();
    // Bed exploder attribution per world
    final Map<String, UUID> lastBedExploderPerWorld = new HashMap<>();

    // Task registry: id -> definition
    private final Map<String, TaskDefinition> registry = new LinkedHashMap<>();
    private final List<String> taskPool = new ArrayList<>();

    public TaskManagerMode(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
        loadTasks();
        // Load any persisted runtime assignments (if present)
        loadAssignmentsFromConfig();
    }

    public String getAssignedTask(Player p) {
        return assignments.get(p.getUniqueId());
    }

    public void assignAndAnnounceTasks(List<Player> players) {
        assignments.clear();
        // Build a randomized selection without duplicates (wrap if more players than tasks)
        List<String> shuffled = new ArrayList<>(taskPool);
        Collections.shuffle(shuffled, new Random());
        int idx = 0;
        for (Player p : players) {
            String taskId = shuffled.get(idx % shuffled.size());
            assignments.put(p.getUniqueId(), taskId);
            TaskDefinition def = registry.get(taskId);
            announceTask(p, def);
            idx++;
        }
        saveAssignmentsToConfig();
    }

    private void announceTask(Player p, TaskDefinition def) {
        if (p == null || def == null) return;
        Title title = Title.title(
                Component.text("Your Task:").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
                Component.text(def.description()).color(NamedTextColor.YELLOW),
                Title.Times.times(Duration.ofMillis(400), Duration.ofMillis(3000), Duration.ofMillis(600))
        );
        p.showTitle(title);
        p.sendMessage(Component.text("[Task Manager] Your task:")
                .color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        p.sendMessage(Component.text(" → "+ def.description()).color(NamedTextColor.YELLOW));
    }

    /** Call when a player has completed their task */
    public void complete(Player p) {
        if (p == null) return;
        String taskId = assignments.get(p.getUniqueId());
        if (taskId == null) return; // not assigned
        // Announce winner and stop the game
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player pl : Bukkit.getOnlinePlayers()) {
                Title t = Title.title(
                        Component.text(p.getName()+" completed their task!").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD),
                        Component.text("Task: "+registry.get(taskId).description()).color(NamedTextColor.YELLOW),
                        Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(4000), Duration.ofMillis(800))
                );
                pl.showTitle(t);
            }
            Bukkit.broadcast(Component.text("[Task Manager] Winner: "+p.getName()).color(NamedTextColor.GREEN));
            try { plugin.getGameManager().stopGame(); } catch (Throwable ignored) {}
        });
    }

    public Map<UUID, String> getAssignments() {
        return Collections.unmodifiableMap(assignments);
    }

    public void saveAssignmentsToConfig() {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            for (var e : assignments.entrySet()) {
                map.put(e.getKey().toString(), e.getValue());
            }
            plugin.getConfig().set("task_manager.runtime.assignments", map);
            plugin.saveConfig();
        } catch (Throwable ignored) {}
    }

    @SuppressWarnings("unchecked")
    public void loadAssignmentsFromConfig() {
        try {
            Object raw = plugin.getConfig().get("task_manager.runtime.assignments");
            if (raw instanceof Map<?,?> m) {
                assignments.clear();
                for (var e : m.entrySet()) {
                    String k = String.valueOf(e.getKey());
                    String v = String.valueOf(e.getValue());
                    try {
                        UUID uuid = UUID.fromString(k);
                        if (isTask(v)) assignments.put(uuid, v);
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        } catch (Throwable ignored) {}
    }

    public TaskDefinition getTask(String id) { return registry.get(id); }

    public boolean isTask(String id) { return registry.containsKey(id); }

    /** Attribute a bed explosion in a world to a player for a short time. Key is world name. */
    public void markBedExploder(Player p) {
        if (p == null || p.getWorld() == null) return;
        lastBedExploderPerWorld.put(p.getWorld().getName(), p.getUniqueId());
        // Clear after a few seconds
        Bukkit.getScheduler().runTaskLater(plugin, () -> lastBedExploderPerWorld.remove(p.getWorld().getName()), 20L*5);
    }

    public UUID getRecentBedExploder(String worldName) {
        return lastBedExploderPerWorld.get(worldName);
    }
    
    /** Load tasks from config and built-in defaults */
    private void loadTasks() {
        registry.clear();
        taskPool.clear();
        
        // Load built-in tasks if enabled
        if (plugin.getConfig().getBoolean("task_manager.include_default_tasks", true)) {
            registerDefaults();
        }
        
        // Load custom tasks from config
        loadCustomTasks();
        
        // Ensure we have at least some tasks
        if (registry.isEmpty()) {
            plugin.getLogger().warning("No tasks loaded! Loading default tasks as fallback.");
            registerDefaults();
        }
    }
    
    /** Load custom tasks from config */
    private void loadCustomTasks() {
        var customTasks = plugin.getConfig().getList("task_manager.custom_tasks");
        if (customTasks == null) return;
        
        for (Object obj : customTasks) {
            if (obj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> taskMap = (Map<String, Object>) obj;
                String id = String.valueOf(taskMap.get("id"));
                String description = String.valueOf(taskMap.get("description"));
                
                if (id != null && !id.equals("null") && description != null && !description.equals("null")) {
                    register(new TaskDefinition(id, description, TaskType.COMPLEX_TASK));
                    plugin.getLogger().info("Loaded custom task: " + id);
                }
            }
        }
    }
    
    /** Add a custom task and save to config */
    public void addCustomTask(String id, String description) {
        // Add to registry
        register(new TaskDefinition(id, description, TaskType.COMPLEX_TASK));
        
        // Save to config
        List<Object> customTasks = (List<Object>) plugin.getConfig().getList("task_manager.custom_tasks");
        if (customTasks == null) {
            customTasks = new ArrayList<>();
        }
        
        Map<String, Object> taskMap = new HashMap<>();
        taskMap.put("id", id);
        taskMap.put("description", description);
        customTasks.add(taskMap);
        
        plugin.getConfig().set("task_manager.custom_tasks", customTasks);
        plugin.saveConfig();
    }
    
    /** Remove a custom task and save to config */
    public boolean removeCustomTask(String id) {
        // Check if it's a custom task (not built-in)
        var customTasks = plugin.getConfig().getList("task_manager.custom_tasks");
        if (customTasks == null) return false;
        
        boolean removed = false;
        Iterator<?> iter = customTasks.iterator();
        while (iter.hasNext()) {
            Object obj = iter.next();
            if (obj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> taskMap = (Map<String, Object>) obj;
                if (id.equals(taskMap.get("id"))) {
                    iter.remove();
                    removed = true;
                    break;
                }
            }
        }
        
        if (removed) {
            // Remove from registry
            registry.remove(id);
            taskPool.remove(id);
            
            // Save config
            plugin.getConfig().set("task_manager.custom_tasks", customTasks);
            plugin.saveConfig();
            
            // Reload tasks to ensure consistency
            loadTasks();
        }
        
        return removed;
    }
    
    /** Get all custom task IDs */
    public List<String> getCustomTaskIds() {
        List<String> customIds = new ArrayList<>();
        var customTasks = plugin.getConfig().getList("task_manager.custom_tasks");
        if (customTasks == null) return customIds;
        
        for (Object obj : customTasks) {
            if (obj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> taskMap = (Map<String, Object>) obj;
                String id = String.valueOf(taskMap.get("id"));
                if (id != null && !id.equals("null")) {
                    customIds.add(id);
                }
            }
        }
        return customIds;
    }
    
    /** Reload tasks from config */
    public void reloadTasks() {
        plugin.reloadConfig();
        loadTasks();
    }

    private void registerDefaults() {
        // 100 unique, balanced tasks inspired by Dream's video - challenging but achievable
        
        // === SPECIAL MULTI-STEP TASKS (10) ===
        register(new TaskDefinition("die_on_bedrock_fall", "Fall from the surface to bedrock and die from fall damage on bedrock", TaskType.DIE_ON_BEDROCK_FALL));
        register(new TaskDefinition("kill_golem_nether_bed", "Kill an iron golem in the Nether using a bed explosion", TaskType.KILL_GOLEM_NETHER_BED));
        register(new TaskDefinition("kill_all_sheep_iron_shovel", "Kill one of every colored sheep with an iron shovel", TaskType.KILL_ALL_SHEEP_IRON_SHOVEL));
        register(new TaskDefinition("sleep_nether_fortress", "Place and sleep in a bed inside a Nether fortress", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("die_holding_diamonds", "Die while holding exactly 10 diamonds", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("stack_64_rotten_flesh", "Obtain a full stack (64) of rotten flesh", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("kill_mob_with_anvil", "Kill any mob by dropping an anvil on it", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("survive_100_damage", "Take 100 points of cumulative damage without dying", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("name_5_mobs", "Name 5 different mobs with name tags", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("collect_all_wood_types", "Collect all 9 types of wood logs", TaskType.COMPLEX_TASK));
        
        // === UNDERGROUND/MINING CHALLENGES (10) ===
        register(new TaskDefinition("mine_1000_blocks", "Mine 1000 blocks total", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("find_12_diamonds", "Find and collect 12 diamonds", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("create_strip_mine", "Create a strip mine at Y=11 that is 100 blocks long", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("die_in_lava_y5", "Die in lava below Y=5", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("place_torch_bedrock", "Place a torch directly on bedrock", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("fill_chest_ores", "Fill a chest with one of each ore type", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("explode_50_tnt", "Explode 50 TNT blocks", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("dig_to_void", "Dig through bedrock and fall into the void", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("create_obsidian_room", "Build a 3x3x3 room made entirely of obsidian", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("find_spawner_break", "Find and break a mob spawner", TaskType.COMPLEX_TASK));
        
        // === COMBAT & MOB CHALLENGES (10) ===
        register(new TaskDefinition("kill_50_mobs", "Kill 50 hostile mobs", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("kill_zombie_gold_sword", "Kill 10 zombies with a golden sword", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("kill_creeper_no_explosion", "Kill 5 creepers without any exploding", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("die_to_baby_zombie", "Get killed by a baby zombie", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("kill_skeleton_own_arrow", "Kill a skeleton with its own arrow", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("survive_5_creeper_explosions", "Survive 5 creeper explosions", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("kill_enderman_water", "Kill an enderman using water", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("tame_wolf_kill_sheep", "Tame a wolf and make it kill 10 sheep", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("kill_witch_potion", "Kill a witch using splash potions", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("ride_spider_jockey", "Find and kill a spider jockey", TaskType.COMPLEX_TASK));
        
        // === NETHER CHALLENGES (10) ===
        register(new TaskDefinition("bridge_lava_lake", "Build a bridge across a lava lake in the Nether", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("collect_16_glowstone", "Collect 16 glowstone dust", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("kill_ghast_fireball", "Kill a ghast by reflecting its fireball", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("find_fortress_chest", "Find and loot a Nether fortress chest", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("collect_10_magma_cream", "Collect 10 magma cream", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("trade_16_gold", "Trade 16 gold ingots with piglins", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("kill_20_piglin", "Kill 20 piglins or zombified piglins", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("strider_cross_lava", "Ride a strider across a lava ocean", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("brew_fire_resistance", "Brew a fire resistance potion", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("die_wither_skeleton", "Get killed by a wither skeleton", TaskType.COMPLEX_TASK));
        
        // === CRAFTING & BUILDING CHALLENGES (10) ===
        register(new TaskDefinition("craft_full_diamond_armor", "Craft a full set of diamond armor", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("build_50_high_tower", "Build a tower 50 blocks high", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("create_auto_farm", "Build an automatic farm with redstone", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("craft_10_paintings", "Craft and place 10 paintings", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("build_nether_portal_overworld", "Build 2 different nether portals in the overworld", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("craft_enchanting_table", "Craft and place an enchanting table with 15 bookshelves", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("create_infinite_water", "Create 5 infinite water sources", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("build_house_5_rooms", "Build a house with at least 5 rooms", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("craft_100_items", "Craft 100 items total (any items)", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("make_map_wall", "Create a 3x3 map wall", TaskType.COMPLEX_TASK));
        
        // === FOOD & FARMING CHALLENGES (10) ===
        register(new TaskDefinition("breed_20_animals", "Breed 20 animals (any type)", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("cook_64_meat", "Cook 64 pieces of meat (any type)", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("grow_100_wheat", "Harvest 100 wheat", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("create_bee_farm", "Create a bee farm with 3 beehives", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("tame_10_wolves", "Tame 10 wolves", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("collect_all_flowers", "Collect one of every flower type", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("make_cake", "Craft and place a cake", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("eat_25_foods", "Eat 25 different food items", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("poison_self_5_times", "Get poisoned 5 times", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("max_saturation", "Achieve maximum saturation with golden carrots", TaskType.COMPLEX_TASK));
        
        // === TRANSPORTATION CHALLENGES (10) ===
        register(new TaskDefinition("travel_1000_blocks", "Travel 1000 blocks in any direction from spawn", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("ride_minecart_500", "Ride a minecart for 500 blocks", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("fly_elytra_1000", "Fly 1000 blocks with elytra", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("boat_cross_ocean", "Cross an ocean biome by boat", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("ride_pig_100", "Ride a pig for 100 blocks", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("build_ice_road", "Build an ice road 50 blocks long", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("horse_jump_5_blocks", "Jump 5 blocks high on a horse", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("swim_500_blocks", "Swim 500 blocks", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("create_bubble_column", "Create a bubble column elevator 30 blocks high", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("ender_pearl_100", "Travel 100 blocks using ender pearls", TaskType.COMPLEX_TASK));
        
        // === COLLECTION CHALLENGES (10) ===
        register(new TaskDefinition("collect_64_bones", "Collect 64 bones", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("collect_32_ender_pearls", "Collect 32 ender pearls", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("collect_16_music_discs", "Collect any music disc", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("collect_100_xp_levels", "Reach level 30 experience", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("fill_inventory", "Completely fill your inventory with unique items", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("collect_64_string", "Collect 64 string", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("collect_32_gunpowder", "Collect 32 gunpowder", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("collect_all_dyes", "Collect one of every dye color", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("collect_10_saddles", "Find and collect 2 saddles", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("collect_totem", "Find a Totem of Undying", TaskType.COMPLEX_TASK));
        
        // === TRADING & VILLAGER CHALLENGES (10) ===
        register(new TaskDefinition("trade_with_5_villagers", "Trade with 5 different villagers", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("max_villager_trade", "Max out a villager's trade (trade until locked)", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("cure_zombie_villager", "Cure a zombie villager", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("create_iron_golem", "Build and spawn an iron golem", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("raid_victory", "Defeat a raid", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("collect_64_emeralds", "Collect 64 emeralds", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("build_villager_breeder", "Build a villager breeding system", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("transport_villager_500", "Transport a villager 500 blocks", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("get_hero_village", "Get Hero of the Village effect", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("trade_enchanted_book", "Trade for an enchanted book from a librarian", TaskType.COMPLEX_TASK));
        
        // === UNIQUE/SPECIAL CHALLENGES (10) ===
        register(new TaskDefinition("sleep_100_times", "Sleep in a bed 10 times", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("die_5_different_ways", "Die in 5 different ways", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("screenshot_sunset", "Watch a sunset from Y=100", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("create_snow_golem_army", "Create 10 snow golems", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("burn_diamond", "Throw a diamond into lava", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("drown_with_respiration", "Drown while wearing Respiration III helmet", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("kill_yourself_tnt", "Kill yourself with your own TNT", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("reach_world_border", "Travel 10000 blocks in one direction", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("survive_50_hearts_damage", "Take 50 hearts of damage total without dying", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("place_1000_blocks", "Place 1000 blocks", TaskType.COMPLEX_TASK));
    }

    private void register(TaskDefinition def) {
        registry.put(def.id(), def);
        taskPool.add(def.id());
    }

    private static String nice(String id) {
        return id.toLowerCase().replace('_',' ').replace("wither skeleton", "witherskeleton");
    }
}
