package com.example.speedrunnerswap.gui;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import com.example.speedrunnerswap.utils.BukkitCompat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ControlGuiListener implements Listener {
    private final SpeedrunnerSwap plugin;

    // Temporary selections per player for runner selector GUI (static so ControlGui can reflect live selection)
    private static final Map<java.util.UUID, Set<String>> pendingRunnerSelections = new HashMap<>();

    public static Set<String> getPendingSelection(java.util.UUID uuid) {
        return pendingRunnerSelections.get(uuid);
    }
    
    // About screen interactions: Back and donation link
    private void handleAboutClick(Player player, ItemStack clicked) {
        if (clicked == null || !clicked.hasItemMeta()) return;
        org.bukkit.Material type = clicked.getType();
        String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
        if (type == org.bukkit.Material.ARROW || (name != null && name.contains("Back"))) {
            new ControlGui(plugin).openMainMenu(player);
            return;
        }
        if (type == org.bukkit.Material.PLAYER_HEAD && name != null && name.contains("Creator")) {
            String url = plugin.getConfig().getString("donation.url", "https://donate.stripe.com/8x29AT0H58K03judnR0Ba01");
            player.sendMessage("§6Support the creator: §b" + url);
            player.sendMessage("§7Copy or click the link above in chat.");
        }
    }

    public static void setPendingSelection(java.util.UUID uuid, Set<String> sel) {
        if (sel == null) pendingRunnerSelections.remove(uuid); else pendingRunnerSelections.put(uuid, sel);
    }

    public ControlGuiListener(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
    }
    
    // Helper method to get button ID from persistent data or by robust name fallback
    private String getButtonId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        org.bukkit.inventory.meta.ItemMeta im = item.getItemMeta();
        try {
            org.bukkit.persistence.PersistentDataContainer pdc = im.getPersistentDataContainer();
            // Prefer new compact key "btn"
            String id = pdc.get(new org.bukkit.NamespacedKey(plugin, "btn"), org.bukkit.persistence.PersistentDataType.STRING);
            if (id != null && !id.isEmpty()) return id;
            // Back-compat: read legacy key used elsewhere
            id = pdc.get(new org.bukkit.NamespacedKey(plugin, "ssw_button_id"), org.bukkit.persistence.PersistentDataType.STRING);
            if (id != null && !id.isEmpty()) return id;
        } catch (Throwable ignored) {}

        // Fallback: infer from display name
        String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(im);
        if (name == null) return null;
        String plain = com.example.speedrunnerswap.utils.TextUtil.stripColors(name).toLowerCase(java.util.Locale.ROOT);
        if (plain.contains("back")) return "back";
        if (plain.contains("start")) return "start";
        if (plain.contains("stop")) return "stop";
        if (plain.contains("pause")) return "pause";
        if (plain.contains("resume")) return "resume";
        if (plain.contains("about")) return "about";
        if (plain.contains("runner") && plain.contains("select")) return "runner_selector";
        if (plain.contains("task") && plain.contains("info")) return "task_info";
        if (plain.contains("difficulty") && (plain.contains("up") || plain.contains("▲"))) return "task_diff_up";
        if (plain.contains("difficulty") && (plain.contains("down") || plain.contains("▼"))) return "task_diff_down";
        return null;
    }

    private boolean isMain(Inventory inv) {
        return inv != null && inv.getHolder() instanceof ControlGuiHolder holder && holder.getType() == ControlGuiHolder.Type.MAIN;
    }

    private boolean isRunnerSelector(Inventory inv) {
        return inv != null && inv.getHolder() instanceof ControlGuiHolder holder && holder.getType() == ControlGuiHolder.Type.RUNNER_SELECTOR;
    }
    
    private boolean isCoordination(Inventory inv) {
        return inv != null && inv.getHolder() instanceof ControlGuiHolder holder && holder.getType() == ControlGuiHolder.Type.COORDINATION;
    }

    private boolean isAbout(Inventory inv) {
        return inv != null && inv.getHolder() instanceof ControlGuiHolder holder && holder.getType() == ControlGuiHolder.Type.ABOUT;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory top = event.getView().getTopInventory();
        if (top == null) return;
        
        // Check if this is one of our GUI inventories
        if (!isMain(top) && !isRunnerSelector(top) && !isCoordination(top) && !isAbout(top)) return;
        
        // ALWAYS cancel the event to prevent item movement
        event.setCancelled(true);
        
        // Debug logging (reduced verbosity)
        plugin.getLogger().fine("GUI Click detected - Slot: " + event.getSlot() + ", RawSlot: " + event.getRawSlot());
        
        // Prevent any click in player inventory when GUI is open
        if (event.getRawSlot() >= top.getSize()) {
            // Click in player inventory - block it completely
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        
        plugin.getLogger().fine("Processing click on item: " + clicked.getType());

        if (isMain(top)) {
            handleMainClick(player, clicked);
        } else if (isRunnerSelector(top)) {
            handleRunnerSelectorClick(player, clicked, event.getRawSlot(), top.getSize());
        } else if (isCoordination(top)) {
            handleCoordinationClick(player, clicked);
        } else if (isAbout(top)) {
            handleAboutClick(player, clicked);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Inventory top = event.getView().getTopInventory();
        if (top == null) return;
        
        // Check if this is one of our GUI inventories
        if (!isMain(top) && !isRunnerSelector(top) && !isCoordination(top)) return;
        
        // ALWAYS cancel drag events in our GUIs
        event.setCancelled(true);
        plugin.getLogger().fine("Blocked drag attempt in GUI");
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryMoveItem(org.bukkit.event.inventory.InventoryMoveItemEvent event) {
        Inventory source = event.getSource();
        Inventory destination = event.getDestination();
        
        // Block any item movement from/to our GUIs
        if ((source != null && (isMain(source) || isRunnerSelector(source) || isCoordination(source) || isAbout(source))) ||
            (destination != null && (isMain(destination) || isRunnerSelector(destination) || isCoordination(destination) || isAbout(destination)))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        Inventory top = event.getView().getTopInventory();
        if (top == null) return;
        if (isRunnerSelector(top)) {
            // Drop any temporary selection when closing runner selector without saving
            pendingRunnerSelections.remove(player.getUniqueId());
        }
    }

    private void handleMainClick(Player player, ItemStack clicked) {
        Material type = clicked.getType();
        boolean running = plugin.getGameManager().isGameRunning();
        
        // Debug logging
        String itemName = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
        plugin.getLogger().fine("Main GUI click - Material: " + type + ", Display Name: " + itemName);
        
        // Check for button ID first (more reliable than material type)
        String buttonId = getButtonId(clicked);
        if (buttonId != null) {
            plugin.getLogger().fine("Found button ID: " + buttonId);
            switch (buttonId) {
                case "back" -> { new ControlGui(plugin).openMainMenu(player); return; }
                case "start" -> {
                    if (!running) {
                        plugin.setCurrentMode(SpeedrunnerSwap.SwapMode.SAPNAP);
                        List<String> names = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
                        plugin.getConfigManager().setRunnerNames(names);
                        plugin.getGameManager().setRunners(new ArrayList<>(Bukkit.getOnlinePlayers()));
                        plugin.getGameManager().startGame();
                        player.sendMessage("§aGame started!");
                    } else {
                        player.sendMessage("§cGame is already running!");
                    }
                    new ControlGui(plugin).openMainMenu(player);
                    return;
                }
                case "stop" -> {
                    if (running) {
                        plugin.getGameManager().stopGame();
                        player.sendMessage("§cGame stopped!");
                    } else {
                        player.sendMessage("§cGame is not running!");
                    }
                    new ControlGui(plugin).openMainMenu(player);
                    return;
                }
                case "pause" -> {
                    if (running && !plugin.getGameManager().isGamePaused()) {
                        plugin.getGameManager().pauseGame();
                        player.sendMessage("§eGame paused!");
                    } else {
                        player.sendMessage("§cCannot pause - game not running or already paused!");
                    }
                    new ControlGui(plugin).openMainMenu(player);
                    return;
                }
                case "resume" -> {
                    if (running && plugin.getGameManager().isGamePaused()) {
                        plugin.getGameManager().resumeGame();
                        player.sendMessage("§aGame resumed!");
                    } else {
                        player.sendMessage("§cCannot resume - game not running or not paused!");
                    }
                    new ControlGui(plugin).openMainMenu(player);
                    return;
                }
                case "about" -> {
                    try {
                        new com.example.speedrunnerswap.gui.AboutGui().openFor(player);
                    } catch (Throwable t) {
                        player.sendMessage("§cAbout screen failed to open.");
                    }
                    return;
                }
                case "task_info" -> {
                    player.sendMessage("§6[Task Manager] Use these commands:");
                    player.sendMessage("§e/swap tasks list §7- show tasks with difficulty + enabled");
                    player.sendMessage("§e/swap tasks enable <ID> §7- enable a task");
                    player.sendMessage("§e/swap tasks disable <ID> §7- disable a task");
                    player.sendMessage("§e/swap tasks difficulty <easy|medium|hard> §7- set difficulty pool");
                    player.sendMessage("§e/swap tasks reload §7- reload tasks.yml");
                    return;
                }
                case "task_diff_up" -> {
                    try {
                        var tmm = plugin.getTaskManagerMode();
                        if (tmm != null) {
                            var next = tmm.getDifficultyFilter().next();
                            tmm.setDifficultyFilter(next);
                            player.sendMessage("§eDifficulty set to §a" + next.name());
                        }
                    } catch (Throwable ignored) {}
                    new ControlGui(plugin).openMainMenu(player);
                    return;
                }
                case "task_diff_down" -> {
                    try {
                        var tmm = plugin.getTaskManagerMode();
                        if (tmm != null) {
                            var prev = tmm.getDifficultyFilter().prev();
                            tmm.setDifficultyFilter(prev);
                            player.sendMessage("§eDifficulty set to §a" + prev.name());
                        }
                    } catch (Throwable ignored) {}
                    new ControlGui(plugin).openMainMenu(player);
                    return;
                }
                case "shuffle" -> {
                    if (plugin.getGameManager().shuffleQueue()) {
                        player.sendMessage("§aShuffled runner queue successfully.");
                    } else {
                        player.sendMessage("§cCannot shuffle queue - need at least 2 runners.");
                    }
                    new ControlGui(plugin).openMainMenu(player);
                    return;
                }
                case "manage_runners", "runner_selector" -> {
                    plugin.getLogger().info("Processing manage_runners button");
                    Set<String> initial = new HashSet<>(plugin.getConfigManager().getRunnerNames());
                    setPendingSelection(player.getUniqueId(), initial);
                    new ControlGui(plugin).openRunnerSelector(player);
                    return;
                }
            }
        }

        if (type == Material.LIME_WOOL) {
            plugin.getLogger().fine("Processing LIME_WOOL (Start Game) click");
            if (!running) {
                // Ensure mode is runner-only
                plugin.setCurrentMode(SpeedrunnerSwap.SwapMode.SAPNAP);
                // In Sapnap mode, always set all online players as runners
                List<String> names = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
                plugin.getConfigManager().setRunnerNames(names);
                plugin.getGameManager().setRunners(new ArrayList<>(Bukkit.getOnlinePlayers()));
                plugin.getGameManager().startGame();
                player.sendMessage("§aGame started!");
            } else {
                player.sendMessage("§cGame is already running!");
            }
            new ControlGui(plugin).openMainMenu(player);
            return;
        }

        if (type == Material.RED_WOOL) {
            plugin.getLogger().fine("Processing RED_WOOL (Stop Game) click");
            if (running) {
                plugin.getGameManager().stopGame();
                player.sendMessage("§cGame stopped!");
            } else {
                player.sendMessage("§cGame is not running!");
            }
            new ControlGui(plugin).openMainMenu(player);
            return;
        }

        if (type == Material.YELLOW_WOOL) {
            plugin.getLogger().fine("Processing YELLOW_WOOL (Pause Game) click");
            if (running && !plugin.getGameManager().isGamePaused()) {
                plugin.getGameManager().pauseGame();
                player.sendMessage("§eGame paused!");
            } else {
                player.sendMessage("§cCannot pause - game not running or already paused!");
            }
            new ControlGui(plugin).openMainMenu(player);
            return;
        }

        if (type == Material.ORANGE_WOOL) {
            plugin.getLogger().fine("Processing ORANGE_WOOL (Resume Game) click");
            if (running && plugin.getGameManager().isGamePaused()) {
                plugin.getGameManager().resumeGame();
                player.sendMessage("§aGame resumed!");
            } else {
                player.sendMessage("§cCannot resume - game not running or not paused!");
            }
            new ControlGui(plugin).openMainMenu(player);
            return;
        }

        if (type == Material.NETHER_STAR) {
            plugin.getLogger().fine("Processing NETHER_STAR (Shuffle Queue) click");
            if (plugin.getGameManager().shuffleQueue()) {
                player.sendMessage("§aShuffled runner queue successfully.");
            } else {
                player.sendMessage("§cCannot shuffle queue - need at least 2 runners.");
            }
            new ControlGui(plugin).openMainMenu(player);
            return;
        }

        if (type == Material.PLAYER_HEAD) {
            String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
            plugin.getLogger().fine("Processing PLAYER_HEAD click with name: " + name);
            if (name != null && name.contains("Manage Runners")) {
                plugin.getLogger().info("Opening runner selector");
                // Initialize pending selection with current config
                Set<String> initial = new HashSet<>(plugin.getConfigManager().getRunnerNames());
                pendingRunnerSelections.put(player.getUniqueId(), initial);
                new ControlGui(plugin).openRunnerSelector(player);
                return;
            }
        }

        if (type == Material.BOOK) {
            String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
            if (name != null && name.contains("Cooperation Tips")) {
                // Show extended cooperation tips in chat
                player.sendMessage("§6=== §e§lSapnap Mode Cooperation Tips §6===");
                player.sendMessage("§b1. Communication is Key!§7 Use voice chat or text to coordinate");
                player.sendMessage("§b2. Share Everything!§7 Leave items for teammates in shared chests");
                player.sendMessage("§b3. Plan Ahead!§7 Discuss strategies while inactive");
                player.sendMessage("§b4. Stay Informed!§7 Active runner should narrate their actions");
                player.sendMessage("§b5. Resource Management!§7 Don't waste materials when time is limited");
                player.sendMessage("§b6. Safe Locations!§7 Try to swap in secure areas");
                player.sendMessage("§b7. Emergency Protocols!§7 Agree on what to do in dangerous situations");
                player.sendMessage("§b8. Goal Coordination!§7 Work together toward the Ender Dragon!");
                player.sendMessage("§6========================================");
                return;
            }
        }

        // Save current as this mode's default
        if (type == Material.WRITABLE_BOOK) {
            String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
            if ("Save as Mode Default".equals(name)) {
                int curr = plugin.getConfigManager().getSwapInterval();
                plugin.getConfigManager().setModeDefaultInterval(plugin.getCurrentMode(), curr);
                player.sendMessage("§aSaved " + curr + "s as default for this mode.");
                new ControlGui(plugin).openMainMenu(player);
                return;
            }
        }

        if (type == Material.GOLDEN_SWORD) {
            String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
            if (name != null && name.contains("Quick Actions")) {
                // Open quick actions menu or perform instant action
                handleQuickActions(player);
                return;
            }
        }
        
        if (type == Material.ENCHANTED_BOOK) {
            String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
            if (name != null && name.contains("Live Analytics")) {
                // Show detailed analytics in chat
                showDetailedAnalytics(player);
                return;
            } else if (name != null && name.contains("Smart Help")) {
                // Show context-sensitive help
                showSmartHelp(player);
                return;
            }
        }
        
        if (type == Material.BARRIER) {
            String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
            if (name != null && name.contains("Performance Alert")) {
                // Show optimization tips
                showOptimizationTips(player);
                return;
            }
        }
        
        if (type == Material.COMPASS) {
            String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
            if (name != null && name.contains("Team Coordination")) {
                new ControlGui(plugin).openTeamCoordinationMenu(player);
                return;
            }
        }
        
        if (type == Material.REDSTONE) {
            String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
            if (name != null && name.contains("Advanced Settings")) {
                // Open the comprehensive advanced settings menu
                plugin.getGuiManager().openSettingsMenu(player);
                return;
            }
        }

        if (type == Material.CLOCK) {
            String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
            if (name != null && name.contains("Game Status")) {
                // Show detailed status in chat
                player.sendMessage("§6=== §b§lSapnap Mode Status §6===");
                player.sendMessage("§eMode: §bMulti-Runner Cooperation");
                player.sendMessage("§eRunning: §f" + plugin.getGameManager().isGameRunning());
                player.sendMessage("§ePaused: §f" + plugin.getGameManager().isGamePaused());
                if (plugin.getGameManager().isGameRunning()) {
                    Player active = plugin.getGameManager().getActiveRunner();
                    player.sendMessage("§eActive Runner: §f" + (active != null ? active.getName() : "None"));
                    player.sendMessage("§eNext Swap: §f" + plugin.getGameManager().getTimeUntilNextSwap() + "s");
                    List<Player> runnersList = plugin.getGameManager().getRunners();
                    String runners = runnersList != null ? runnersList.stream().map(Player::getName).collect(Collectors.joining(", ")) : "None";
                    player.sendMessage("§eRunners: §f" + runners);
                    player.sendMessage("§eQueue Position: §f" + getQueueInfo(player));
                }
                player.sendMessage("§6===========================");
                return;
            }
        }

        if (type == Material.ARMOR_STAND || type == Material.ENDER_EYE || type == Material.ENDER_PEARL || type == Material.BEDROCK || type == Material.POTION) {
            String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
            if (name != null && name.contains("Inactive State:")) {
                // Cycle freeze mode EFFECTS -> SPECTATOR -> LIMBO -> CAGE -> EFFECTS
                String mode = plugin.getConfigManager().getFreezeMode();
                String next = switch (mode.toUpperCase()) {
                    case "EFFECTS" -> "SPECTATOR";
                    case "SPECTATOR" -> "LIMBO";
                    case "LIMBO" -> "CAGE";
                    default -> "EFFECTS";
                };
                plugin.getConfigManager().setFreezeMode(next);
                player.sendMessage("§eInactive runner state: §a" + next);
                // Re-apply to all
                plugin.getGameManager().reapplyStates();
                new ControlGui(plugin).openMainMenu(player);
                return;
            }
        }

        if (type == Material.SLIME_BLOCK || type == Material.MAGMA_BLOCK) {
            boolean enabled = plugin.getConfigManager().isSafeSwapEnabled();
            plugin.getConfigManager().setSafeSwapEnabled(!enabled);
            player.sendMessage("§eSafe Swap: " + (!enabled ? "§aEnabled" : "§cDisabled"));
            new ControlGui(plugin).openMainMenu(player);
            return;
        }

        if (type == Material.WHITE_BED || type == Material.RED_BED) {
            boolean enabled = plugin.getConfigManager().isSinglePlayerSleepEnabled();
            plugin.getConfigManager().setSinglePlayerSleepEnabled(!enabled);
            player.sendMessage("§eSingle Player Sleep: " + (!enabled ? "§aEnabled" : "§cDisabled"));
            new ControlGui(plugin).openMainMenu(player);
            return;
        }

        // Experimental intervals toggle
        if (type == Material.LEVER || type == Material.REDSTONE_TORCH) {
            String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
            if (name != null && name.startsWith("Experimental Intervals:")) {
                boolean enabled = plugin.getConfigManager().isBetaIntervalEnabled();
                plugin.getConfigManager().setBetaIntervalEnabled(!enabled);
                player.sendMessage("§eExperimental Intervals: " + (!enabled ? "§aEnabled" : "§cDisabled"));
                new ControlGui(plugin).openMainMenu(player);
                return;
            }
        }

        // Apply default on mode switch toggle
        if (type == Material.NOTE_BLOCK || type == Material.GRAY_DYE) {
            String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
            if (name != null && name.startsWith("Apply Mode Default on Switch:")) {
                boolean current = plugin.getConfigManager().getApplyDefaultOnModeSwitch();
                plugin.getConfigManager().setApplyDefaultOnModeSwitch(!current);
                player.sendMessage("§eApply Default on Switch: " + (!current ? "§aYes" : "§cNo"));
                new ControlGui(plugin).openMainMenu(player);
                return;
            }
        }

        // Reset interval to mode default
        if (type == Material.BARRIER) {
            String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
            if ("Reset Interval".equals(name)) {
                plugin.getConfigManager().applyModeDefaultInterval(plugin.getCurrentMode());
                plugin.getGameManager().refreshSwapSchedule();
                player.sendMessage("§eInterval reset to mode default.");
                new ControlGui(plugin).openMainMenu(player);
                return;
            }
        }

        if (type == Material.ARROW) {
            String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
            
            // Handle interval adjustment arrows FIRST (higher priority)
            if (name != null) {
                if (name.equals("-5s")) {
                    int current = plugin.getConfigManager().getSwapInterval();
                    boolean beta = plugin.getConfigManager().isBetaIntervalEnabled();
                    int minAllowed = beta ? 10 : plugin.getConfigManager().getMinSwapInterval();
                    int maxAllowed = plugin.getConfigManager().getSwapIntervalMax();
                    int interval = current - 5;
                    interval = beta ? Math.max(minAllowed, interval) : Math.max(minAllowed, Math.min(maxAllowed, interval));
                    plugin.getConfigManager().setSwapInterval(interval);
                    player.sendMessage("§eInterval decreased to: §a" + interval + "s");
                    if (interval < 30) {
                        player.sendMessage("§cBETA: Intervals below 30s are experimental and may be unstable.");
                        if (interval < 15) player.sendMessage("§cWarning: Intervals under 15s may impact performance.");
                    }
                    if (interval > maxAllowed) {
                        player.sendMessage("§cBETA: Intervals above configured maximum are experimental.");
                    }
                    plugin.getGameManager().refreshSwapSchedule();
                    new ControlGui(plugin).openMainMenu(player);
                    return;
                } else if (name.equals("+5s")) {
                    int current = plugin.getConfigManager().getSwapInterval();
                    boolean beta = plugin.getConfigManager().isBetaIntervalEnabled();
                    int minAllowed = beta ? 10 : plugin.getConfigManager().getMinSwapInterval();
                    int maxAllowed = plugin.getConfigManager().getSwapIntervalMax();
                    int interval = current + 5;
                    interval = beta ? Math.max(minAllowed, interval) : Math.max(minAllowed, Math.min(maxAllowed, interval));
                    plugin.getConfigManager().setSwapInterval(interval);
                    player.sendMessage("§eInterval increased to: §a" + interval + "s");
                    if (interval > maxAllowed) {
                        player.sendMessage("§cBETA: Intervals above configured maximum are experimental and may be unstable.");
                    }
                    plugin.getGameManager().refreshSwapSchedule();
                    new ControlGui(plugin).openMainMenu(player);
                    return;
                }
            }
            
            // Handle back button (lower priority)
            if ("§7§lBack".equals(name)) {
                plugin.getGuiManager().openModeSelector(player);
                return;
            }
        }

        if (type == Material.COMPARATOR) {
            boolean randomize = plugin.getConfigManager().isSwapRandomized();
            plugin.getConfigManager().setSwapRandomized(!randomize);
            player.sendMessage("§eRandomize swaps: " + (!randomize ? "§aEnabled" : "§cDisabled"));
            new ControlGui(plugin).openMainMenu(player);
            return;
        }

        if (type == Material.PAPER) {
            String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
            plugin.getLogger().info("Processing PAPER click with name: " + name);
            if ("Status".equals(name)) {
                // Print status
                player.sendMessage("§6=== Runner-Only Status ===");
                player.sendMessage("§eGame Running: §f" + plugin.getGameManager().isGameRunning());
                player.sendMessage("§eGame Paused: §f" + plugin.getGameManager().isGamePaused());
                if (plugin.getGameManager().isGameRunning()) {
                    Player active = plugin.getGameManager().getActiveRunner();
                    player.sendMessage("§eActive Runner: §f" + (active != null ? active.getName() : "None"));
                    player.sendMessage("§eTime Until Next Swap: §f" + plugin.getGameManager().getTimeUntilNextSwap() + "s");
                    List<Player> runnersList = plugin.getGameManager().getRunners();
                    String runners = runnersList != null ? runnersList.stream().map(Player::getName).collect(Collectors.joining(", ")) : "None";
                    player.sendMessage("§eRunners: §f" + runners);
                }
                return;
            }
        }
        
        // Catch-all for unhandled clicks - helps debug GUI issues
        plugin.getLogger().warning("Unhandled GUI click - Material: " + type + ", Display Name: " + itemName);
        player.sendMessage("§eClick detected but not handled. Material: " + type + ". Check console for details.");
    }

    private void handleRunnerSelectorClick(Player player, ItemStack clicked, int rawSlot, int size) {
        // In Sapnap mode, all players are runners, so return to main menu
        // In Sapnap mode, runner selection is allowed (no hunters). Do not early-return here.

        Material type = clicked.getType();
        // Bottom row buttons
        if (rawSlot >= size - 9) {
            if (type == Material.BARRIER) {
                // Discard
                pendingRunnerSelections.remove(player.getUniqueId());
                new ControlGui(plugin).openMainMenu(player);
                return;
            } else if (type == Material.ARROW) {
                new ControlGui(plugin).openMainMenu(player);
                return;
            } else if (type == Material.EMERALD_BLOCK) {
                // Apply
                Set<String> sel = pendingRunnerSelections.remove(player.getUniqueId());
                if (sel == null) sel = new HashSet<>();
                plugin.getConfigManager().setRunnerNames(new ArrayList<>(sel));
                List<Player> players = new ArrayList<>();
                for (String name : sel) {
                    Player p = Bukkit.getPlayerExact(name);
                    if (p != null && p.isOnline()) players.add(p);
                }
                plugin.getGameManager().setRunners(players);
                player.sendMessage("§aRunners set: §f" + String.join(", ", sel));
                new ControlGui(plugin).openMainMenu(player);
                return;
            }
        }

        // Toggle player selection based on the head name
        if (type == Material.PLAYER_HEAD) {
            String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
            if (name == null || name.isBlank()) return;
            Set<String> sel = pendingRunnerSelections.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>(plugin.getConfigManager().getRunnerNames()));
            if (sel.contains(name)) sel.remove(name); else sel.add(name);
            // Refresh the selector GUI
            new ControlGui(plugin).openRunnerSelector(player);
        }
    }

    private String getQueueInfo(Player player) {
        List<Player> runners = plugin.getGameManager().getRunners();
        Player active = plugin.getGameManager().getActiveRunner();
        
        // Handle null runners list
        if (runners == null || runners.isEmpty()) {
            return "No runners configured";
        }
        
        if (!runners.contains(player)) {
            return "Not in queue";
        }
        
        if (player == active) {
            return "Currently Active";
        }
        
        int position = runners.indexOf(player);
        int activeIndex = runners.indexOf(active);
        
        if (activeIndex == -1) {
            return "Position " + (position + 1) + " of " + runners.size();
        }
        
        // Calculate how many swaps until this player's turn
        int swapsUntilTurn;
        if (position > activeIndex) {
            swapsUntilTurn = position - activeIndex;
        } else {
            swapsUntilTurn = (runners.size() - activeIndex) + position;
        }
        
        return "Next in " + swapsUntilTurn + " swap(s)";
    }
    
    private void handleCoordinationClick(Player player, ItemStack clicked) {
        Material type = clicked.getType();
        
        if (type == Material.ARROW) {
            // Back to main menu
            new ControlGui(plugin).openMainMenu(player);
            return;
        }
        
        if (type == Material.BELL) {
            // Broadcast location with enhanced error handling
            String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
            if (name != null && name.contains("Broadcast Location")) {
                try {
                    String location = String.format("%d, %d, %d in %s", 
                        (int) player.getLocation().getX(),
                        (int) player.getLocation().getY(), 
                        (int) player.getLocation().getZ(),
                        player.getWorld().getName());
                    
                    String message = "§6[§eTeam§6] §b" + player.getName() + "§7 is at: §f" + location;
                    
                    // Broadcast to all runners with null checks
                    List<Player> runners = plugin.getGameManager().getRunners();
                    int broadcastCount = 0;
                    if (runners != null) {
                        for (Player runner : runners) {
                            if (runner != null && runner.isOnline()) {
                                runner.sendMessage(message);
                                broadcastCount++;
                            }
                        }
                    }
                    
                    player.sendMessage("§aLocation broadcasted to " + broadcastCount + " team members!");
                } catch (Exception e) {
                    player.sendMessage("§cError broadcasting location. Please try again.");
                }
                return;
            }
        }
        
        if (type == Material.CHEST) {
            // Share inventory status
            String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
            if (name != null && name.contains("Inventory Status")) {
                List<String> importantItems = new ArrayList<>();
                
                // Check for key items
                for (ItemStack item : player.getInventory().getContents()) {
                    if (item == null || item.getType() == Material.AIR) continue;
                    
                    Material mat = item.getType();
                    int amount = item.getAmount();
                    
                    // Tools, weapons, food, and important items
                    if (mat.name().contains("SWORD") || mat.name().contains("AXE") || 
                        mat.name().contains("PICKAXE") || mat.name().contains("SHOVEL") ||
                        mat == Material.BOW || mat == Material.CROSSBOW ||
                        mat == Material.BREAD || mat == Material.COOKED_BEEF ||
                        mat == Material.ENDER_PEARL || mat == Material.BLAZE_ROD ||
                        mat == Material.DIAMOND || mat == Material.EMERALD ||
                        mat == Material.IRON_INGOT || mat == Material.GOLD_INGOT) {
                        
                        String itemName = mat.name().toLowerCase().replace("_", " ");
                        importantItems.add(amount + "x " + itemName);
                    }
                }
                
                String statusMsg;
                if (importantItems.isEmpty()) {
                    statusMsg = "§6[§eTeam§6] §b" + player.getName() + "§7 has no important items";
                } else {
                    statusMsg = "§6[§eTeam§6] §b" + player.getName() + "§7 has: §f" + String.join(", ", importantItems);
                }
                
                // Broadcast to all runners
                List<Player> runners = plugin.getGameManager().getRunners();
                if (runners != null) {
                    for (Player runner : runners) {
                        if (runner != null && runner.isOnline()) {
                            runner.sendMessage(statusMsg);
                        }
                    }
                }
                
                player.sendMessage("§aInventory status shared with team!");
                return;
            }
        }
        
        if (type == Material.REDSTONE_TORCH) {
            // Emergency pause request
            String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
            if (name != null && name.contains("Emergency Pause")) {
                if (plugin.getGameManager().isGameRunning() && !plugin.getGameManager().isGamePaused()) {
                    plugin.getGameManager().pauseGame();
                    
                    String pauseMsg = "§c[§lEMERGENCY§c] §6" + player.getName() + "§e requested emergency pause!";
                    
                    // Notify all runners
                    List<Player> runners = plugin.getGameManager().getRunners();
                    if (runners != null) {
                        for (Player runner : runners) {
                            if (runner != null && runner.isOnline()) {
                                runner.sendMessage(pauseMsg);
                                // Cross-platform title display
                                BukkitCompat.showTitle(
                                    runner,
                                    "§c§lEMERGENCY PAUSE",
                                    "§6Requested by " + player.getName(),
                                    10, 40, 10
                                );
                            }
                        }
                    }
                    
                    player.sendMessage("§aGame paused! Emergency pause activated.");
                } else {
                    player.sendMessage("§cGame is not running or already paused!");
                }
                return;
            }
        }
        
        if (type == Material.ENDER_EYE) {
            // Mark waypoint (placeholder - requires more complex implementation)
            String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
            if (name != null && name.contains("Mark Waypoint")) {
                // For now, just broadcast the location as a waypoint
                String location = String.format("%d, %d, %d", 
                    (int) player.getLocation().getX(),
                    (int) player.getLocation().getY(), 
                    (int) player.getLocation().getZ());
                
                String waypointMsg = "§d[§lWAYPOINT§d] §b" + player.getName() + "§7 marked: §f" + location + "§7 - Check your compass!";
                
                // Broadcast to all runners
                List<Player> runners = plugin.getGameManager().getRunners();
                if (runners != null) {
                    for (Player runner : runners) {
                        if (runner != null && runner.isOnline()) {
                            runner.sendMessage(waypointMsg);
                        }
                    }
                }
                
                player.sendMessage("§aWaypoint marked and shared with team!");
                return;
            }
        }
    }
    
    // Quick Actions Handler - Lightning fast team coordination with enhanced error handling
    private void handleQuickActions(Player player) {
        if (!plugin.getGameManager().isGameRunning()) {
            player.sendMessage("§cQuick actions only available during active games!");
            return;
        }
        
        try {
            // Instant location broadcast with enhanced information
            String location = String.format("%d, %d, %d in %s", 
                (int) player.getLocation().getX(),
                (int) player.getLocation().getY(), 
                (int) player.getLocation().getZ(),
                player.getWorld().getName());
            
            // Enhanced status information
            double health = Math.round(player.getHealth() * 10.0) / 10.0; // Round to 1 decimal
            int food = player.getFoodLevel();
            String gamemode = player.getGameMode().name().toLowerCase();
            
            String quickMsg = "§e[§lQUICK§e] §b" + player.getName() + "§7: §f" + location + 
                             "§7 | Health: §c" + health + "/20 §7| Food: §6" + food + "/20 §7| Mode: §d" + gamemode;
            
            // Broadcast to all runners with error handling
            List<Player> runners = plugin.getGameManager().getRunners();
            if (runners != null && !runners.isEmpty()) {
                for (Player runner : runners) {
                    if (runner != null && runner.isOnline()) {
                        runner.sendMessage(quickMsg);
                    }
                }
            }
            
            player.sendMessage("§a⚡ Quick status broadcasted to " + (runners != null ? runners.size() : 0) + " runners!");
            
            // Send title notification to active runner if different
            Player activeRunner = plugin.getGameManager().getActiveRunner();
            if (activeRunner != null && !activeRunner.equals(player) && activeRunner.isOnline()) {
                BukkitCompat.showTitle(
                    activeRunner,
                    "§e§lTEAM UPDATE",
                    "§b" + player.getName() + "§7 shared location",
                    5, 30, 5
                );
            }
        } catch (Exception e) {
            player.sendMessage("§cError sending quick update. Please try again.");
            // Log error for debugging if needed
        }
    }
    
    // Detailed Analytics Display
    private void showDetailedAnalytics(Player player) {
        if (!plugin.getGameManager().isGameRunning()) {
            player.sendMessage("§cAnalytics only available during active games!");
            return;
        }
        
        player.sendMessage("§6========== §b§lDETAILED ANALYTICS §6==========");
        
        // Game state info
        Player activeRunner = plugin.getGameManager().getActiveRunner();
        List<Player> runners = plugin.getGameManager().getRunners();
        int timeLeft = plugin.getGameManager().getTimeUntilNextSwap();
        
        player.sendMessage("§e§lGame Status:");
        player.sendMessage("§7  Active Runner: §b" + (activeRunner != null ? activeRunner.getName() : "None"));
        player.sendMessage("§7  Total Runners: §e" + (runners != null ? runners.size() : 0));
        player.sendMessage("§7  Next Swap: §e" + timeLeft + "s");
        player.sendMessage("§7  Game Paused: §f" + plugin.getGameManager().isGamePaused());
        
        if (timeLeft <= 15) {
            player.sendMessage("§c§lSwap Imminent - Immediate Actions:");
            player.sendMessage("§7  ⚡ Finish current objective quickly!");
            player.sendMessage("§7  ⚡ Move to safe location if possible");
            player.sendMessage("§7  ⚡ Communicate handoff plans");
            player.sendMessage("§7  ⚡ Prepare next runner via voice chat");
            player.sendMessage("§7  ⚡ Use emergency pause if in danger");
        } else {
            player.sendMessage("§a§lGame Active - Cooperation Tips:");
            player.sendMessage("§7  ✓ Use Quick Actions for instant communication");
            player.sendMessage("§7  ✓ Monitor Live Analytics for performance");
            player.sendMessage("§7  ✓ Share locations and inventory status");
            player.sendMessage("§7  ✓ Mark waypoints for important locations");
            player.sendMessage("§7  ✓ Active runner should narrate actions");
            player.sendMessage("§7  ✓ Waiting runners should plan ahead");
        }
    }

    // Smart context-aware help
    private void showSmartHelp(Player player) {
        boolean running = plugin.getGameManager().isGameRunning();
        boolean paused = plugin.getGameManager().isGamePaused();
        int timeLeft = plugin.getGameManager().getTimeUntilNextSwap();

        player.sendMessage("§6========== §e§lSMART HELP §6==========");
        if (!running) {
            player.sendMessage("§aGetting Started:");
            player.sendMessage("§7  ✓ Select runners via Manage Runners");
            player.sendMessage("§7  ✓ Adjust interval with ±5s arrows");
            player.sendMessage("§7  ✓ Configure freeze mode and safe swaps");
            player.sendMessage("§7  ✓ Click §aStart Game§7 when ready");
        } else if (paused) {
            player.sendMessage("§eGame Paused:");
            player.sendMessage("§7  ✓ Discuss next objectives");
            player.sendMessage("§7  ✓ Use coordination hub to share info");
            player.sendMessage("§7  ✓ Click §aResume§7 when ready");
        } else {
            if (timeLeft <= 15) {
                player.sendMessage("§cSwap Imminent:");
                player.sendMessage("§7  ⚡ Prepare handoff and move to safety");
            } else {
                player.sendMessage("§aActive Play Tips:");
                player.sendMessage("§7  ✓ Use Quick Actions for instant updates");
                player.sendMessage("§7  ✓ Monitor Live Analytics and Performance Alerts");
                player.sendMessage("§7  ✓ Use coordination tools for teamwork");
            }
        }
        player.sendMessage("§6================================");
    }

    // Optimization tips with TPS/memory awareness
    private void showOptimizationTips(Player player) {
        player.sendMessage("§6========== §c§lOPTIMIZATION TIPS §6==========");
        try {
            Double tps = BukkitCompat.getServerTPS();
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory() / 1024 / 1024;
            long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
            int memoryPercent = (int) ((usedMemory * 100) / maxMemory);

            player.sendMessage("§7Current Performance:");
            player.sendMessage("§7  TPS: §f" + (tps != null ? String.format("%.2f", tps) : "N/A"));
            player.sendMessage("§7  Memory: §f" + usedMemory + "/" + maxMemory + "MB (" + memoryPercent + "%)");

            if (tps != null && tps < 18.0) {
                player.sendMessage("§eServer TPS low:");
                player.sendMessage("§7  ⚠ §cReduce team size (remove 1-2 runners)");
                player.sendMessage("§7  ⚠ §cIncrease swap interval (60s+ recommended)");
                player.sendMessage("§7  ⚠ §cConsider pausing temporarily");
            }
            if (memoryPercent > 85) {
                player.sendMessage("§eHigh memory usage:");
                player.sendMessage("§7  ⚠ §cReduce render distance (8-12 chunks)");
                player.sendMessage("§7  ⚠ §cClose unnecessary applications");
                player.sendMessage("§7  ⚠ §cRestart server if possible");
            }
        } catch (Exception e) {
            player.sendMessage("§7  Performance metrics unavailable");
        }

        player.sendMessage("");
        player.sendMessage("§eGeneral Recommendations:");
        player.sendMessage("§7  1. Keep team size between 2-4 players");
        player.sendMessage("§7  2. Use 45-60s swap intervals for stability");
        player.sendMessage("§7  3. Enable Safe Swaps to prevent lag spikes");
        player.sendMessage("§7  4. Prefer SPECTATOR freeze mode for performance");
        player.sendMessage("§7  5. Restart sessions if problems persist");
        player.sendMessage("§6" + "=".repeat(44));
    }
}
