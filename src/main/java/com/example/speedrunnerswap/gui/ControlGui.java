package com.example.speedrunnerswap.gui;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ControlGui {
    private final SpeedrunnerSwap plugin;

    public ControlGui(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
    }

    // Simple in-GUI statistics view to keep users inside Control GUI flow
    public void openStatsMenu(Player player) {
        int size = 45;
        String title = "§9§lStatistics";
        Inventory inv = com.example.speedrunnerswap.utils.GuiCompat.createInventory(new ControlGuiHolder(ControlGuiHolder.Type.STATS), size, title);

        // Filler
        ItemStack filler = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        com.example.speedrunnerswap.utils.GuiCompat.setDisplayName(fm, " ");
        filler.setItemMeta(fm);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);

        // Back
        inv.setItem(0, button(Material.ARROW, "back", "§7§lBack", List.of("§7Return to control menu")));

        // Basic stats pulled from GameManager
        boolean running = plugin.getGameManager().isGameRunning();
        List<String> lore = new ArrayList<>();
        lore.add("§7Running: " + (running ? "§aYes" : "§cNo"));
        if (running) {
            Player active = plugin.getGameManager().getActiveRunner();
            lore.add("§7Active: §e" + (active != null ? active.getName() : "None"));
            lore.add("§7Next swap: §e" + plugin.getGameManager().getTimeUntilNextSwap() + "s");
        }
        inv.setItem(22, named(Material.PAPER, "§b§lSession Overview", lore));

        plugin.getGuiManager().openInventorySoon(player, inv);
    }

    public void openMainMenu(Player player) {
        boolean running = plugin.getGameManager().isGameRunning();
        List<Player> runners = plugin.getGameManager().getRunners();
        int runnerCount = runners != null ? runners.size() : 0;
        boolean paused = plugin.getGameManager().isGamePaused();
        Player activeRunner = plugin.getGameManager().getActiveRunner();

        // Fixed, clean layout: 5 rows (45 slots)
        int rows = 5;
        int size = rows * 9;
        String title = "§b§lSapnap: Multi-Runner Cooperation" + (running ? (paused ? " §e§l[PAUSED]" : " §a§l[LIVE]") : "");

        Inventory inv = com.example.speedrunnerswap.utils.GuiCompat.createInventory(new ControlGuiHolder(ControlGuiHolder.Type.MAIN), size, title);

        // Border-only filler for a clean look
        ItemStack border = new ItemStack(Material.CYAN_STAINED_GLASS_PANE);
        ItemMeta bm = border.getItemMeta();
        com.example.speedrunnerswap.utils.GuiCompat.setDisplayName(bm, " ");
        border.setItemMeta(bm);
        fillBorder(inv, border);

        // Top row
        inv.setItem(0, button(Material.ARROW, "back", "§7§lBack", List.of("§7Return to mode selector")));
        inv.setItem(8, button(Material.BOOK, "about", "§b§lAbout", List.of("§7Info + donate link")));
        List<String> statusLore = new ArrayList<>();
        statusLore.add("§7Runners: §b" + runnerCount);
        statusLore.add("§7Status: " + (running ? (paused ? "§ePaused" : "§aRunning") : "§cNot Running"));
        if (running) {
            statusLore.add("§7Active: §a" + (activeRunner != null ? activeRunner.getName() : "None"));
            statusLore.add("§7Next Swap: §e" + plugin.getGameManager().getTimeUntilNextSwap() + "s");
        }
        inv.setItem(4, named(Material.CLOCK, "§6§lGame Status", statusLore));

        // Row 2: Game control cluster
        if (!running) {
            List<String> startLore = new ArrayList<>();
            startLore.add("§7Begin cooperative swapping");
            if (runnerCount < 2) {
                startLore.add("§cNeed at least 2 runners!");
                inv.setItem(10, button(Material.GRAY_WOOL, "start", "Start Game", startLore));
            } else {
                startLore.add("§7Interval: §a" + plugin.getConfigManager().getSwapInterval() + "s");
                inv.setItem(10, button(Material.LIME_WOOL, "start", "§a§lStart Game", startLore));
            }
        } else {
            inv.setItem(10, button(Material.RED_WOOL, "stop", "§c§lStop Game", List.of("§7End current session")));
        }
        if (running && !paused) {
            inv.setItem(12, button(Material.YELLOW_WOOL, "pause", "§e§lPause", List.of("§7Temporarily pause swapping")));
        } else if (running && paused) {
            inv.setItem(12, button(Material.ORANGE_WOOL, "resume", "§a§lResume", List.of("§7Resume cooperative swapping")));
        } else {
            inv.setItem(12, button(Material.GRAY_WOOL, "pause", "Pause", List.of("§7Game not running")));
        }
        inv.setItem(14, button(Material.NETHER_STAR, "shuffle", "§d§lShuffle Queue", List.of("§7Randomize runner order")));
        inv.setItem(16, button(Material.PLAYER_HEAD, "runner_selector", "§b§lManage Runners", List.of("§7Select/deselect participants")));

        // Row 3: Interval and timing
        boolean randomize = plugin.getConfigManager().isSwapRandomized();
        List<String> randomizeLore = new ArrayList<>();
        randomizeLore.add("§7Current: " + (randomize ? "§aRandom" : "§bFixed"));
        if (randomize) randomizeLore.add("§7Range: §e" + plugin.getConfigManager().getMinSwapInterval() + "-" + plugin.getConfigManager().getMaxSwapInterval() + "s");
        inv.setItem(19, named(Material.COMPARATOR, (randomize ? "§a" : "§b") + "§lTiming: " + (randomize ? "Random" : "Fixed"), randomizeLore));

        int interval = plugin.getConfigManager().getSwapInterval();
        List<String> intervalLore = new ArrayList<>();
        intervalLore.add("§7Base swap interval");
        inv.setItem(21, named(Material.PAPER, "Interval: " + interval + "s", intervalLore));
        inv.setItem(20, named(Material.ARROW, "-5s", List.of("§7Decrease interval")));
        inv.setItem(22, named(Material.ARROW, "+5s", List.of("§7Increase interval")));

        boolean betaToggle = plugin.getConfigManager().isBetaIntervalEnabled();
        inv.setItem(23, named(betaToggle ? Material.REDSTONE_TORCH : Material.LEVER, "Experimental Intervals: " + (betaToggle ? "ON" : "OFF"), List.of("§7Allow <30s and >max intervals")));

        // Task Manager summary (only shown in Task mode)
        if (plugin.getCurrentMode() == SpeedrunnerSwap.SwapMode.TASK) {
            com.example.speedrunnerswap.task.TaskManagerMode tmm = plugin.getTaskManagerMode();
            com.example.speedrunnerswap.task.TaskDifficulty diff = tmm != null ? tmm.getDifficultyFilter() : com.example.speedrunnerswap.task.TaskDifficulty.MEDIUM;
            int enabledCount = (tmm != null) ? tmm.getCandidateCount() : 0;
            List<String> taskLore = new ArrayList<>();
            taskLore.add("§7Difficulty: §e" + diff.name());
            taskLore.add("§7Enabled tasks: §a" + enabledCount);
            taskLore.add("§7Click for task commands help");
            inv.setItem(24, button(Material.TARGET, "task_info", "§6§lTask Manager", taskLore));
            inv.setItem(25, button(Material.ARROW, "task_diff_up", "§a§lDifficulty ▲", List.of("§7Increase difficulty")));
            inv.setItem(26, button(Material.ARROW, "task_diff_down", "§c§lDifficulty ▼", List.of("§7Decrease difficulty")));
        }

        // Row 4: Safety and mode
        String freeze = plugin.getConfigManager().getFreezeMode();
        Material freezeIcon = switch (freeze.toUpperCase()) {
            case "SPECTATOR" -> Material.ENDER_EYE;
            case "LIMBO" -> Material.ENDER_PEARL;
            case "CAGE" -> Material.BEDROCK;
            default -> Material.POTION;
        };
        inv.setItem(28, named(freezeIcon, "§6§lInactive State: §a" + freeze, List.of("§7Cycle inactive-runner behavior")));

        boolean safeSwap = plugin.getConfigManager().isSafeSwapEnabled();
        inv.setItem(29, named(safeSwap ? Material.SLIME_BLOCK : Material.MAGMA_BLOCK, (safeSwap ? "§a" : "§c") + "§lSafe Swaps: " + (safeSwap ? "ON" : "OFF"), List.of("§7Prevent dangerous swap locations")));

        boolean singlePlayerSleep = plugin.getConfigManager().isSinglePlayerSleepEnabled();
        inv.setItem(30, named(singlePlayerSleep ? Material.WHITE_BED : Material.RED_BED, (singlePlayerSleep ? "§a" : "§c") + "§lSingle Sleep: " + (singlePlayerSleep ? "ON" : "OFF"), List.of("§7Only active runner can sleep")));

        boolean applyDefault = plugin.getConfigManager().getApplyDefaultOnModeSwitch();
        inv.setItem(31, named(applyDefault ? Material.NOTE_BLOCK : Material.GRAY_DYE, "Apply Mode Default on Switch: " + (applyDefault ? "Yes" : "No"), List.of("§7Apply mode default interval on switch")));

        inv.setItem(32, named(Material.WRITABLE_BOOK, "Save as Mode Default", List.of("§7Set current interval as default")));

        // Row 5: Utilities
        inv.setItem(34, button(Material.SPYGLASS, "statistics", "§9§lStatistics", List.of("§7View session stats")));

        plugin.getGuiManager().openInventorySoon(player, inv);
    }

    // Border filler similar to GuiManager
    private void fillBorder(Inventory inv, ItemStack filler) {
        int size = inv.getSize();
        int cols = 9;
        int rows = size / cols;
        for (int c = 0; c < cols; c++) {
            inv.setItem(c, filler); // top
            inv.setItem((rows - 1) * cols + c, filler); // bottom
        }
        for (int r = 1; r < rows - 1; r++) {
            inv.setItem(r * cols, filler); // left
            inv.setItem(r * cols + (cols - 1), filler); // right
        }
    }

    public void openRunnerSelector(Player player) {
        int rows = Math.max(2, plugin.getConfigManager().getGuiTeamSelectorRows());
        int size = rows * 9;
        String title = plugin.getConfigManager().getGuiTeamSelectorTitle();
        Inventory inv = com.example.speedrunnerswap.utils.GuiCompat.createInventory(new ControlGuiHolder(ControlGuiHolder.Type.RUNNER_SELECTOR), size, title);

        // Filler
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        com.example.speedrunnerswap.utils.GuiCompat.setDisplayName(fm, " ");
        filler.setItemMeta(fm);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);

        // Online players as selectable entries; prefer pending selections if present
        java.util.Set<String> pending = com.example.speedrunnerswap.gui.ControlGuiListener.getPendingSelection(player.getUniqueId());
        java.util.List<String> selected = pending != null ? new java.util.ArrayList<>(pending) : plugin.getConfigManager().getRunnerNames();
        int idx = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            ItemStack icon = new ItemStack(Material.PLAYER_HEAD);
            try {
                org.bukkit.inventory.meta.SkullMeta sm = (org.bukkit.inventory.meta.SkullMeta) icon.getItemMeta();
                sm.setOwningPlayer(p);
                com.example.speedrunnerswap.utils.GuiCompat.setDisplayName(sm, p.getName());
                boolean isSel = selected.contains(p.getName());
                java.util.List<String> legacy = new java.util.ArrayList<>();
                legacy.add(isSel ? "Selected: Yes" : "Selected: No");
                com.example.speedrunnerswap.utils.GuiCompat.setLore(sm, legacy);
                icon.setItemMeta(sm);
            } catch (Throwable t) {
                ItemMeta im = icon.getItemMeta();
                com.example.speedrunnerswap.utils.GuiCompat.setDisplayName(im, p.getName());
                icon.setItemMeta(im);
            }
            if (idx < size - 9) {
                inv.setItem(idx, icon);
            }
            idx++;
        }

        // Back to main (PDC-tagged)
        inv.setItem(size - 8, button(Material.ARROW, "back", "§7§lBack", List.of("§7Return to control menu")));

        // Save / Cancel buttons (PDC-tagged)
        inv.setItem(size - 6, button(Material.EMERALD_BLOCK, "save_selection", "Save", List.of("Apply selected runners")));
        inv.setItem(size - 4, button(Material.BARRIER, "cancel_selection", "Cancel", List.of("Discard changes")));

        plugin.getGuiManager().openInventorySoon(player, inv);
    }

    private ItemStack named(Material mat, String name, List<String> loreText) {
        ItemStack it = new ItemStack(mat);
        ItemMeta im = it.getItemMeta();
        com.example.speedrunnerswap.utils.GuiCompat.setDisplayName(im, name);
        if (loreText != null && !loreText.isEmpty()) {
            com.example.speedrunnerswap.utils.GuiCompat.setLore(im, loreText);
        }
        it.setItemMeta(im);
        return it;
    }
    
    // Helper to tag buttons with a compact persistent ID used by ControlGuiListener
    private ItemStack button(Material mat, String id, String display, java.util.List<String> lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta im = it.getItemMeta();
        com.example.speedrunnerswap.utils.GuiCompat.setDisplayName(im, display);
        if (lore != null && !lore.isEmpty()) com.example.speedrunnerswap.utils.GuiCompat.setLore(im, lore);
        try {
            org.bukkit.persistence.PersistentDataContainer pdc = im.getPersistentDataContainer();
            pdc.set(new org.bukkit.NamespacedKey(plugin, "btn"), org.bukkit.persistence.PersistentDataType.STRING, id);
        } catch (Throwable ignored) {}
        it.setItemMeta(im);
        return it;
    }
    
    
    // Team Coordination Menu - New feature for enhanced cooperation
    public void openTeamCoordinationMenu(Player player) {
        Inventory inv = com.example.speedrunnerswap.utils.GuiCompat.createInventory(new ControlGuiHolder(ControlGuiHolder.Type.COORDINATION), 45, "§e§lTeam Coordination Hub");
        
        // Filler
        ItemStack filler = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        com.example.speedrunnerswap.utils.GuiCompat.setDisplayName(fm, " ");
        filler.setItemMeta(fm);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);
        
        // Back button
        inv.setItem(0, button(Material.ARROW, "back", "§7§lBack", List.of("§7Return to main menu")));
        
        // Quick communication tools
        inv.setItem(10, button(Material.BELL, "coord_broadcast", "§6§lBroadcast Location",
            List.of("§7Share your current coordinates", "§7with all team members")));

        inv.setItem(11, button(Material.CHEST, "coord_inventory", "§6§lInventory Status",
            List.of("§7Share key inventory highlights")));

        inv.setItem(12, button(Material.REDSTONE_TORCH, "coord_emergency_pause", "§c§lEmergency Pause",
            List.of("§7Request immediate game pause")));

        inv.setItem(13, button(Material.ENDER_EYE, "coord_waypoint", "§d§lMark Waypoint",
            List.of("§7Set a temporary team waypoint")));
        
        // Team status overview
        boolean gameRunning = plugin.getGameManager().isGameRunning();
        if (gameRunning) {
            List<Player> runners = plugin.getGameManager().getRunners();
            Player activeRunner = plugin.getGameManager().getActiveRunner();
            
            inv.setItem(19, named(Material.PLAYER_HEAD, "§b§lTeam Overview", 
                List.of("§7Active: §a" + (activeRunner != null ? activeRunner.getName() : "None"),
                        "§7Total Runners: §e" + runners.size(),
                        "§7Next swap in: §e" + plugin.getGameManager().getTimeUntilNextSwap() + "s")));
        } else {
            inv.setItem(19, named(Material.BARRIER, "§7Team Overview", List.of("§7Game not currently running")));
        }
        
        // Cooperation tips
        List<String> tipsLore = new ArrayList<>();
        tipsLore.add("§7Enhanced cooperation tips:");
        tipsLore.add("§a• §fUse F3+F to adjust view distance");
        tipsLore.add("§a• §fPress Tab to see all player positions");
        tipsLore.add("§a• §fLeave signs with important info");
        tipsLore.add("§a• §fKeep shared storage accessible");
        tipsLore.add("§a• §fUse compass to stay oriented");
        tipsLore.add("§a• §fCommunicate upcoming plans");
        inv.setItem(25, named(Material.BOOK, "§e§lAdvanced Tips", tipsLore));
        
        // Voice chat integration with smart suggestions
        List<String> voiceChatLore = new ArrayList<>();
        voiceChatLore.add("§7Smart voice communication setup:");
        voiceChatLore.add("§a• §fDiscord: Best for team coordination");
        voiceChatLore.add("§a• §fTeamSpeak: Professional alternative");
        voiceChatLore.add("§a• §fMumble: Low-latency option");
        voiceChatLore.add("");
        voiceChatLore.add("§6Active Runner Protocol:");
        voiceChatLore.add("§e1. §7Narrate all major actions");
        voiceChatLore.add("§e2. §7Call out incoming threats");
        voiceChatLore.add("§e3. §7Share resource locations");
        voiceChatLore.add("§e4. §7Announce swap preparation");
        voiceChatLore.add("");
        voiceChatLore.add("§bWaiting Runners:");
        voiceChatLore.add("§b• §7Plan ahead strategically");
        voiceChatLore.add("§b• §7Prepare resources for handoff");
        voiceChatLore.add("§b• §7Watch for coordination opportunities");
        inv.setItem(28, named(Material.NOTE_BLOCK, "§d§lSmart Voice Chat", voiceChatLore));
        
        // Advanced team analysis
        if (gameRunning) {
            List<Player> runners = plugin.getGameManager().getRunners();
            Player activeRunner = plugin.getGameManager().getActiveRunner();
            
            List<String> analysisLore = new ArrayList<>();
            analysisLore.add("§6§lTeam Performance Analysis:");
            analysisLore.add("§7Active: §a" + (activeRunner != null ? activeRunner.getName() : "None"));
            analysisLore.add("§7Total Runners: §e" + runners.size());
            analysisLore.add("§7Next swap: §e" + plugin.getGameManager().getTimeUntilNextSwap() + "s");
            
            // Smart recommendations based on team size
            analysisLore.add("");
            analysisLore.add("§b§lSmart Recommendations:");
            if (runners.size() <= 2) {
                analysisLore.add("§e• Small team: Focus on efficiency");
                analysisLore.add("§e• Longer intervals recommended");
            } else if (runners.size() >= 5) {
                analysisLore.add("§c• Large team: Monitor performance");
                analysisLore.add("§c• Consider shorter intervals");
                analysisLore.add("§c• Watch for coordination delays");
            } else {
                analysisLore.add("§a• Optimal team size detected");
                analysisLore.add("§a• Perfect for balanced cooperation");
            }
            
            // Performance insights
            int timeLeft = plugin.getGameManager().getTimeUntilNextSwap();
            if (timeLeft <= 10) {
                analysisLore.add("§c• §lSwap imminent - prepare for transition!");
            } else if (timeLeft >= 60) {
                analysisLore.add("§b• Good time for strategic planning");
            }
            
            inv.setItem(34, named(Material.SPYGLASS, "§6§lTeam Analytics", analysisLore));
        } else {
            inv.setItem(34, named(Material.BARRIER, "§7Team Analytics", List.of("§7Available during active games")));
        }
        
        // Emergency protocols
        List<String> emergencyLore = new ArrayList<>();
        emergencyLore.add("§c§lEmergency Procedures:");
        emergencyLore.add("§7Quick response protocols:");
        emergencyLore.add("");
        emergencyLore.add("§c1. Immediate Danger:");
        emergencyLore.add("§7   • Use emergency pause button");
        emergencyLore.add("§7   • Call out in voice chat");
        emergencyLore.add("§7   • Broadcast location if trapped");
        emergencyLore.add("");
        emergencyLore.add("§e2. Resource Emergency:");
        emergencyLore.add("§7   • Share inventory status");
        emergencyLore.add("§7   • Mark waypoint to resources");
        emergencyLore.add("§7   • Coordinate supply drop");
        emergencyLore.add("");
        emergencyLore.add("§b3. Communication Loss:");
        emergencyLore.add("§7   • Use in-game chat");
        emergencyLore.add("§7   • Leave signs at key locations");
        emergencyLore.add("§7   • Follow predetermined protocols");
        inv.setItem(16, named(Material.REDSTONE_TORCH, "§c§lEmergency Protocols", emergencyLore));
        plugin.getGuiManager().openInventorySoon(player, inv);
    }
}
