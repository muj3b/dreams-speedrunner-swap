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

    public void openMainMenu(Player player) {
        // Cache frequently accessed data for better performance
        boolean running = plugin.getGameManager().isGameRunning();
        List<Player> runners = plugin.getGameManager().getRunners();
        int runnerCount = runners != null ? runners.size() : 0;
        
        // Debug logging to help troubleshoot
        plugin.getLogger().info("Opening GUI for " + player.getName() + ", runners: " + runnerCount);
        
        // Smart adaptive sizing based on team size and features needed
        int baseRows = plugin.getConfigManager().getGuiMainMenuRows();
        
        // Adaptive row calculation for optimal UX
        int adaptiveRows = baseRows;
        if (running && runnerCount >= 4) {
            adaptiveRows = Math.max(baseRows, 4); // Larger for big teams
        } else if (running && runnerCount <= 2) {
            adaptiveRows = Math.max(baseRows, 3); // Compact for small teams
        }
        
        int size = Math.max(27, adaptiveRows * 9); // Minimum 3 rows
        String title = "§b§lSapnap: Multi-Runner Cooperation";
        
        // Add live indicator to title
        if (running) {
            boolean paused = plugin.getGameManager().isGamePaused();
            title += paused ? "§e §l[PAUSED]" : "§a §l[LIVE]";
        }

        Inventory inv = Bukkit.createInventory(new ControlGuiHolder(ControlGuiHolder.Type.MAIN), size, net.kyori.adventure.text.Component.text(title));

        // Smart filler with adaptive theming
        Material fillerMaterial = running ? 
            (plugin.getGameManager().isGamePaused() ? Material.YELLOW_STAINED_GLASS_PANE : Material.CYAN_STAINED_GLASS_PANE) :
            Material.GRAY_STAINED_GLASS_PANE;
        
        ItemStack filler = new ItemStack(fillerMaterial);
        ItemMeta fm = filler.getItemMeta();
        com.example.speedrunnerswap.utils.GuiCompat.setDisplayName(fm, " ");
        filler.setItemMeta(fm);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);

        // Get fresh state for GUI population
        boolean paused = plugin.getGameManager().isGamePaused();
        Player activeRunner = plugin.getGameManager().getActiveRunner();

        // Smart positioning system - adapts to GUI size
        int topRow = 0;
        int midRow = Math.max(1, size / 18); // Middle row
        int bottomRow = (size / 9) - 1; // Last row
        
        // Auto-refresh indicator (adaptive top-row positioning)
        if (running) {
            List<String> refreshLore = new ArrayList<>();
            refreshLore.add("§7§oLive updating every 5 seconds");
            refreshLore.add("§7§oClose and reopen to force refresh");
            refreshLore.add("");
            refreshLore.add("§a✓ §fReal-time coordination data");
            refreshLore.add("§a✓ §fLive performance metrics");
            refreshLore.add("§a✓ §fInstant team updates");
            inv.setItem((topRow * 9) + 8, named(Material.CLOCK, "§a§lLive Dashboard", refreshLore));
        } else {
            inv.setItem((topRow * 9) + 8, named(Material.GRAY_DYE, "§7Static View", List.of("§7Start game for live updates")));
        }
        
        // Back to Mode Selector (consistent top-left position)
        inv.setItem(topRow * 9, named(Material.ARROW, "§7§lBack", List.of("§7Return to mode selector")));

        // Quick Status Display
        List<String> statusLore = new ArrayList<>();
        statusLore.add("§7Runners: §b" + runnerCount);
        if (running) {
            statusLore.add("§7Active: §a" + (activeRunner != null ? activeRunner.getName() : "None"));
            statusLore.add("§7Next Swap: §e" + plugin.getGameManager().getTimeUntilNextSwap() + "s");
            statusLore.add("§7Status: " + (paused ? "§ePaused" : "§aRunning"));
        } else {
            statusLore.add("§7Status: §cNot Running");
        }
        // Adaptive status display (center position based on GUI size)
        inv.setItem((topRow * 9) + 4, named(Material.CLOCK, "§6§lGame Status", statusLore));
        
        // Smart control layout - adapts to available space and uses calculated rows
        int controlStart = (midRow * 9) + 1; // Start controls in middle row
        int controlSpacing = size >= 45 ? 2 : 1; // Better spacing in larger GUIs

        // Smart start/stop positioning
        if (!running) {
            List<String> startLore = new ArrayList<>();
            startLore.add("§7Begin cooperative swapping");
            if (runnerCount < 2) {
                startLore.add("§cNeed at least 2 runners!");
                inv.setItem(controlStart, named(Material.GRAY_WOOL, "Start Game", startLore));
            } else {
                startLore.add("§7Interval: §a" + plugin.getConfigManager().getSwapInterval() + "s");
                startLore.add("§7Ready with " + runnerCount + " runners");
                inv.setItem(controlStart, named(Material.LIME_WOOL, "§a§lStart Game", startLore));
            }
        } else {
            inv.setItem(controlStart, named(Material.RED_WOOL, "§c§lStop Game", List.of("§7End current cooperation session")));
        }

        // Smart pause/resume positioning
        if (running && !paused) {
            inv.setItem(controlStart + controlSpacing, named(Material.YELLOW_WOOL, "§e§lPause", List.of("§7Temporarily pause swapping", "§7All runners will be frozen")));
        } else if (running && paused) {
            inv.setItem(controlStart + controlSpacing, named(Material.ORANGE_WOOL, "§a§lResume", List.of("§7Resume cooperative swapping")));
        } else {
            inv.setItem(controlStart + controlSpacing, named(Material.GRAY_WOOL, "Pause", List.of("§7Game not running")));
        }

        // Enhanced shuffle with more info
        List<String> shuffleLore = new ArrayList<>();
        shuffleLore.add("§7Randomize runner queue order");
        if (running && activeRunner != null) {
            shuffleLore.add("§7" + activeRunner.getName() + " stays active");
        }
        
        // Always show runner management for Sapnap mode
        List<String> runnerLore = new ArrayList<>();
        runnerLore.add("§7Manage cooperation participants");
        runnerLore.add("§7Current: §b" + runnerCount + " runners");
        runnerLore.add("§7All online players can join!");
        
        // Adaptive shuffle positioning
        if (runnerCount < 2) {
            shuffleLore.add("§cNeed multiple runners");
            inv.setItem(controlStart + (controlSpacing * 2), named(Material.GRAY_DYE, "Shuffle Queue", shuffleLore));
        } else {
            inv.setItem(controlStart + (controlSpacing * 2), named(Material.NETHER_STAR, "§d§lShuffle Queue", shuffleLore));
        }

        // Adaptive runner management positioning
        inv.setItem(controlStart + (controlSpacing * 3), named(Material.PLAYER_HEAD, "§b§lManage Runners", runnerLore));

        // Enhanced randomize toggle with more info
        boolean randomize = plugin.getConfigManager().isSwapRandomized();
        List<String> randomizeLore = new ArrayList<>();
        randomizeLore.add("§7Current: " + (randomize ? "§aRandom timing" : "§bFixed timing"));
        if (randomize) {
            randomizeLore.add("§7Range: §e" + plugin.getConfigManager().getMinSwapInterval() + "-" + plugin.getConfigManager().getMaxSwapInterval() + "s");
            randomizeLore.add("§7Adds unpredictability!");
        } else {
            randomizeLore.add("§7Every §e" + plugin.getConfigManager().getSwapInterval() + "s §7exactly");
        }
        inv.setItem(22, named(Material.COMPARATOR, (randomize ? "§a" : "§b") + "§lTiming: " + (randomize ? "Random" : "Fixed"), randomizeLore));

        // Runner timer visibility (cycle FULL/LAST 10s/HIDDEN)
        String runnerVis = plugin.getConfigManager().getRunnerTimerVisibility();
        String runnerLabel = switch (runnerVis.toLowerCase()) {
            case "always" -> "FULL";
            case "never" -> "HIDDEN";
            default -> "LAST 10s";
        };
        inv.setItem(20, named(
                Material.CLOCK,
                "Runner Timer: " + runnerLabel,
                List.of("Cycle active runner timer visibility",
                        "FULL / LAST 10s / HIDDEN")));

        // Waiting timer visibility (cycle FULL/LAST 10s/HIDDEN)
        String waitingVis = plugin.getConfigManager().getWaitingTimerVisibility();
        String waitingLabel = switch (waitingVis.toLowerCase()) {
            case "last_10" -> "LAST 10s";
            case "never" -> "HIDDEN";
            default -> "FULL";
        };
        inv.setItem(21, named(
                Material.CLOCK,
                "Waiting Timer: " + waitingLabel,
                List.of("Cycle waiting runner timer visibility",
                        "FULL / LAST 10s / HIDDEN")));

        // Interval display and adjusters (±5s quick buttons). These support a beta mode to go below 30s
        int interval = plugin.getConfigManager().getSwapInterval();
        boolean isBeta = plugin.getConfigManager().isBetaIntervalEnabled();
        List<String> intervalLore = new java.util.ArrayList<>();
        intervalLore.add("§7Base swap interval");
        if (isBeta && interval < 30) {
            intervalLore.add("§cBETA: Running below 30s may be unstable");
            if (interval < 15) intervalLore.add("§cWarning: <15s may impact performance");
        }
        if (isBeta && interval > plugin.getConfigManager().getSwapIntervalMax()) {
            intervalLore.add("§cBETA: Running above configured maximum may be unstable");
        }
        inv.setItem(23, named(Material.PAPER, "Interval: " + interval + "s", intervalLore));
        inv.setItem(18, named(Material.ARROW, "-5s", List.of("Decrease interval (±5s)")));
        inv.setItem(26, named(Material.ARROW, "+5s", List.of("Increase interval (±5s)")));

        // Experimental intervals toggle (runner-only control GUI)
        boolean betaToggle = plugin.getConfigManager().isBetaIntervalEnabled();
        inv.setItem(25, named(
                betaToggle ? Material.REDSTONE_TORCH : Material.LEVER,
                "Experimental Intervals: " + (betaToggle ? "ON" : "OFF"),
                List.of("Allow <30s and >max intervals", "Shows red warnings in UI")
        ));

        // Enhanced freeze mode with better descriptions
        String freeze = plugin.getConfigManager().getFreezeMode();
        List<String> freezeLore = new ArrayList<>();
        freezeLore.add("§7How inactive runners are handled:");
        freezeLore.add("§f• §bEFFECTS§7: Blind/Dark/Slow effects");
        freezeLore.add("§f• §bSPECTATOR§7: Free-roam spectator mode");
        freezeLore.add("§f• §bLIMBO§7: Teleport to safe limbo area");
        freezeLore.add("§f• §bCAGE§7: Secure bedrock containment");
        freezeLore.add("");
        freezeLore.add("§7Currently: §a" + freeze);
        freezeLore.add("§7Click to cycle through options");
        Material freezeIcon = switch (freeze.toUpperCase()) {
            case "SPECTATOR" -> Material.ENDER_EYE;
            case "LIMBO" -> Material.ENDER_PEARL;
            case "CAGE" -> Material.BEDROCK;
            default -> Material.POTION; // EFFECTS
        };
        inv.setItem(19, named(freezeIcon, "§6§lInactive State: §a" + freeze, freezeLore));

        // Enhanced Safe Swap with more details
        boolean safeSwap = plugin.getConfigManager().isSafeSwapEnabled();
        List<String> safeLore = new ArrayList<>();
        safeLore.add("§7Prevents dangerous swap locations");
        if (safeSwap) {
            safeLore.add("§aProtection: §fActive");
            safeLore.add("§7Avoids: Lava, fire, cactus, void");
            safeLore.add("§7Scan radius: §e" + plugin.getConfigManager().getSafeSwapHorizontalRadius() + " blocks");
        } else {
            safeLore.add("§cProtection: §fDisabled");
            safeLore.add("§7⚠ Runners may spawn in danger!");
        }
        inv.setItem(6, named(safeSwap ? Material.SLIME_BLOCK : Material.MAGMA_BLOCK,
                (safeSwap ? "§a" : "§c") + "§lSafe Swaps: " + (safeSwap ? "ON" : "OFF"), safeLore));

        // Enhanced Single Player Sleep with cooperation context
        boolean singlePlayerSleep = plugin.getConfigManager().isSinglePlayerSleepEnabled();
        List<String> sleepLore = new ArrayList<>();
        sleepLore.add("§7Night-time cooperation mechanic");
        if (singlePlayerSleep) {
            sleepLore.add("§aOnly active runner can sleep");
            sleepLore.add("§7Perfect for caged teammates!");
        } else {
            sleepLore.add("§cAll players must sleep normally");
            sleepLore.add("§7May cause issues with frozen players");
        }
        sleepLore.add("§7Recommended: §aON §7for Sapnap mode");
        inv.setItem(8, named(singlePlayerSleep ? Material.WHITE_BED : Material.RED_BED,
                (singlePlayerSleep ? "§a" : "§c") + "§lSingle Sleep: " + (singlePlayerSleep ? "ON" : "OFF"), sleepLore));

        // Enhanced queue insights with upcoming order
        if (running && runnerCount >= 2 && runners != null && !runners.isEmpty()) {
            List<String> queueLore = new ArrayList<>();
            queueLore.add("§7Upcoming swap order:");
            
            // Get next 3 runners in queue - use the runners variable from method start
            int activeIndex = activeRunner != null && runners.contains(activeRunner) ? runners.indexOf(activeRunner) : -1;
            
            for (int i = 1; i <= Math.min(3, runnerCount); i++) {
                if (activeIndex == -1) break; // Safety check
                int nextIndex = (activeIndex + i) % runnerCount;
                if (runners != null && nextIndex >= 0 && nextIndex < runners.size() && runners.get(nextIndex) != null) {
                    Player nextPlayer = runners.get(nextIndex);
                    String pos = switch (i) {
                        case 1 -> "§a▶ Next: ";
                        case 2 -> "§e▶ Then: ";
                        default -> "§7▶ After: ";
                    };
                    queueLore.add(pos + "§f" + nextPlayer.getName());
                }
            }
            
            if (runnerCount > 3) {
                queueLore.add("§7... and " + (runnerCount - 3) + " more");
            }
            
            inv.setItem(5, named(Material.ENDER_PEARL, "§d§lQueue Preview", queueLore));
        } else {
            inv.setItem(5, named(Material.GRAY_DYE, "§7Queue Preview", List.of("§7Game not running or insufficient runners")));
        }
        
        // Real-time swap prediction with detailed timing
        if (running && !paused) {
            int timeLeft = plugin.getGameManager().getTimeUntilNextSwap();
            List<String> predictionLore = new ArrayList<>();
            
            if (timeLeft > 60) {
                predictionLore.add("§7Next swap in: §e" + (timeLeft / 60) + "m " + (timeLeft % 60) + "s");
            } else {
                predictionLore.add("§7Next swap in: §e" + timeLeft + "s");
            }
            
            // Color code urgency
            Material clockMaterial;
            String urgency;
            if (timeLeft <= 10) {
                clockMaterial = Material.REDSTONE;
                urgency = "§c§lIMMINENT!";
                predictionLore.add(urgency);
            } else if (timeLeft <= 30) {
                clockMaterial = Material.GOLD_INGOT;
                urgency = "§6Soon";
            } else {
                clockMaterial = Material.EMERALD;
                urgency = "§aLater";
            }
            
            if (plugin.getConfigManager().isSwapRandomized()) {
                predictionLore.add("§7Random timing active");
                predictionLore.add("§7Range: " + plugin.getConfigManager().getMinSwapInterval() + "-" + plugin.getConfigManager().getMaxSwapInterval() + "s");
            }
            
            inv.setItem(1, named(clockMaterial, "§6§lSwap Prediction", predictionLore));
        } else {
            inv.setItem(1, named(Material.BARRIER, "§7Swap Prediction", List.of("§7Game not running or paused")));
        }
        
        // Enhanced session statistics with achievements and smart monitoring
        if (running) {
            List<String> statsLore = new ArrayList<>();
            
            // Session achievements and milestones
            statsLore.add("§b§lLive Session Metrics:");
            statsLore.add("§7Current Session: §aActive");
            statsLore.add("§7Active Runner: §b" + (activeRunner != null ? activeRunner.getName() : "None"));
            statsLore.add("§7Runners Count: §e" + runnerCount);
            
            // Smart performance monitoring with alerts
            try {
                double tps = Bukkit.getTPS()[0];
                String tpsColor = tps >= 18.0 ? "§a" : tps >= 15.0 ? "§e" : "§c";
                statsLore.add("§7Server TPS: " + tpsColor + String.format("%.1f", tps));
                
                if (tps < 15.0) {
                    statsLore.add("§c⚠ Performance Alert: Reduce players!");
                } else if (tps >= 19.5) {
                    statsLore.add("§a✓ Excellent performance!");
                }
            } catch (Exception e) {
                statsLore.add("§7Performance: §7Monitoring unavailable");
            }
            
            // Enhanced memory monitoring with recommendations
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory() / 1024 / 1024;
            long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
            int memoryPercent = (int) ((usedMemory * 100) / maxMemory);
            String memColor = memoryPercent < 70 ? "§a" : memoryPercent < 85 ? "§e" : "§c";
            statsLore.add("§7Memory: " + memColor + memoryPercent + "% §7(" + usedMemory + "/" + maxMemory + "MB)");
            
            if (memoryPercent > 85) {
                statsLore.add("§c⚠ High memory usage detected!");
            }
            
            // Session achievements
            statsLore.add("");
            statsLore.add("§6§lSession Achievements:");
            
            // Calculate time-based achievements
            int timeLeft = plugin.getGameManager().getTimeUntilNextSwap();
            boolean isRandomized = plugin.getConfigManager().isSwapRandomized();
            
            if (runnerCount >= 4) {
                statsLore.add("§a✓ Multi-Team Coordination");
            }
            if (isRandomized) {
                statsLore.add("§a✓ Unpredictability Master");
            }
            if (timeLeft <= 30 && timeLeft > 0) {
                statsLore.add("§e⚡ Imminent Swap Alert");
            }
            
            // Add tip based on current state
            statsLore.add("");
            if (timeLeft <= 15) {
                statsLore.add("§e§lTip: §7Prepare for swap transition!");
            } else if (memoryPercent > 80) {
                statsLore.add("§c§lTip: §7Consider reducing view distance");
            } else {
                statsLore.add("§b§lTip: §7Communication is key to success!");
            }
            
            inv.setItem(2, named(Material.ENCHANTED_BOOK, "§b§lLive Analytics", statsLore));
        } else {
            List<String> offlineStats = new ArrayList<>();
            offlineStats.add("§7Session not active");
            offlineStats.add("§7Start a game to see:");
            offlineStats.add("§a• §fReal-time performance metrics");
            offlineStats.add("§a• §fAchievement tracking");
            offlineStats.add("§a• §fSmart recommendations");
            offlineStats.add("§a• §fLive coordination alerts");
            inv.setItem(2, named(Material.GRAY_DYE, "§7Live Analytics", offlineStats));
        }
        
        // Advanced coordination features with live updates
        List<String> coordLore = new ArrayList<>();
        coordLore.add("§7Advanced cooperation dashboard:");
        coordLore.add("§a• §fReal-time team communication");
        coordLore.add("§a• §fSmart inventory sharing alerts");
        coordLore.add("§a• §fCoordinated pause system");
        coordLore.add("§a• §fTeam waypoint management");
        coordLore.add("§a• §fPerformance monitoring");
        coordLore.add("");
        coordLore.add("§eClick for full coordination suite");
        inv.setItem(3, named(Material.COMPASS, "§e§lTeam Coordination", coordLore));
        
        // Quick Actions Toolbar - New Feature!
        if (running) {
            List<String> quickLore = new ArrayList<>();
            quickLore.add("§7Lightning-fast actions:");
            quickLore.add("§f➤ §aInstant location share");
            quickLore.add("§f➤ §cEmergency stop");
            quickLore.add("§f➤ §eQuick pause/resume");
            quickLore.add("§f➤ §bTeam status broadcast");
            quickLore.add("");
            quickLore.add("§7Right-click for instant actions!");
            inv.setItem(9, named(Material.GOLDEN_SWORD, "§6§lQuick Actions", quickLore));
        } else {
            inv.setItem(9, named(Material.GRAY_DYE, "§7Quick Actions", List.of("§7Available during active games")));
        }

        // Advanced settings with smart bottom-row positioning
        int advancedSlot = (bottomRow * 9) + 8; // Bottom-right corner
        inv.setItem(advancedSlot, named(Material.REDSTONE, "§6§lAdvanced Settings", 
                List.of("§7Access all configuration options", "§7Fine-tune your cooperation experience")));

        // Context-sensitive help system (adaptive positioning)
        List<String> helpLore = new ArrayList<>();
        helpLore.add("§6§lSmart Help System:");
        helpLore.add("");
        if (!running) {
            helpLore.add("§e➤ Start by selecting runners");
            helpLore.add("§e➤ Adjust swap interval if needed");
            helpLore.add("§e➤ Configure freeze mode for inactive players");
            helpLore.add("§e➤ Click 'Start Game' when ready");
        } else if (paused) {
            helpLore.add("§c➤ Game is paused - click Resume");
            helpLore.add("§c➤ Good time for strategy discussion");
            helpLore.add("§c➤ Check team coordination tools");
        } else {
            int timeLeft = plugin.getGameManager().getTimeUntilNextSwap();
            if (timeLeft <= 15) {
                helpLore.add("§c➤ Swap imminent! Prepare for transition");
                helpLore.add("§c➤ Finish current objectives quickly");
                helpLore.add("§c➤ Move to safe location if possible");
            } else {
                helpLore.add("§a➤ Game running smoothly");
                helpLore.add("§a➤ Use coordination tools for teamwork");
                helpLore.add("§a➤ Monitor performance metrics");
                helpLore.add("§a➤ Communicate with your team");
            }
        }
        helpLore.add("");
        helpLore.add("§7Click for detailed guidance");
        
        int helpSlot = (bottomRow * 9); // Bottom-left corner
        inv.setItem(helpSlot, named(Material.ENCHANTED_BOOK, "§e§lSmart Help", helpLore));
        
        // Performance warning indicator (adaptive positioning if needed)
        try {
            double tps = Bukkit.getTPS()[0];
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory() / 1024 / 1024;
            long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
            int memoryPercent = (int) ((usedMemory * 100) / maxMemory);
            
            if (running && (tps < 15.0 || memoryPercent > 85)) {
                List<String> warningLore = new ArrayList<>();
                warningLore.add("§c§lPerformance Warning!");
                warningLore.add("");
                if (tps < 15.0) {
                    warningLore.add("§c• Low TPS detected: " + String.format("%.1f", tps));
                    warningLore.add("§e• Consider reducing team size");
                }
                if (memoryPercent > 85) {
                    warningLore.add("§c• High memory usage: " + memoryPercent + "%");
                    warningLore.add("§e• Consider reducing view distance");
                }
                warningLore.add("");
                warningLore.add("§7Click for optimization tips");
                
                int warningSlot = (bottomRow * 9) + 1; // Bottom row, second position
                inv.setItem(warningSlot, named(Material.BARRIER, "§c§lPerformance Alert", warningLore));
            }
        } catch (Exception e) {
            // TPS not available, skip warning
        }

        // Reset interval to current mode default (replaces Status)
        int modeDefault = plugin.getConfigManager().getModeDefaultInterval(plugin.getCurrentMode());
        inv.setItem(24, named(Material.BARRIER, "Reset Interval", List.of("Reset to mode default: "+modeDefault+"s")));

        // Apply default on mode switch toggle
        boolean applyDefault = plugin.getConfigManager().getApplyDefaultOnModeSwitch();
        inv.setItem(17, named(applyDefault ? Material.NOTE_BLOCK : Material.GRAY_DYE,
                "Apply Mode Default on Switch: " + (applyDefault ? "Yes" : "No"),
                List.of("When switching modes, apply the default",
                        "interval if the game is not running")));

        // Save current as this mode's default
        inv.setItem(16, named(Material.WRITABLE_BOOK, "Save as Mode Default",
                List.of("Set current interval ("+interval+"s)", "as this mode's default")));

        player.openInventory(inv);
    }

    public void openRunnerSelector(Player player) {
        int rows = Math.max(2, plugin.getConfigManager().getGuiTeamSelectorRows());
        int size = rows * 9;
        String title = plugin.getConfigManager().getGuiTeamSelectorTitle();
        Inventory inv = Bukkit.createInventory(new ControlGuiHolder(ControlGuiHolder.Type.RUNNER_SELECTOR), size, net.kyori.adventure.text.Component.text(title));

        // Filler
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        com.example.speedrunnerswap.utils.GuiCompat.setDisplayName(fm, " ");
        filler.setItemMeta(fm);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);

        // Online players as selectable entries
        java.util.List<String> selected = plugin.getConfigManager().getRunnerNames();
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

        // Back to main
        inv.setItem(size - 8, named(Material.ARROW, "§7§lBack", List.of("§7Return to control menu")));

        // Save / Cancel buttons
        inv.setItem(size - 6, named(Material.EMERALD_BLOCK, "Save", List.of("Apply selected runners")));
        inv.setItem(size - 4, named(Material.BARRIER, "Cancel", List.of("Discard changes")));

        player.openInventory(inv);
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
    
    // Team Coordination Menu - New feature for enhanced cooperation
    public void openTeamCoordinationMenu(Player player) {
        Inventory inv = Bukkit.createInventory(new ControlGuiHolder(ControlGuiHolder.Type.COORDINATION), 45, net.kyori.adventure.text.Component.text("§e§lTeam Coordination Hub"));
        
        // Filler
        ItemStack filler = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        com.example.speedrunnerswap.utils.GuiCompat.setDisplayName(fm, " ");
        filler.setItemMeta(fm);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);
        
        // Back button
        inv.setItem(0, named(Material.ARROW, "§7§lBack", List.of("§7Return to main menu")));
        
        // Quick communication tools
        inv.setItem(10, named(Material.BELL, "§6§lBroadcast Location", 
            List.of("§7Share your current coordinates", "§7with all team members", "§7Click to announce position")));
        
        inv.setItem(11, named(Material.CHEST, "§6§lInventory Status", 
            List.of("§7Share key items in inventory", "§7Shows tools, food, resources", "§7Click to broadcast status")));
        
        inv.setItem(12, named(Material.REDSTONE_TORCH, "§c§lEmergency Pause", 
            List.of("§7Request immediate game pause", "§7Use for urgent situations", "§7Will notify all runners")));
        
        inv.setItem(13, named(Material.ENDER_EYE, "§d§lMark Waypoint", 
            List.of("§7Set a team waypoint here", "§7All runners will see direction", "§7Great for meeting points")));
        
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
        
        player.openInventory(inv);
    }
}
