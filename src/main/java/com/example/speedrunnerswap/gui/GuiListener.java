package com.example.speedrunnerswap.gui;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.NamespacedKey;

public class GuiListener implements Listener {
    private final SpeedrunnerSwap plugin;
    private final GuiManager guiManager;

    public GuiListener(SpeedrunnerSwap plugin, GuiManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        // If this is a ControlGui inventory (Sapnap mode), let ControlGuiListener handle it exclusively
        org.bukkit.inventory.Inventory top = event.getView().getTopInventory();
        if (top != null && top.getHolder() instanceof ControlGuiHolder) {
            return;
        }

        String title = getPlainTitle(event.getView());

        // Always cancel any click when a plugin GUI is open (regardless of item)
        if (isPluginGui(title)) {
            event.setCancelled(true);
            event.setResult(org.bukkit.event.Event.Result.DENY);

            // Only handle clicks in the top inventory; always block bottom/player inventory
            int topSize = event.getView().getTopInventory().getSize();
            if (event.getRawSlot() >= topSize) {
                return;
            }

            ItemStack clickedItem = event.getCurrentItem();

            // Admin permission check
            if (!player.hasPermission("speedrunnerswap.admin") && isAdminMenu(title)) {
                player.sendMessage("§cYou do not have permission to use this menu.");
                return;
            }

            // If there is no clickable item, do nothing further
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            // Route to appropriate handler
            handleGuiClick(event, title);
        }
    }

    // Add drag event handler
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        // Skip ControlGui inventories
        org.bukkit.inventory.Inventory top = event.getView().getTopInventory();
        if (top != null && top.getHolder() instanceof ControlGuiHolder) return;
        String title = getPlainTitle(event.getView());
        
        // Always cancel drag events in plugin GUIs
        if (isPluginGui(title)) {
            event.setCancelled(true);
            event.setResult(org.bukkit.event.Event.Result.DENY);
        }
    }

    // Cross-version safe title extractor without compile-time kyori dependency
    private String getPlainTitle(org.bukkit.inventory.InventoryView view) {
        if (view == null) return "";
        // Try Paper's title() via reflection, then fall back to getTitle() and toString()
        try {
            java.lang.reflect.Method m = view.getClass().getMethod("title");
            Object comp = m.invoke(view);
            if (comp != null) {
                // Try Adventure PlainTextComponentSerializer via reflection
                try {
                    Class<?> serCls = Class.forName("net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer");
                    java.lang.reflect.Method plainText = serCls.getMethod("plainText");
                    Object serializer = plainText.invoke(null);
                    Class<?> componentCls = Class.forName("net.kyori.adventure.text.Component");
                    java.lang.reflect.Method serialize = serializer.getClass().getMethod("serialize", componentCls);
                    Object s = serialize.invoke(serializer, comp);
                    if (s != null) return String.valueOf(s);
                } catch (Throwable ignored) {
                    // Fallback: try common "content" accessor or toString
                    try {
                        java.lang.reflect.Method content = comp.getClass().getMethod("content");
                        Object s = content.invoke(comp);
                        if (s != null) return String.valueOf(s);
                    } catch (Throwable ignored2) {}
                    return String.valueOf(comp);
                }
            }
        } catch (Throwable ignored) {}
        try {
            java.lang.reflect.Method m2 = view.getClass().getMethod("getTitle");
            Object s2 = m2.invoke(view);
            return s2 != null ? String.valueOf(s2) : "";
        } catch (Throwable ignored) {}
        return "";
    }
    
    // Block item movement events
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryMoveItem(org.bukkit.event.inventory.InventoryMoveItemEvent event) {
        // This would affect hopper/dropper interactions, but we need to be careful
        // Only block if the inventory belongs to our GUI
    }

    private boolean isPluginGui(String title) {
        if (title == null || title.isEmpty()) return false;
        // Include all plugin menus and the kit editor
        return title.contains("SpeedrunnerSwap") ||
               title.contains("Speedrunner Swap") ||
               title.contains("Mode Selector") ||
               title.contains("Choose Gamemode") ||
               title.contains("Mode Selection") ||
               title.contains("Confirm Mode Switch") ||
               title.contains("Main Menu") ||
               title.contains("Team Selector") ||
               title.contains("Settings") ||
               title.contains("Kits") ||
               (title.contains("Edit ") && title.contains(" Kit")) ||
               title.contains("Kit Editor") ||
               title.contains("Effects") ||
               title.contains("Power-ups") ||
               title.contains("Power-up Durations") ||
               title.contains("World Border") ||
               title.contains("Bounty") ||
               title.contains("Last Stand") ||
               title.contains("Compass") ||
               title.contains("Sudden Death") ||
               title.contains("Statistics") ||
               title.contains("Statistics & Tracking") ||
               title.contains("Dangerous Blocks") ||
               title.contains("Custom Tasks") ||
               // Add explicit mode menus and per-mode settings
               title.contains("Task Manager") ||
               title.contains("Task Settings") ||
               title.contains("Dream Settings") ||
               title.contains("Advanced Config") ||
               title.contains("List Editor") ||
               title.contains("Dream") ||
               title.contains("Sapnap");
    }

    private boolean isAdminMenu(String title) {
        if (title == null || title.isEmpty()) return false;
        return title.contains("Settings") ||
               title.contains("Kits") ||
               (title.contains("Edit ") && title.contains(" Kit")) ||
               title.contains("Effects") ||
               title.contains("Power-ups") ||
               title.contains("Power-up Durations") ||
               title.contains("World Border") ||
               title.contains("Bounty") ||
               title.contains("Last Stand") ||
               title.contains("Compass") ||
               title.contains("Sudden Death");
    }

    private void handleGuiClick(InventoryClickEvent event, String title) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        
        // If this button carries a specific id, route it first (before generic back buttons)
        String earlyId = getButtonId(clickedItem);
        if (earlyId != null) {
            switch (earlyId) {
                case "back_mode" -> { guiManager.openModeSelector(player); return; }
                case "statistics" -> { guiManager.openStatisticsMenu(player); return; }
                case "world_border" -> { guiManager.openWorldBorderMenu(player); return; }
                case "power_ups" -> { guiManager.openPowerUpsMenu(player); return; }
                case "team_selector" -> { guiManager.openTeamSelector(player); return; }
                case "task_settings" -> { guiManager.openTaskSettingsMenu(player); return; }
                case "dream_settings" -> { guiManager.openDreamSettingsMenu(player); return; }
                case "advanced_config_root" -> { guiManager.openAdvancedConfigMenu(player, "", 0); return; }
                case "direct_dream_menu" -> {
                    // Direct access to Dream mode main menu
                    plugin.setCurrentMode(com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.DREAM);
                    guiManager.openMainMenu(player);
                    return;
                }
                case "direct_sapnap_menu" -> {
                    // Direct access to Sapnap mode main menu
                    plugin.setCurrentMode(com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.SAPNAP);
                    try {
                        new com.example.speedrunnerswap.gui.ControlGui(plugin).openMainMenu(player);
                    } catch (Throwable t) {
                        player.sendMessage("§cSapnap GUI failed to open: " + t.getMessage());
                        plugin.getLogger().warning("Sapnap GUI error: " + t.getMessage());
                    }
                    return;
                }
                case "direct_task_menu" -> {
                    // Direct access to Task Manager mode main menu
                    plugin.setCurrentMode(com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.TASK);
                    guiManager.openMainMenu(player);
                    return;
                }
                case "broadcast_settings" -> { guiManager.openBroadcastSettingsMenu(player); return; }
                case "limbo_settings" -> { guiManager.openLimboSettingsMenu(player); return; }
                case "ui_performance" -> { guiManager.openUIPerformanceMenu(player); return; }
                case "custom_tasks_menu" -> { guiManager.openCustomTasksMenu(player); return; }
                case "dangerous_blocks" -> { guiManager.openDangerousBlocksMenu(player); return; }
            }
        }

        // Handle back buttons first
        if (isBackButton(clickedItem)) {
            guiManager.openMainMenu(player);
            return;
        }

        // Special-case handling for submenus not covered by a general section
        if (maybeHandlePowerUpDurationsMenu(event, title)) return;
        if (maybeHandleDangerousBlocksMenu(event, title)) return;

        // Route to specific menu handlers
        if (title.contains("Mode Selector")) {
            handleModeSelectorClick(event);
            return;
        }
        // Handle the new direct gamemode selector
        if (title.contains("Choose Gamemode")) {
            handleDirectGamemodeSelectorClick(event);
            return;
        }
        // Main menu: support both legacy "Main Menu" and configurable titles like "SpeedrunnerSwap Menu"
        if (title.contains("Main Menu") || title.contains("SpeedrunnerSwap")) {
            // If someone opened this while in Sapnap mode, redirect
            if (plugin.getCurrentMode() == com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.SAPNAP && plugin.getGameManager().isGameRunning()) {
                ((Player) event.getWhoClicked()).sendMessage("§cYou're in Sapnap mode. Stop the game to switch.");
                event.setCancelled(true);
                return;
            }
            handleMainMenuClick(event);
        } else if (title.contains("Team Selector")) {
            handleTeamSelectorClick(event);
        // Check specific "... Settings" menus BEFORE the generic settings title
        } else if (title.contains("World Border")) {
            handleWorldBorderClick(event);
        } else if (title.contains("Compass")) {
            handleCompassSettingsClick(event);
        } else if (title.contains("Sudden Death")) {
            handleSuddenDeathClick(event);
        } else if (title.contains("Broadcast Settings")) {
            handleBroadcastSettingsClick(event);
        } else if (title.contains("Limbo Configuration")) {
            handleLimboSettingsClick(event);
        } else if (title.contains("UI Performance")) {
            handleUIPerformanceClick(event);
        } else if (title.contains("Settings")) {
            // Only the main settings menu should land here; specific menus are handled above
            handleSettingsClick(event);
        } else if (title.contains("Edit ") && title.contains(" Kit")) {
            handleKitEditorClick(event, title);
        } else if (title.contains("Kits")) {
            handleKitsMenuClick(event);
        } else if (title.contains("Power-ups")) {
            handlePowerUpsMenuClick(event);
        } else if (title.contains("Effects")) {
            handleEffectsMenuClick(event, title);
        } else if (title.contains("Bounty")) {
            handleBountyMenuClick(event);
        } else if (title.contains("Last Stand")) {
            handleLastStandClick(event);
        } else if (title.contains("Statistics")) {
            handleStatisticsClick(event);
        } else if (title.contains("Custom Tasks")) {
            handleCustomTasksClick(event);
        } else if (title.contains("Task Settings")) {
            handleTaskSettingsClick(event);
        } else if (title.contains("Dream Settings")) {
            handleDreamSettingsClick(event);
        } else if (title.contains("Advanced Config")) {
            handleAdvancedConfigClick(event);
        } else if (title.contains("List Editor")) {
            handleConfigListClick(event);
        }
    }

    private void handleDirectGamemodeSelectorClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String id = getButtonId(clicked);
        
        // The button IDs are already handled in the early routing above,
        // but let's add a fallback handler for safety
        if ("direct_dream_menu".equals(id)) {
            plugin.setCurrentMode(com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.DREAM);
            guiManager.openMainMenu(player);
        } else if ("direct_sapnap_menu".equals(id)) {
            plugin.setCurrentMode(com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.SAPNAP);
            try {
                new com.example.speedrunnerswap.gui.ControlGui(plugin).openMainMenu(player);
            } catch (Throwable t) {
                player.sendMessage("§cSapnap GUI failed to open: " + t.getMessage());
                plugin.getLogger().warning("Sapnap GUI error: " + t.getMessage());
            }
        } else if ("direct_task_menu".equals(id)) {
            plugin.setCurrentMode(com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.TASK);
            guiManager.openMainMenu(player);
        }
    }

    private void handleModeSelectorClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String id = getButtonId(clicked);
        
        if ("mode_dream".equals(id)) {
            if (plugin.getGameManager().isGameRunning()) {
                if (!player.hasPermission("speedrunnerswap.admin")) {
                    player.sendMessage("§cStop the current game before switching modes.");
                    return;
                }
                guiManager.openForceConfirm(player, com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.DREAM);
                return;
            }
            plugin.setCurrentMode(com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.DREAM);
            guiManager.openMainMenu(player);
            return;
        }
        
        if ("mode_task".equals(id)) {
            if (plugin.getGameManager().isGameRunning()) {
                if (!player.hasPermission("speedrunnerswap.admin")) {
                    player.sendMessage("§cStop the current game before switching modes.");
                    return;
                }
                guiManager.openForceConfirm(player, com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.TASK);
                return;
            }
            plugin.setCurrentMode(com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.TASK);
            guiManager.openMainMenu(player);
            return;
        }
        
        if ("mode_sapnap".equals(id)) {
            if (plugin.getGameManager().isGameRunning()) {
                if (!player.hasPermission("speedrunnerswap.admin")) {
                    player.sendMessage("§cStop the current game before switching modes.");
                    return;
                }
                guiManager.openForceConfirm(player, com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.SAPNAP);
                return;
            }
            plugin.setCurrentMode(com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.SAPNAP);
            try {
                new com.example.speedrunnerswap.gui.ControlGui(plugin).openMainMenu(player);
            } catch (Throwable t) {
                player.sendMessage("§cSapnap GUI failed to open: " + t.getMessage());
            }
            return;
        }
        
        if ("admin_force_stop".equals(id)) {
            if (!player.hasPermission("speedrunnerswap.admin")) {
                player.sendMessage("§cYou don't have permission for admin actions.");
                return;
            }
            if (plugin.getGameManager().isGameRunning()) {
                plugin.getGameManager().stopGame();
                player.sendMessage("§aGame force-stopped. You can now switch modes.");
            }
            guiManager.openModeSelector(player);
            return;
        }
        
        // Handle force confirm dialogs
        if ("force_yes_dream".equals(id)) {
            if (!player.hasPermission("speedrunnerswap.admin")) return;
            if (plugin.getGameManager().isGameRunning()) plugin.getGameManager().stopGame();
            plugin.setCurrentMode(com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.DREAM);
            guiManager.openMainMenu(player);
            return;
        }
        
        if ("force_yes_sapnap".equals(id)) {
            if (!player.hasPermission("speedrunnerswap.admin")) return;
            if (plugin.getGameManager().isGameRunning()) plugin.getGameManager().stopGame();
            plugin.setCurrentMode(com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.SAPNAP);
            try { 
                new com.example.speedrunnerswap.gui.ControlGui(plugin).openMainMenu(player); 
            } catch (Throwable ignored) {
                player.sendMessage("§cSapnap GUI failed to open.");
            }
            return;
        }
        
        if ("force_yes_task".equals(id)) {
            if (!player.hasPermission("speedrunnerswap.admin")) return;
            if (plugin.getGameManager().isGameRunning()) plugin.getGameManager().stopGame();
            plugin.setCurrentMode(com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.TASK);
            guiManager.openMainMenu(player);
            return;
        }
        
        if ("force_no".equals(id)) {
            guiManager.openModeSelector(player);
            return;
        }
    }

    private void handleMainMenuClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || !clicked.hasItemMeta()) return;

        // Prefer explicit button IDs when present
        String buttonId = getButtonId(clicked);
        if (buttonId != null) {
            switch (buttonId) {
                case "start_game" -> {
                    if (!plugin.getGameManager().isGameRunning()) {
                        if (plugin.getGameManager().startGame()) {
                            player.sendMessage("§aGame started successfully!");
                        } else {
                            player.sendMessage("§cFailed to start game. Check team assignments.");
                        }
                    }
                    guiManager.openMainMenu(player);
                }
                case "stop_game" -> {
                    if (plugin.getGameManager().isGameRunning()) {
                        plugin.getGameManager().stopGame();
                        player.sendMessage("§cGame stopped.");
                    }
                    guiManager.openMainMenu(player);
                }
                case "pause_game" -> {
                    if (plugin.getGameManager().isGameRunning() && !plugin.getGameManager().isGamePaused()) {
                        plugin.getGameManager().pauseGame();
                        player.sendMessage("§eGame paused.");
                    }
                    guiManager.openMainMenu(player);
                }
                case "resume_game" -> {
                    if (plugin.getGameManager().isGameRunning() && plugin.getGameManager().isGamePaused()) {
                        plugin.getGameManager().resumeGame();
                        player.sendMessage("§aGame resumed.");
                    }
                    guiManager.openMainMenu(player);
                }
                case "team_selector" -> guiManager.openTeamSelector(player);
                case "advanced_settings" -> guiManager.openSettingsMenu(player);
                case "power_ups" -> guiManager.openPowerUpsMenu(player);
                case "world_border" -> guiManager.openWorldBorderMenu(player);
                case "kits" -> guiManager.openKitsMenu(player);
                case "bounty" -> guiManager.openBountyMenu(player);
                case "last_stand" -> guiManager.openLastStandMenu(player);
                case "dream_tracking" -> guiManager.openCompassSettingsMenu(player);
                case "sudden_death" -> guiManager.openSuddenDeathMenu(player);
                case "statistics" -> guiManager.openStatisticsMenu(player);
                case "task_settings" -> guiManager.openTaskSettingsMenu(player);
                case "custom_tasks_menu" -> guiManager.openCustomTasksMenu(player);
                case "task_assignments" -> {
                    // Show task assignments
                    var tmm = plugin.getTaskManagerMode();
                    if (tmm != null && !tmm.getAssignments().isEmpty()) {
                        player.sendMessage("§6=== Current Task Assignments ===");
                        for (var entry : tmm.getAssignments().entrySet()) {
                            String name = plugin.getServer().getOfflinePlayer(entry.getKey()).getName();
                            var task = tmm.getTask(entry.getValue());
                            player.sendMessage("§b" + name + ": §7" + (task != null ? task.description() : entry.getValue()));
                        }
                        player.sendMessage("§6===========================");
                    } else {
                        player.sendMessage("§eNo task assignments yet. Start a game to assign tasks.");
                    }
                }
                case "reroll_tasks" -> {
                    if (!plugin.getGameManager().isGameRunning()) {
                        var tmm = plugin.getTaskManagerMode();
                        if (tmm != null) {
                            tmm.assignAndAnnounceTasks(plugin.getGameManager().getRunners());
                            player.sendMessage("§aTask assignments have been rerolled!");
                        }
                    } else {
                        player.sendMessage("§cCannot reroll tasks while game is running.");
                    }
                    guiManager.openMainMenu(player);
                }
            }
            return;
        }

        // No fallback needed - all buttons should have proper IDs
        // If we reach here, it's an unhandled button
        plugin.getLogger().warning("Unhandled main menu click: " + 
            (clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName() ? 
                com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta()) : 
                clicked.getType().name()));
    }



    private void handleSettingsClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || !clicked.hasItemMeta()) return;

        String buttonId = getButtonId(clicked);
        if (buttonId != null) {
            switch (buttonId) {
                case "back_main": {
                    guiManager.openMainMenu(player);
                    return;
                }
                case "swap_interval": {
                    // No-op: interval is adjusted via ±5s arrows
                    break;
                }
                case "interval_minus": {
                    int current = plugin.getConfigManager().getSwapInterval();
                    boolean beta = plugin.getConfigManager().isBetaIntervalEnabled();
                    int minAllowed = beta ? 10 : plugin.getConfigManager().getMinSwapInterval();
                    int maxAllowed = plugin.getConfigManager().getSwapIntervalMax();
                    int val = Math.max(minAllowed, current - 5);
                    if (!beta) val = Math.min(maxAllowed, val);
                    plugin.getConfigManager().setSwapInterval(val);
                    if (plugin.getGameManager().isGameRunning()) plugin.getGameManager().refreshSwapSchedule();
                    break;
                }
                case "interval_plus": {
                    int current = plugin.getConfigManager().getSwapInterval();
                    boolean beta = plugin.getConfigManager().isBetaIntervalEnabled();
                    int minAllowed = beta ? 10 : plugin.getConfigManager().getMinSwapInterval();
                    int maxAllowed = plugin.getConfigManager().getSwapIntervalMax();
                    int val = current + 5;
                    val = Math.max(minAllowed, val);
                    if (!beta) val = Math.min(maxAllowed, val);
                    plugin.getConfigManager().setSwapInterval(val);
                    if (plugin.getGameManager().isGameRunning()) plugin.getGameManager().refreshSwapSchedule();
                    break;
                }
                case "random_swaps":
                    toggleRandomSwaps(player);
                    break;
                case "beta_intervals": {
                    boolean en = plugin.getConfigManager().isBetaIntervalEnabled();
                    plugin.getConfigManager().setBetaIntervalEnabled(!en);
                    break;
                }
                case "random_range": {
                    int min = plugin.getConfigManager().getMinSwapInterval();
                    int max = plugin.getConfigManager().getMaxSwapInterval();
                    int delta = event.isShiftClick() ? 15 : 5;
                    if (event.isLeftClick()) {
                        min = Math.max(1, Math.min(min + delta, max));
                        plugin.getConfig().set("swap.min_interval", min);
                    }
                    if (event.isRightClick()) {
                        max = Math.max(min, max - delta);
                        plugin.getConfig().set("swap.max_interval", max);
                    }
                    plugin.saveConfig();
                    break;
                }
                case "safe_swaps":
                    toggleSafeSwaps(player);
                    break;
                case "pause_disconnect": {
                    boolean enabled = plugin.getConfigManager().isPauseOnDisconnect();
                    plugin.getConfig().set("swap.pause_on_disconnect", !enabled);
                    plugin.saveConfig();
                    break;
                }
                case "hunter_swap": {
                    boolean enabled = plugin.getConfigManager().isHunterSwapEnabled();
                    plugin.getConfig().set("swap.hunter_swap.enabled", !enabled);
                    plugin.saveConfig();
                    break;
                }
                case "hunter_swap_interval": {
                    int val = plugin.getConfigManager().getHunterSwapInterval();
                    int delta = event.isShiftClick() ? 60 : 30;
                    if (event.isLeftClick()) val += delta;
                    if (event.isRightClick()) val -= delta;
                    val = Math.max(30, Math.min(600, val));
                    plugin.getConfig().set("swap.hunter_swap.interval", val);
                    plugin.saveConfig();
                    break;
                }
                case "hot_potato": {
                    boolean enabled = plugin.getConfigManager().isHotPotatoModeEnabled();
                    plugin.getConfig().set("swap.hot_potato_mode.enabled", !enabled);
                    plugin.saveConfig();
                    break;
                }
                case "single_sleep": {
                    boolean enabled = plugin.getConfigManager().isSinglePlayerSleepEnabled();
                    plugin.getConfigManager().setSinglePlayerSleepEnabled(!enabled);
                    player.sendMessage("§eSingle Player Sleep: " + (!enabled ? "§aEnabled" : "§cDisabled"));
                    break;
                }
                case "freeze_toggle": {
                    boolean enabled = plugin.getConfigManager().isFreezeMechanicEnabled();
                    plugin.getConfig().set("freeze_mechanic.enabled", !enabled);
                    plugin.saveConfig();
                    if (plugin.getGameManager().isGameRunning()) {
                        plugin.getGameManager().refreshFreezeMechanic();
                    }
                    break;
                }
                case "freeze_mode": {
                    String current = plugin.getConfigManager().getFreezeMode();
                    String next = switch (current.toUpperCase()) {
                        case "EFFECTS" -> "SPECTATOR";
                        case "SPECTATOR" -> "LIMBO";
                        case "LIMBO" -> "CAGE";
                        default -> "EFFECTS";
                    };
                    plugin.getConfig().set("freeze_mode", next);
                    plugin.saveConfig();
                    if (plugin.getGameManager().isGameRunning()) {
                        plugin.getGameManager().refreshFreezeMechanic();
                    }
                    break;
                }
                case "freeze_duration": {
                    int val = plugin.getConfigManager().getFreezeDurationTicks();
                    int delta = event.isShiftClick() ? 100 : 20;
                    if (event.isLeftClick()) val += delta;
                    if (event.isRightClick()) val -= delta;
                    val = Math.max(20, Math.min(20 * 60 * 5, val)); // 1s..5m
                    plugin.getConfig().set("freeze_mechanic.duration_ticks", val);
                    plugin.saveConfig();
                    if (plugin.getGameManager().isGameRunning()) {
                        plugin.getGameManager().refreshFreezeMechanic();
                    }
                    break;
                }
                case "freeze_check_interval": {
                    int val = plugin.getConfigManager().getFreezeCheckIntervalTicks();
                    int delta = event.isShiftClick() ? 20 : 5;
                    if (event.isLeftClick()) val += delta;
                    if (event.isRightClick()) val -= delta;
                    val = Math.max(1, Math.min(200, val));
                    plugin.getConfig().set("freeze_mechanic.check_interval_ticks", val);
                    plugin.saveConfig();
                    if (plugin.getGameManager().isGameRunning()) {
                        plugin.getGameManager().refreshFreezeMechanic();
                    }
                    break;
                }
                case "freeze_max_distance": {
                    int val = (int) Math.round(plugin.getConfigManager().getFreezeMaxDistance());
                    int delta = event.isShiftClick() ? 20 : 5;
                    if (event.isLeftClick()) val += delta;
                    if (event.isRightClick()) val -= delta;
                    val = Math.max(5, Math.min(256, val));
                    plugin.getConfig().set("freeze_mechanic.max_distance", val);
                    plugin.saveConfig();
                    if (plugin.getGameManager().isGameRunning()) {
                        plugin.getGameManager().refreshFreezeMechanic();
                    }
                    break;
                }
                case "tracker_toggle": {
                    boolean enabled = plugin.getConfigManager().isTrackerEnabled();
                    plugin.getConfigManager().setTrackerEnabled(!enabled);
                    if (!enabled) {
                        plugin.getTrackerManager().startTracking();
                        for (org.bukkit.entity.Player hunter : plugin.getGameManager().getHunters()) {
                            if (hunter.isOnline()) plugin.getTrackerManager().giveTrackingCompass(hunter);
                        }
                    } else {
                        plugin.getTrackerManager().stopTracking();
                    }
                    break;
                }
                case "force_swap":
                    plugin.getGameManager().triggerImmediateSwap();
                    break;
                case "force_hunter_shuffle":
                    plugin.getGameManager().triggerImmediateHunterSwap();
                    break;
                case "update_compasses":
                    plugin.getTrackerManager().updateAllHunterCompasses();
                    break;
                case "swap_min_interval": {
                    int min = plugin.getConfigManager().getMinSwapInterval();
                    if (event.isLeftClick()) min += event.isShiftClick() ? 15 : 5;
                    if (event.isRightClick()) min -= event.isShiftClick() ? 15 : 5;
                    int max = plugin.getConfigManager().getMaxSwapInterval();
                    min = Math.max(1, Math.min(min, max));
                    plugin.getConfig().set("swap.min_interval", min);
                    plugin.saveConfig();
                    break;
                }
                case "swap_max_interval": {
                    int max = plugin.getConfigManager().getMaxSwapInterval();
                    if (event.isLeftClick()) max += event.isShiftClick() ? 15 : 5;
                    if (event.isRightClick()) max -= event.isShiftClick() ? 15 : 5;
                    int min = plugin.getConfigManager().getMinSwapInterval();
                    max = Math.max(min, max);
                    plugin.getConfig().set("swap.max_interval", max);
                    plugin.saveConfig();
                    break;
                }
                case "jitter_stddev": {
                    int sd = (int) Math.round(plugin.getConfigManager().getJitterStdDev());
                    if (event.isLeftClick()) sd += event.isShiftClick() ? 5 : 1;
                    if (event.isRightClick()) sd -= event.isShiftClick() ? 5 : 1;
                    sd = Math.max(0, Math.min(600, sd));
                    plugin.getConfig().set("swap.jitter.stddev", sd);
                    plugin.saveConfig();
                    break;
                }
                case "jitter_clamp": {
                    boolean clamp = plugin.getConfigManager().isClampJitter();
                    plugin.getConfig().set("swap.jitter.clamp", !clamp);
                    plugin.saveConfig();
                    break;
                }
                case "grace_period": {
                    int gp = plugin.getConfigManager().getGracePeriodTicks();
                    if (event.isLeftClick()) gp += event.isShiftClick() ? 40 : 10;
                    if (event.isRightClick()) gp -= event.isShiftClick() ? 40 : 10;
                    gp = Math.max(0, Math.min(20 * 60, gp));
                    plugin.getConfig().set("swap.grace_period_ticks", gp);
                    plugin.saveConfig();
                    break;
                }
                case "safe_h_radius": {
                    int val = plugin.getConfigManager().getSafeSwapHorizontalRadius();
                    if (event.isLeftClick()) val += event.isShiftClick() ? 5 : 1;
                    if (event.isRightClick()) val -= event.isShiftClick() ? 5 : 1;
                    val = Math.max(1, Math.min(64, val));
                    plugin.getConfig().set("safe_swap.horizontal_radius", val);
                    plugin.saveConfig();
                    break;
                }
                case "safe_v_distance": {
                    int val = plugin.getConfigManager().getSafeSwapVerticalDistance();
                    if (event.isLeftClick()) val += event.isShiftClick() ? 5 : 1;
                    if (event.isRightClick()) val -= event.isShiftClick() ? 5 : 1;
                    val = Math.max(1, Math.min(64, val));
                    plugin.getConfig().set("safe_swap.vertical_distance", val);
                    plugin.saveConfig();
                    break;
                }
                case "dangerous_blocks": {
                    guiManager.openDangerousBlocksMenu(player);
                    break;
                }
                case "voice_chat": {
                    boolean enabled = plugin.getConfigManager().isVoiceChatIntegrationEnabled();
                    plugin.getConfigManager().setVoiceChatIntegrationEnabled(!enabled);
                    break;
                }
                case "mute_inactive_toggle": {
                    boolean enabled = plugin.getConfigManager().isMuteInactiveRunners();
                    plugin.getConfig().set("voice_chat.mute_inactive_runners", !enabled);
                    plugin.saveConfig();
                    break;
                }
                case "reset_all_settings": {
                    player.sendMessage("§c[Settings] Reset All Settings is not yet confirmed via GUI to avoid accidental resets.");
                    player.sendMessage("§7Use config.yml or ask to add a confirmation flow.");
                    break;
                }
                case "ui_actionbar_ticks": {
                    int val = plugin.getConfigManager().getActionBarUpdateTicks();
                    if (event.isLeftClick()) val += 5;
                    if (event.isRightClick()) val -= 5;
                    val = Math.max(1, Math.min(200, val));
                    plugin.getConfig().set("ui.update_ticks.actionbar", val);
                    plugin.saveConfig();
                    break;
                }
                case "ui_title_ticks": {
                    int val = plugin.getConfigManager().getTitleUpdateTicks();
                    if (event.isLeftClick()) val += event.isShiftClick() ? 5 : 1;
                    if (event.isRightClick()) val -= event.isShiftClick() ? 5 : 1;
                    val = Math.max(1, Math.min(200, val));
                    plugin.getConfig().set("ui.update_ticks.title", val);
                    plugin.saveConfig();
                    break;
                }
                case "set_limbo_here": {
                    org.bukkit.Location loc = player.getLocation();
                    plugin.getConfig().set("limbo.world", loc.getWorld().getName());
                    plugin.getConfig().set("limbo.x", loc.getX());
                    plugin.getConfig().set("limbo.y", loc.getY());
                    plugin.getConfig().set("limbo.z", loc.getZ());
                    plugin.saveConfig();
                    player.sendMessage("§aLimbo location updated.");
                    break;
                }
                case "set_spawn_here": {
                    org.bukkit.Location loc = player.getLocation();
                    plugin.getConfig().set("spawn.world", loc.getWorld().getName());
                    plugin.getConfig().set("spawn.x", loc.getX());
                    plugin.getConfig().set("spawn.y", loc.getY());
                    plugin.getConfig().set("spawn.z", loc.getZ());
                    plugin.saveConfig();
                    player.sendMessage("§aSpawn location updated.");
                    break;
                }
                case "beta_intervals_toggle": {
                    boolean enabled = plugin.getConfigManager().isBetaIntervalEnabled();
                    plugin.getConfigManager().setBetaIntervalEnabled(!enabled);
                    break;
                }
                case "apply_mode_default_toggle": {
                    boolean enabled = plugin.getConfigManager().getApplyDefaultOnModeSwitch();
                    plugin.getConfigManager().setApplyDefaultOnModeSwitch(!enabled);
                    break;
                }
                case "swap_interval_reset": {
                    plugin.getConfigManager().applyModeDefaultInterval(plugin.getCurrentMode());
                    break;
                }
                case "swap_interval_save_default": {
                    int curr = plugin.getConfigManager().getSwapInterval();
                    plugin.getConfigManager().setModeDefaultInterval(plugin.getCurrentMode(), curr);
                    break;
                }
                // duplicate routing cases removed (handled in early routing or above)
            }
        } else {
            // Handle timer visibility clocks and ±5s arrows (created without explicit IDs)
            String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
            if (name.equals("§e§lActive Runner Timer")) {
                cycleRunnerTimer(player);
            } else if (name.equals("§e§lWaiting Runner Timer")) {
                cycleWaitingTimer(player);
            } else if ("-5s".equals(name) || "+5s".equals(name)) {
                int current = plugin.getConfigManager().getSwapInterval();
                boolean beta = plugin.getConfigManager().isBetaIntervalEnabled();
                int min = beta ? 10 : plugin.getConfigManager().getMinSwapInterval();
                int max = plugin.getConfigManager().getSwapIntervalMax();
                int delta = "+5s".equals(name) ? 5 : -5;
                int val = current + delta;
                val = beta ? Math.max(min, val) : Math.max(min, Math.min(max, val));
                plugin.getConfigManager().setSwapInterval(val);
            }
        }

        // Refresh the settings menu
        guiManager.openSettingsMenu(player);
        
        // Update game state
        plugin.getGameManager().refreshSwapSchedule();
        plugin.getGameManager().refreshActionBar();
    }

    private void cycleRunnerTimer(Player player) {
        String current = plugin.getConfigManager().getRunnerTimerVisibility();
        String next = guiManager.getNextVisibility(current);
        plugin.getConfigManager().setRunnerTimerVisibility(next);
    }

    private void cycleWaitingTimer(Player player) {
        String current = plugin.getConfigManager().getWaitingTimerVisibility();
        String next = guiManager.getNextVisibility(current);
        plugin.getConfigManager().setWaitingTimerVisibility(next);
    }

    private String getButtonId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;

        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        // Primary key used by current GUI
        NamespacedKey sswKey = new NamespacedKey(plugin, "ssw_button_id");
        if (container.has(sswKey, PersistentDataType.STRING)) {
            return container.get(sswKey, PersistentDataType.STRING);
        }
        // Backward compatibility
        NamespacedKey legacyKey = new NamespacedKey(plugin, "button_id");
        if (container.has(legacyKey, PersistentDataType.STRING)) {
            return container.get(legacyKey, PersistentDataType.STRING);
        }
        return null;
    }

    private boolean isBackButton(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        String text = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(item.getItemMeta());
        if (text == null) return false;
        boolean looksLikeBack = text != null && text.toLowerCase().contains("back");
        // Support both older barrier-style and newer arrow-style back buttons
        return looksLikeBack && (item.getType() == Material.BARRIER || item.getType() == Material.ARROW);
    }

    // Helper methods for settings actions

    private void toggleRandomSwaps(Player player) {
        boolean current = plugin.getConfigManager().isSwapRandomized();
        plugin.getConfigManager().setSwapRandomized(!current);
    }

    private void toggleSafeSwaps(Player player) {
        boolean current = plugin.getConfigManager().isSafeSwapEnabled();
        plugin.getConfigManager().setSafeSwapEnabled(!current);
    }

    // Other menu handlers
    private void handleTeamSelectorClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        // Handle clear teams button by ID
        String id = getButtonId(clicked);
        if ("clear_teams".equals(id)) {
            plugin.getGameManager().setRunners(new java.util.ArrayList<>());
            plugin.getGameManager().setHunters(new java.util.ArrayList<>());
            player.sendMessage("§aCleared all teams.");
            guiManager.openTeamSelector(player);
            return;
        }

        String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());

        // Select assignment team - check mode restrictions
        com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode currentMode = plugin.getCurrentMode();
        if (name != null) {
            if (name.contains("§b§lRunners")) {
                guiManager.setPlayerTeam(player, com.example.speedrunnerswap.models.Team.RUNNER);
                return;
            }
            if (name.contains("§c§lHunters")) {
                // Only allow hunter selection in Dream mode
                if (currentMode != com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.TASK) {
                    guiManager.setPlayerTeam(player, com.example.speedrunnerswap.models.Team.HUNTER);
                } else {
                    player.sendMessage("§cHunters are not available in Task Manager mode!");
                }
                return;
            }
        }

        // Assign a player by clicking their head, using current selected team
        if (clicked.getType() == Material.PLAYER_HEAD && clicked.getItemMeta() != null) {
            String targetName = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
            // displayName might be colored; strip any color prefixes/suffixes
            targetName = targetName.replace("§b", "").replace("§c", "").replace("§f", "").replace("§r", "");
            Player target = Bukkit.getPlayerExact(targetName);
            if (target == null) {
                player.sendMessage("§cPlayer not found or offline: " + targetName);
                return;
            }

            com.example.speedrunnerswap.models.Team selected = guiManager.getSelectedTeam(player);
            if (selected == com.example.speedrunnerswap.models.Team.NONE) {
                player.sendMessage("§eSelect a team (Runners/Hunters) first.");
                return;
            }

            // Build updated team lists
            java.util.List<Player> newRunners = new java.util.ArrayList<>(plugin.getGameManager().getRunners());
            java.util.List<Player> newHunters = new java.util.ArrayList<>(plugin.getGameManager().getHunters());
            newRunners.remove(target);
            newHunters.remove(target);
            if (selected == com.example.speedrunnerswap.models.Team.RUNNER) {
                newRunners.add(target);
            } else if (selected == com.example.speedrunnerswap.models.Team.HUNTER) {
                newHunters.add(target);
            }

            // Apply to runtime and config
            plugin.getGameManager().setRunners(newRunners);
            plugin.getGameManager().setHunters(newHunters);

            // Refresh GUI to reflect changes
            guiManager.openTeamSelector(player);
        }
    }

    // removed duplicate; implemented below
    

    private void handlePowerUpsMenuClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());

        if ("§e§lToggle Power-ups".equals(name)) {
            boolean current = plugin.getConfigManager().isPowerUpsEnabled();
            plugin.getConfigManager().setPowerUpsEnabled(!current);
            guiManager.openPowerUpsMenu(player);
            return;
        }

        if (name.contains("Positive Effects")) {
            guiManager.openPositiveEffectsMenu(player);
            return;
        }
        if (name.contains("Negative Effects")) {
            guiManager.openNegativeEffectsMenu(player);
            return;
        }
        if (name.contains("Effect Durations")) {
            guiManager.openPowerUpDurationsMenu(player);
            return;
        }
        // Other clicks: ignore safely
    }

    // Recognize and handle clicks inside the Power-up Durations menu
    // Title: "§6§lPower-up Durations"
    private boolean maybeHandlePowerUpDurationsMenu(InventoryClickEvent event, String title) {
        if (!title.contains("Power-up Durations")) return false;
        handlePowerUpDurationsClick(event);
        return true;
    }

    private boolean maybeHandleDangerousBlocksMenu(InventoryClickEvent event, String title) {
        if (!title.contains("Dangerous Blocks")) return false;
        handleDangerousBlocksClick(event);
        return true;
    }

    private void handlePowerUpDurationsClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());

        boolean changed = false;
        switch (name) {
            case "§e§lMin Duration (s)" -> {
                int val = plugin.getConfigManager().getPowerUpsMinSeconds();
                if (event.isLeftClick()) val += 5;
                if (event.isRightClick()) val -= 5;
                val = Math.max(1, Math.min(600, val));
                plugin.getConfigManager().setPowerUpsMinSeconds(val);
                changed = true;
            }
            case "§e§lMax Duration (s)" -> {
                int val = plugin.getConfigManager().getPowerUpsMaxSeconds();
                if (event.isLeftClick()) val += 5;
                if (event.isRightClick()) val -= 5;
                val = Math.max(1, Math.min(1800, val));
                plugin.getConfigManager().setPowerUpsMaxSeconds(val);
                changed = true;
            }
            case "§e§lMin Level" -> {
                int val = plugin.getConfigManager().getPowerUpsMinLevel();
                if (event.isLeftClick()) val += 1;
                if (event.isRightClick()) val -= 1;
                val = Math.max(1, Math.min(5, val));
                plugin.getConfigManager().setPowerUpsMinLevel(val);
                changed = true;
            }
            case "§e§lMax Level" -> {
                int val = plugin.getConfigManager().getPowerUpsMaxLevel();
                if (event.isLeftClick()) val += 1;
                if (event.isRightClick()) val -= 1;
                val = Math.max(1, Math.min(5, val));
                plugin.getConfigManager().setPowerUpsMaxLevel(val);
                changed = true;
            }
            default -> {}
        }
        if (isBackButton(clicked)) {
            guiManager.openPowerUpsMenu(player);
        } else if (changed) {
            guiManager.openPowerUpDurationsMenu(player);
        }
    }

    private void handleDangerousBlocksClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
        if (isBackButton(clicked)) {
            guiManager.openSettingsMenu(player);
            return;
        }
        try {
            org.bukkit.Material mat = org.bukkit.Material.matchMaterial(name.replace("§e", "").trim());
            if (mat != null) {
                java.util.Set<org.bukkit.Material> set = plugin.getConfigManager().getDangerousBlocks();
                if (set.contains(mat)) {
                    // Remove and persist back to config list
                    set.remove(mat);
                    java.util.List<String> list = new java.util.ArrayList<>();
                    for (org.bukkit.Material m : set) list.add(m.name());
                    plugin.getConfig().set("safe_swap.dangerous_blocks", list);
                    plugin.saveConfig();
                }
                guiManager.openDangerousBlocksMenu(player);
            }
        } catch (Exception ignored) {}
    }

    private void handleWorldBorderClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
        boolean changed = false;

        switch (name) {
            case "§e§lToggle World Border" -> {
                boolean enabled = plugin.getConfig().getBoolean("world_border.enabled", true);
                // Flip
                plugin.getConfig().set("world_border.enabled", !enabled);
                plugin.saveConfig();
                // Hook start/stop
                if (!enabled) {
                    plugin.getWorldBorderManager().startBorderShrinking();
                } else {
                    plugin.getWorldBorderManager().stopBorderShrinking();
                }
                changed = true;
            }
            case "§a§lInitial Border Size" -> {
                int val = plugin.getConfig().getInt("world_border.initial_size", 2000);
                if (event.isLeftClick()) val += event.isShiftClick() ? 500 : 100;
                if (event.isRightClick()) val -= event.isShiftClick() ? 500 : 100;
                val = Math.max(100, Math.min(29999984, val));
                plugin.getConfig().set("world_border.initial_size", val);
                plugin.saveConfig();
                changed = true;
            }
            case "§c§lFinal Border Size" -> {
                int val = plugin.getConfig().getInt("world_border.final_size", 100);
                if (event.isLeftClick()) val += event.isShiftClick() ? 100 : 50;
                if (event.isRightClick()) val -= event.isShiftClick() ? 100 : 50;
                val = Math.max(50, Math.min(1000, val));
                plugin.getConfig().set("world_border.final_size", val);
                plugin.saveConfig();
                changed = true;
            }
            case "§6§lShrink Duration" -> {
                int val = plugin.getConfig().getInt("world_border.shrink_duration", 1800);
                if (event.isLeftClick()) val += event.isShiftClick() ? 900 : 300;
                if (event.isRightClick()) val -= event.isShiftClick() ? 900 : 300;
                val = Math.max(300, Math.min(7200, val));
                plugin.getConfig().set("world_border.shrink_duration", val);
                plugin.saveConfig();
                changed = true;
            }
            case "§e§lWarning Settings" -> {
                int dist = plugin.getConfig().getInt("world_border.warning_distance", 50);
                int interval = plugin.getConfig().getInt("world_border.warning_interval", 300);
                if (event.isLeftClick()) dist += event.isShiftClick() ? 50 : 10;
                if (event.isRightClick()) dist -= event.isShiftClick() ? 50 : 10;
                if (event.isShiftClick()) {
                    // While shift-clicking, also tweak interval for convenience
                    if (event.isLeftClick()) interval += 30;
                    if (event.isRightClick()) interval -= 30;
                }
                dist = Math.max(5, Math.min(1000, dist));
                interval = Math.max(30, Math.min(3600, interval));
                plugin.getConfig().set("world_border.warning_distance", dist);
                plugin.getConfig().set("world_border.warning_interval", interval);
                plugin.saveConfig();
                changed = true;
            }
            default -> {}
        }

        if (changed) {
            guiManager.openWorldBorderMenu(player);
        }
    }

    private void handleKitsMenuClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());

        switch (name) {
            case "§e§lKits: §aEnabled", "§e§lKits: §cDisabled" -> {
                boolean enabled = plugin.getConfigManager().isKitsEnabled();
                plugin.getConfigManager().setKitsEnabled(!enabled);
                guiManager.openKitsMenu(player);
            }
            case "§b§lGive Runner Kit" -> plugin.getKitManager().applyRunnerKit(player);
            case "§c§lGive Hunter Kit" -> plugin.getKitManager().applyHunterKit(player);
            case "§b§lEdit Runner Kit" -> guiManager.openKitEditor(player, "runner");
            case "§c§lEdit Hunter Kit" -> guiManager.openKitEditor(player, "hunter");
            default -> {}
        }
    }

    private void handleBountyMenuClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());

        if (name.contains("Bounty Status")) {
            boolean enabled = plugin.getConfig().getBoolean("bounty.enabled", true);
            plugin.getConfig().set("bounty.enabled", !enabled);
            plugin.saveConfig();
            guiManager.openBountyMenu(player);
            return;
        }
        if (name.equals("§e§lAssign New Bounty")) {
            plugin.getBountyManager().assignNewBounty();
            guiManager.openBountyMenu(player);
            return;
        }
        if (name.equals("§c§lClear Bounty")) {
            plugin.getBountyManager().clearBounty();
            guiManager.openBountyMenu(player);
            return;
        }
        if (name.equals("§6§lBounty Cooldown (s)")) {
            int val = plugin.getConfig().getInt("bounty.cooldown", 300);
            if (event.isLeftClick()) val += 30;
            if (event.isRightClick()) val -= 30;
            val = Math.max(30, Math.min(3600, val));
            plugin.getConfig().set("bounty.cooldown", val);
            plugin.saveConfig();
            guiManager.openBountyMenu(player);
            return;
        }
        if (name.equals("§e§lGlow Duration (s)")) {
            int val = plugin.getConfig().getInt("bounty.glow_duration", 300);
            if (event.isLeftClick()) val += 30;
            if (event.isRightClick()) val -= 30;
            val = Math.max(30, Math.min(3600, val));
            plugin.getConfig().set("bounty.glow_duration", val);
            plugin.saveConfig();
            guiManager.openBountyMenu(player);
            return;
        }
        if (name.equals("§e§lReward Strength (s)")) {
            int val = plugin.getConfig().getInt("bounty.rewards.strength_duration", 300);
            if (event.isLeftClick()) val += 30;
            if (event.isRightClick()) val -= 30;
            val = Math.max(30, Math.min(3600, val));
            plugin.getConfig().set("bounty.rewards.strength_duration", val);
            plugin.saveConfig();
            guiManager.openBountyMenu(player);
            return;
        }
        if (name.equals("§e§lReward Speed (s)")) {
            int val = plugin.getConfig().getInt("bounty.rewards.speed_duration", 300);
            if (event.isLeftClick()) val += 30;
            if (event.isRightClick()) val -= 30;
            val = Math.max(30, Math.min(3600, val));
            plugin.getConfig().set("bounty.rewards.speed_duration", val);
            plugin.saveConfig();
            guiManager.openBountyMenu(player);
            return;
        }
    }

    private void handleLastStandClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        
        String buttonId = getButtonId(clicked);
        if (buttonId != null) {
            switch (buttonId) {
                case "toggle_last_stand" -> {
                    boolean enabled = plugin.getConfigManager().isLastStandEnabled();
                    plugin.getConfig().set("last_stand.enabled", !enabled);
                    plugin.saveConfig();
                }
                case "last_stand_threshold" -> {
                    int threshold = plugin.getConfig().getInt("last_stand.health_threshold", 4);
                    if (event.isLeftClick()) threshold++;
                    if (event.isRightClick()) threshold--;
                    threshold = Math.max(1, Math.min(20, threshold));
                    plugin.getConfig().set("last_stand.health_threshold", threshold);
                    plugin.saveConfig();
                }
                case "last_stand_duration" -> {
                    int duration = plugin.getConfig().getInt("last_stand.duration", 30);
                    if (event.isLeftClick()) duration += 5;
                    if (event.isRightClick()) duration -= 5;
                    duration = Math.max(5, Math.min(300, duration));
                    plugin.getConfig().set("last_stand.duration", duration);
                    plugin.saveConfig();
                }
                case "last_stand_strength" -> {
                    int amp = plugin.getConfigManager().getLastStandStrengthAmplifier();
                    if (event.isLeftClick()) amp++;
                    if (event.isRightClick()) amp--;
                    amp = Math.max(0, Math.min(5, amp));
                    plugin.getConfig().set("last_stand.strength_amplifier", amp);
                    plugin.saveConfig();
                }
                case "last_stand_speed" -> {
                    int amp = plugin.getConfigManager().getLastStandSpeedAmplifier();
                    if (event.isLeftClick()) amp++;
                    if (event.isRightClick()) amp--;
                    amp = Math.max(0, Math.min(5, amp));
                    plugin.getConfig().set("last_stand.speed_amplifier", amp);
                    plugin.saveConfig();
                }
            }
            guiManager.openLastStandMenu(player);
            return;
        }
        
        // Fallback to old display name handling
        String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
        switch (name) {
            case "§e§lLast Stand: §aEnabled", "§e§lLast Stand: §cDisabled" -> {
                boolean enabled = plugin.getConfigManager().isLastStandEnabled();
                plugin.getConfig().set("last_stand.enabled", !enabled);
                plugin.saveConfig();
                guiManager.openLastStandMenu(player);
            }
            case "§6§lLast Stand Duration" -> {
                int duration = plugin.getConfigManager().getLastStandDuration();
                if (event.isLeftClick()) duration += 100;
                if (event.isRightClick()) duration -= 100;
                duration = Math.max(100, duration);
                plugin.getConfig().set("last_stand.duration_ticks", duration);
                plugin.saveConfig();
                guiManager.openLastStandMenu(player);
            }
            case "§e§lStrength Amplifier" -> {
                int amp = plugin.getConfigManager().getLastStandStrengthAmplifier();
                if (event.isLeftClick()) amp++;
                if (event.isRightClick()) amp--;
                amp = Math.max(0, Math.min(5, amp));
                plugin.getConfig().set("last_stand.strength_amplifier", amp);
                plugin.saveConfig();
                guiManager.openLastStandMenu(player);
            }
            case "§b§lSpeed Amplifier" -> {
                int amp = plugin.getConfigManager().getLastStandSpeedAmplifier();
                if (event.isLeftClick()) amp++;
                if (event.isRightClick()) amp--;
                amp = Math.max(0, Math.min(5, amp));
                plugin.getConfig().set("last_stand.speed_amplifier", amp);
                plugin.saveConfig();
                guiManager.openLastStandMenu(player);
            }
            default -> {}
        }
    }

    private void handleCompassSettingsClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());

        switch (name) {
            case "§e§lCompass Jamming: §aEnabled", "§e§lCompass Jamming: §cDisabled" -> {
                boolean enabled = plugin.getConfigManager().isCompassJammingEnabled();
                // Write both paths for robustness
                plugin.getConfig().set("tracker.compass_jamming.enabled", !enabled);
                plugin.getConfig().set("sudden_death.compass_jamming.enabled", !enabled);
                plugin.saveConfig();
                guiManager.openCompassSettingsMenu(player);
            }
            case "§6§lJam Duration (ticks)" -> {
                int duration = plugin.getConfigManager().getCompassJamDuration();
                if (event.isLeftClick()) duration += 20;
                if (event.isRightClick()) duration -= 20;
                duration = Math.max(20, duration);
                plugin.getConfig().set("tracker.compass_jamming.duration_ticks", duration);
                plugin.getConfig().set("sudden_death.compass_jamming.duration_ticks", duration);
                plugin.saveConfig();
                guiManager.openCompassSettingsMenu(player);
            }
            case "§e§lSet End Portal Hint (this world)" -> {
                plugin.getConfigManager().setEndPortalHint(player.getWorld(), player.getLocation());
                player.sendMessage("§aSet End Portal hint for world §f" + player.getWorld().getName());
                guiManager.openCompassSettingsMenu(player);
            }
            case "§c§lClear End Portal Hint (this world)" -> {
                plugin.getConfigManager().clearEndPortalHint(player.getWorld());
                player.sendMessage("§eCleared End Portal hint for world §f" + player.getWorld().getName());
                guiManager.openCompassSettingsMenu(player);
            }
            case "§6§lJam Max Distance (blocks)" -> {
                int dist = plugin.getConfigManager().getCompassJamMaxDistance();
                if (event.isLeftClick()) dist += 25;
                if (event.isRightClick()) dist -= 25;
                dist = Math.max(0, Math.min(100000, dist));
                plugin.getConfigManager().setCompassJamMaxDistance(dist);
                guiManager.openCompassSettingsMenu(player);
            }
            default -> {}
        }
    }

    private void handleSuddenDeathClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        
        String buttonId = getButtonId(clicked);
        if (buttonId != null) {
            switch (buttonId) {
                case "toggle_sudden_death" -> {
                    boolean enabled = plugin.getConfig().getBoolean("sudden_death.enabled", false);
                    plugin.getConfig().set("sudden_death.enabled", !enabled);
                    plugin.saveConfig();
                }
                case "sudden_death_time" -> {
                    int time = plugin.getConfig().getInt("sudden_death.trigger_time", 1800);
                    int delta = 5 * 60; // 5 minutes
                    if (event.isLeftClick()) time += delta;
                    if (event.isRightClick()) time -= delta;
                    time = Math.max(300, Math.min(21600, time)); // 5 min to 6 hours
                    plugin.getConfig().set("sudden_death.trigger_time", time);
                    plugin.saveConfig();
                }
                case "sudden_death_no_regen" -> {
                    boolean noRegen = plugin.getConfig().getBoolean("sudden_death.no_regen", true);
                    plugin.getConfig().set("sudden_death.no_regen", !noRegen);
                    plugin.saveConfig();
                }
                case "sudden_death_one_hit" -> {
                    boolean oneHit = plugin.getConfig().getBoolean("sudden_death.one_hit_kill", false);
                    plugin.getConfig().set("sudden_death.one_hit_kill", !oneHit);
                    plugin.saveConfig();
                }
                case "activate_sudden_death_now" -> {
                    plugin.getSuddenDeathManager().activateSuddenDeath();
                    player.sendMessage("§4Sudden Death activated!");
                }
                case "cancel_sudden_death" -> {
                    plugin.getSuddenDeathManager().cancelSchedule();
                    player.sendMessage("§eScheduled Sudden Death cancelled.");
                }
                case "schedule_sudden_death" -> {
                    plugin.getSuddenDeathManager().scheduleSuddenDeath();
                    player.sendMessage("§eSudden Death scheduled.");
                }
            }
            guiManager.openSuddenDeathMenu(player);
            return;
        }
        
        // Fallback to old display name handling 
        String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
        switch (name) {
            case "§4§lSudden Death: §aActive" -> {
                // Toggle off
                plugin.getSuddenDeathManager().deactivate();
            }
            case "§4§lSudden Death: §cInactive" -> {
                // No immediate action; user can Schedule or Activate explicitly
            }
            case "§e§lCancel Scheduled Sudden Death" -> plugin.getSuddenDeathManager().cancelSchedule();
            case "§e§lSchedule Sudden Death" -> plugin.getSuddenDeathManager().scheduleSuddenDeath();
            case "§c§lActivate Now" -> plugin.getSuddenDeathManager().activateSuddenDeath();
            case "§6§lActivation Delay (minutes)" -> {
                long seconds = plugin.getConfig().getLong("sudden_death.activation_delay", 1200);
                long delta = 5 * 60L;
                if (event.isLeftClick()) seconds += delta;
                if (event.isRightClick()) seconds -= delta;
                long min = 5 * 60L; // 5 minutes
                long max = 360 * 60L; // 6 hours
                seconds = Math.max(min, Math.min(max, seconds));
                plugin.getConfig().set("sudden_death.activation_delay", seconds);
                plugin.saveConfig();
            }
            default -> {}
        }
        // Refresh
        guiManager.openSuddenDeathMenu(player);
    }

    private void handleKitEditorClick(InventoryClickEvent event, String title) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
        if (!name.equals("§a§lSave Kit")) return;

        // Extract kit type from title: "§e§lEdit <kit> Kit"
        String plainTitle = getPlainTitle(event.getView());
        String kitType = "runner";
        int idxEdit = plainTitle.indexOf("Edit ");
        int idxKit = plainTitle.indexOf(" Kit");
        if (idxEdit >= 0 && idxKit > idxEdit + 5) {
            kitType = plainTitle.substring(idxEdit + 5, idxKit).trim().toLowerCase();
        }

        // Gather contents (first 45 slots), armor in 45..48 -> [helmet, chest, legs, boots]
        org.bukkit.inventory.Inventory top = event.getView().getTopInventory();
        ItemStack[] contents = new ItemStack[45];
        for (int i = 0; i < 45; i++) contents[i] = top.getItem(i);

        ItemStack[] armor = new ItemStack[4];
        ItemStack helmet = top.getItem(45);
        ItemStack chest = top.getItem(46);
        ItemStack legs = top.getItem(47);
        ItemStack boots = top.getItem(48);
        // Order expected by KitConfigManager.saveKit: [boots, leggings, chestplate, helmet]
        armor[0] = boots;
        armor[1] = legs;
        armor[2] = chest;
        armor[3] = helmet;

        // Persist to kits.yml via KitConfigManager
        plugin.getKitConfigManager().saveKit(kitType, contents, armor);
        player.sendMessage("§aSaved " + kitType + " kit.");

        // Return to kits menu
        guiManager.openKitsMenu(player);
    }

    private void handleCustomTasksClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
        
        if (name.equals("§7§lBack")) {
            guiManager.openSettingsMenu(player);
        } else if (name.startsWith("§e§lInclude Default Tasks:")) {
            boolean current = plugin.getConfig().getBoolean("task_manager.include_default_tasks", true);
            plugin.getConfig().set("task_manager.include_default_tasks", !current);
            plugin.saveConfig();
            var taskMode = plugin.getTaskManagerMode();
            if (taskMode != null) {
                taskMode.reloadTasks();
            }
            guiManager.openCustomTasksMenu(player);
        } else if (name.equals("§a§lAdd New Task")) {
            guiManager.promptTaskInput(player, "id");
        } else if (name.equals("§6§lReload Tasks")) {
            var taskMode = plugin.getTaskManagerMode();
            if (taskMode != null) {
                taskMode.reloadTasks();
                player.sendMessage("§a[Task Manager] Tasks reloaded from config!");
            }
            guiManager.openCustomTasksMenu(player);
        } else if (name.startsWith("§e")) {
            // Custom task item - remove it
            String taskId = name.substring(2); // Remove color code
            var taskMode = plugin.getTaskManagerMode();
            if (taskMode != null && taskMode.removeCustomTask(taskId)) {
                player.sendMessage("§e[Task Manager] Removed task: " + taskId);
            }
            guiManager.openCustomTasksMenu(player);
        }
    }
    
    private void handleAdvancedConfigClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String id = getButtonId(clicked);
        if (id == null) return;

        try {
            if ("back_settings".equals(id)) {
                guiManager.openSettingsMenu(player);
                return;
            }
            if ("advanced_config_root".equals(id)) {
                guiManager.openAdvancedConfigMenu(player, "", 0);
                return;
            }
            if (id.startsWith("cfg:nav:")) {
                String path = id.substring("cfg:nav:".length());
                guiManager.openAdvancedConfigMenu(player, path, 0);
                return;
            }
            if (id.startsWith("cfg:page:")) {
                String rest = id.substring("cfg:page:".length());
                int idx = rest.lastIndexOf(':');
                String path = idx >= 0 ? rest.substring(0, idx) : "";
                int page = idx >= 0 ? Integer.parseInt(rest.substring(idx + 1)) : 0;
                guiManager.openAdvancedConfigMenu(player, path, page);
                return;
            }
            if (id.startsWith("cfg:bool:")) {
                String path = id.substring("cfg:bool:".length());
                boolean cur = plugin.getConfig().getBoolean(path, false);
                plugin.getConfig().set(path, !cur);
                plugin.saveConfig();
                guiManager.openAdvancedConfigMenu(player, getParentPath(path), 0);
                return;
            }
            if (id.startsWith("cfg:num:")) {
                String path = id.substring("cfg:num:".length());
                Object val = plugin.getConfig().get(path);
                // Use double for general numeric handling; store as int if original is integer-like
                double num = 0;
                if (val instanceof Number n) num = n.doubleValue();
                double delta = event.isShiftClick() ? 10 : 1;
                if (event.isLeftClick()) num += delta;
                if (event.isRightClick()) num -= delta;
                // If original was integer-like, round to nearest int
                if (val instanceof Integer || (Math.rint(num) == num)) {
                    plugin.getConfig().set(path, (int) Math.round(num));
                } else {
                    plugin.getConfig().set(path, num);
                }
                plugin.saveConfig();
                guiManager.openAdvancedConfigMenu(player, getParentPath(path), 0);
                return;
            }
            if (id.startsWith("cfg:str:")) {
                String path = id.substring("cfg:str:".length());
                plugin.getChatInputHandler().expectConfigString(player, path);
                player.closeInventory();
                player.sendMessage("§e[Config] Enter new value for §f" + path + " §7(type 'cancel' to abort)");
                return;
            }
            if (id.startsWith("cfg:list:")) {
                String path = id.substring("cfg:list:".length());
                guiManager.openConfigListEditor(player, path, 0);
                return;
            }
        } catch (Exception ex) {
            player.sendMessage("§c[Config] Error: " + ex.getMessage());
        }
    }

    private void handleConfigListClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String id = getButtonId(clicked);
        if (id == null) return;

        try {
            if (id.startsWith("cfg:nav:")) {
                String parent = id.substring("cfg:nav:".length());
                guiManager.openAdvancedConfigMenu(player, parent == null ? "" : parent, 0);
                return;
            }
            if (id.startsWith("cfg:list_add:")) {
                String path = id.substring("cfg:list_add:".length());
                plugin.getChatInputHandler().expectConfigListAdd(player, path);
                player.closeInventory();
                player.sendMessage("§e[Config] Enter list item to add to §f" + path + " §7(type 'cancel' to abort)");
                return;
            }
            if (id.startsWith("cfg:list_del:")) {
                String rest = id.substring("cfg:list_del:".length());
                int idx = rest.lastIndexOf(':');
                String path = rest.substring(0, idx);
                int index = Integer.parseInt(rest.substring(idx + 1));
                java.util.List<String> list = plugin.getConfig().getStringList(path);
                if (index >= 0 && index < list.size()) {
                    list.remove(index);
                    plugin.getConfig().set(path, list);
                    plugin.saveConfig();
                }
                guiManager.openConfigListEditor(player, path, 0);
                return;
            }
            if (id.startsWith("cfg:list_page:")) {
                String rest = id.substring("cfg:list_page:".length());
                int idx = rest.lastIndexOf(':');
                String path = rest.substring(0, idx);
                int page = Integer.parseInt(rest.substring(idx + 1));
                guiManager.openConfigListEditor(player, path, page);
                return;
            }
        } catch (Exception ex) {
            player.sendMessage("§c[Config] Error: " + ex.getMessage());
        }
    }

    private String getParentPath(String path) {
        if (path == null || path.isEmpty()) return "";
        int idx = path.lastIndexOf('.')
        ;
        return idx < 0 ? "" : path.substring(0, idx);
    }
    
    private void handleTaskSettingsClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        // Handle button IDs first (new actions)
        String buttonId = getButtonId(clicked);
        if (buttonId != null) {
            switch (buttonId) {
                case "reroll_tasks" -> {
                    if (plugin.getGameManager().isGameRunning()) {
                        player.sendMessage("§cYou can only reroll before the game starts.");
                        return;
                    }
                    if (plugin.getCurrentMode() != com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.TASK) {
                        player.sendMessage("§cSwitch to Task Manager mode first: /swap mode task");
                        return;
                    }
                    var tmm = plugin.getTaskManagerMode();
                    if (tmm == null) {
                        player.sendMessage("§cTask Manager not initialized.");
                        return;
                    }
                    java.util.List<org.bukkit.entity.Player> selectedRunners = new java.util.ArrayList<>();
                    for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                        var st = plugin.getGameManager().getPlayerState(p);
                        if (st != null && st.getSelectedTeam() == com.example.speedrunnerswap.models.Team.RUNNER) selectedRunners.add(p);
                    }
                    if (selectedRunners.isEmpty()) {
                        player.sendMessage("§cNo selected runners found. Use the Team Selector first.");
                        return;
                    }
                    tmm.assignAndAnnounceTasks(selectedRunners);
                    player.sendMessage("§aRerolled tasks for §f" + selectedRunners.size() + "§a selected runners.");
                    // Refresh the menu to reflect disabled/enabled state
                    guiManager.openTaskSettingsMenu(player);
                    return;
                }
                case "show_assignments" -> {
                    var tmm = plugin.getTaskManagerMode();
                    if (tmm == null) {
                        player.sendMessage("§cTask Manager not initialized.");
                        return;
                    }
                    var map = tmm.getAssignments();
                    if (map.isEmpty()) {
                        player.sendMessage("§7No task assignments.");
                        return;
                    }
                    player.sendMessage("§6Task Assignments:");
                    for (var e : map.entrySet()) {
                        java.util.UUID uuid = e.getKey();
                        String taskId = e.getValue();
                        String pname = plugin.getServer().getOfflinePlayer(uuid).getName();
                        if (pname == null) pname = uuid.toString().substring(0, 8);
                        var def = tmm.getTask(taskId);
                        String desc = def != null ? def.description() : taskId;
                        player.sendMessage("§e" + pname + "§7: §f" + desc + " (§8" + taskId + "§7)");
                    }
                    return;
                }
                default -> {}
            }
        }

        String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());

        switch (name) {
            case "§7§lBack" -> guiManager.openTaskManagerMenu(player);
            case "§e§lPause On Disconnect: §aEnabled", "§e§lPause On Disconnect: §cDisabled" -> {
                boolean cur = plugin.getConfig().getBoolean("task_manager.pause_on_disconnect", true);
                plugin.getConfig().set("task_manager.pause_on_disconnect", !cur);
                plugin.saveConfig();
            }
            case "§e§lRemove On Timeout: §aYes", "§e§lRemove On Timeout: §cNo" -> {
                boolean cur = plugin.getConfig().getBoolean("task_manager.remove_on_timeout", true);
                plugin.getConfig().set("task_manager.remove_on_timeout", !cur);
                plugin.saveConfig();
            }
            case "§e§lAllow Late Joiners: §aYes", "§e§lAllow Late Joiners: §cNo" -> {
                boolean cur = plugin.getConfig().getBoolean("task_manager.allow_late_joiners", false);
                plugin.getConfig().set("task_manager.allow_late_joiners", !cur);
                plugin.saveConfig();
            }
            case "§e§lEnd When One Left: §aYes", "§e§lEnd When One Left: §cNo" -> {
                boolean cur = plugin.getConfig().getBoolean("task_manager.end_when_one_left", false);
                plugin.getConfig().set("task_manager.end_when_one_left", !cur);
                plugin.saveConfig();
            }
            case "§6§lRejoin Grace (s)" -> {
                int val = plugin.getConfig().getInt("task_manager.rejoin_grace_seconds", 180);
                int delta = event.isShiftClick() ? 30 : 10;
                if (event.isLeftClick()) val += delta;
                if (event.isRightClick()) val -= delta;
                val = Math.max(10, Math.min(3600, val));
                plugin.getConfig().set("task_manager.rejoin_grace_seconds", val);
                plugin.saveConfig();
            }
            case "§6§lReload Tasks" -> {
                var tmm = plugin.getTaskManagerMode();
                if (tmm != null) tmm.reloadTasks();
                player.sendMessage("[Task Manager] Reloaded tasks.");
            }
            default -> {}
        }
        guiManager.openTaskSettingsMenu(player);
    }

    private void handleDreamSettingsClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
        switch (name) {
            case "§7§lBack" -> guiManager.openDreamMenu(player);
            case "§e§lTracker: §aEnabled", "§e§lTracker: §cDisabled" -> {
                boolean en = plugin.getConfigManager().isTrackerEnabled();
                plugin.getConfigManager().setTrackerEnabled(!en);
            }
            case "§e§lSingle Player Sleep: §aEnabled", "§e§lSingle Player Sleep: §cDisabled" -> {
                boolean en = plugin.getConfigManager().isSinglePlayerSleepEnabled();
                plugin.getConfigManager().setSinglePlayerSleepEnabled(!en);
            }
            default -> {}
        }
        guiManager.openDreamSettingsMenu(player);
    }

    private void handleStatisticsClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());

        switch (name) {
            case "§e§lDisplay Statistics" -> plugin.getStatsManager().displayStats();
            case "§a§lStart Tracking" -> plugin.getStatsManager().startTracking();
            case "§c§lStop Tracking" -> plugin.getStatsManager().stopTracking();
            case "§e§lStats: §aEnabled", "§e§lStats: §cDisabled" -> {
                boolean current = plugin.getConfig().getBoolean("stats.enabled", true);
                plugin.getConfig().set("stats.enabled", !current);
                plugin.saveConfig();
            }
            case "§e§lPeriodic Display: §aEnabled", "§e§lPeriodic Display: §cDisabled" -> {
                boolean current = plugin.getConfig().getBoolean("stats.periodic_display", false);
                plugin.getConfig().set("stats.periodic_display", !current);
                plugin.saveConfig();
            }
            case "§6§lPeriodic Interval (s)" -> {
                int val = plugin.getConfig().getInt("stats.periodic_display_interval", 300);
                if (event.isLeftClick()) val += 30;
                if (event.isRightClick()) val -= 30;
                val = Math.max(30, Math.min(3600, val));
                plugin.getConfig().set("stats.periodic_display_interval", val);
                plugin.saveConfig();
            }
            case "§6§lDistance Update (ticks)" -> {
                int val = plugin.getConfig().getInt("stats.distance_update_ticks", 20);
                if (event.isLeftClick()) val += 5;
                if (event.isRightClick()) val -= 5;
                val = Math.max(1, Math.min(200, val));
                plugin.getConfig().set("stats.distance_update_ticks", val);
                plugin.saveConfig();
            }
            default -> {}
        }
        guiManager.openStatisticsMenu(player);
    }

    private void handleEffectsMenuClick(InventoryClickEvent event, String title) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        java.util.List<String> lore = com.example.speedrunnerswap.utils.GuiCompat.getLore(clicked.getItemMeta());
        if (lore == null) return;
        String effectId = null;
        for (String text : lore) {
            int idx = text.indexOf("Effect ID:");
            if (idx >= 0) {
                // format: "Effect ID: <ID>"
                int split = text.indexOf(':');
                if (split >= 0 && split + 1 < text.length()) {
                    effectId = text.substring(split + 1).trim();
                    // Strip legacy color/formatting codes to get the raw ID
                    effectId = effectId.replaceAll("§[0-9A-FK-ORa-fk-or]", "");
                }
                break;
            }
        }
        if (effectId == null || effectId.isEmpty()) return;

        boolean positive = title.contains("Positive Effects");
        java.util.List<String> list = new java.util.ArrayList<>(positive
            ? plugin.getConfig().getStringList("power_ups.good_effects")
            : plugin.getConfig().getStringList("power_ups.bad_effects"));

        if (list.contains(effectId)) list.remove(effectId); else list.add(effectId);
        if (positive) plugin.getConfig().set("power_ups.good_effects", list);
        else plugin.getConfig().set("power_ups.bad_effects", list);
        plugin.saveConfig();

        // Refresh menu
        if (positive) guiManager.openPositiveEffectsMenu(player); else guiManager.openNegativeEffectsMenu(player);
    }
    
    private void handleBroadcastSettingsClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        
        String id = getButtonId(clicked);
        if (id != null) {
            switch (id) {
                case "back_settings" -> {
                    guiManager.openSettingsMenu(player);
                    return;
                }
                case "toggle_broadcasts" -> {
                    boolean current = plugin.getConfig().getBoolean("broadcasts.enabled", true);
                    plugin.getConfig().set("broadcasts.enabled", !current);
                    plugin.saveConfig();
                }
                case "toggle_game_events" -> {
                    boolean current = plugin.getConfig().getBoolean("broadcasts.game_events", true);
                    plugin.getConfig().set("broadcasts.game_events", !current);
                    plugin.saveConfig();
                }
                case "toggle_team_changes" -> {
                    boolean current = plugin.getConfig().getBoolean("broadcasts.team_changes", true);
                    plugin.getConfig().set("broadcasts.team_changes", !current);
                    plugin.saveConfig();
                }
            }
        }
        guiManager.openBroadcastSettingsMenu(player);
    }
    
    private void handleLimboSettingsClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        
        String id = getButtonId(clicked);
        if (id != null) {
            switch (id) {
                case "back_settings" -> {
                    guiManager.openSettingsMenu(player);
                    return;
                }
                case "limbo_world" -> {
                    plugin.getChatInputHandler().expectConfigString(player, "limbo.world");
                    player.closeInventory();
                    player.sendMessage("§e[Config] Enter world name for limbo §7(type 'cancel' to abort)");
                    return;
                }
                case "limbo_coords" -> {
                    plugin.getChatInputHandler().expectConfigString(player, "limbo.coords");
                    player.closeInventory();
                    player.sendMessage("§e[Config] Enter coordinates as 'x,y,z' §7(type 'cancel' to abort)");
                    return;
                }
                case "limbo_set_current" -> {
                    plugin.getConfig().set("limbo.world", player.getWorld().getName());
                    plugin.getConfig().set("limbo.x", player.getLocation().getX());
                    plugin.getConfig().set("limbo.y", player.getLocation().getY());
                    plugin.getConfig().set("limbo.z", player.getLocation().getZ());
                    plugin.saveConfig();
                    player.sendMessage("§aLimbo location set to your current position!");
                }
            }
        }
        guiManager.openLimboSettingsMenu(player);
    }
    
    private void handleUIPerformanceClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        
        String id = getButtonId(clicked);
        if (id != null) {
            switch (id) {
                case "back_settings" -> {
                    guiManager.openSettingsMenu(player);
                    return;
                }
                case "actionbar_rate" -> {
                    int current = plugin.getConfig().getInt("ui.update_ticks.actionbar", 20);
                    if (event.isLeftClick()) current += 5;
                    if (event.isRightClick()) current -= 5;
                    current = Math.max(1, Math.min(200, current));
                    plugin.getConfig().set("ui.update_ticks.actionbar", current);
                    plugin.saveConfig();
                }
                case "title_rate" -> {
                    int current = plugin.getConfig().getInt("ui.update_ticks.title", 10);
                    if (event.isLeftClick()) current += 5;
                    if (event.isRightClick()) current -= 5;
                    current = Math.max(1, Math.min(200, current));
                    plugin.getConfig().set("ui.update_ticks.title", current);
                    plugin.saveConfig();
                }
                case "tracker_rate" -> {
                    int current = plugin.getConfig().getInt("tracker.update_ticks", 20);
                    if (event.isLeftClick()) current += 5;
                    if (event.isRightClick()) current -= 5;
                    current = Math.max(1, Math.min(200, current));
                    plugin.getConfig().set("tracker.update_ticks", current);
                    plugin.saveConfig();
                }
                case "performance_info" -> {
                    player.sendMessage("§6=== UI Performance Tips ===");
                    player.sendMessage("§7Lower tick values = more responsive UI but higher CPU usage");
                    player.sendMessage("§7Higher tick values = less responsive UI but better performance");
                    player.sendMessage("§7Recommended range: 10-20 ticks (0.5-1 second)");
                    return;
                }
            }
        }
        guiManager.openUIPerformanceMenu(player);
    }
}
