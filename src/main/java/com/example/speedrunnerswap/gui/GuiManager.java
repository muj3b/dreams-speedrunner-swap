package com.example.speedrunnerswap.gui;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import com.example.speedrunnerswap.models.Team;
import java.util.logging.Level;
import org.bukkit.enchantments.Enchantment;

import java.util.ArrayList;
import java.util.List;

public class GuiManager {
    
    private final SpeedrunnerSwap plugin;
    private final String BACK_BUTTON_TITLE = "§c§lBack to Main Menu";
    
    public GuiManager(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
    }

    // New direct gamemode selector - opens each gamemode's actual main menu
    public void openDirectGamemodeSelector(Player player) {
        int rows = 3;
        org.bukkit.inventory.Inventory inv = org.bukkit.Bukkit.createInventory(null, rows * 9, net.kyori.adventure.text.Component.text("§6§lSpeedrunner Swap - Choose Gamemode"));

        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        fillBorder(inv, filler);

        boolean gameRunning = plugin.getGameManager().isGameRunning();
        String currentMode = plugin.getCurrentMode().name();

        // Dream's Speedrunners vs Hunters mode - opens Dream's main menu directly
        java.util.List<String> dreamLore = new java.util.ArrayList<>();
        dreamLore.add("§e§lSpeedrunners §7+ §c§lHunters");
        dreamLore.add("§7Classic manhunt with tracking & PvP");
        dreamLore.add("");
        dreamLore.add("§b• §7Hunters track runners with compasses");
        dreamLore.add("§b• §7Power-ups, world border, bounties");
        dreamLore.add("§b• §7Team vs team competition");
        dreamLore.add("");
        dreamLore.add("§a§lClick to open Dream menu directly!");
        if (currentMode.equals("DREAM")) {
            dreamLore.add("§e● Currently Selected Mode");
        }
        ItemStack dream = createGuiButton(Material.DIAMOND_SWORD, "§a§lDream: Speedrunners vs Hunters", dreamLore, "direct_dream_menu");
        if (currentMode.equals("DREAM")) dream = createGlowingItem(dream);
        inv.setItem(11, dream);

        // Sapnap's Multi-Runner Cooperation mode - opens Sapnap's main menu directly
        java.util.List<String> sapLore = new java.util.ArrayList<>();
        sapLore.add("§b§lMulti-Runner Cooperation");
        sapLore.add("§7Pure teamwork - no hunters!");
        sapLore.add("");
        sapLore.add("§b• §7Share one character between runners");
        sapLore.add("§b• §7Cooperative dragon defeat");
        sapLore.add("§b• §7Queue-based rotation system");
        sapLore.add("");
        sapLore.add("§a§lClick to open Sapnap menu directly!");
        if (currentMode.equals("SAPNAP")) {
            sapLore.add("§e● Currently Selected Mode");
        }
        ItemStack sapnap = createGuiButton(Material.DIAMOND_BOOTS, "§b§lSapnap: Multi-Runner Swap", sapLore, "direct_sapnap_menu");
        if (currentMode.equals("SAPNAP")) sapnap = createGlowingItem(sapnap);
        inv.setItem(13, sapnap);

        // Task Manager mode with secret tasks - opens Task Manager's main menu directly
        java.util.List<String> taskLore = new java.util.ArrayList<>();
        taskLore.add("§d§lSecret Task Competition");
        taskLore.add("§7Each runner gets hidden objectives");
        taskLore.add("");
        taskLore.add("§b• §7Individual secret tasks");
        taskLore.add("§b• §7First to complete wins");
        taskLore.add("§b• §7Customizable task pool");
        taskLore.add("");
        taskLore.add("§a§lClick to open Task Manager menu directly!");
        if (currentMode.equals("TASK")) {
            taskLore.add("§e● Currently Selected Mode");
        }
        ItemStack taskMode = createGuiButton(Material.TARGET, "§6§lTask Manager Swap", taskLore, "direct_task_menu");
        if (currentMode.equals("TASK")) taskMode = createGlowingItem(taskMode);
        inv.setItem(15, taskMode);

        player.openInventory(inv);
    }

    // Keep the old mode selector for backward compatibility if needed
    public void openModeSelector(Player player) {
        int rows = 3;
        org.bukkit.inventory.Inventory inv = org.bukkit.Bukkit.createInventory(null, rows * 9, net.kyori.adventure.text.Component.text("§6§lSpeedrunner Swap - Mode Selection"));

        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        fillBorder(inv, filler);

        boolean gameRunning = plugin.getGameManager().isGameRunning();
        String currentMode = plugin.getCurrentMode().name();

        // Dream's Speedrunners vs Hunters mode
        java.util.List<String> dreamLore = new java.util.ArrayList<>();
        dreamLore.add("§e§lSpeedrunners §7+ §c§lHunters");
        dreamLore.add("§7Classic manhunt with tracking & PvP");
        dreamLore.add("");
        dreamLore.add("§b• §7Hunters track runners with compasses");
        dreamLore.add("§b• §7Power-ups, world border, bounties");
        dreamLore.add("§b• §7Team vs team competition");
        if (currentMode.equals("DREAM")) {
            dreamLore.add("");
            dreamLore.add(gameRunning ? "§a▶ Currently Active" : "§e● Currently Selected");
        }
        ItemStack dream = createGuiButton(Material.DIAMOND_SWORD, "§a§lDream: Speedrunners vs Hunters", dreamLore, "mode_dream");
        if (currentMode.equals("DREAM")) dream = createGlowingItem(dream);
        inv.setItem(11, dream);

        // Sapnap's Multi-Runner Cooperation mode  
        java.util.List<String> sapLore = new java.util.ArrayList<>();
        sapLore.add("§b§lMulti-Runner Cooperation");
        sapLore.add("§7Pure teamwork - no hunters!");
        sapLore.add("");
        sapLore.add("§b• §7Share one character between runners");
        sapLore.add("§b• §7Cooperative dragon defeat");
        sapLore.add("§b• §7Queue-based rotation system");
        if (currentMode.equals("SAPNAP")) {
            sapLore.add("");
            sapLore.add(gameRunning ? "§a▶ Currently Active" : "§e● Currently Selected");
        }
        ItemStack sapnap = createGuiButton(Material.DIAMOND_BOOTS, "§b§lSapnap: Multi-Runner Swap", sapLore, "mode_sapnap");
        if (currentMode.equals("SAPNAP")) sapnap = createGlowingItem(sapnap);
        inv.setItem(13, sapnap);

        // Task Manager mode with secret tasks
        java.util.List<String> taskLore = new java.util.ArrayList<>();
        taskLore.add("§d§lSecret Task Competition");
        taskLore.add("§7Each runner gets hidden objectives");
        taskLore.add("");
        taskLore.add("§b• §7Individual secret tasks");
        taskLore.add("§b• §7First to complete wins");
        taskLore.add("§b• §7Customizable task pool");
        if (currentMode.equals("TASK")) {
            taskLore.add("");
            taskLore.add(gameRunning ? "§a▶ Currently Active" : "§e● Currently Selected");
        }
        ItemStack taskMode = createGuiButton(Material.TARGET, "§6§lTask Manager Swap", taskLore, "mode_task");
        if (currentMode.equals("TASK")) taskMode = createGlowingItem(taskMode);
        inv.setItem(15, taskMode);

        // Force-stop for admins when game is running
        if (gameRunning && player.hasPermission("speedrunnerswap.admin")) {
            List<String> forceLore = java.util.List.of("§cStop current game and switch modes", "§7Admin-only quick mode switching");
            inv.setItem(22, createGuiButton(Material.BARRIER, "§c§lForce Stop & Switch", forceLore, "admin_force_stop"));
        } else if (gameRunning) {
            List<String> infoLore = java.util.List.of("§7A game is currently running", "§7Stop it before switching modes");
            inv.setItem(22, createItem(Material.PAPER, "§e§lGame In Progress", infoLore));
        }

        player.openInventory(inv);
    }

    public void openForceConfirm(Player player, com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode target) {
        String name = target == com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.SAPNAP ? "Sapnap" : "Dream";
        org.bukkit.inventory.Inventory inv = org.bukkit.Bukkit.createInventory(null, 9, net.kyori.adventure.text.Component.text("§4§lConfirm Mode Switch"));
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);

        // Back
        inv.setItem(0, createGuiButton(Material.ARROW, "§7§lBack", java.util.List.of("§7Return"), "back_mode"));
        // Confirm / Cancel
        String id = (target == com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.SAPNAP) ? "force_yes_sapnap" : "force_yes_dream";
        ItemStack yes = createGuiButton(Material.REDSTONE_BLOCK, "§c§lConfirm", java.util.List.of("§7End current game", "§7Switch to §f" + name), id);
        ItemStack no = createGuiButton(Material.EMERALD_BLOCK, "§a§lCancel", java.util.List.of("§7Do nothing"), "force_no");
        inv.setItem(3, no);
        inv.setItem(5, yes);
        player.openInventory(inv);
    }

    public String formatTime(int seconds) {
        int minutes = seconds / 60;
        int hours = minutes / 60;
        minutes %= 60;
        seconds %= 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    // Dangerous Blocks editor
    public void openDangerousBlocksMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("§6§lDangerous Blocks"));
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        fillBorder(inv, filler);
        inv.setItem(0, createItem(Material.ARROW, "§7§lBack", List.of("§7Return to settings")));
        java.util.Set<org.bukkit.Material> set = plugin.getConfigManager().getDangerousBlocks();
        int slot = 10;
        for (org.bukkit.Material mat : org.bukkit.Material.values()) {
            if (slot >= 44) break; // show a subset to avoid overflow
            if (!set.contains(mat)) continue; // only show configured ones to keep concise
            ItemStack it = createItem(mat, "§e" + mat.name(), List.of("§7Click to remove from list"));
            inv.setItem(slot++, it);
        }
        // Hint to add more via config for now
        inv.setItem(49, createItem(Material.PAPER, "§7Note", List.of("§7To add new blocks, use config.yml", "§7GUI supports removal quickly")));
        player.openInventory(inv);
    }
    
    public void openPositiveEffectsMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 36, Component.text("§a§lPositive Effects"));
        
        // Border-only filler for cleaner look
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        fillBorder(inventory, filler);

        // Add all available positive effects
        List<ItemStack> effectItems = new ArrayList<>();
        effectItems.add(createEffectItem(Material.POTION, "Speed", "SPEED"));
        effectItems.add(createEffectItem(Material.POTION, "Jump Boost", "JUMP"));
        effectItems.add(createEffectItem(Material.POTION, "Strength", "INCREASE_DAMAGE"));
        effectItems.add(createEffectItem(Material.POTION, "Regeneration", "REGENERATION"));
        effectItems.add(createEffectItem(Material.POTION, "Resistance", "DAMAGE_RESISTANCE"));
        effectItems.add(createEffectItem(Material.POTION, "Fire Resistance", "FIRE_RESISTANCE"));
        effectItems.add(createEffectItem(Material.POTION, "Water Breathing", "WATER_BREATHING"));
        effectItems.add(createEffectItem(Material.POTION, "Night Vision", "NIGHT_VISION"));

        for (int i = 0; i < effectItems.size(); i++) {
            inventory.setItem(10 + i, effectItems.get(i));
        }

        // Back button
        inventory.setItem(35, createItem(Material.BARRIER, BACK_BUTTON_TITLE));

        player.openInventory(inventory);
    }

    public void openNegativeEffectsMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 36, Component.text("§c§lNegative Effects"));
        
        // Border-only filler for cleaner look
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        fillBorder(inventory, filler);

        // Add all available negative effects
        List<ItemStack> effectItems = new ArrayList<>();
        effectItems.add(createEffectItem(Material.POTION, "Slowness", "SLOW"));
        effectItems.add(createEffectItem(Material.POTION, "Weakness", "WEAKNESS"));
        effectItems.add(createEffectItem(Material.POTION, "Poison", "POISON"));
        effectItems.add(createEffectItem(Material.POTION, "Blindness", "BLINDNESS"));
        effectItems.add(createEffectItem(Material.POTION, "Hunger", "HUNGER"));
        effectItems.add(createEffectItem(Material.POTION, "Mining Fatigue", "SLOW_DIGGING"));
        effectItems.add(createEffectItem(Material.POTION, "Nausea", "CONFUSION"));
        effectItems.add(createEffectItem(Material.POTION, "Glowing", "GLOWING"));

        for (int i = 0; i < effectItems.size(); i++) {
            inventory.setItem(10 + i, effectItems.get(i));
        }

        // Back button
        inventory.setItem(35, createItem(Material.BARRIER, BACK_BUTTON_TITLE));

        player.openInventory(inventory);
    }

    public ItemStack createEffectItem(Material material, String displayName, String effectId) {
        List<String> lore = new ArrayList<>();
        lore.add("§7Effect ID: §f" + effectId);
        lore.add("§7Click to toggle this effect");
        boolean isEnabled = plugin.getConfig().getStringList("power_ups.good_effects").contains(effectId) ||
                          plugin.getConfig().getStringList("power_ups.bad_effects").contains(effectId);
        lore.add(isEnabled ? "§aCurrently enabled" : "§cCurrently disabled");
        
        return createItem(material, "§e§l" + displayName, lore);
    }

    public void openPowerUpsMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 36, Component.text("§e§lPower-ups Menu"));
        
        // Border-only filler for cleaner look
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        fillBorder(inventory, filler);

        // Toggle button
        List<String> powerUpToggleLore = new ArrayList<>();
        boolean powerUpsEnabled = plugin.getConfigManager().isPowerUpsEnabled();
        powerUpToggleLore.add("§7Current status: " + (powerUpsEnabled ? "§aEnabled" : "§cDisabled"));
        powerUpToggleLore.add("§7Click to toggle");
        ItemStack toggleItem = createItem(
            powerUpsEnabled ? Material.LIME_DYE : Material.GRAY_DYE,
            "§e§lToggle Power-ups",
            powerUpToggleLore
        );
        inventory.setItem(4, toggleItem);

        // Positive effects section (detailed list)
        List<String> positiveEffectsLore = new ArrayList<>();
        positiveEffectsLore.add("§7Current effects:");
        for (String effect : plugin.getConfig().getStringList("power_ups.good_effects")) {
            positiveEffectsLore.add("§a• " + effect.toLowerCase());
        }
        positiveEffectsLore.add("");
        positiveEffectsLore.add("§7Click to modify");
        ItemStack goodEffectsItem = createItem(Material.SPLASH_POTION, "§a§lPositive Effects", positiveEffectsLore);
        inventory.setItem(11, goodEffectsItem);

        // Negative effects section (detailed list)
        List<String> negativeEffectsLore = new ArrayList<>();
        negativeEffectsLore.add("§7Current effects:");
        for (String effect : plugin.getConfig().getStringList("power_ups.bad_effects")) {
            negativeEffectsLore.add("§c• " + effect.toLowerCase());
        }
        negativeEffectsLore.add("");
        negativeEffectsLore.add("§7Click to modify");
        ItemStack badEffectsItem = createItem(Material.LINGERING_POTION, "§c§lNegative Effects", negativeEffectsLore);
        inventory.setItem(15, badEffectsItem);

        // Duration settings
        int minSec = plugin.getConfigManager().getPowerUpsMinSeconds();
        int maxSec = plugin.getConfigManager().getPowerUpsMaxSeconds();
        int minLvl = plugin.getConfigManager().getPowerUpsMinLevel();
        int maxLvl = plugin.getConfigManager().getPowerUpsMaxLevel();
        List<String> durationLore = new ArrayList<>();
        durationLore.add("§7Duration: §e" + minSec + "-" + maxSec + "s");
        durationLore.add("§7Level: §e" + minLvl + "-" + maxLvl);
        durationLore.add("");
        durationLore.add("§7Click to modify timings");
        ItemStack durationItem = createItem(Material.CLOCK, "§6§lEffect Durations", durationLore);
        inventory.setItem(22, durationItem);

        // Back button
        inventory.setItem(31, createItem(Material.BARRIER, BACK_BUTTON_TITLE));

        player.openInventory(inventory);
    }

    public void openPowerUpDurationsMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("§6§lPower-up Durations"));
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        fillBorder(inv, filler);

        ItemStack back = createItem(Material.ARROW, "§7§lBack", List.of("§7Return to power-ups"));
        inv.setItem(0, back);

        int minSec = plugin.getConfigManager().getPowerUpsMinSeconds();
        int maxSec = plugin.getConfigManager().getPowerUpsMaxSeconds();
        int minLvl = plugin.getConfigManager().getPowerUpsMinLevel();
        int maxLvl = plugin.getConfigManager().getPowerUpsMaxLevel();

        ItemStack minDur = createItem(Material.CLOCK, "§e§lMin Duration (s)", List.of("§7Current: §f" + minSec, "§7Left/Right: ±5"));
        ItemStack maxDur = createItem(Material.CLOCK, "§e§lMax Duration (s)", List.of("§7Current: §f" + maxSec, "§7Left/Right: ±5"));
        ItemStack minLvlItem = createItem(Material.EXPERIENCE_BOTTLE, "§e§lMin Level", List.of("§7Current: §f" + minLvl, "§7Left/Right: ±1"));
        ItemStack maxLvlItem = createItem(Material.EXPERIENCE_BOTTLE, "§e§lMax Level", List.of("§7Current: §f" + maxLvl, "§7Left/Right: ±1"));

        inv.setItem(10, minDur);
        inv.setItem(12, maxDur);
        inv.setItem(14, minLvlItem);
        inv.setItem(16, maxLvlItem);

        player.openInventory(inv);
    }

    public void openWorldBorderMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, Component.text("§c§lWorld Border Settings"));
        
        // Border-only filler for cleaner look
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        fillBorder(inventory, filler);

        // Toggle button
        boolean isEnabled = plugin.getConfig().getBoolean("world_border.enabled", true);
        List<String> toggleLore = new ArrayList<>();
        toggleLore.add("§7Current status: " + (isEnabled ? "§aEnabled" : "§cDisabled"));
        toggleLore.add("§7Click to toggle");
        ItemStack toggleItem = createItem(
            isEnabled ? Material.LIME_DYE : Material.GRAY_DYE,
            "§e§lToggle World Border",
            toggleLore
        );
        inventory.setItem(4, toggleItem);

        // Initial size setting
        int initialSize = plugin.getConfig().getInt("world_border.initial_size", 2000);
        List<String> initialSizeLore = new ArrayList<>();
        initialSizeLore.add("§7Current size: §e" + initialSize + " blocks");
        initialSizeLore.add("§7Left-click: §a+100 blocks");
        initialSizeLore.add("§7Right-click: §c-100 blocks");
        initialSizeLore.add("§7Shift + Left-click: §a+500 blocks");
        initialSizeLore.add("§7Shift + Right-click: §c-500 blocks");
        ItemStack initialSizeItem = createItem(Material.GRASS_BLOCK, "§a§lInitial Border Size", initialSizeLore);
        inventory.setItem(11, initialSizeItem);

        // Final size setting
        int finalSize = plugin.getConfig().getInt("world_border.final_size", 100);
        List<String> finalSizeLore = new ArrayList<>();
        finalSizeLore.add("§7Current size: §e" + finalSize + " blocks");
        finalSizeLore.add("§7Left-click: §a+50 blocks");
        finalSizeLore.add("§7Right-click: §c-50 blocks");
        finalSizeLore.add("§7Shift + Left-click: §a+100 blocks");
        finalSizeLore.add("§7Shift + Right-click: §c-100 blocks");
        ItemStack finalSizeItem = createItem(Material.BEDROCK, "§c§lFinal Border Size", finalSizeLore);
        inventory.setItem(13, finalSizeItem);

        // Shrink duration setting
        int shrinkDuration = plugin.getConfig().getInt("world_border.shrink_duration", 1800);
        List<String> durationLore = new ArrayList<>();
        durationLore.add("§7Current duration: §e" + formatTime(shrinkDuration));
        durationLore.add("§7Left-click: §a+5 minutes");
        durationLore.add("§7Right-click: §c-5 minutes");
        durationLore.add("§7Shift + Left-click: §a+15 minutes");
        durationLore.add("§7Shift + Right-click: §c-15 minutes");
        ItemStack durationItem = createItem(Material.CLOCK, "§6§lShrink Duration", durationLore);
        inventory.setItem(15, durationItem);

        // Warning settings
        int warningDistance = plugin.getConfig().getInt("world_border.warning_distance", 50);
        List<String> warningLore = new ArrayList<>();
        warningLore.add("§7Warning distance: §e" + warningDistance + " blocks");
        warningLore.add("§7Warning interval: §e" + 
            plugin.getConfig().getInt("world_border.warning_interval", 300) + " seconds");
        warningLore.add("§7Click to modify warnings");
        ItemStack warningItem = createItem(Material.BELL, "§e§lWarning Settings", warningLore);
        inventory.setItem(22, warningItem);

        // Back button
        inventory.setItem(26, createItem(Material.BARRIER, BACK_BUTTON_TITLE));

        player.openInventory(inventory);
    }

    public void openMainMenu(Player player) {
        // Route to mode-specific menu with proper handling
        switch (plugin.getCurrentMode()) {
            case DREAM:
                openDreamMenu(player);
                break;
            case SAPNAP:
                // Use enhanced ControlGui for Sapnap mode
                try {
                    new com.example.speedrunnerswap.gui.ControlGui(plugin).openMainMenu(player);
                } catch (Throwable t) {
                    player.sendMessage("§cFailed to open Sapnap menu: " + t.getMessage());
                    plugin.getLogger().warning("Sapnap GUI error: " + t.getMessage());
                }
                break;
            case TASK:
                openTaskManagerMenu(player);
                break;
        }
    }
    
    public void openDreamMenu(Player player) {
        try {
            String title = "§a§lDream: Speedrunners vs Hunters";
            int rows = 6;

            Inventory inventory = Bukkit.createInventory(null, rows * 9, Component.text(title));

            // Use green glass pane for Dream mode theme
            ItemStack filler = createItem(Material.GREEN_STAINED_GLASS_PANE, " ");
            fillBorder(inventory, filler);

            boolean gameRunning = plugin.getGameManager().isGameRunning();
            boolean gamePaused = plugin.getGameManager().isGamePaused();
            int runnerCount = plugin.getGameManager().getRunners().size();
            int hunterCount = plugin.getGameManager().getHunters().size();

            // Back to mode selector
            inventory.setItem(0, createGuiButton(Material.ARROW, "§7§lBack", 
                java.util.List.of("§7Return to mode selector"), "back_mode"));

            // === GAME CONTROL SECTION (Top Row) ===
            // Start/Stop Game
            if (!gameRunning) {
                List<String> startLore = new ArrayList<>();
                startLore.add("§7Begin manhunt gameplay");
                if (runnerCount == 0 || hunterCount == 0) {
                    startLore.add("§cNeed both runners and hunters!");
                    startLore.add("§cRunners: " + runnerCount + ", Hunters: " + hunterCount);
                    inventory.setItem(10, createItem(Material.GRAY_CONCRETE, "§c§lCannot Start", startLore));
                } else {
                    startLore.add("§7Ready: §b" + runnerCount + " runners§7, §c" + hunterCount + " hunters");
                    startLore.add("§7Swap interval: §e" + plugin.getConfigManager().getSwapInterval() + "s");
                    inventory.setItem(10, createGuiButton(Material.LIME_CONCRETE, "§a§lStart Hunt", startLore, "start_game"));
                }
            } else {
                List<String> stopLore = new ArrayList<>();
                stopLore.add("§7End current manhunt");
                stopLore.add("§7Status: " + (gamePaused ? "§ePaused" : "§aRunning"));
                inventory.setItem(10, createGuiButton(Material.RED_CONCRETE, "§c§lStop Hunt", stopLore, "stop_game"));
            }

            // Pause/Resume Game
            if (gameRunning && !gamePaused) {
                inventory.setItem(11, createGuiButton(Material.YELLOW_CONCRETE, "§e§lPause Hunt", 
                    List.of("§7Temporarily pause the game"), "pause_game"));
            } else if (gameRunning && gamePaused) {
                inventory.setItem(11, createGuiButton(Material.ORANGE_CONCRETE, "§a§lResume Hunt", 
                    List.of("§7Resume the manhunt"), "resume_game"));
            } else {
                inventory.setItem(11, createItem(Material.GRAY_CONCRETE, "§7Pause Hunt", 
                    List.of("§7Game not running")));
            }

            // Game Status
            List<String> statusLore = new ArrayList<>();
            statusLore.add("§7Current game information");
            statusLore.add("§7Running: " + (gameRunning ? "§aYes" : "§cNo"));
            statusLore.add("§7Runners: §b" + runnerCount);
            statusLore.add("§7Hunters: §c" + hunterCount);
            if (gameRunning) {
                Player activeRunner = plugin.getGameManager().getActiveRunner();
                statusLore.add("§7Active: §f" + (activeRunner != null ? activeRunner.getName() : "None"));
                statusLore.add("§7Next Swap: §e" + plugin.getGameManager().getTimeUntilNextSwap() + "s");
            }
            inventory.setItem(13, createItem(Material.CLOCK, "§6§lGame Status", statusLore));

            // === TEAM MANAGEMENT SECTION ===
            inventory.setItem(19, createGuiButton(Material.PLAYER_HEAD, "§e§lTeam Selector", 
                List.of("§7Assign players to Runner/Hunter teams", 
                        "§7Current: §b" + runnerCount + " runners§7, §c" + hunterCount + " hunters"), 
                "team_selector"));

            // === DREAM-SPECIFIC FEATURES ===
            inventory.setItem(21, createGuiButton(Material.COMPASS, "§6§lHunter Tracking", 
                List.of("§7Configure compass tracking system", 
                        "§7Current: " + (plugin.getConfigManager().isTrackerEnabled() ? "§aEnabled" : "§cDisabled"),
                        "§7§oHunters use compasses to find runners",
                        "§7§oAdjust update rate and jamming settings",
                        "§7§nClick to configure tracking"), 
                "dream_tracking"));

            inventory.setItem(23, createGuiButton(Material.POTION, "§d§lPower-ups", 
                List.of("§7Random effects on swap", 
                        "§7Current: " + (plugin.getConfigManager().isPowerUpsEnabled() ? "§aEnabled" : "§cDisabled"),
                        "§7§oBoth positive and negative effects",
                        "§7§oAdds unpredictability to gameplay",
                        "§7§nClick to configure effects"), 
                "power_ups"));

            inventory.setItem(25, createGuiButton(Material.BARRIER, "§c§lWorld Border", 
                List.of("§7Shrinking world boundary", 
                        "§7Forces final confrontation",
                        "§7§oGradually shrinks over time",
                        "§7§oCreates intense endgame scenarios",
                        "§7§nClick to configure border"), 
                "world_border"));

            // === ADVANCED FEATURES ===
            inventory.setItem(28, createGuiButton(Material.GOLD_INGOT, "§6§lBounty System", 
                List.of("§7Special hunter rewards", 
                        "§7Current: " + (plugin.getConfig().getBoolean("bounty.enabled", false) ? "§aEnabled" : "§cDisabled"),
                        "§7§oTarget specific runners for bonuses",
                        "§7§oGives glowing effect to targets",
                        "§7§nClick to configure bounties"), 
                "bounty"));

            inventory.setItem(30, createGuiButton(Material.TOTEM_OF_UNDYING, "§e§lLast Stand", 
                List.of("§7Final runner power boost", 
                        "§7Current: " + (plugin.getConfigManager().isLastStandEnabled() ? "§aEnabled" : "§cDisabled"),
                        "§7§oStrength and speed when low health",
                        "§7§oGives runners final fighting chance",
                        "§7§nClick to configure effects"), 
                "last_stand"));

            inventory.setItem(32, createGuiButton(Material.DRAGON_HEAD, "§4§lSudden Death", 
                List.of("§7End dimension final battle", 
                        "§7Current: " + (plugin.getConfig().getBoolean("sudden_death.enabled", false) ? "§aEnabled" : "§cDisabled"),
                        "§7§oForces final showdown in The End",
                        "§7§oActivates after set time limit",
                        "§7§nClick to configure sudden death"), 
                "sudden_death"));

            inventory.setItem(34, createGuiButton(Material.DIAMOND_CHESTPLATE, "§b§lCustom Kits", 
                List.of("§7Starting equipment setup", 
                        "§7Current: " + (plugin.getConfigManager().isKitsEnabled() ? "§aEnabled" : "§cDisabled"),
                        "§7§oDifferent kits for each team",
                        "§7§oBalances early game advantages",
                        "§7§nClick to edit kits"), 
                "kits"));

            // === SETTINGS & ADMIN ===
            inventory.setItem(40, createGuiButton(Material.REDSTONE, "§6§lAdvanced Settings", 
                List.of("§7Access all configuration options", 
                        "§7Swap intervals, timers, safety settings"), 
                "advanced_settings"));

            inventory.setItem(42, createGuiButton(Material.BOOK, "§a§lStatistics", 
                List.of("§7View and manage game statistics", 
                        "§7Track performance across games"), 
                "statistics"));

            player.openInventory(inventory);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error opening Dream main menu", e);
            player.sendMessage("§cFailed to open Dream menu: " + e.getMessage());
        }
    }

    public void openTaskManagerMenu(Player player) {
        try {
            String title = "§6§lTask Manager: Secret Missions";
            int rows = 6;
            Inventory inventory = Bukkit.createInventory(null, rows * 9, Component.text(title));
            
            // Use orange glass pane for Task Manager theme
            ItemStack filler = createItem(Material.ORANGE_STAINED_GLASS_PANE, " ");
            fillBorder(inventory, filler);

            boolean gameRunning = plugin.getGameManager().isGameRunning();
            boolean gamePaused = plugin.getGameManager().isGamePaused();
            int runnerCount = plugin.getGameManager().getRunners().size();
            var taskMode = plugin.getTaskManagerMode();
            int assignedTasks = taskMode != null ? taskMode.getAssignments().size() : 0;

            // Back to mode selector
            inventory.setItem(0, createGuiButton(Material.ARROW, "§7§lBack", 
                java.util.List.of("§7Return to mode selector"), "back_mode"));

            // === GAME CONTROL SECTION ===
            // Start/Stop Game
            if (!gameRunning) {
                List<String> startLore = new ArrayList<>();
                startLore.add("§7Begin secret task competition");
                if (runnerCount < 2) {
                    startLore.add("§cNeed at least 2 runners!");
                    startLore.add("§cCurrent runners: " + runnerCount);
                    inventory.setItem(10, createItem(Material.GRAY_CONCRETE, "§c§lCannot Start", startLore));
                } else {
                    startLore.add("§7Ready: §b" + runnerCount + " runners");
                    startLore.add("§7Each will get a secret task");
                    startLore.add("§7First to complete wins!");
                    inventory.setItem(10, createGuiButton(Material.LIME_CONCRETE, "§a§lStart Competition", startLore, "start_game"));
                }
            } else {
                List<String> stopLore = new ArrayList<>();
                stopLore.add("§7End current task competition");
                stopLore.add("§7Status: " + (gamePaused ? "§ePaused" : "§aRunning"));
                if (assignedTasks > 0) {
                    stopLore.add("§7Tasks assigned: §e" + assignedTasks);
                }
                inventory.setItem(10, createGuiButton(Material.RED_CONCRETE, "§c§lEnd Competition", stopLore, "stop_game"));
            }

            // Pause/Resume Game
            if (gameRunning && !gamePaused) {
                inventory.setItem(11, createGuiButton(Material.YELLOW_CONCRETE, "§e§lPause Competition", 
                    List.of("§7Temporarily pause all tasks"), "pause_game"));
            } else if (gameRunning && gamePaused) {
                inventory.setItem(11, createGuiButton(Material.ORANGE_CONCRETE, "§a§lResume Competition", 
                    List.of("§7Resume task competition"), "resume_game"));
            } else {
                inventory.setItem(11, createItem(Material.GRAY_CONCRETE, "§7Pause Competition", 
                    List.of("§7Game not running")));
            }

            // Game Status
            List<String> statusLore = new ArrayList<>();
            statusLore.add("§7Current competition status");
            statusLore.add("§7Running: " + (gameRunning ? "§aYes" : "§cNo"));
            statusLore.add("§7Runners: §b" + runnerCount);
            if (gameRunning) {
                Player activeRunner = plugin.getGameManager().getActiveRunner();
                statusLore.add("§7Active: §f" + (activeRunner != null ? activeRunner.getName() : "None"));
                statusLore.add("§7Tasks Assigned: §e" + assignedTasks);
                statusLore.add("§7Next Swap: §e" + plugin.getGameManager().getTimeUntilNextSwap() + "s");
            }
            inventory.setItem(13, createItem(Material.CLOCK, "§6§lCompetition Status", statusLore));

            // === RUNNER MANAGEMENT (No hunters in Task Manager mode) ===
            List<String> runnerLore = new ArrayList<>();
            runnerLore.add("§7Manage competition participants");
            runnerLore.add("§7Current: §b" + runnerCount + " runners");
            runnerLore.add("§7Each runner gets a secret task");
            runnerLore.add("§cNote: No hunters in Task Manager mode");
            inventory.setItem(19, createGuiButton(Material.PLAYER_HEAD, "§b§lRunner Management", runnerLore, "team_selector"));

            // === TASK MANAGEMENT SECTION ===
            inventory.setItem(21, createGuiButton(Material.TARGET, "§d§lTask Settings", 
                List.of("§7Configure task competition rules", 
                        "§7Timeouts, late joiners, disconnects",
                        "§7§oPause on disconnect behavior",
                        "§7§oRejoin grace periods and timeouts",
                        "§7§oGame ending conditions",
                        "§7§nClick to configure rules"), 
                "task_settings"));

            inventory.setItem(23, createGuiButton(Material.WRITABLE_BOOK, "§6§lCustom Tasks", 
                List.of("§7Manage custom task pool", 
                        "§7Add, remove, or modify tasks", 
                        "§7Built-in tasks: " + (plugin.getConfig().getBoolean("task_manager.include_default_tasks", true) ? "§aIncluded" : "§cExcluded"),
                        "§7§oCreate your own challenge tasks",
                        "§7§oEdit descriptions and requirements",
                        "§7§nClick to manage tasks"), 
                "custom_tasks_menu"));

            inventory.setItem(25, createGuiButton(Material.BOOK, "§b§lTask Assignments", 
                List.of("§7View current task assignments", 
                        "§7Reroll tasks (when not running)", 
                        "§7Currently assigned: §e" + assignedTasks,
                        "§7§oSee who has which secret task",
                        "§7§oReassign tasks before game starts",
                        "§7§nClick to view assignments"), 
                "task_assignments"));

            // === COMPETITION FEATURES ===
            inventory.setItem(28, createGuiButton(Material.EXPERIENCE_BOTTLE, "§a§lTask Statistics", 
                List.of("§7View competition statistics", 
                        "§7Track completion rates and times",
                        "§7§oPlayer performance metrics",
                        "§7§oGame duration and success rates",
                        "§7§nClick to view statistics"), 
                "statistics"));

            inventory.setItem(30, createGuiButton(Material.NETHER_STAR, "§e§lShuffle Tasks", 
                List.of("§7Reassign all runner tasks", 
                        gameRunning ? "§cCannot shuffle during game" : "§7Generate new task assignments",
                        "§7§oGives everyone new secret tasks",
                        "§7§oOnly works before game starts",
                        gameRunning ? "" : "§7§nClick to reroll tasks"), 
                gameRunning ? "" : "reroll_tasks"));

            // === SETTINGS & ADMIN ===
            inventory.setItem(40, createGuiButton(Material.REDSTONE, "§6§lAdvanced Settings", 
                List.of("§7Access all configuration options", 
                        "§7Swap intervals, timers, disconnection handling"), 
                "advanced_settings"));

            // === TASK MANAGER SPECIFIC INFO ===
            List<String> infoLore = new ArrayList<>();
            infoLore.add("§6Task Manager Mode Information:");
            infoLore.add("§b• §7Each runner gets a secret task");
            infoLore.add("§b• §7First to complete their task wins");
            infoLore.add("§b• §7No hunters - pure competition");
            infoLore.add("§b• §7Tasks are hidden from other players");
            infoLore.add("§b• §7Swap control to give everyone chances");
            inventory.setItem(49, createItem(Material.PAPER, "§e§lMode Information", infoLore));

            player.openInventory(inventory);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error opening Task Manager menu", e);
            player.sendMessage("§cFailed to open Task Manager menu: " + e.getMessage());
        }
    }

    public void openTaskSettingsMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("§6§lTask Settings"));
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        fillBorder(inv, filler);

        // Back
        inv.setItem(0, createItem(Material.ARROW, "§7§lBack", List.of("§7Return to Task Manager")));

        boolean pause = plugin.getConfig().getBoolean("task_manager.pause_on_disconnect", true);
        ItemStack pauseToggle = createItem(pause ? Material.REDSTONE_TORCH : Material.LEVER,
                "§e§lPause On Disconnect: " + (pause ? "§aEnabled" : "§cDisabled"),
                List.of("§7When a runner disconnects, pause the game"));
        inv.setItem(10, pauseToggle);

        int grace = plugin.getConfig().getInt("task_manager.rejoin_grace_seconds", 180);
        ItemStack graceItem = createItem(Material.CLOCK, "§6§lRejoin Grace (s)",
                List.of("§7Current: §f" + grace, "§7Left/Right: ±10", "§7Shift: ±30"));
        inv.setItem(12, graceItem);

        boolean remove = plugin.getConfig().getBoolean("task_manager.remove_on_timeout", true);
        ItemStack removeToggle = createItem(remove ? Material.BARRIER : Material.IRON_DOOR,
                "§e§lRemove On Timeout: " + (remove ? "§aYes" : "§cNo"),
                List.of("§7If runner exceeds grace, remove from queue"));
        inv.setItem(14, removeToggle);

        boolean allowLate = plugin.getConfig().getBoolean("task_manager.allow_late_joiners", false);
        ItemStack allowToggle = createItem(allowLate ? Material.LIME_DYE : Material.GRAY_DYE,
                "§e§lAllow Late Joiners: " + (allowLate ? "§aYes" : "§cNo"),
                List.of("§7Allow players to join mid-game and receive tasks"));
        inv.setItem(16, allowToggle);

        boolean endOne = plugin.getConfig().getBoolean("task_manager.end_when_one_left", false);
        ItemStack endOneToggle = createItem(endOne ? Material.REDSTONE_BLOCK : Material.GRAY_DYE,
                "§e§lEnd When One Left: " + (endOne ? "§aYes" : "§cNo"),
                List.of("§7End the game automatically when only one runner remains"));
        inv.setItem(18, endOneToggle);

        // Reroll Tasks button (only enabled before game starts)
        boolean gameRunning = plugin.getGameManager().isGameRunning();
        ItemStack rerollTasks = createGuiButton(
                gameRunning ? Material.GRAY_DYE : Material.BOOK,
                "§d§lReroll Tasks" + (gameRunning ? " §8(Game Running)" : ""),
                gameRunning ? 
                    List.of("§7Reroll tasks for all selected runners", "§c§lDisabled while game is running") :
                    List.of("§7Reroll tasks for all selected runners", "§7Must have runners selected via Team Selector"),
                "reroll_tasks");
        inv.setItem(20, rerollTasks);

        // Show Task Assignments button
        ItemStack showAssignments = createGuiButton(
                Material.WRITTEN_BOOK,
                "§b§lShow Task Assignments",
                List.of("§7Display current task assignments", "§7Shows assignments in chat"),
                "show_assignments");
        inv.setItem(22, showAssignments);

        // Assignment viewer
        var tmm = plugin.getTaskManagerMode();
        if (tmm != null) {
            int slot = 28;
            for (var entry : tmm.getAssignments().entrySet()) {
                java.util.UUID uuid = entry.getKey();
                String taskId = entry.getValue();
                var def = tmm.getTask(taskId);
                String name = plugin.getServer().getOfflinePlayer(uuid).getName();
                if (name == null) name = uuid.toString().substring(0, 8);
                ItemStack it = createItem(Material.PAPER, "§e" + name, List.of("§7" + (def != null ? def.description() : taskId)));
                inv.setItem(slot++, it);
                if (slot >= 44) break;
            }
            if (tmm.getAssignments().isEmpty()) {
                inv.setItem(31, createItem(Material.BOOK, "§7No assignments yet", List.of("§7Start a Task game to assign")));
            }
        }

        // Reload tasks button
        inv.setItem(53, createItem(Material.BOOK, "§6§lReload Tasks", List.of("§7Reload tasks from config")));

        player.openInventory(inv);
    }

    public void openDreamSettingsMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("§a§lDream Settings"));
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        fillBorder(inv, filler);
        inv.setItem(0, createItem(Material.ARROW, "§7§lBack", List.of("§7Return to Dream menu")));

        boolean tracker = plugin.getConfigManager().isTrackerEnabled();
        ItemStack trackerToggle = createGuiButton(tracker ? Material.COMPASS : Material.GRAY_DYE,
                "§e§lTracker: " + (tracker ? "§aEnabled" : "§cDisabled"),
                List.of("§7Toggle hunter tracking compass"),
                "dream_tracker_toggle");
        inv.setItem(11, trackerToggle);

        boolean singleSleep = plugin.getConfigManager().isSinglePlayerSleepEnabled();
        ItemStack sleepToggle = createGuiButton(singleSleep ? Material.RED_BED : Material.GRAY_BED,
                "§e§lSingle Player Sleep: " + (singleSleep ? "§aEnabled" : "§cDisabled"),
                List.of("§7Only active runner can sleep"),
                "dream_single_sleep_toggle");
        inv.setItem(15, sleepToggle);

        player.openInventory(inv);
    }

    public void openTeamSelector(Player player) {
        String title = plugin.getConfigManager().getGuiTeamSelectorTitle();
        int rows = plugin.getConfigManager().getGuiTeamSelectorRows();
        rows = Math.max(rows, 4); // ensure space for selector + player heads row
        rows = clampRows(rows);
        
        Inventory inventory = Bukkit.createInventory(null, rows * 9, Component.text(title));
        
        // Border-only filler for better visual organization
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        fillBorder(inventory, filler);
        
        // Back button with enhanced tooltip
        List<String> backLore = new ArrayList<>();
        backLore.add("§7Return to main menu");
        backLore.add("§7Current selections will be saved");
        ItemStack back = createItem(Material.ARROW, "§7§lBack", backLore);
        inventory.setItem(0, back);
        
        // Clear teams quick action
        ItemStack clearTeams = createGuiButton(Material.BARRIER, "§c§lClear All Teams", List.of(
            "§7Remove all existing team assignments"
        ), "clear_teams");
        inventory.setItem(8, clearTeams);
        
        // Runner team button with enhanced information
        List<String> runnerLore = new ArrayList<>();
        runnerLore.add("§7Click to select players as runners");
        runnerLore.add("");
        runnerLore.add("§7Current Runners: §b" + plugin.getGameManager().getRunners().size());
        runnerLore.add("§7Role:");
        runnerLore.add("§7• Complete objectives");
        runnerLore.add("§7• Avoid hunters");
        runnerLore.add("§7• Swap between players");
        ItemStack runnerTeam = createItem(Material.DIAMOND_BOOTS, "§b§lRunners", runnerLore);
        inventory.setItem(2, plugin.getGameManager().getPlayerState(player).getSelectedTeam() == Team.RUNNER ? 
            createGlowingItem(runnerTeam) : createNormalItem(runnerTeam));
        
        // Hunter team button with enhanced information
        List<String> hunterLore = new ArrayList<>();
        hunterLore.add("§7Click to select players as hunters");
        hunterLore.add("");
        hunterLore.add("§7Current Hunters: §c" + plugin.getGameManager().getHunters().size());
        hunterLore.add("§7Role:");
        hunterLore.add("§7• Track runners");
        hunterLore.add("§7• Eliminate runners");
        hunterLore.add("§7• Prevent objectives");
        ItemStack hunterTeam = createItem(Material.IRON_SWORD, "§c§lHunters", hunterLore);
        inventory.setItem(6, plugin.getGameManager().getPlayerState(player).getSelectedTeam() == Team.HUNTER ? 
            createGlowingItem(hunterTeam) : createNormalItem(hunterTeam));
        
        // Team selection instructions
        List<String> instructionsLore = new ArrayList<>();
        instructionsLore.add("§7How to assign teams:");
        instructionsLore.add("§71. Select a team above");
        instructionsLore.add("§72. Click player heads below");
        instructionsLore.add("§73. Confirm your selections");
        ItemStack instructions = createItem(Material.BOOK, "§e§lInstructions", instructionsLore);
        inventory.setItem(4, instructions);
        
        // Player heads with enhanced information
        int slot = 18;
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (slot >= inventory.getSize()) break;
            
            ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) playerHead.getItemMeta();
            meta.setOwningPlayer(onlinePlayer);
            
            Team currentTeam = plugin.getGameManager().getPlayerState(onlinePlayer).getSelectedTeam();
            // Apply team color to player name
            net.kyori.adventure.text.format.TextColor nameColor = currentTeam == Team.RUNNER ?
                net.kyori.adventure.text.format.NamedTextColor.AQUA :
                currentTeam == Team.HUNTER ?
                net.kyori.adventure.text.format.NamedTextColor.RED :
                net.kyori.adventure.text.format.NamedTextColor.WHITE;
            meta.displayName(net.kyori.adventure.text.Component.text(onlinePlayer.getName()).color(nameColor));
            
            List<String> lore = new ArrayList<>();
            if (plugin.getGameManager().isRunner(onlinePlayer)) {
                lore.add("§bCurrently a Runner");
                lore.add("§7Click while Hunter team is selected");
                lore.add("§7to change to Hunter");
            } else if (plugin.getGameManager().isHunter(onlinePlayer)) {
                lore.add("§cCurrently a Hunter");
                lore.add("§7Click while Runner team is selected");
                lore.add("§7to change to Runner");
            } else {
                lore.add("§7No team assigned");
                lore.add("§7Select a team above first");
                lore.add("§7then click to assign");
            }
            
            // Add online status
            lore.add("");
            lore.add(onlinePlayer.isOnline() ? "§a✔ Online" : "§c✘ Offline");
            
            List<net.kyori.adventure.text.Component> componentLore = new ArrayList<>();
            for (String line : lore) {
                componentLore.add(net.kyori.adventure.text.Component.text(line));
            }
            meta.lore(componentLore);
            playerHead.setItemMeta(meta);
            
            // Add glow effect for current team members
            if (currentTeam != Team.NONE) {
                playerHead = createGlowingItem(playerHead);
            }
            
            inventory.setItem(slot++, playerHead);
        }
        
        player.openInventory(inventory);
    }
    
    public void openSettingsMenu(Player player) {
        String title = "§6§lAdvanced Settings - All Options";
        int rows = 6;
        rows = clampRows(rows);
        
        Inventory inventory = Bukkit.createInventory(null, rows * 9, Component.text(title));
        
        // Use redstone glass pane for advanced settings theme
        ItemStack filler = createItem(Material.RED_STAINED_GLASS_PANE, " ");
        fillBorder(inventory, filler);
        
        // Back button with enhanced tooltip
        inventory.setItem(0, createGuiButton(Material.ARROW, "§7§lBack", 
            List.of("§7Return to main menu"), "back_main"));

        // === SWAP MECHANICS SECTION (Row 1) ===
        // Swap Type Toggle
        boolean isRandomized = plugin.getConfigManager().isSwapRandomized();
        inventory.setItem(10, createGuiButton(
                isRandomized ? Material.CLOCK : Material.REPEATER,
                "§e§lSwap Type: " + (isRandomized ? "§aRandom" : "§bFixed"),
                List.of("§7Toggle between fixed and random intervals",
                        isRandomized ? "§7Random adds unpredictability" : "§7Fixed provides consistent timing"),
                "random_swaps"));

        // Swap Interval Controls
        int currentInterval = plugin.getConfigManager().getSwapInterval();
        boolean isBeta = plugin.getConfigManager().isBetaIntervalEnabled();
        List<String> intervalLore = new ArrayList<>();
        intervalLore.add("§7Current: §e" + currentInterval + " seconds");
        intervalLore.add("§7Use arrows to adjust (±5s)");
        if (isBeta && currentInterval < 30) {
            intervalLore.add("§cBETA: Below 30s may be unstable");
        }
        inventory.setItem(11, createGuiButton(Material.PAPER, "§e§lSwap Interval", intervalLore, "swap_interval"));
        inventory.setItem(12, createGuiButton(Material.ARROW, "§c-5s", List.of("§7Decrease interval"), "interval_minus"));
        inventory.setItem(14, createGuiButton(Material.ARROW, "§a+5s", List.of("§7Increase interval"), "interval_plus"));

        // Experimental Mode Toggle
        inventory.setItem(15, createGuiButton(
                isBeta ? Material.REDSTONE_TORCH : Material.LEVER,
                "§e§lExperimental: " + (isBeta ? "§aON" : "§cOFF"),
                List.of("§7Allow <30s and >max intervals",
                        "§7Shows warnings when used"),
                "beta_intervals"));

        // Random Interval Range (only if randomized)
        if (isRandomized) {
            int minInterval = plugin.getConfigManager().getMinSwapInterval();
            int maxInterval = plugin.getConfigManager().getMaxSwapInterval();
            inventory.setItem(16, createGuiButton(Material.COMPASS, "§6§lRandom Range",
                List.of("§7Min: §e" + minInterval + "s", 
                        "§7Max: §e" + maxInterval + "s",
                        "§7Left/Right: ±5s, Shift: ±15s"),
                "random_range"));
        }

        // === SAFETY & MECHANICS SECTION (Row 2) ===
        // Safe Swap Toggle
        boolean safeSwap = plugin.getConfigManager().isSafeSwapEnabled();
        inventory.setItem(19, createGuiButton(
                safeSwap ? Material.SLIME_BLOCK : Material.MAGMA_BLOCK,
                "§e§lSafe Swaps: " + (safeSwap ? "§aON" : "§cOFF"),
                List.of("§7Prevent dangerous spawn locations",
                        "§7Radius: §e" + plugin.getConfigManager().getSafeSwapHorizontalRadius() + " blocks"),
                "safe_swaps"));

        // Grace Period
        int graceTicks = plugin.getConfigManager().getGracePeriodTicks();
        inventory.setItem(20, createGuiButton(Material.SHIELD, "§6§lGrace Period",
                List.of("§7Current: §e" + (graceTicks / 20.0) + "s",
                        "§7Protection after swaps",
                        "§7Left/Right: ±0.5s, Shift: ±2s"),
                "grace_period"));

        // Pause on Disconnect
        boolean pauseDisconnect = plugin.getConfigManager().isPauseOnDisconnect();
        inventory.setItem(21, createGuiButton(
                pauseDisconnect ? Material.REDSTONE_TORCH : Material.LEVER,
                "§e§lPause on Disconnect: " + (pauseDisconnect ? "§aON" : "§cOFF"),
                List.of("§7Auto-pause if active runner leaves"),
                "pause_disconnect"));

        // Inactive Runner State
        String freezeMode = plugin.getConfigManager().getFreezeMode();
        Material freezeIcon = switch (freezeMode.toUpperCase()) {
            case "SPECTATOR" -> Material.ENDER_EYE;
            case "LIMBO" -> Material.ENDER_PEARL;
            case "CAGE" -> Material.BEDROCK;
            default -> Material.POTION;
        };
        inventory.setItem(22, createGuiButton(freezeIcon, "§6§lInactive State: §a" + freezeMode,
                List.of("§7How inactive runners are handled",
                        "§7Click to cycle through options"),
                "freeze_mode"));

        // === TIMER VISIBILITY SECTION (Row 3) ===
        // Runner Timer Visibility
        String runnerVis = plugin.getConfigManager().getRunnerTimerVisibility();
        inventory.setItem(28, createGuiButton(Material.CLOCK, "§b§lActive Runner Timer",
                List.of("§7Current: §e" + getVisibilityDisplay(runnerVis),
                        "§7Click to cycle visibility"),
                "runner_timer"));

        // Waiting Timer Visibility
        String waitingVis = plugin.getConfigManager().getWaitingTimerVisibility();
        inventory.setItem(29, createGuiButton(Material.CLOCK, "§d§lWaiting Runner Timer",
                List.of("§7Current: §e" + getVisibilityDisplay(waitingVis),
                        "§7Click to cycle visibility"),
                "waiting_timer"));

        // Hunter Timer Visibility
        String hunterVis = plugin.getConfigManager().getHunterTimerVisibility();
        inventory.setItem(30, createGuiButton(Material.CLOCK, "§c§lHunter Timer",
                List.of("§7Current: §e" + getVisibilityDisplay(hunterVis),
                        "§7Click to cycle visibility"),
                "hunter_timer"));

        // === HUNTER-SPECIFIC FEATURES (Row 4) ===
        // Tracker Toggle (Dream mode only)
        if (plugin.getCurrentMode() == com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.DREAM) {
            boolean trackerEnabled = plugin.getConfigManager().isTrackerEnabled();
            inventory.setItem(37, createGuiButton(
                    trackerEnabled ? Material.COMPASS : Material.GRAY_DYE,
                    "§e§lHunter Tracking: " + (trackerEnabled ? "§aON" : "§cOFF"),
                    List.of("§7Hunters get tracking compasses",
                            "§7Update rate: §e" + (plugin.getConfigManager().getTrackerUpdateTicks() / 20) + "s"),
                    "tracker_toggle"));

            // Hunter Swap Feature
            boolean hunterSwap = plugin.getConfigManager().isHunterSwapEnabled();
            inventory.setItem(38, createGuiButton(
                    hunterSwap ? Material.CROSSBOW : Material.GRAY_DYE,
                    "§e§lHunter Swap: " + (hunterSwap ? "§aON" : "§cOFF"),
                    List.of("§7Periodically shuffle hunter focus",
                            "§7Interval: §e" + plugin.getConfigManager().getHunterSwapInterval() + "s"),
                    "hunter_swap"));

            // Hot Potato Mode
            boolean hotPotato = plugin.getConfigManager().isHotPotatoModeEnabled();
            inventory.setItem(39, createGuiButton(
                    hotPotato ? Material.BLAZE_POWDER : Material.GRAY_DYE,
                    "§e§lHot Potato: " + (hotPotato ? "§aON" : "§cOFF"),
                    List.of("§7Swap when runner takes damage",
                            "§7Overrides normal timer"),
                    "hot_potato"));
        }

        // === QUALITY OF LIFE SECTION (Row 5) ===
        // Single Player Sleep
        boolean singleSleep = plugin.getConfigManager().isSinglePlayerSleepEnabled();
        inventory.setItem(46, createGuiButton(
                singleSleep ? Material.WHITE_BED : Material.RED_BED,
                "§e§lSingle Player Sleep: " + (singleSleep ? "§aON" : "§cOFF"),
                List.of("§7Only active runner can skip night",
                        "§7Recommended for cage/spectator modes"),
                "single_sleep"));

        // Voice Chat Integration
        boolean voiceChat = plugin.getConfigManager().isVoiceChatIntegrationEnabled();
        inventory.setItem(47, createGuiButton(
                voiceChat ? Material.NOTE_BLOCK : Material.GRAY_DYE,
                "§e§lVoice Chat: " + (voiceChat ? "§aON" : "§cOFF"),
                List.of("§7Integrate with Simple Voice Chat",
                        "§7Mute inactive runners: " + (plugin.getConfigManager().isMuteInactiveRunners() ? "§aYes" : "§cNo")),
                "voice_chat"));

        // Statistics Tracking
        boolean statsEnabled = plugin.getConfig().getBoolean("stats.enabled", true);
        inventory.setItem(48, createGuiButton(
                statsEnabled ? Material.BOOK : Material.GRAY_DYE,
                "§e§lStatistics: " + (statsEnabled ? "§aON" : "§cOFF"),
                List.of("§7Track game performance metrics",
                        "§7Distance, time, completion rates"),
                "stats_toggle"));

        // === QUICK ACCESS BUTTONS ===
        // Mode-specific feature access
        switch (plugin.getCurrentMode()) {
            case DREAM -> {
                inventory.setItem(43, createGuiButton(Material.POTION, "§d§lPower-ups Settings",
                    List.of("§7Configure random effects on swap"), "power_ups"));
                inventory.setItem(44, createGuiButton(Material.BARRIER, "§c§lWorld Border Settings",
                    List.of("§7Configure shrinking world boundary"), "world_border"));
            }
            case TASK -> {
                inventory.setItem(43, createGuiButton(Material.TARGET, "§6§lTask Settings",
                    List.of("§7Configure task competition rules"), "task_settings"));
                inventory.setItem(44, createGuiButton(Material.WRITABLE_BOOK, "§6§lCustom Tasks",
                    List.of("§7Manage custom task pool"), "custom_tasks_menu"));
            }
            case SAPNAP -> {
                inventory.setItem(43, createGuiButton(Material.BEDROCK, "§b§lCage Settings",
                    List.of("§7Configure cage behavior"), "cage_settings"));
            }
        }

        // Dangerous Blocks Editor (Safe Swap)
        if (safeSwap) {
            inventory.setItem(49, createGuiButton(Material.MAGMA_BLOCK, "§6§lDangerous Blocks",
                List.of("§7Edit list of avoided blocks"), "dangerous_blocks"));
        }
        
        // === ADVANCED CONFIG SECTIONS (Row 6) ===
        // Broadcast Settings
        inventory.setItem(50, createGuiButton(Material.BELL, "§e§lBroadcast Settings",
            List.of("§7Configure game event announcements",
                    "§7Swap events, team changes, etc."), "broadcast_settings"));
        
        // Limbo Configuration (if freeze mode is LIMBO)
        if ("LIMBO".equalsIgnoreCase(freezeMode)) {
            inventory.setItem(51, createGuiButton(Material.ENDER_PEARL, "§6§lLimbo Settings",
                List.of("§7Configure limbo world and coordinates"), "limbo_settings"));
        }
        
        // UI Performance Settings
        inventory.setItem(52, createGuiButton(Material.COMPARATOR, "§d§lUI Performance",
            List.of("§7Update frequencies for timers",
                    "§7Actionbar and title refresh rates"), "ui_performance"));
        
        // Full Config Browser
        inventory.setItem(53, createGuiButton(Material.WRITABLE_BOOK, "§c§lAdvanced Config Browser",
            List.of("§7Direct access to all config values",
                    "§7Edit any setting not in main GUI"), "advanced_config_root"));

        // Reset to Defaults
        inventory.setItem(53, createGuiButton(Material.BARRIER, "§c§lReset All Settings",
            List.of("§7Reset all settings to defaults",
                    "§cThis cannot be undone!"),
            "reset_all_settings"));

        player.openInventory(inventory);
    }


    
    public void openCustomTasksMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("§d§lCustom Tasks Manager"));
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        fillBorder(inv, filler);
        
        ItemStack back = createItem(Material.ARROW, "§7§lBack", List.of("§7Return to Settings"));
        inv.setItem(0, back);
        
        // Header info
        boolean includeDefaults = plugin.getConfig().getBoolean("task_manager.include_default_tasks", true);
        ItemStack toggleDefaults = createItem(
            includeDefaults ? Material.LIME_DYE : Material.GRAY_DYE,
            "§e§lInclude Default Tasks: " + (includeDefaults ? "§aEnabled" : "§cDisabled"),
            List.of("§7Toggle whether built-in tasks are included")
        );
        inv.setItem(4, toggleDefaults);
        
        // Add new task button
        ItemStack addTask = createItem(Material.EMERALD, "§a§lAdd New Task", 
            List.of("§7Click to add a custom task", "§7You'll be prompted in chat"));
        inv.setItem(8, addTask);
        
        // Reload tasks button
        ItemStack reloadTasks = createItem(Material.BOOK, "§6§lReload Tasks",
            List.of("§7Reload all tasks from config"));
        inv.setItem(53, reloadTasks);
        
        // Display custom tasks
        var taskMode = plugin.getTaskManagerMode();
        if (taskMode != null) {
            List<String> customIds = taskMode.getCustomTaskIds();
            int slot = 9;
            for (String taskId : customIds) {
                if (slot >= 45) break; // Leave room for bottom border
                
                var task = taskMode.getTask(taskId);
                if (task != null) {
                    ItemStack taskItem = createItem(Material.PAPER, "§e" + taskId,
                        List.of("§7" + task.description(), "", "§cClick to remove"));
                    inv.setItem(slot, taskItem);
                }
                slot++;
            }
            
            // If no custom tasks
            if (customIds.isEmpty()) {
                ItemStack noTasks = createItem(Material.BARRIER, "§cNo Custom Tasks",
                    List.of("§7Click 'Add New Task' to create one"));
                inv.setItem(22, noTasks);
            }
        }
        
        player.openInventory(inv);
    }
    
    // Method to prompt player for task input
    public void promptTaskInput(Player player, String type) {
        player.closeInventory();
        if (type.equals("id")) {
            player.sendMessage(Component.text("[Task Manager] Enter a unique task ID (e.g., 'build_nether_house'):").color(NamedTextColor.YELLOW));
            player.sendMessage(Component.text("Type 'cancel' to abort").color(NamedTextColor.GRAY));
            // Store state for chat listener
            plugin.getChatInputHandler().expectTaskId(player);
        } else if (type.equals("description")) {
            player.sendMessage(Component.text("[Task Manager] Enter the task description:").color(NamedTextColor.YELLOW));
            player.sendMessage(Component.text("Type 'cancel' to abort").color(NamedTextColor.GRAY));
            // Store state for chat listener
            plugin.getChatInputHandler().expectTaskDescription(player);
        }
    }
    
    private int clampRows(int rows) {
        if (rows < 1) return 1;
        if (rows > 6) return 6;
        return rows;
    }

    public ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));

        if (lore.length > 0) {
            List<Component> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(Component.text(line));
            }
            meta.lore(loreList);
        }

        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createGlowingItem(ItemStack item) {
        ItemStack glowingItem = item.clone();
        ItemMeta meta = glowingItem.getItemMeta();
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        glowingItem.setItemMeta(meta);
        return glowingItem;
    }

    public ItemStack createNormalItem(ItemStack item) {
        ItemStack normalItem = item.clone();
        ItemMeta meta = normalItem.getItemMeta();
        meta.removeEnchant(Enchantment.UNBREAKING);
        meta.removeItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        normalItem.setItemMeta(meta);
        return normalItem;
    }
    
    public ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        
        if (lore != null && !lore.isEmpty()) {
            List<Component> componentLore = new ArrayList<>();
            for (String line : lore) {
                componentLore.add(Component.text(line));
            }
            meta.lore(componentLore);
        }
        
        item.setItemMeta(meta);
        return item;
    }

    // Visual utility: fill only the outer border of an inventory with a filler item
    private void fillBorder(Inventory inv, ItemStack filler) {
        if (inv == null || filler == null) return;
        int size = inv.getSize();
        if (size < 9) return;
        int rows = size / 9;
        // Top and bottom rows
        for (int c = 0; c < 9; c++) {
            inv.setItem(c, filler);
            inv.setItem((rows - 1) * 9 + c, filler);
        }
        // Left and right columns
        for (int r = 0; r < rows; r++) {
            inv.setItem(r * 9, filler);
            inv.setItem(r * 9 + 8, filler);
        }
    }
    
    @SuppressWarnings("unused")
    private List<String> getStatusLore() {
        List<String> lore = new ArrayList<>();
        
        lore.add("§7Game Running: " + (plugin.getGameManager().isGameRunning() ? "§aYes" : "§cNo"));
        lore.add("§7Game Paused: " + (plugin.getGameManager().isGamePaused() ? "§eYes" : "§aNo"));
        
        if (plugin.getGameManager().isGameRunning()) {
            Player activeRunner = plugin.getGameManager().getActiveRunner();
            lore.add("§7Active Runner: §f" + (activeRunner != null ? activeRunner.getName() : "None"));
            lore.add("§7Next Swap: §f" + plugin.getGameManager().getTimeUntilNextSwap() + "s");
            
            lore.add("");
            lore.add("§bRunners:");
            for (Player runner : plugin.getGameManager().getRunners()) {
                lore.add("§7- §f" + runner.getName());
            }
            
            lore.add("");
            lore.add("§cHunters:");
            for (Player hunter : plugin.getGameManager().getHunters()) {
                lore.add("§7- §f" + hunter.getName());
            }
        }
        
        return lore;
    }
    
    // Advanced Config Browser
    public void openAdvancedConfigMenu(Player player, String path, int page) {
        if (path == null) path = "";
        org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfig();
        org.bukkit.configuration.ConfigurationSection section = path.isEmpty() ? cfg.getConfigurationSection("") : cfg.getConfigurationSection(path);
        if (section == null) {
            // If leaf value, bounce to parent
            String parent = getParentPath(path);
            section = parent == null ? cfg.getConfigurationSection("") : cfg.getConfigurationSection(parent);
            path = parent == null ? "" : parent;
        }
        java.util.Set<String> keys = section == null ? java.util.Collections.emptySet() : section.getKeys(false);

        int rows = 6;
        String title = "§6§lAdvanced Config" + (path.isEmpty() ? "" : " §7(" + path + ")");
        Inventory inv = Bukkit.createInventory(null, rows * 9, Component.text(title));
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        fillBorder(inv, filler);

        // Back to Settings or parent
        if (path.isEmpty()) {
            inv.setItem(0, createGuiButton(Material.ARROW, "§7§lBack", List.of("§7Return to Settings"), "back_settings"));
        } else {
            String parent = getParentPath(path);
            String id = parent == null ? "advanced_config_root" : ("cfg:nav:" + parent);
            inv.setItem(0, createGuiButton(Material.ARROW, "§7§lUp: §f" + (parent == null ? "root" : parent), List.of("§7Go to parent section"), id));
        }

        // Pagination
        java.util.List<String> sorted = new java.util.ArrayList<>(keys);
        java.util.Collections.sort(sorted);
        int start = page * 28;
        int end = Math.min(start + 28, sorted.size());
        int slot = 10;
        for (int i = start; i < end; i++) {
            String key = sorted.get(i);
            String full = path.isEmpty() ? key : path + "." + key;
            Object val = cfg.get(full);
            ItemStack it;
            if (val instanceof org.bukkit.configuration.ConfigurationSection) {
                it = createGuiButton(Material.CHEST, "§e§l" + key + "/", List.of("§7Open section", "§8" + full), "cfg:nav:" + full);
            } else if (val instanceof Boolean) {
                boolean b = (Boolean) val;
                it = createGuiButton(b ? Material.LIME_DYE : Material.GRAY_DYE, "§a§l" + key + " = " + (b ? "§atrue" : "§cfalse"), List.of("§7Toggle", "§8" + full), "cfg:bool:" + full);
            } else if (val instanceof Number) {
                String display = String.valueOf(val);
                it = createGuiButton(Material.REPEATER, "§6§l" + key + " = §e" + display, List.of("§7Left/Right: ±1", "§7Shift: ±10", "§8" + full), "cfg:num:" + full);
            } else if (val instanceof java.util.List) {
                // assume string list for generic editor
                it = createGuiButton(Material.PAPER, "§b§l" + key + "[]", List.of("§7Edit list", "§8" + full), "cfg:list:" + full);
            } else {
                String s = cfg.getString(full, String.valueOf(val));
                it = createGuiButton(Material.NAME_TAG, "§d§l" + key + " = §f" + (s == null ? "null" : truncate(s, 20)), List.of("§7Click to edit string", "§8" + full), "cfg:str:" + full);
            }
            inv.setItem(slot, it);
            slot++;
            if ((slot + 1) % 9 == 0) slot += 2; // skip border columns
        }

        // Page controls
        if (page > 0) inv.setItem(45, createGuiButton(Material.ARROW, "§7§lPrev Page", List.of(), "cfg:page:" + path + ":" + (page - 1)));
        if (end < sorted.size()) inv.setItem(53, createGuiButton(Material.ARROW, "§7§lNext Page", List.of(), "cfg:page:" + path + ":" + (page + 1)));

        Bukkit.getPlayer(player.getUniqueId()).openInventory(inv);
    }

    public void openConfigListEditor(Player player, String path, int page) {
        org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfig();
        java.util.List<String> list = cfg.getStringList(path);
        int rows = 6;
        String title = "§6§lList Editor §7(" + path + ")";
        Inventory inv = Bukkit.createInventory(null, rows * 9, Component.text(title));
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        fillBorder(inv, filler);

        // Back
        inv.setItem(0, createGuiButton(Material.ARROW, "§7§lBack", List.of("§7Return to Advanced Config"), "cfg:nav:" + getParentPath(path)));
        // Add new
        inv.setItem(8, createGuiButton(Material.EMERALD, "§a§lAdd Item", List.of("§7Click to add via chat"), "cfg:list_add:" + path));

        // Items
        int start = page * 28;
        int end = Math.min(start + 28, list.size());
        int slot = 10;
        for (int i = start; i < end; i++) {
            String item = list.get(i);
            String id = "cfg:list_del:" + path + ":" + i;
            inv.setItem(slot, createGuiButton(Material.PAPER, "§f" + truncate(item, 30), List.of("§cClick to remove", "§8index:" + i), id));
            slot++;
            if ((slot + 1) % 9 == 0) slot += 2;
        }
        // Pages
        if (page > 0) inv.setItem(45, createGuiButton(Material.ARROW, "§7§lPrev Page", List.of(), "cfg:list_page:" + path + ":" + (page - 1)));
        if (end < list.size()) inv.setItem(53, createGuiButton(Material.ARROW, "§7§lNext Page", List.of(), "cfg:list_page:" + path + ":" + (page + 1)));

        Bukkit.getPlayer(player.getUniqueId()).openInventory(inv);
    }

    private String getParentPath(String path) {
        if (path == null || path.isEmpty()) return null;
        int idx = path.lastIndexOf('.');
        if (idx < 0) return null;
        return path.substring(0, idx);
    }

    private String truncate(String s, int n) {
        if (s == null) return "null";
        if (s.length() <= n) return s;
        return s.substring(0, n - 1) + "…";
    }
    
    // Helper methods for menu identification
    private boolean isItem(ItemStack item, Material material, String name) {
        if (item == null || item.getType() != material || !item.hasItemMeta()) {
            return false;
        }
        String displayName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
        return name.equals(displayName);
    }
    
    public boolean isMainMenu(Inventory inventory) {
        if (inventory == null || inventory.getHolder() != null || inventory.getViewers().isEmpty()) {
            return false;
        }
        String title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(inventory.getViewers().get(0).getOpenInventory().title());
        return plugin.getConfigManager().getGuiMainMenuTitle().equals(title);
    }
    
    public boolean isTeamSelector(Inventory inventory) {
        if (inventory == null || inventory.getHolder() != null || inventory.getViewers().isEmpty()) {
            return false;
        }
        String title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(inventory.getViewers().get(0).getOpenInventory().title());
        return plugin.getConfigManager().getGuiTeamSelectorTitle().equals(title);
    }
    
    public boolean isSettingsMenu(Inventory inventory) {
        if (inventory == null || inventory.getHolder() != null || inventory.getViewers().isEmpty()) {
            return false;
        }
        String title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(inventory.getViewers().get(0).getOpenInventory().title());
        return plugin.getConfigManager().getGuiSettingsTitle().equals(title);
    }
    
    // Helper methods for item identification
    public boolean isBackButton(ItemStack item) {
        return isItem(item, Material.ARROW, "§7§lBack");
    }
    
    public boolean isStartButton(ItemStack item) {
        return isItem(item, Material.GREEN_CONCRETE, "§a§lStart Game");
    }
    
    public boolean isStopButton(ItemStack item) {
        return isItem(item, Material.RED_CONCRETE, "§c§lStop Game");
    }
    
    public boolean isPauseButton(ItemStack item) {
        return isItem(item, Material.YELLOW_CONCRETE, "§e§lPause Game");
    }
    
    public boolean isResumeButton(ItemStack item) {
        return isItem(item, Material.LIME_CONCRETE, "§a§lResume Game");
    }
    
    public boolean isTeamSelectorButton(ItemStack item) {
        return isItem(item, Material.PLAYER_HEAD, "§e§lTeam Selector");
    }
    
    public boolean isSettingsButton(ItemStack item) {
        return isItem(item, Material.COMPARATOR, "§a§lSettings");
    }
    
    public boolean isRunnerTeamButton(ItemStack item) {
        return isItem(item, Material.DIAMOND_BOOTS, "§b§lRunners");
    }
    
    public boolean isHunterTeamButton(ItemStack item) {
        return isItem(item, Material.IRON_SWORD, "§c§lHunters");
    }
    
    private static final String BUTTON_ID_KEY = "ssw_button_id";

    public ItemStack createGuiButton(Material material, String name, List<String> lore, String buttonId) {
        ItemStack item = createItem(material, name, lore);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, BUTTON_ID_KEY),
                org.bukkit.persistence.PersistentDataType.STRING,
                buttonId
            );
            item.setItemMeta(meta);
        }
        return item;
    }

    public String getButtonId(ItemStack item) {
        if (item != null && item.hasItemMeta()) {
            return item.getItemMeta().getPersistentDataContainer().get(
                new org.bukkit.NamespacedKey(plugin, BUTTON_ID_KEY),
                org.bukkit.persistence.PersistentDataType.STRING
            );
        }
        return null;
    }

    public boolean isSwapIntervalButton(ItemStack item) {
        String buttonId = getButtonId(item);
        return "swap_interval".equals(buttonId) && item != null && item.getType() == Material.CLOCK;
    }

    public boolean isRandomizeSwapButton(ItemStack item) {
        String buttonId = getButtonId(item);
        return "random_swaps".equals(buttonId) && item != null && 
               (item.getType() == Material.CLOCK || item.getType() == Material.REPEATER);
    }

    public boolean isSafeSwapButton(ItemStack item) {
        String buttonId = getButtonId(item);
        return "safe_swaps".equals(buttonId) && item != null && 
               (item.getType() == Material.TOTEM_OF_UNDYING || item.getType() == Material.BARRIER);
    }

    public boolean isActiveRunnerTimerButton(ItemStack item) {
        return isItem(item, Material.CLOCK, "§e§lActive Runner Timer");
    }

    public boolean isWaitingRunnerTimerButton(ItemStack item) {
        return isItem(item, Material.CLOCK, "§e§lWaiting Runner Timer");
    }

    public boolean isHunterTimerButton(ItemStack item) {
        return isItem(item, Material.CLOCK, "§e§lHunter Timer");
    }

    private String getVisibilityDisplay(String visibility) {
        switch (visibility) {
            case "always":
                return "§aAlways Show";
            case "last_10":
                return "§eLast 10 Seconds";
            case "never":
                return "§cNever Show";
            default:
                return "§7Unknown";
        }
    }

    public String getNextVisibility(String current) {
        switch (current) {
            case "always":
                return "last_10";
            case "last_10":
                return "never";
            case "never":
                return "always";
            default:
                return "last_10";
        }
    }
    
    // Helper methods for team management
    public void setPlayerTeam(Player player, Team team) {
        plugin.getGameManager().getPlayerState(player).setSelectedTeam(team);
        updateTeamSelectors();
    }
    
    public void updateTeamSelectors() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isTeamSelector(player.getOpenInventory().getTopInventory())) {
                openTeamSelector(player);
            }
        }
    }
    
    // Helper methods for game state
    public void updateMainMenus() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isMainMenu(player.getOpenInventory().getTopInventory())) {
                openMainMenu(player);
            }
        }
    }
    
    public void updateSettingsMenus() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isSettingsMenu(player.getOpenInventory().getTopInventory())) {
                openSettingsMenu(player);
            }
        }
    }
    
    
    // Fix for missing getSelectedTeam method
    public Team getSelectedTeam(Player player) {
        return plugin.getGameManager().getPlayerState(player).getSelectedTeam();
    }
    
    // Fix for missing isSwapRandomized method
    public boolean isSwapRandomized() {
        return plugin.getConfigManager().isSwapRandomized();
    }
    
    // Statistics Menu
    public void openStatisticsMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, Component.text("§b§lStatistics & Tracking"));
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        fillBorder(inventory, filler);
        
        // Back button
        inventory.setItem(0, createGuiButton(Material.ARROW, "§7§lBack", 
            List.of("§7Return to Settings"), "back_settings"));
        
        // Statistics toggles
        boolean statsEnabled = plugin.getConfig().getBoolean("stats.enabled", true);
        boolean distanceTracking = plugin.getConfig().getBoolean("stats.distance_tracking", true);
        int distanceUpdateTicks = plugin.getConfig().getInt("stats.distance_update_ticks", 20);
        
        ItemStack statsToggle = createGuiButton(
            statsEnabled ? Material.LIME_DYE : Material.GRAY_DYE,
            "§e§lStatistics: " + (statsEnabled ? "§aEnabled" : "§cDisabled"),
            List.of("§7Toggle statistics collection", "§7Currently: " + (statsEnabled ? "§aOn" : "§cOff")),
            "toggle_stats");
        inventory.setItem(11, statsToggle);
        
        ItemStack distanceToggle = createGuiButton(
            distanceTracking ? Material.COMPASS : Material.BARRIER,
            "§6§lDistance Tracking: " + (distanceTracking ? "§aEnabled" : "§cDisabled"),
            List.of("§7Track distance between players", "§7Update Rate: §f" + distanceUpdateTicks + " ticks"),
            "toggle_distance_tracking");
        inventory.setItem(13, distanceToggle);
        
        ItemStack updateRate = createGuiButton(Material.REPEATER, "§6§lUpdate Rate",
            List.of("§7Current: §f" + distanceUpdateTicks + " ticks", "§7Left/Right click: ±5 ticks"),
            "stats_update_rate");
        inventory.setItem(15, updateRate);
        
        player.openInventory(inventory);
    }
    
    // Kits Menu
    public void openKitsMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, Component.text("§6§lKit Management"));
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        fillBorder(inventory, filler);
        
        // Back button
        inventory.setItem(0, createGuiButton(Material.ARROW, "§7§lBack", 
            List.of("§7Return to Settings"), "back_settings"));
        
        // Kit options
        boolean runnerKitEnabled = plugin.getConfig().getBoolean("kits.runner_kit.enabled", false);
        boolean hunterKitEnabled = plugin.getConfig().getBoolean("kits.hunter_kit.enabled", false);
        
        ItemStack runnerKit = createGuiButton(
            runnerKitEnabled ? Material.DIAMOND_SWORD : Material.WOODEN_SWORD,
            "§a§lRunner Kit: " + (runnerKitEnabled ? "§aEnabled" : "§cDisabled"),
            List.of("§7Starting equipment for runners", "§7Click to edit kit contents"),
            "edit_runner_kit");
        inventory.setItem(20, runnerKit);
        
        ItemStack hunterKit = createGuiButton(
            hunterKitEnabled ? Material.BOW : Material.STICK,
            "§c§lHunter Kit: " + (hunterKitEnabled ? "§aEnabled" : "§cDisabled"),
            List.of("§7Starting equipment for hunters", "§7Click to edit kit contents"),
            "edit_hunter_kit");
        inventory.setItem(24, hunterKit);
        
        player.openInventory(inventory);
    }
    
    // Kit Editor
    public void openKitEditor(Player player, String kitType) {
        String title = kitType.equals("runner") ? "§a§lRunner Kit Editor" : "§c§lHunter Kit Editor";
        Inventory inventory = Bukkit.createInventory(null, 54, Component.text(title));
        
        // Instructions
        ItemStack instructions = createItem(Material.PAPER, "§e§lInstructions",
            List.of("§7Place items you want in the kit", "§7Empty slots will be ignored", 
                    "§7Click 'Save Kit' when finished"));
        inventory.setItem(4, instructions);
        
        // Save button
        ItemStack save = createGuiButton(Material.EMERALD, "§a§lSave Kit",
            List.of("§7Save current contents as kit"), "save_" + kitType + "_kit");
        inventory.setItem(49, save);
        
        // Back button
        inventory.setItem(45, createGuiButton(Material.ARROW, "§7§lBack", 
            List.of("§7Return to Kit Menu"), "back_kits"));
        
        player.openInventory(inventory);
    }
    
    // Bounty Menu
    public void openBountyMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, Component.text("§6§lBounty System"));
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        fillBorder(inventory, filler);
        
        // Back button
        inventory.setItem(0, createGuiButton(Material.ARROW, "§7§lBack", 
            List.of("§7Return to Settings"), "back_settings"));
        
        // Bounty system settings
        boolean bountyEnabled = plugin.getConfig().getBoolean("bounty.enabled", false);
        int baseBounty = plugin.getConfig().getInt("bounty.base_amount", 10);
        int killMultiplier = plugin.getConfig().getInt("bounty.kill_multiplier", 2);
        int maxBounty = plugin.getConfig().getInt("bounty.max_amount", 100);
        
        ItemStack enableToggle = createGuiButton(
            bountyEnabled ? Material.GOLD_INGOT : Material.BARRIER,
            "§6§lBounty System: " + (bountyEnabled ? "§aEnabled" : "§cDisabled"),
            List.of("§7Toggle bounty rewards for kills"),
            "toggle_bounty");
        inventory.setItem(11, enableToggle);
        
        ItemStack baseAmount = createGuiButton(Material.GOLD_NUGGET, "§6§lBase Bounty",
            List.of("§7Current: §f" + baseBounty + " gold", "§7Left/Right click: ±5"),
            "bounty_base");
        inventory.setItem(13, baseAmount);
        
        ItemStack multiplier = createGuiButton(Material.EXPERIENCE_BOTTLE, "§6§lKill Multiplier",
            List.of("§7Current: §fx" + killMultiplier, "§7Increases bounty per kill"),
            "bounty_multiplier");
        inventory.setItem(15, multiplier);
        
        ItemStack maxAmount = createGuiButton(Material.GOLD_BLOCK, "§6§lMax Bounty",
            List.of("§7Current: §f" + maxBounty + " gold", "§7Left/Right click: ±10"),
            "bounty_max");
        inventory.setItem(17, maxAmount);
        
        player.openInventory(inventory);
    }
    
    // Last Stand Menu
    public void openLastStandMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, Component.text("§c§lLast Stand Mode"));
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        fillBorder(inventory, filler);
        
        // Back button
        inventory.setItem(0, createGuiButton(Material.ARROW, "§7§lBack", 
            List.of("§7Return to Settings"), "back_settings"));
        
        // Last Stand settings
        boolean lastStandEnabled = plugin.getConfig().getBoolean("last_stand.enabled", false);
        int healthThreshold = plugin.getConfig().getInt("last_stand.health_threshold", 4);
        int duration = plugin.getConfig().getInt("last_stand.duration", 30);
        
        ItemStack enableToggle = createGuiButton(
            lastStandEnabled ? Material.TOTEM_OF_UNDYING : Material.BARRIER,
            "§c§lLast Stand: " + (lastStandEnabled ? "§aEnabled" : "§cDisabled"),
            List.of("§7Special effects when low on health",
                    "§7Click to toggle on/off"),
            "toggle_last_stand");
        inventory.setItem(11, enableToggle);
        
        ItemStack threshold = createGuiButton(Material.RED_DYE, "§c§lHealth Threshold",
            List.of("§7Current: §f" + healthThreshold + " hearts", 
                    "§7Left click: +1 heart",
                    "§7Right click: -1 heart"),
            "last_stand_threshold");
        inventory.setItem(13, threshold);
        
        ItemStack durationItem = createGuiButton(Material.CLOCK, "§c§lDuration",
            List.of("§7Current: §f" + duration + " seconds", 
                    "§7Left click: +5 seconds",
                    "§7Right click: -5 seconds"),
            "last_stand_duration");
        inventory.setItem(15, durationItem);
        
        ItemStack strengthAmp = createGuiButton(Material.IRON_SWORD, "§e§lStrength Amplifier",
            List.of("§7Current: §f" + plugin.getConfigManager().getLastStandStrengthAmplifier(),
                    "§7Left click: +1 level",
                    "§7Right click: -1 level"),
            "last_stand_strength");
        inventory.setItem(20, strengthAmp);
        
        ItemStack speedAmp = createGuiButton(Material.SUGAR, "§b§lSpeed Amplifier",
            List.of("§7Current: §f" + plugin.getConfigManager().getLastStandSpeedAmplifier(),
                    "§7Left click: +1 level",
                    "§7Right click: -1 level"),
            "last_stand_speed");
        inventory.setItem(24, speedAmp);
        
        player.openInventory(inventory);
    }
    
    // Compass Settings Menu
    public void openCompassSettingsMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, Component.text("§b§lCompass Settings"));
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        fillBorder(inventory, filler);
        
        // Back button
        inventory.setItem(0, createGuiButton(Material.ARROW, "§7§lBack", 
            List.of("§7Return to Settings"), "back_settings"));
        
        // Compass settings
        boolean compassEnabled = plugin.getConfig().getBoolean("compass.enabled", true);
        int updateInterval = plugin.getConfig().getInt("compass.update_interval", 20);
        int trackingRange = plugin.getConfig().getInt("compass.tracking_range", 100);
        boolean showDistance = plugin.getConfig().getBoolean("compass.show_distance", true);
        
        ItemStack enableToggle = createGuiButton(
            compassEnabled ? Material.COMPASS : Material.BARRIER,
            "§b§lCompass Tracking: " + (compassEnabled ? "§aEnabled" : "§cDisabled"),
            List.of("§7Toggle compass tracking system"),
            "toggle_compass");
        inventory.setItem(11, enableToggle);
        
        ItemStack updateRate = createGuiButton(Material.REPEATER, "§b§lUpdate Interval",
            List.of("§7Current: §f" + updateInterval + " ticks", "§7Left/Right click: ±5"),
            "compass_update");
        inventory.setItem(13, updateRate);
        
        ItemStack range = createGuiButton(Material.SPYGLASS, "§b§lTracking Range",
            List.of("§7Current: §f" + trackingRange + " blocks", "§7Left/Right click: ±10"),
            "compass_range");
        inventory.setItem(15, range);
        
        ItemStack distanceToggle = createGuiButton(
            showDistance ? Material.MAP : Material.PAPER,
            "§b§lShow Distance: " + (showDistance ? "§aEnabled" : "§cDisabled"),
            List.of("§7Show distance to tracked player"),
            "compass_distance");
        inventory.setItem(17, distanceToggle);
        
        player.openInventory(inventory);
    }
    
    // Sudden Death Menu
    public void openSuddenDeathMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, Component.text("§4§lSudden Death Mode"));
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        fillBorder(inventory, filler);
        
        // Back button
        inventory.setItem(0, createGuiButton(Material.ARROW, "§7§lBack", 
            List.of("§7Return to Settings"), "back_settings"));
        
        // Sudden Death settings
        boolean suddenDeathEnabled = plugin.getConfig().getBoolean("sudden_death.enabled", false);
        int triggerTime = plugin.getConfig().getInt("sudden_death.trigger_time", 1800); // 30 minutes
        boolean noRegen = plugin.getConfig().getBoolean("sudden_death.no_regen", true);
        boolean oneHit = plugin.getConfig().getBoolean("sudden_death.one_hit_kill", false);
        boolean gameRunning = plugin.getGameManager().isGameRunning();
        boolean suddenDeathActive = plugin.getSuddenDeathManager().isActive();
        
        ItemStack enableToggle = createGuiButton(
            suddenDeathEnabled ? Material.WITHER_SKELETON_SKULL : Material.BARRIER,
            "§4§lSudden Death: " + (suddenDeathEnabled ? "§aEnabled" : "§cDisabled"),
            List.of("§7Activate after long games",
                    "§7Click to toggle on/off"),
            "toggle_sudden_death");
        inventory.setItem(11, enableToggle);
        
        ItemStack triggerTimeItem = createGuiButton(Material.CLOCK, "§4§lTrigger Time",
            List.of("§7Current: §f" + (triggerTime/60) + " minutes", 
                    "§7Left click: +5 minutes",
                    "§7Right click: -5 minutes"),
            "sudden_death_time");
        inventory.setItem(13, triggerTimeItem);
        
        ItemStack noRegenToggle = createGuiButton(
            noRegen ? Material.ROTTEN_FLESH : Material.GOLDEN_APPLE,
            "§4§lNo Regeneration: " + (noRegen ? "§aEnabled" : "§cDisabled"),
            List.of("§7Disable natural health regen",
                    "§7Click to toggle"),
            "sudden_death_no_regen");
        inventory.setItem(15, noRegenToggle);
        
        ItemStack oneHitToggle = createGuiButton(
            oneHit ? Material.NETHERITE_SWORD : Material.WOODEN_SWORD,
            "§4§lOne Hit Kill: " + (oneHit ? "§aEnabled" : "§cDisabled"),
            List.of("§7Any damage kills instantly",
                    "§7§cExtremely dangerous mode!",
                    "§7Click to toggle"),
            "sudden_death_one_hit");
        inventory.setItem(17, oneHitToggle);
        
        // Control buttons
        if (gameRunning && !suddenDeathActive) {
            ItemStack activateNow = createGuiButton(Material.TNT, "§c§lActivate Now",
                List.of("§7Force immediate sudden death",
                        "§7§cWarning: Will teleport all players!"),
                "activate_sudden_death_now");
            inventory.setItem(29, activateNow);
        }
        
        if (plugin.getSuddenDeathManager().isScheduled()) {
            ItemStack cancel = createGuiButton(Material.BARRIER, "§e§lCancel Scheduled",
                List.of("§7Cancel automatic sudden death"),
                "cancel_sudden_death");
            inventory.setItem(31, cancel);
        } else if (gameRunning) {
            ItemStack schedule = createGuiButton(Material.CLOCK, "§e§lSchedule Activation",
                List.of("§7Auto-activate after delay time"),
                "schedule_sudden_death");
            inventory.setItem(31, schedule);
        }
        
        player.openInventory(inventory);
    }
    
    public void openBroadcastSettingsMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, Component.text("§e§lBroadcast Settings"));
        ItemStack filler = createItem(Material.YELLOW_STAINED_GLASS_PANE, " ");
        fillBorder(inventory, filler);
        
        // Back button
        inventory.setItem(0, createGuiButton(Material.ARROW, "§7§lBack",
            List.of("§7Return to Advanced Settings"), "back_settings"));
        
        // Broadcast toggles
        boolean broadcastEnabled = plugin.getConfig().getBoolean("broadcasts.enabled", true);
        inventory.setItem(10, createGuiButton(
            broadcastEnabled ? Material.BELL : Material.GRAY_DYE,
            "§e§lBroadcast Events: " + (broadcastEnabled ? "§aON" : "§cOFF"),
            List.of("§7Toggle all broadcast messages"), "toggle_broadcasts"));
        
        boolean gameEvents = plugin.getConfig().getBoolean("broadcasts.game_events", true);
        inventory.setItem(12, createGuiButton(
            gameEvents ? Material.EMERALD : Material.GRAY_DYE,
            "§e§lGame Events: " + (gameEvents ? "§aON" : "§cOFF"),
            List.of("§7Broadcast start/stop/pause events"), "toggle_game_events"));
        
        boolean teamChanges = plugin.getConfig().getBoolean("broadcasts.team_changes", true);
        inventory.setItem(14, createGuiButton(
            teamChanges ? Material.PLAYER_HEAD : Material.GRAY_DYE,
            "§e§lTeam Changes: " + (teamChanges ? "§aON" : "§cOFF"),
            List.of("§7Broadcast team assignment changes"), "toggle_team_changes"));
        
        player.openInventory(inventory);
    }
    
    public void openLimboSettingsMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, Component.text("§6§lLimbo Configuration"));
        ItemStack filler = createItem(Material.PURPLE_STAINED_GLASS_PANE, " ");
        fillBorder(inventory, filler);
        
        // Back button
        inventory.setItem(0, createGuiButton(Material.ARROW, "§7§lBack",
            List.of("§7Return to Advanced Settings"), "back_settings"));
        
        // Current limbo settings
        String limboWorld = plugin.getConfig().getString("limbo.world", "world");
        double limboX = plugin.getConfig().getDouble("limbo.x", 0.5);
        double limboY = plugin.getConfig().getDouble("limbo.y", 200.0);
        double limboZ = plugin.getConfig().getDouble("limbo.z", 0.5);
        
        inventory.setItem(10, createGuiButton(Material.GRASS_BLOCK, "§e§lLimbo World",
            List.of("§7Current: §f" + limboWorld,
                    "§7Click to change world"), "limbo_world"));
        
        inventory.setItem(12, createGuiButton(Material.COMPASS, "§e§lLimbo Coordinates",
            List.of("§7X: §f" + limboX,
                    "§7Y: §f" + limboY,
                    "§7Z: §f" + limboZ,
                    "§7Click to edit coordinates"), "limbo_coords"));
        
        inventory.setItem(14, createGuiButton(Material.ENDER_PEARL, "§a§lSet to Current Location",
            List.of("§7Set limbo to your current position"), "limbo_set_current"));
        
        player.openInventory(inventory);
    }
    
    public void openUIPerformanceMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, Component.text("§d§lUI Performance Settings"));
        ItemStack filler = createItem(Material.MAGENTA_STAINED_GLASS_PANE, " ");
        fillBorder(inventory, filler);
        
        // Back button
        inventory.setItem(0, createGuiButton(Material.ARROW, "§7§lBack",
            List.of("§7Return to Advanced Settings"), "back_settings"));
        
        // Update frequency settings
        int actionbarTicks = plugin.getConfig().getInt("ui.update_ticks.actionbar", 20);
        int titleTicks = plugin.getConfig().getInt("ui.update_ticks.title", 10);
        int trackerTicks = plugin.getConfig().getInt("tracker.update_ticks", 20);
        
        inventory.setItem(10, createGuiButton(Material.CLOCK, "§e§lActionbar Update Rate",
            List.of("§7Current: §f" + actionbarTicks + " ticks (" + (actionbarTicks/20.0) + "s)",
                    "§7How often timer actionbars update",
                    "§7Left/Right: ±5 ticks"), "actionbar_rate"));
        
        inventory.setItem(12, createGuiButton(Material.EXPERIENCE_BOTTLE, "§e§lTitle Update Rate",
            List.of("§7Current: §f" + titleTicks + " ticks (" + (titleTicks/20.0) + "s)",
                    "§7How often waiting runner titles update",
                    "§7Left/Right: ±5 ticks"), "title_rate"));
        
        inventory.setItem(14, createGuiButton(Material.COMPASS, "§e§lTracker Update Rate",
            List.of("§7Current: §f" + trackerTicks + " ticks (" + (trackerTicks/20.0) + "s)",
                    "§7How often hunter compasses update",
                    "§7Left/Right: ±5 ticks"), "tracker_rate"));
        
        inventory.setItem(16, createGuiButton(Material.REDSTONE, "§c§lPerformance Tips",
            List.of("§7Lower values = more responsive UI",
                    "§7Higher values = better server performance",
                    "§7Recommended: 10-20 ticks"), "performance_info"));
        
        player.openInventory(inventory);
    }
}
