package com.example.speedrunnerswap.task;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import com.example.speedrunnerswap.utils.BukkitCompat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

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
        
        // Cross-platform title with fade timings in ticks
        BukkitCompat.showTitle(p, "§6§lYOUR SECRET TASK", "§e" + def.description(), 10, 80, 16);

        // Show completion command information shortly after
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            BukkitCompat.showTitle(p, "§a§lMANUAL COMPLETION", "§bUse: /swap complete confirm", 6, 50, 10);
        }, 60L); // Show after ~3 seconds

        // Chat messages with detailed instructions
        p.sendMessage("§6§l[Task Manager] Your secret task assigned!");
        p.sendMessage("§e → " + def.description());
        p.sendMessage("");
        p.sendMessage("§a§lCOMPLETION OPTIONS:");
        p.sendMessage("§7• §fSome tasks complete automatically when detected");
        p.sendMessage("§7• §fFor manual completion: §e/swap complete confirm");
        p.sendMessage("§7• §fTo view your task again: §e/swap complete");
        p.sendMessage("");
        p.sendMessage("§6⚠ Manual completion will instantly win the game!");
        p.sendMessage("§7Only use it when you have actually completed your task.");
        p.sendMessage("§6" + "=".repeat(45));
    }

    /** Call when a player has completed their task */
    public void complete(Player p) {
        if (p == null) return;
        String taskId = assignments.get(p.getUniqueId());
        if (taskId == null) return; // not assigned
        // Announce winner and stop the game
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player pl : Bukkit.getOnlinePlayers()) {
                BukkitCompat.showTitle(pl, "§a§lTASK COMPLETE!", "§e" + p.getName() + " §7completed: §f" + registry.get(taskId).description(), 10, 80, 16);
                pl.sendMessage("§a[Task Manager] Winner: §f" + p.getName());
            }
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
        List<?> rawList = plugin.getConfig().getList("task_manager.custom_tasks");
        List<Object> customTasks = new ArrayList<>();
        if (rawList != null) customTasks.addAll(rawList);

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
        // 100 CHALLENGING tasks inspired by the user's examples - no easy tasks!
        
        // === ULTRA CHALLENGING MULTI-STEP TASKS (15) ===
        register(new TaskDefinition("die_on_bedrock_fall", "Fall from the surface to bedrock and die from fall damage on bedrock", TaskType.DIE_ON_BEDROCK_FALL));
        register(new TaskDefinition("kill_golem_nether_bed", "Kill an iron golem in the Nether using a bed explosion", TaskType.KILL_GOLEM_NETHER_BED));
        register(new TaskDefinition("kill_all_sheep_iron_shovel", "Kill one of every colored sheep with an iron shovel", TaskType.KILL_ALL_SHEEP_IRON_SHOVEL));
        register(new TaskDefinition("sleep_nether_fortress", "Place and sleep in a bed inside a Nether fortress", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("kill_wither_skeleton_wooden_sword", "Kill a wither skeleton using only a wooden sword", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("survive_creeper_explosion_point_blank", "Survive a creeper explosion at point-blank range", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("kill_enderman_with_snowballs", "Kill an enderman using only snowballs", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("die_in_end_void_holding_elytra", "Die in the End void while holding elytra", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("kill_phantom_melee_only", "Kill a phantom using only melee attacks (no bow/crossbow)", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("tame_wolf_using_rotten_flesh", "Tame a wolf using only rotten flesh", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("kill_piglin_brute_leather_armor", "Kill a piglin brute while wearing full leather armor", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("survive_lava_swim_30_seconds", "Survive swimming in lava for 30 seconds straight", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("kill_elder_guardian_stone_sword", "Kill an elder guardian using a stone sword", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("complete_raid_wooden_tools", "Complete a raid using only wooden tools", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("kill_ender_dragon_punch", "Deal the killing blow to the ender dragon with your fist", TaskType.COMPLEX_TASK));
        
        // === EXTREME UNDERGROUND CHALLENGES (10) ===
        register(new TaskDefinition("mine_obsidian_wooden_pickaxe", "Break obsidian using a wooden pickaxe (won't drop, just break it)", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("find_diamond_y_minus_50", "Find and mine a diamond at Y level -50 or below", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("die_by_suffocation_gravel", "Die by suffocation from gravel or sand", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("create_lava_pool_underground", "Create a 3x3 lava pool at Y level 5 or below", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("find_stronghold_no_eyes", "Find a stronghold without using any Eyes of Ender", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("mine_50_ancient_debris", "Mine 5 ancient debris blocks", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("create_beacon_underground", "Activate a beacon below Y level 0", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("kill_mob_fall_damage_mine", "Kill any mob using fall damage in a mine shaft", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("place_water_nether", "Place a water bucket in the Nether (it will evaporate)", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("dig_to_void_pickaxe_only", "Dig from surface to Y=0 using only pickaxes (no TNT/other tools)", TaskType.COMPLEX_TASK));
        
        // === DEADLY NETHER CHALLENGES (10) ===
        register(new TaskDefinition("kill_ghast_melee_attack", "Kill a ghast with a melee attack (not fireball reflection)", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("swim_lava_ocean_100_blocks", "Swim 100 blocks through a lava ocean in the Nether", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("kill_hoglin_no_armor", "Kill a hoglin while wearing no armor", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("collect_soul_sand_magma_cube", "Collect soul sand while a magma cube is attacking you", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("build_house_basalt_deltas", "Build a 5x5 house in the basalt deltas biome", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("kill_blaze_water_bucket", "Kill a blaze using a water bucket (splash damage)", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("collect_crying_obsidian_ruined_portal", "Collect 10 crying obsidian from ruined portals", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("kill_piglin_their_own_crossbow", "Kill a piglin using their own crossbow", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("survive_surrounded_by_fire", "Survive being completely surrounded by fire for 10 seconds", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("die_by_wither_effect_nether", "Die from the wither effect while in the Nether", TaskType.COMPLEX_TASK));
        
        // === IMPOSSIBLE END CHALLENGES (10) ===
        register(new TaskDefinition("kill_enderman_staring_contest", "Kill an enderman after staring at it for 5 seconds", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("break_end_crystal_fist", "Break an end crystal using your fist", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("survive_dragon_breath_10_seconds", "Survive standing in dragon's breath for 10 seconds", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("kill_shulker_their_own_bullet", "Kill a shulker using their own shulker bullet", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("collect_chorus_fruit_levitation", "Collect chorus fruit while affected by levitation", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("build_tower_end_spawn", "Build a 20-block tall tower on the End spawn platform", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("kill_endermite_end_dimension", "Kill an endermite in the End dimension", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("collect_dragon_egg_no_piston", "Collect the dragon egg without using pistons", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("survive_void_damage_elytra", "Take void damage and survive using elytra", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("punch_ender_dragon_100_times", "Punch the ender dragon 20 times with your fist", TaskType.COMPLEX_TASK));
        
        // === EXTREME COMBAT CHALLENGES (10) ===
        register(new TaskDefinition("kill_iron_golem_cactus", "Kill an iron golem using cactus damage", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("kill_ravager_fishing_rod", "Kill a ravager using only a fishing rod", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("kill_warden_no_sound", "Kill a warden without making any sound", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("survive_skeleton_army_10", "Survive being attacked by 10+ skeletons simultaneously", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("kill_witch_their_own_potion", "Kill a witch using their own splash potion", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("kill_vindicator_their_axe", "Kill a vindicator using their own axe", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("survive_creeper_chain_explosion", "Survive a chain explosion of 5+ creepers", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("kill_zombie_pigman_gold_sword", "Kill a zombified piglin using a golden sword", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("kill_spider_jockey_separately", "Kill both the spider and skeleton of a spider jockey", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("survive_wither_boss_melee", "Survive fighting the Wither using only melee attacks", TaskType.COMPLEX_TASK));
        
        // === DEADLY BUILDING CHALLENGES (10) ===
        register(new TaskDefinition("build_bridge_lava_lake", "Build a bridge across a lava lake (minimum 20 blocks)", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("build_house_monster_spawner", "Build a house with a monster spawner inside it", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("create_floating_island_void", "Create a floating island above the void in the End", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("build_tower_lightning_storm", "Build a 50-block tall tower during a thunderstorm", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("create_pixel_art_nether_roof", "Create pixel art on the Nether roof (10x10 minimum)", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("build_underwater_base_no_doors", "Build an underwater base without using doors or air pockets", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("create_redstone_trap_works", "Create a working redstone trap that kills a mob", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("build_castle_desert_temple", "Build a castle on top of a desert temple", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("create_farm_end_island", "Create a working farm on an End island", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("build_house_one_chunk", "Build a house that spans exactly one chunk (16x16)", TaskType.COMPLEX_TASK));
        
        // === INSANE SURVIVAL CHALLENGES (10) ===
        register(new TaskDefinition("survive_day_half_heart", "Survive an entire day cycle with half a heart", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("survive_drowning_air_pocket", "Survive drowning by finding an air pocket underwater", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("eat_only_poisonous_food_day", "Eat only poisonous food (spider eyes, etc.) for one day", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("survive_desert_no_water", "Survive in a desert for 10 minutes without drinking water", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("survive_cave_no_torches", "Survive in a cave system for 5 minutes without placing torches", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("survive_blizzard_powder_snow", "Survive being trapped in powder snow for 30 seconds", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("survive_hunger_games_no_food", "Survive with empty hunger bar for 2 minutes", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("survive_fall_water_bucket_clutch", "Survive a 50+ block fall using water bucket clutch", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("survive_mob_spawner_room_1_minute", "Survive in a mob spawner room for 1 minute", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("survive_on_single_block_void", "Survive on a single block above the void for 2 minutes", TaskType.COMPLEX_TASK));
        
        // === EXTREME COLLECTION CHALLENGES (15) ===
        register(new TaskDefinition("collect_stack_rotten_flesh_zombies", "Collect 64 rotten flesh by killing zombies only", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("collect_all_armor_trims", "Collect 5 different armor trim templates", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("collect_every_enchanted_book", "Collect 10 different enchanted books", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("collect_stack_gunpowder_creepers", "Collect 64 gunpowder by killing creepers only", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("collect_all_pottery_sherds", "Collect 5 different pottery sherds", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("collect_stack_bones_skeletons", "Collect 64 bones by killing skeletons only", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("collect_every_music_disc", "Collect 5 different music discs", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("collect_stack_string_spiders", "Collect 64 string by killing spiders only", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("collect_all_banner_patterns", "Collect 5 different banner patterns", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("collect_stack_slimeballs_slimes", "Collect 64 slimeballs by killing slimes only", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("collect_every_horse_armor", "Collect leather, iron, gold, and diamond horse armor", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("collect_stack_blaze_rods_blazes", "Collect 64 blaze rods by killing blazes only", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("collect_all_smithing_templates", "Collect 3 different smithing templates", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("collect_stack_ender_pearls_endermen", "Collect 64 ender pearls by killing endermen only", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("collect_every_suspicious_stew", "Collect 5 different suspicious stews", TaskType.COMPLEX_TASK));
        
        // === FINAL IMPOSSIBLE CHALLENGES (10) ===
        register(new TaskDefinition("kill_every_hostile_mob_type", "Kill one of every hostile mob type in the game", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("complete_all_advancements_hour", "Complete 20 different advancements", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("create_max_level_enchant_table", "Create a max-level enchanting table setup (15 bookshelves)", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("defeat_three_bosses", "Defeat the Ender Dragon, Wither, and Elder Guardian", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("create_every_potion_type", "Brew 10 different types of potions", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("max_out_villager_trades", "Max out trades with 5 different villager professions", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("create_automatic_farm_system", "Create 3 different automatic farms (crop, mob, etc.)", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("survive_hardcore_week", "Survive for 7 in-game days without dying once", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("create_nether_highway_1000", "Create a Nether highway spanning 1000 blocks", TaskType.COMPLEX_TASK));
        register(new TaskDefinition("become_minecraft_god", "Reach maximum level (technically impossible - 30+ levels)", TaskType.COMPLEX_TASK));
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

    @SuppressWarnings("unused")
    private static String nice(String id) {
        return id.toLowerCase().replace('_',' ').replace("wither skeleton", "witherskeleton");
    }
}
