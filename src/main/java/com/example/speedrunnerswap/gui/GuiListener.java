package com.example.speedrunnerswap.gui;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        // Always cancel clicks in our GUIs
        if (isPluginGui(title)) {
            event.setCancelled(true);

            // Admin permission check
            if (!player.hasPermission("speedrunnerswap.admin") && isAdminMenu(title)) {
                player.sendMessage(Component.text("§cYou do not have permission to use this menu."));
                return;
            }

            // Route to appropriate handler
            handleGuiClick(event, title);
        }
    }

    private boolean isPluginGui(String title) {
        if (title == null || title.isEmpty()) return false;
        // Include all plugin menus and the kit editor
        return title.contains("SpeedrunnerSwap") ||
               title.contains("Main Menu") ||
               title.contains("Team Selector") ||
               title.contains("Settings") ||
               title.contains("Kits") ||
               (title.contains("Edit ") && title.contains(" Kit")) ||
               title.contains("Effects") ||
               title.contains("Power-ups") ||
               title.contains("Power-up Durations") ||
               title.contains("World Border") ||
               title.contains("Bounty") ||
               title.contains("Last Stand") ||
               title.contains("Compass") ||
               title.contains("Sudden Death") ||
               title.contains("Statistics");
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
        
        // Handle back buttons first
        if (isBackButton(clickedItem)) {
            guiManager.openMainMenu(player);
            return;
        }

        // Special-case handling for submenus not covered by a general section
        if (maybeHandlePowerUpDurationsMenu(event, title)) return;

        // Route to specific menu handlers
        // Main menu: support both legacy "Main Menu" and configurable titles like "SpeedrunnerSwap Menu"
        if (title.contains("Main Menu") || title.contains("SpeedrunnerSwap")) {
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
                case "team_selector" -> guiManager.openTeamSelector(player);
                case "settings" -> guiManager.openSettingsMenu(player);
                case "power_ups" -> guiManager.openPowerUpsMenu(player);
                case "world_border" -> guiManager.openWorldBorderMenu(player);
                case "kits" -> guiManager.openKitsMenu(player);
                case "bounty" -> guiManager.openBountyMenu(player);
                case "last_stand" -> guiManager.openLastStandMenu(player);
                case "compass" -> guiManager.openCompassSettingsMenu(player);
                case "sudden_death" -> guiManager.openSuddenDeathMenu(player);
                case "statistics" -> guiManager.openStatisticsMenu(player);
            }
            return;
        }

        // Fallback: route based on display name text
        String name = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName());
        if (guiManager.isTeamSelectorButton(clicked) || name.toLowerCase().contains("team selector")) {
            guiManager.openTeamSelector(player);
        } else if (guiManager.isSettingsButton(clicked) || name.toLowerCase().contains("settings")) {
            guiManager.openSettingsMenu(player);
        } else if (name.toLowerCase().contains("power-ups") || name.toLowerCase().contains("power ups")) {
            guiManager.openPowerUpsMenu(player);
        } else if (name.toLowerCase().contains("world border") || clicked.getType() == Material.BARRIER) {
            // Note: BARRIER also used for back button; back handled earlier.
            guiManager.openWorldBorderMenu(player);
        } else if (name.toLowerCase().contains("kits")) {
            guiManager.openKitsMenu(player);
        } else if (name.toLowerCase().contains("bounty")) {
            guiManager.openBountyMenu(player);
        } else if (name.toLowerCase().contains("last stand")) {
            guiManager.openLastStandMenu(player);
        } else if (name.toLowerCase().contains("compass")) {
            guiManager.openCompassSettingsMenu(player);
        } else if (name.toLowerCase().contains("sudden death")) {
            guiManager.openSuddenDeathMenu(player);
        } else if (name.toLowerCase().contains("statistics") || name.toLowerCase().contains("status")) {
            guiManager.openStatisticsMenu(player);
        } else if (name.equals("§a§lStart Game")) {
            if (!plugin.getGameManager().isGameRunning()) {
                if (plugin.getGameManager().startGame()) {
                    player.sendMessage(Component.text("§aGame started."));
                } else {
                    player.sendMessage(Component.text("§cCannot start game. Check teams."));
                }
            }
            guiManager.openMainMenu(player);
        } else if (name.equals("§e§lPause Game")) {
            if (plugin.getGameManager().isGameRunning() && !plugin.getGameManager().isGamePaused()) {
                plugin.getGameManager().pauseGame();
                player.sendMessage(Component.text("§eGame paused."));
            }
            guiManager.openMainMenu(player);
        } else if (name.equals("§a§lResume Game")) {
            if (plugin.getGameManager().isGameRunning() && plugin.getGameManager().isGamePaused()) {
                plugin.getGameManager().resumeGame();
                player.sendMessage(Component.text("§aGame resumed."));
            }
            guiManager.openMainMenu(player);
        } else if (name.equals("§c§lStop Game")) {
            if (plugin.getGameManager().isGameRunning()) {
                plugin.getGameManager().stopGame();
                player.sendMessage(Component.text("§cGame stopped."));
            }
            guiManager.openMainMenu(player);
        }
    }

    private void handleSettingsClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || !clicked.hasItemMeta()) return;

        String buttonId = getButtonId(clicked);
        if (buttonId != null) {
            switch (buttonId) {
                case "swap_interval": {
                    int current = plugin.getConfigManager().getSwapInterval();
                    int delta = event.isShiftClick() ? 60 : 30;
                    if (event.isLeftClick()) current += delta;
                    if (event.isRightClick()) current -= delta;
                    current = Math.max(30, Math.min(300, current));
                    plugin.getConfigManager().setSwapInterval(current);
                    break;
                }
                case "random_swaps":
                    toggleRandomSwaps(player);
                    break;
                case "safe_swaps":
                    toggleSafeSwaps(player);
                    break;
                case "pause_on_disconnect": {
                    boolean enabled = plugin.getConfigManager().isPauseOnDisconnect();
                    plugin.getConfig().set("swap.pause_on_disconnect", !enabled);
                    plugin.saveConfig();
                    break;
                }
                case "hunter_swap_toggle": {
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
                case "hot_potato_toggle": {
                    boolean enabled = plugin.getConfigManager().isHotPotatoModeEnabled();
                    plugin.getConfig().set("swap.hot_potato_mode.enabled", !enabled);
                    plugin.saveConfig();
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
            }
        } else {
            // Handle timer visibility clocks (created without explicit IDs)
            String name = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName());
            if (name.equals("§e§lActive Runner Timer")) {
                cycleRunnerTimer(player);
            } else if (name.equals("§e§lWaiting Runner Timer")) {
                cycleWaitingTimer(player);
            } else if (name.equals("§e§lHunter Timer")) {
                cycleHunterTimer(player);
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

    private void cycleHunterTimer(Player player) {
        String current = plugin.getConfigManager().getHunterTimerVisibility();
        String next = guiManager.getNextVisibility(current);
        plugin.getConfigManager().setHunterTimerVisibility(next);
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
        if (item.getItemMeta().displayName() == null) return false;
        String text = PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
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
            player.sendMessage(Component.text("§aCleared all teams."));
            guiManager.openTeamSelector(player);
            return;
        }

        String name = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName());

        // Select assignment team
        if (name != null) {
            if (name.contains("§b§lRunners")) {
                guiManager.setPlayerTeam(player, com.example.speedrunnerswap.models.Team.RUNNER);
                return;
            }
            if (name.contains("§c§lHunters")) {
                guiManager.setPlayerTeam(player, com.example.speedrunnerswap.models.Team.HUNTER);
                return;
            }
        }

        // Assign a player by clicking their head, using current selected team
        if (clicked.getType() == Material.PLAYER_HEAD && clicked.getItemMeta().displayName() != null) {
            String targetName = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName());
            // displayName might be colored; strip any color prefixes/suffixes
            targetName = targetName.replace("§b", "").replace("§c", "").replace("§f", "").replace("§r", "");
            Player target = Bukkit.getPlayerExact(targetName);
            if (target == null) {
                player.sendMessage(Component.text("§cPlayer not found or offline: " + targetName));
                return;
            }

            com.example.speedrunnerswap.models.Team selected = guiManager.getSelectedTeam(player);
            if (selected == com.example.speedrunnerswap.models.Team.NONE) {
                player.sendMessage(Component.text("§eSelect a team (Runners/Hunters) first."));
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

        String name = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName());

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

    private void handlePowerUpDurationsClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String name = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName());

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

    private void handleWorldBorderClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String name = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName());
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

        String name = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName());

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
        String name = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName());

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
        }
    }

    private void handleLastStandClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String name = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName());

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
            default -> {}
        }
    }

    private void handleCompassSettingsClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String name = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName());

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
                player.sendMessage(Component.text("§aSet End Portal hint for world §f" + player.getWorld().getName()));
                guiManager.openCompassSettingsMenu(player);
            }
            case "§c§lClear End Portal Hint (this world)" -> {
                plugin.getConfigManager().clearEndPortalHint(player.getWorld());
                player.sendMessage(Component.text("§eCleared End Portal hint for world §f" + player.getWorld().getName()));
                guiManager.openCompassSettingsMenu(player);
            }
            default -> {}
        }
    }

    private void handleSuddenDeathClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String name = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName());

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

        String name = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName());
        if (!name.equals("§a§lSave Kit")) return;

        // Extract kit type from title: "§e§lEdit <kit> Kit"
        String plainTitle = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
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
        player.sendMessage(Component.text("§aSaved " + kitType + " kit."));

        // Return to kits menu
        guiManager.openKitsMenu(player);
    }

    private void handleStatisticsClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String name = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName());

        switch (name) {
            case "§e§lDisplay Statistics" -> plugin.getStatsManager().displayStats();
            case "§a§lStart Tracking" -> plugin.getStatsManager().startTracking();
            case "§c§lStop Tracking" -> plugin.getStatsManager().stopTracking();
            default -> {}
        }
        guiManager.openStatisticsMenu(player);
    }

    private void handleEffectsMenuClick(InventoryClickEvent event, String title) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        java.util.List<net.kyori.adventure.text.Component> lore = clicked.getItemMeta().lore();
        if (lore == null) return;
        String effectId = null;
        for (net.kyori.adventure.text.Component line : lore) {
            String text = PlainTextComponentSerializer.plainText().serialize(line);
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
}
