/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.NamespacedKey
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.persistence.PersistentDataContainer
 *  org.bukkit.persistence.PersistentDataType
 *  org.bukkit.plugin.Plugin
 */
package com.example.speedrunnerswap.gui;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import com.example.speedrunnerswap.gui.GuiManager;
import com.example.speedrunnerswap.models.Team;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public class GuiListener
implements Listener {
    private final SpeedrunnerSwap plugin;
    private final GuiManager guiManager;

    public GuiListener(SpeedrunnerSwap plugin, GuiManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
    }

    @EventHandler(priority=EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player)event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
        }
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (this.isPluginGui(title)) {
            event.setCancelled(true);
            if (!player.hasPermission("speedrunnerswap.admin") && this.isAdminMenu(title)) {
                player.sendMessage((Component)Component.text((String)"\u00a7cYou do not have permission to use this menu."));
                return;
            }
            this.handleGuiClick(event, title);
        }
    }

    private boolean isPluginGui(String title) {
        if (title == null || title.isEmpty()) {
            return false;
        }
        return title.contains("SpeedrunnerSwap") || title.contains("Main Menu") || title.contains("Team Selector") || title.contains("Settings") || title.contains("Kits") || title.contains("Edit ") && title.contains(" Kit") || title.contains("Effects") || title.contains("Power-ups") || title.contains("Power-up Durations") || title.contains("World Border") || title.contains("Bounty") || title.contains("Last Stand") || title.contains("Compass") || title.contains("Sudden Death") || title.contains("Statistics");
    }

    private boolean isAdminMenu(String title) {
        if (title == null || title.isEmpty()) {
            return false;
        }
        return title.contains("Settings") || title.contains("Kits") || title.contains("Edit ") && title.contains(" Kit") || title.contains("Effects") || title.contains("Power-ups") || title.contains("Power-up Durations") || title.contains("World Border") || title.contains("Bounty") || title.contains("Last Stand") || title.contains("Compass") || title.contains("Sudden Death");
    }

    private void handleGuiClick(InventoryClickEvent event, String title) {
        Player player = (Player)event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        if (this.isBackButton(clickedItem)) {
            this.guiManager.openMainMenu(player);
            return;
        }
        if (this.maybeHandlePowerUpDurationsMenu(event, title)) {
            return;
        }
        if (title.contains("Main Menu") || title.contains("SpeedrunnerSwap")) {
            this.handleMainMenuClick(event);
        } else if (title.contains("Team Selector")) {
            this.handleTeamSelectorClick(event);
        } else if (title.contains("World Border")) {
            this.handleWorldBorderClick(event);
        } else if (title.contains("Compass")) {
            this.handleCompassSettingsClick(event);
        } else if (title.contains("Sudden Death")) {
            this.handleSuddenDeathClick(event);
        } else if (title.contains("Settings")) {
            this.handleSettingsClick(event);
        } else if (title.contains("Edit ") && title.contains(" Kit")) {
            this.handleKitEditorClick(event, title);
        } else if (title.contains("Kits")) {
            this.handleKitsMenuClick(event);
        } else if (title.contains("Power-ups")) {
            this.handlePowerUpsMenuClick(event);
        } else if (title.contains("Effects")) {
            this.handleEffectsMenuClick(event, title);
        } else if (title.contains("Bounty")) {
            this.handleBountyMenuClick(event);
        } else if (title.contains("Last Stand")) {
            this.handleLastStandClick(event);
        } else if (title.contains("Statistics")) {
            this.handleStatisticsClick(event);
        }
    }

    private void handleMainMenuClick(InventoryClickEvent event) {
        Player player = (Player)event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        String buttonId = this.getButtonId(clicked);
        if (buttonId != null) {
            switch (buttonId) {
                case "team_selector": {
                    this.guiManager.openTeamSelector(player);
                    break;
                }
                case "settings": {
                    this.guiManager.openSettingsMenu(player);
                    break;
                }
                case "power_ups": {
                    this.guiManager.openPowerUpsMenu(player);
                    break;
                }
                case "world_border": {
                    this.guiManager.openWorldBorderMenu(player);
                    break;
                }
                case "kits": {
                    this.guiManager.openKitsMenu(player);
                    break;
                }
                case "bounty": {
                    this.guiManager.openBountyMenu(player);
                    break;
                }
                case "last_stand": {
                    this.guiManager.openLastStandMenu(player);
                    break;
                }
                case "compass": {
                    this.guiManager.openCompassSettingsMenu(player);
                    break;
                }
                case "sudden_death": {
                    this.guiManager.openSuddenDeathMenu(player);
                    break;
                }
                case "statistics": {
                    this.guiManager.openStatisticsMenu(player);
                }
            }
            return;
        }
        String name = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName());
        if (this.guiManager.isTeamSelectorButton(clicked) || name.toLowerCase().contains("team selector")) {
            this.guiManager.openTeamSelector(player);
        } else if (this.guiManager.isSettingsButton(clicked) || name.toLowerCase().contains("settings")) {
            this.guiManager.openSettingsMenu(player);
        } else if (name.toLowerCase().contains("power-ups") || name.toLowerCase().contains("power ups")) {
            this.guiManager.openPowerUpsMenu(player);
        } else if (name.toLowerCase().contains("world border") || clicked.getType() == Material.BARRIER) {
            this.guiManager.openWorldBorderMenu(player);
        } else if (name.toLowerCase().contains("kits")) {
            this.guiManager.openKitsMenu(player);
        } else if (name.toLowerCase().contains("bounty")) {
            this.guiManager.openBountyMenu(player);
        } else if (name.toLowerCase().contains("last stand")) {
            this.guiManager.openLastStandMenu(player);
        } else if (name.toLowerCase().contains("compass")) {
            this.guiManager.openCompassSettingsMenu(player);
        } else if (name.toLowerCase().contains("sudden death")) {
            this.guiManager.openSuddenDeathMenu(player);
        } else if (name.toLowerCase().contains("statistics") || name.toLowerCase().contains("status")) {
            this.guiManager.openStatisticsMenu(player);
        } else if (name.equals("\u00a7a\u00a7lStart Game")) {
            if (!this.plugin.getGameManager().isGameRunning()) {
                if (this.plugin.getGameManager().startGame()) {
                    player.sendMessage((Component)Component.text((String)"\u00a7aGame started."));
                } else {
                    player.sendMessage((Component)Component.text((String)"\u00a7cCannot start game. Check teams."));
                }
            }
            this.guiManager.openMainMenu(player);
        } else if (name.equals("\u00a7e\u00a7lPause Game")) {
            if (this.plugin.getGameManager().isGameRunning() && !this.plugin.getGameManager().isGamePaused()) {
                this.plugin.getGameManager().pauseGame();
                player.sendMessage((Component)Component.text((String)"\u00a7eGame paused."));
            }
            this.guiManager.openMainMenu(player);
        } else if (name.equals("\u00a7a\u00a7lResume Game")) {
            if (this.plugin.getGameManager().isGameRunning() && this.plugin.getGameManager().isGamePaused()) {
                this.plugin.getGameManager().resumeGame();
                player.sendMessage((Component)Component.text((String)"\u00a7aGame resumed."));
            }
            this.guiManager.openMainMenu(player);
        } else if (name.equals("\u00a7c\u00a7lStop Game")) {
            if (this.plugin.getGameManager().isGameRunning()) {
                this.plugin.getGameManager().stopGame();
                player.sendMessage((Component)Component.text((String)"\u00a7cGame stopped."));
            }
            this.guiManager.openMainMenu(player);
        }
    }

    /*
     * Enabled aggressive block sorting
     */
    private void handleSettingsClick(InventoryClickEvent event) {
        Player player;
        block68: {
            ItemStack clicked;
            block69: {
                player = (Player)event.getWhoClicked();
                clicked = event.getCurrentItem();
                if (clicked == null || !clicked.hasItemMeta()) {
                    return;
                }
                String buttonId = this.getButtonId(clicked);
                if (buttonId == null) break block69;
                switch (buttonId) {
                    case "swap_interval": {
                        int delta;
                        int current = this.plugin.getConfigManager().getSwapInterval();
                        int n = delta = event.isShiftClick() ? 60 : 30;
                        if (event.isLeftClick()) {
                            current += delta;
                        }
                        if (event.isRightClick()) {
                            current -= delta;
                        }
                        current = Math.max(30, Math.min(300, current));
                        this.plugin.getConfigManager().setSwapInterval(current);
                        break;
                    }
                    case "random_swaps": {
                        this.toggleRandomSwaps(player);
                        break;
                    }
                    case "safe_swaps": {
                        this.toggleSafeSwaps(player);
                        break;
                    }
                    case "pause_on_disconnect": {
                        boolean enabled = this.plugin.getConfigManager().isPauseOnDisconnect();
                        this.plugin.getConfig().set("swap.pause_on_disconnect", (Object)(!enabled ? 1 : 0));
                        this.plugin.saveConfig();
                        break;
                    }
                    case "hunter_swap_toggle": {
                        boolean enabled = this.plugin.getConfigManager().isHunterSwapEnabled();
                        this.plugin.getConfig().set("swap.hunter_swap.enabled", (Object)(!enabled ? 1 : 0));
                        this.plugin.saveConfig();
                        break;
                    }
                    case "hunter_swap_interval": {
                        int delta;
                        int val = this.plugin.getConfigManager().getHunterSwapInterval();
                        int n = delta = event.isShiftClick() ? 60 : 30;
                        if (event.isLeftClick()) {
                            val += delta;
                        }
                        if (event.isRightClick()) {
                            val -= delta;
                        }
                        val = Math.max(30, Math.min(600, val));
                        this.plugin.getConfig().set("swap.hunter_swap.interval", (Object)val);
                        this.plugin.saveConfig();
                        break;
                    }
                    case "hot_potato_toggle": {
                        boolean enabled = this.plugin.getConfigManager().isHotPotatoModeEnabled();
                        this.plugin.getConfig().set("swap.hot_potato_mode.enabled", (Object)(!enabled ? 1 : 0));
                        this.plugin.saveConfig();
                        break;
                    }
                    case "freeze_toggle": {
                        boolean enabled = this.plugin.getConfigManager().isFreezeMechanicEnabled();
                        this.plugin.getConfig().set("freeze_mechanic.enabled", (Object)(!enabled ? 1 : 0));
                        this.plugin.saveConfig();
                        if (this.plugin.getGameManager().isGameRunning()) {
                            this.plugin.getGameManager().refreshFreezeMechanic();
                            break;
                        }
                        break block68;
                    }
                    case "freeze_mode": {
                        String current = this.plugin.getConfigManager().getFreezeMode();
                        String next = switch (current.toUpperCase()) {
                            case "EFFECTS" -> "SPECTATOR";
                            case "SPECTATOR" -> "LIMBO";
                            default -> "EFFECTS";
                        };
                        this.plugin.getConfig().set("freeze_mode", (Object)next);
                        this.plugin.saveConfig();
                        if (this.plugin.getGameManager().isGameRunning()) {
                            this.plugin.getGameManager().refreshFreezeMechanic();
                            break;
                        }
                        break block68;
                    }
                    case "freeze_duration": {
                        int delta;
                        int val = this.plugin.getConfigManager().getFreezeDurationTicks();
                        int n = delta = event.isShiftClick() ? 100 : 20;
                        if (event.isLeftClick()) {
                            val += delta;
                        }
                        if (event.isRightClick()) {
                            val -= delta;
                        }
                        val = Math.max(20, Math.min(6000, val));
                        this.plugin.getConfig().set("freeze_mechanic.duration_ticks", (Object)val);
                        this.plugin.saveConfig();
                        if (this.plugin.getGameManager().isGameRunning()) {
                            this.plugin.getGameManager().refreshFreezeMechanic();
                            break;
                        }
                        break block68;
                    }
                    case "freeze_check_interval": {
                        int delta;
                        int val = this.plugin.getConfigManager().getFreezeCheckIntervalTicks();
                        int n = delta = event.isShiftClick() ? 20 : 5;
                        if (event.isLeftClick()) {
                            val += delta;
                        }
                        if (event.isRightClick()) {
                            val -= delta;
                        }
                        val = Math.max(1, Math.min(200, val));
                        this.plugin.getConfig().set("freeze_mechanic.check_interval_ticks", (Object)val);
                        this.plugin.saveConfig();
                        if (this.plugin.getGameManager().isGameRunning()) {
                            this.plugin.getGameManager().refreshFreezeMechanic();
                            break;
                        }
                        break block68;
                    }
                    case "freeze_max_distance": {
                        int delta;
                        int val = (int)Math.round(this.plugin.getConfigManager().getFreezeMaxDistance());
                        int n = delta = event.isShiftClick() ? 20 : 5;
                        if (event.isLeftClick()) {
                            val += delta;
                        }
                        if (event.isRightClick()) {
                            val -= delta;
                        }
                        val = Math.max(5, Math.min(256, val));
                        this.plugin.getConfig().set("freeze_mechanic.max_distance", (Object)val);
                        this.plugin.saveConfig();
                        if (this.plugin.getGameManager().isGameRunning()) {
                            this.plugin.getGameManager().refreshFreezeMechanic();
                            break;
                        }
                        break block68;
                    }
                    case "tracker_toggle": {
                        boolean enabled = this.plugin.getConfigManager().isTrackerEnabled();
                        this.plugin.getConfigManager().setTrackerEnabled(!enabled);
                        if (!enabled) {
                            this.plugin.getTrackerManager().startTracking();
                            for (Player hunter : this.plugin.getGameManager().getHunters()) {
                                if (!hunter.isOnline()) continue;
                                this.plugin.getTrackerManager().giveTrackingCompass(hunter);
                            }
                            break block68;
                        } else {
                            this.plugin.getTrackerManager().stopTracking();
                            break;
                        }
                    }
                    case "force_swap": {
                        this.plugin.getGameManager().triggerImmediateSwap();
                        break;
                    }
                    case "force_hunter_shuffle": {
                        this.plugin.getGameManager().triggerImmediateHunterSwap();
                        break;
                    }
                    case "update_compasses": {
                        this.plugin.getTrackerManager().updateAllHunterCompasses();
                        break;
                    }
                }
                break block68;
            }
            String name = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName());
            if (name.equals("\u00a7e\u00a7lActive Runner Timer")) {
                this.cycleRunnerTimer(player);
            } else if (name.equals("\u00a7e\u00a7lWaiting Runner Timer")) {
                this.cycleWaitingTimer(player);
            } else if (name.equals("\u00a7e\u00a7lHunter Timer")) {
                this.cycleHunterTimer(player);
            }
        }
        this.guiManager.openSettingsMenu(player);
        this.plugin.getGameManager().refreshSwapSchedule();
        this.plugin.getGameManager().refreshActionBar();
    }

    private void cycleRunnerTimer(Player player) {
        String current = this.plugin.getConfigManager().getRunnerTimerVisibility();
        String next = this.guiManager.getNextVisibility(current);
        this.plugin.getConfigManager().setRunnerTimerVisibility(next);
    }

    private void cycleWaitingTimer(Player player) {
        String current = this.plugin.getConfigManager().getWaitingTimerVisibility();
        String next = this.guiManager.getNextVisibility(current);
        this.plugin.getConfigManager().setWaitingTimerVisibility(next);
    }

    private void cycleHunterTimer(Player player) {
        String current = this.plugin.getConfigManager().getHunterTimerVisibility();
        String next = this.guiManager.getNextVisibility(current);
        this.plugin.getConfigManager().setHunterTimerVisibility(next);
    }

    private String getButtonId(ItemStack item) {
        NamespacedKey sswKey;
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        if (container.has(sswKey = new NamespacedKey((Plugin)this.plugin, "ssw_button_id"), PersistentDataType.STRING)) {
            return (String)container.get(sswKey, PersistentDataType.STRING);
        }
        NamespacedKey legacyKey = new NamespacedKey((Plugin)this.plugin, "button_id");
        if (container.has(legacyKey, PersistentDataType.STRING)) {
            return (String)container.get(legacyKey, PersistentDataType.STRING);
        }
        return null;
    }

    private boolean isBackButton(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        if (item.getItemMeta().displayName() == null) {
            return false;
        }
        String text = PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
        boolean looksLikeBack = text != null && text.toLowerCase().contains("back");
        return looksLikeBack && (item.getType() == Material.BARRIER || item.getType() == Material.ARROW);
    }

    private void toggleRandomSwaps(Player player) {
        boolean current = this.plugin.getConfigManager().isSwapRandomized();
        this.plugin.getConfigManager().setSwapRandomized(!current);
    }

    private void toggleSafeSwaps(Player player) {
        boolean current = this.plugin.getConfigManager().isSafeSwapEnabled();
        this.plugin.getConfigManager().setSafeSwapEnabled(!current);
    }

    private void handleTeamSelectorClick(InventoryClickEvent event) {
        Player player = (Player)event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        String id = this.getButtonId(clicked);
        if ("clear_teams".equals(id)) {
            this.plugin.getGameManager().setRunners(new ArrayList<Player>());
            this.plugin.getGameManager().setHunters(new ArrayList<Player>());
            player.sendMessage((Component)Component.text((String)"\u00a7aCleared all teams."));
            this.guiManager.openTeamSelector(player);
            return;
        }
        String name = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName());
        if (name != null) {
            if (name.contains("\u00a7b\u00a7lRunners")) {
                this.guiManager.setPlayerTeam(player, Team.RUNNER);
                return;
            }
            if (name.contains("\u00a7c\u00a7lHunters")) {
                this.guiManager.setPlayerTeam(player, Team.HUNTER);
                return;
            }
        }
        if (clicked.getType() == Material.PLAYER_HEAD && clicked.getItemMeta().displayName() != null) {
            String targetName = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName());
            Player target = Bukkit.getPlayerExact((String)(targetName = targetName.replace("\u00a7b", "").replace("\u00a7c", "").replace("\u00a7f", "").replace("\u00a7r", "")));
            if (target == null) {
                player.sendMessage((Component)Component.text((String)("\u00a7cPlayer not found or offline: " + targetName)));
                return;
            }
            Team selected = this.guiManager.getSelectedTeam(player);
            if (selected == Team.NONE) {
                player.sendMessage((Component)Component.text((String)"\u00a7eSelect a team (Runners/Hunters) first."));
                return;
            }
            ArrayList<Player> newRunners = new ArrayList<Player>(this.plugin.getGameManager().getRunners());
            ArrayList<Player> newHunters = new ArrayList<Player>(this.plugin.getGameManager().getHunters());
            newRunners.remove(target);
            newHunters.remove(target);
            if (selected == Team.RUNNER) {
                newRunners.add(target);
            } else if (selected == Team.HUNTER) {
                newHunters.add(target);
            }
            this.plugin.getGameManager().setRunners(newRunners);
            this.plugin.getGameManager().setHunters(newHunters);
            this.guiManager.openTeamSelector(player);
        }
    }

    private void handlePowerUpsMenuClick(InventoryClickEvent event) {
        Player player = (Player)event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        String name = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName());
        if ("\u00a7e\u00a7lToggle Power-ups".equals(name)) {
            boolean current = this.plugin.getConfigManager().isPowerUpsEnabled();
            this.plugin.getConfigManager().setPowerUpsEnabled(!current);
            this.guiManager.openPowerUpsMenu(player);
            return;
        }
        if (name.contains("Positive Effects")) {
            this.guiManager.openPositiveEffectsMenu(player);
            return;
        }
        if (name.contains("Negative Effects")) {
            this.guiManager.openNegativeEffectsMenu(player);
            return;
        }
        if (name.contains("Effect Durations")) {
            this.guiManager.openPowerUpDurationsMenu(player);
            return;
        }
    }

    private boolean maybeHandlePowerUpDurationsMenu(InventoryClickEvent event, String title) {
        if (!title.contains("Power-up Durations")) {
            return false;
        }
        this.handlePowerUpDurationsClick(event);
        return true;
    }

    private void handlePowerUpDurationsClick(InventoryClickEvent event) {
        Player player = (Player)event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        String name = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName());
        boolean changed = false;
        switch (name) {
            case "\u00a7e\u00a7lMin Duration (s)": {
                int val = this.plugin.getConfigManager().getPowerUpsMinSeconds();
                if (event.isLeftClick()) {
                    val += 5;
                }
                if (event.isRightClick()) {
                    val -= 5;
                }
                val = Math.max(1, Math.min(600, val));
                this.plugin.getConfigManager().setPowerUpsMinSeconds(val);
                changed = true;
                break;
            }
            case "\u00a7e\u00a7lMax Duration (s)": {
                int val = this.plugin.getConfigManager().getPowerUpsMaxSeconds();
                if (event.isLeftClick()) {
                    val += 5;
                }
                if (event.isRightClick()) {
                    val -= 5;
                }
                val = Math.max(1, Math.min(1800, val));
                this.plugin.getConfigManager().setPowerUpsMaxSeconds(val);
                changed = true;
                break;
            }
            case "\u00a7e\u00a7lMin Level": {
                int val = this.plugin.getConfigManager().getPowerUpsMinLevel();
                if (event.isLeftClick()) {
                    ++val;
                }
                if (event.isRightClick()) {
                    --val;
                }
                val = Math.max(1, Math.min(5, val));
                this.plugin.getConfigManager().setPowerUpsMinLevel(val);
                changed = true;
                break;
            }
            case "\u00a7e\u00a7lMax Level": {
                int val = this.plugin.getConfigManager().getPowerUpsMaxLevel();
                if (event.isLeftClick()) {
                    ++val;
                }
                if (event.isRightClick()) {
                    --val;
                }
                val = Math.max(1, Math.min(5, val));
                this.plugin.getConfigManager().setPowerUpsMaxLevel(val);
                changed = true;
                break;
            }
        }
        if (this.isBackButton(clicked)) {
            this.guiManager.openPowerUpsMenu(player);
        } else if (changed) {
            this.guiManager.openPowerUpDurationsMenu(player);
        }
    }

    private void handleWorldBorderClick(InventoryClickEvent event) {
        Player player = (Player)event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        String name = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName());
        boolean changed = false;
        switch (name) {
            case "\u00a7e\u00a7lToggle World Border": {
                boolean enabled = this.plugin.getConfig().getBoolean("world_border.enabled", true);
                this.plugin.getConfig().set("world_border.enabled", (Object)(!enabled ? 1 : 0));
                this.plugin.saveConfig();
                if (!enabled) {
                    this.plugin.getWorldBorderManager().startBorderShrinking();
                } else {
                    this.plugin.getWorldBorderManager().stopBorderShrinking();
                }
                changed = true;
                break;
            }
            case "\u00a7a\u00a7lInitial Border Size": {
                int val = this.plugin.getConfig().getInt("world_border.initial_size", 2000);
                if (event.isLeftClick()) {
                    val += event.isShiftClick() ? 500 : 100;
                }
                if (event.isRightClick()) {
                    val -= event.isShiftClick() ? 500 : 100;
                }
                val = Math.max(100, Math.min(29999984, val));
                this.plugin.getConfig().set("world_border.initial_size", (Object)val);
                this.plugin.saveConfig();
                changed = true;
                break;
            }
            case "\u00a7c\u00a7lFinal Border Size": {
                int val = this.plugin.getConfig().getInt("world_border.final_size", 100);
                if (event.isLeftClick()) {
                    val += event.isShiftClick() ? 100 : 50;
                }
                if (event.isRightClick()) {
                    val -= event.isShiftClick() ? 100 : 50;
                }
                val = Math.max(50, Math.min(1000, val));
                this.plugin.getConfig().set("world_border.final_size", (Object)val);
                this.plugin.saveConfig();
                changed = true;
                break;
            }
            case "\u00a76\u00a7lShrink Duration": {
                int val = this.plugin.getConfig().getInt("world_border.shrink_duration", 1800);
                if (event.isLeftClick()) {
                    val += event.isShiftClick() ? 900 : 300;
                }
                if (event.isRightClick()) {
                    val -= event.isShiftClick() ? 900 : 300;
                }
                val = Math.max(300, Math.min(7200, val));
                this.plugin.getConfig().set("world_border.shrink_duration", (Object)val);
                this.plugin.saveConfig();
                changed = true;
                break;
            }
            case "\u00a7e\u00a7lWarning Settings": {
                int dist = this.plugin.getConfig().getInt("world_border.warning_distance", 50);
                int interval = this.plugin.getConfig().getInt("world_border.warning_interval", 300);
                if (event.isLeftClick()) {
                    dist += event.isShiftClick() ? 50 : 10;
                }
                if (event.isRightClick()) {
                    dist -= event.isShiftClick() ? 50 : 10;
                }
                if (event.isShiftClick()) {
                    if (event.isLeftClick()) {
                        interval += 30;
                    }
                    if (event.isRightClick()) {
                        interval -= 30;
                    }
                }
                dist = Math.max(5, Math.min(1000, dist));
                interval = Math.max(30, Math.min(3600, interval));
                this.plugin.getConfig().set("world_border.warning_distance", (Object)dist);
                this.plugin.getConfig().set("world_border.warning_interval", (Object)interval);
                this.plugin.saveConfig();
                changed = true;
                break;
            }
        }
        if (changed) {
            this.guiManager.openWorldBorderMenu(player);
        }
    }

    private void handleKitsMenuClick(InventoryClickEvent event) {
        String name;
        Player player = (Player)event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        switch (name = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName())) {
            case "\u00a7e\u00a7lKits: \u00a7aEnabled": 
            case "\u00a7e\u00a7lKits: \u00a7cDisabled": {
                boolean enabled = this.plugin.getConfigManager().isKitsEnabled();
                this.plugin.getConfigManager().setKitsEnabled(!enabled);
                this.guiManager.openKitsMenu(player);
                break;
            }
            case "\u00a7b\u00a7lGive Runner Kit": {
                this.plugin.getKitManager().applyRunnerKit(player);
                break;
            }
            case "\u00a7c\u00a7lGive Hunter Kit": {
                this.plugin.getKitManager().applyHunterKit(player);
                break;
            }
            case "\u00a7b\u00a7lEdit Runner Kit": {
                this.guiManager.openKitEditor(player, "runner");
                break;
            }
            case "\u00a7c\u00a7lEdit Hunter Kit": {
                this.guiManager.openKitEditor(player, "hunter");
                break;
            }
        }
    }

    private void handleBountyMenuClick(InventoryClickEvent event) {
        Player player = (Player)event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        String name = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName());
        if (name.contains("Bounty Status")) {
            boolean enabled = this.plugin.getConfig().getBoolean("bounty.enabled", true);
            this.plugin.getConfig().set("bounty.enabled", (Object)(!enabled ? 1 : 0));
            this.plugin.saveConfig();
            this.guiManager.openBountyMenu(player);
            return;
        }
        if (name.equals("\u00a7e\u00a7lAssign New Bounty")) {
            this.plugin.getBountyManager().assignNewBounty();
            this.guiManager.openBountyMenu(player);
            return;
        }
        if (name.equals("\u00a7c\u00a7lClear Bounty")) {
            this.plugin.getBountyManager().clearBounty();
            this.guiManager.openBountyMenu(player);
        }
    }

    private void handleLastStandClick(InventoryClickEvent event) {
        String name;
        Player player = (Player)event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        switch (name = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName())) {
            case "\u00a7e\u00a7lLast Stand: \u00a7aEnabled": 
            case "\u00a7e\u00a7lLast Stand: \u00a7cDisabled": {
                boolean enabled = this.plugin.getConfigManager().isLastStandEnabled();
                this.plugin.getConfig().set("last_stand.enabled", (Object)(!enabled ? 1 : 0));
                this.plugin.saveConfig();
                this.guiManager.openLastStandMenu(player);
                break;
            }
            case "\u00a76\u00a7lLast Stand Duration": {
                int duration = this.plugin.getConfigManager().getLastStandDuration();
                if (event.isLeftClick()) {
                    duration += 100;
                }
                if (event.isRightClick()) {
                    duration -= 100;
                }
                duration = Math.max(100, duration);
                this.plugin.getConfig().set("last_stand.duration_ticks", (Object)duration);
                this.plugin.saveConfig();
                this.guiManager.openLastStandMenu(player);
                break;
            }
            case "\u00a7e\u00a7lStrength Amplifier": {
                int amp = this.plugin.getConfigManager().getLastStandStrengthAmplifier();
                if (event.isLeftClick()) {
                    ++amp;
                }
                if (event.isRightClick()) {
                    --amp;
                }
                amp = Math.max(0, Math.min(5, amp));
                this.plugin.getConfig().set("last_stand.strength_amplifier", (Object)amp);
                this.plugin.saveConfig();
                this.guiManager.openLastStandMenu(player);
                break;
            }
        }
    }

    private void handleCompassSettingsClick(InventoryClickEvent event) {
        String name;
        Player player = (Player)event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        switch (name = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName())) {
            case "\u00a7e\u00a7lCompass Jamming: \u00a7aEnabled": 
            case "\u00a7e\u00a7lCompass Jamming: \u00a7cDisabled": {
                boolean enabled = this.plugin.getConfigManager().isCompassJammingEnabled();
                this.plugin.getConfig().set("tracker.compass_jamming.enabled", (Object)(!enabled ? 1 : 0));
                this.plugin.getConfig().set("sudden_death.compass_jamming.enabled", (Object)(!enabled ? 1 : 0));
                this.plugin.saveConfig();
                this.guiManager.openCompassSettingsMenu(player);
                break;
            }
            case "\u00a76\u00a7lJam Duration (ticks)": {
                int duration = this.plugin.getConfigManager().getCompassJamDuration();
                if (event.isLeftClick()) {
                    duration += 20;
                }
                if (event.isRightClick()) {
                    duration -= 20;
                }
                duration = Math.max(20, duration);
                this.plugin.getConfig().set("tracker.compass_jamming.duration_ticks", (Object)duration);
                this.plugin.getConfig().set("sudden_death.compass_jamming.duration_ticks", (Object)duration);
                this.plugin.saveConfig();
                this.guiManager.openCompassSettingsMenu(player);
                break;
            }
            case "\u00a7e\u00a7lSet End Portal Hint (this world)": {
                this.plugin.getConfigManager().setEndPortalHint(player.getWorld(), player.getLocation());
                player.sendMessage((Component)Component.text((String)("\u00a7aSet End Portal hint for world \u00a7f" + player.getWorld().getName())));
                this.guiManager.openCompassSettingsMenu(player);
                break;
            }
            case "\u00a7c\u00a7lClear End Portal Hint (this world)": {
                this.plugin.getConfigManager().clearEndPortalHint(player.getWorld());
                player.sendMessage((Component)Component.text((String)("\u00a7eCleared End Portal hint for world \u00a7f" + player.getWorld().getName())));
                this.guiManager.openCompassSettingsMenu(player);
                break;
            }
        }
    }

    private void handleSuddenDeathClick(InventoryClickEvent event) {
        String name;
        Player player = (Player)event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        switch (name = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName())) {
            case "\u00a74\u00a7lSudden Death: \u00a7aActive": {
                this.plugin.getSuddenDeathManager().deactivate();
                break;
            }
            case "\u00a74\u00a7lSudden Death: \u00a7cInactive": {
                break;
            }
            case "\u00a7e\u00a7lCancel Scheduled Sudden Death": {
                this.plugin.getSuddenDeathManager().cancelSchedule();
                break;
            }
            case "\u00a7e\u00a7lSchedule Sudden Death": {
                this.plugin.getSuddenDeathManager().scheduleSuddenDeath();
                break;
            }
            case "\u00a7c\u00a7lActivate Now": {
                this.plugin.getSuddenDeathManager().activateSuddenDeath();
                break;
            }
            case "\u00a76\u00a7lActivation Delay (minutes)": {
                long seconds = this.plugin.getConfig().getLong("sudden_death.activation_delay", 1200L);
                long delta = 300L;
                if (event.isLeftClick()) {
                    seconds += delta;
                }
                if (event.isRightClick()) {
                    seconds -= delta;
                }
                long min = 300L;
                long max = 21600L;
                seconds = Math.max(min, Math.min(max, seconds));
                this.plugin.getConfig().set("sudden_death.activation_delay", (Object)seconds);
                this.plugin.saveConfig();
                break;
            }
        }
        this.guiManager.openSuddenDeathMenu(player);
    }

    private void handleKitEditorClick(InventoryClickEvent event, String title) {
        ItemStack boots;
        Player player = (Player)event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        String name = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName());
        if (!name.equals("\u00a7a\u00a7lSave Kit")) {
            return;
        }
        String plainTitle = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        String kitType = "runner";
        int idxEdit = plainTitle.indexOf("Edit ");
        int idxKit = plainTitle.indexOf(" Kit");
        if (idxEdit >= 0 && idxKit > idxEdit + 5) {
            kitType = plainTitle.substring(idxEdit + 5, idxKit).trim().toLowerCase();
        }
        Inventory top = event.getView().getTopInventory();
        ItemStack[] contents = new ItemStack[45];
        for (int i = 0; i < 45; ++i) {
            contents[i] = top.getItem(i);
        }
        ItemStack[] armor = new ItemStack[4];
        ItemStack helmet = top.getItem(45);
        ItemStack chest = top.getItem(46);
        ItemStack legs = top.getItem(47);
        armor[0] = boots = top.getItem(48);
        armor[1] = legs;
        armor[2] = chest;
        armor[3] = helmet;
        this.plugin.getKitConfigManager().saveKit(kitType, contents, armor);
        player.sendMessage((Component)Component.text((String)("\u00a7aSaved " + kitType + " kit.")));
        this.guiManager.openKitsMenu(player);
    }

    private void handleStatisticsClick(InventoryClickEvent event) {
        String name;
        Player player = (Player)event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        switch (name = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName())) {
            case "\u00a7e\u00a7lDisplay Statistics": {
                this.plugin.getStatsManager().displayStats();
                break;
            }
            case "\u00a7a\u00a7lStart Tracking": {
                this.plugin.getStatsManager().startTracking();
                break;
            }
            case "\u00a7c\u00a7lStop Tracking": {
                this.plugin.getStatsManager().stopTracking();
                break;
            }
        }
        this.guiManager.openStatisticsMenu(player);
    }

    private void handleEffectsMenuClick(InventoryClickEvent event, String title) {
        Player player = (Player)event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        List lore = clicked.getItemMeta().lore();
        if (lore == null) {
            return;
        }
        String effectId = null;
        for (Component line : lore) {
            String text = PlainTextComponentSerializer.plainText().serialize(line);
            int idx = text.indexOf("Effect ID:");
            if (idx < 0) continue;
            int split = text.indexOf(58);
            if (split < 0 || split + 1 >= text.length()) break;
            effectId = text.substring(split + 1).trim();
            effectId = effectId.replaceAll("\u00a7[0-9A-FK-ORa-fk-or]", "");
            break;
        }
        if (effectId == null || effectId.isEmpty()) {
            return;
        }
        boolean positive = title.contains("Positive Effects");
        ArrayList<String> list = new ArrayList<String>(positive ? this.plugin.getConfig().getStringList("power_ups.good_effects") : this.plugin.getConfig().getStringList("power_ups.bad_effects"));
        if (list.contains(effectId)) {
            list.remove(effectId);
        } else {
            list.add(effectId);
        }
        if (positive) {
            this.plugin.getConfig().set("power_ups.good_effects", list);
        } else {
            this.plugin.getConfig().set("power_ups.bad_effects", list);
        }
        this.plugin.saveConfig();
        if (positive) {
            this.guiManager.openPositiveEffectsMenu(player);
        } else {
            this.guiManager.openNegativeEffectsMenu(player);
        }
    }
}

