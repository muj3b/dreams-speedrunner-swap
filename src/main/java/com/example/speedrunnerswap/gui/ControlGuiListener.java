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
import org.bukkit.event.player.PlayerQuitEvent;
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

    private void handleStatsClick(Player player, ItemStack clicked) {
        if (clicked == null || !clicked.hasItemMeta()) return;
        String id = getButtonId(clicked);
        if ("back".equals(id)) { new ControlGui(plugin).openMainMenu(player); return; }
        // Fallback: allow arrow named Back
        Material type = clicked.getType();
        String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
        if (type == Material.ARROW || (name != null && name.contains("Back"))) {
            new ControlGui(plugin).openMainMenu(player);
        }
    }
    
    // About screen clicks are handled exclusively by AboutGuiListener.

    public static void setPendingSelection(java.util.UUID uuid, Set<String> sel) {
        if (sel == null || sel.isEmpty()) pendingRunnerSelections.remove(uuid); else pendingRunnerSelections.put(uuid, sel);
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
        if (plain.contains("coordination")) return "coordination";
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
    
    private boolean isStats(Inventory inv) {
        return inv != null && inv.getHolder() instanceof ControlGuiHolder holder && holder.getType() == ControlGuiHolder.Type.STATS;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory top = event.getView().getTopInventory();
        if (top == null) return;
        
        // Check if this is one of our GUI inventories
        if (!isMain(top) && !isRunnerSelector(top) && !isCoordination(top) && !isAbout(top) && !isStats(top)) return;
        
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
        } else if (isStats(top)) {
            handleStatsClick(player, clicked);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Inventory top = event.getView().getTopInventory();
        if (top == null) return;
        
        // Check if this is one of our GUI inventories
        if (!isMain(top) && !isRunnerSelector(top) && !isCoordination(top) && !isStats(top) && !isAbout(top)) return;
        
        // ALWAYS cancel drag events in our GUIs
        event.setCancelled(true);
        plugin.getLogger().fine("Blocked drag attempt in GUI");
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryMoveItem(org.bukkit.event.inventory.InventoryMoveItemEvent event) {
        Inventory source = event.getSource();
        Inventory destination = event.getDestination();
        
        // Block any item movement from/to our GUIs
        if ((source != null && (isMain(source) || isRunnerSelector(source) || isCoordination(source) || isAbout(source) || isStats(source))) ||
            (destination != null && (isMain(destination) || isRunnerSelector(destination) || isCoordination(destination) || isAbout(destination) || isStats(destination)))) {
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

    // Also clear any pending selection if the player disconnects while selector is open or pending
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        java.util.UUID id = event.getPlayer().getUniqueId();
        pendingRunnerSelections.remove(id);
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
                case "back" -> { plugin.getGuiManager().openModeSelector(player); return; }
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
                case "statistics" -> {
                    new ControlGui(plugin).openStatsMenu(player);
                    return;
                }
                case "coordination" -> {
                    new ControlGui(plugin).openTeamCoordinationMenu(player);
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

        if (type == Material.ENDER_EYE || type == Material.ENDER_PEARL || type == Material.BEDROCK || type == Material.POTION) {
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
        // Prefer PDC-tagged button ids first
        String id = getButtonId(clicked);
        if (id != null) {
            switch (id) {
                case "back" -> { new ControlGui(plugin).openMainMenu(player); return; }
                case "cancel_selection" -> {
                    pendingRunnerSelections.remove(player.getUniqueId());
                    new ControlGui(plugin).openMainMenu(player);
                    return;
                }
                case "save_selection" -> {
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
        }
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
        String id = getButtonId(clicked);
        if ("back".equals(id)) {
            new ControlGui(plugin).openMainMenu(player);
            return;
        }
        if ("coord_broadcast".equals(id)) {
            broadcastLocation(player);
            return;
        }
        if ("coord_inventory".equals(id)) {
            shareInventorySummary(player);
            return;
        }
        if ("coord_emergency_pause".equals(id)) {
            handleEmergencyPause(player);
            return;
        }
        if ("coord_waypoint".equals(id)) {
            markWaypoint(player);
        }
    }

    private void broadcastLocation(Player player) {
        try {
            String location = String.format("%d, %d, %d in %s",
                (int) player.getLocation().getX(),
                (int) player.getLocation().getY(),
                (int) player.getLocation().getZ(),
                player.getWorld().getName());
            String message = "§6[§eTeam§6] §b" + player.getName() + "§7 is at: §f" + location;
            int broadcastCount = 0;
            List<Player> runners = plugin.getGameManager().getRunners();
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
    }

    private void shareInventorySummary(Player player) {
        List<String> importantItems = new ArrayList<>();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;
            Material mat = item.getType();
            int amount = item.getAmount();
            if (mat.name().contains("SWORD") || mat.name().contains("AXE") || mat.name().contains("PICKAXE") || mat.name().contains("SHOVEL") ||
                mat == Material.BOW || mat == Material.CROSSBOW || mat == Material.BREAD || mat == Material.COOKED_BEEF ||
                mat == Material.ENDER_PEARL || mat == Material.BLAZE_ROD || mat == Material.DIAMOND || mat == Material.EMERALD ||
                mat == Material.IRON_INGOT || mat == Material.GOLD_INGOT) {
                String itemName = mat.name().toLowerCase().replace("_", " ");
                importantItems.add(amount + "x " + itemName);
            }
        }
        String statusMsg = importantItems.isEmpty()
            ? "§6[§eTeam§6] §b" + player.getName() + "§7 has no important items"
            : "§6[§eTeam§6] §b" + player.getName() + "§7 has: §f" + String.join(", ", importantItems);
        List<Player> runners = plugin.getGameManager().getRunners();
        if (runners != null) {
            for (Player runner : runners) {
                if (runner != null && runner.isOnline()) {
                    runner.sendMessage(statusMsg);
                }
            }
        }
        player.sendMessage("§aInventory status shared with team!");
    }

    private void handleEmergencyPause(Player player) {
        if (plugin.getGameManager().isGameRunning() && !plugin.getGameManager().isGamePaused()) {
            plugin.getGameManager().pauseGame();
            String pauseMsg = "§c[§lEMERGENCY§c] §6" + player.getName() + "§e requested emergency pause!";
            List<Player> runners = plugin.getGameManager().getRunners();
            if (runners != null) {
                for (Player runner : runners) {
                    if (runner != null && runner.isOnline()) {
                        runner.sendMessage(pauseMsg);
                        BukkitCompat.showTitle(runner, "§c§lEMERGENCY PAUSE", "§6Requested by " + player.getName(), 10, 40, 10);
                    }
                }
            }
            player.sendMessage("§aGame paused! Emergency pause activated.");
        } else {
            player.sendMessage("§cGame is not running or already paused!");
        }
    }

    private void markWaypoint(Player player) {
        String location = String.format("%d, %d, %d",
            (int) player.getLocation().getX(),
            (int) player.getLocation().getY(),
            (int) player.getLocation().getZ());
        String waypointMsg = "§d[§lWAYPOINT§d] §b" + player.getName() + "§7 marked: §f" + location + "§7 - Check your compass!";
        List<Player> runners = plugin.getGameManager().getRunners();
        if (runners != null) {
            for (Player runner : runners) {
                if (runner != null && runner.isOnline()) {
                    runner.sendMessage(waypointMsg);
                }
            }
        }
        player.sendMessage("§aWaypoint marked and shared with team!");
    }
}
