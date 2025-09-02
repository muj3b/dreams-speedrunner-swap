package com.example.speedrunnerswap.gui;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import net.kyori.adventure.text.Component;
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
        return title.contains("SpeedrunnerSwap") || 
               title.contains("Settings") || 
               title.contains("Team Selector") ||
               title.contains("Kits") ||
               title.contains("Effects") ||
               title.contains("Power-ups") ||
               title.contains("World Border") ||
               title.contains("Bounty") ||
               title.contains("Last Stand") ||
               title.contains("Compass") ||
               title.contains("Sudden Death") ||
               title.contains("Statistics");
    }

    private boolean isAdminMenu(String title) {
        return title.contains("Settings") || 
               title.contains("Kits") || 
               title.contains("Effects") ||
               title.contains("Power-ups") ||
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

        // Route to specific menu handlers
        if (title.contains("Main Menu")) {
            handleMainMenuClick(event);
        } else if (title.contains("Team Selector")) {
            handleTeamSelectorClick(event);
        } else if (title.contains("Settings")) {
            handleSettingsClick(event);
        } else if (title.contains("Kits")) {
            handleKitsMenuClick(event);
        } else if (title.contains("Power-ups")) {
            handlePowerUpsMenuClick(event);
        } else if (title.contains("Effects")) {
            handleEffectsMenuClick(event, title);
        } else if (title.contains("World Border")) {
            handleWorldBorderClick(event);
        } else if (title.contains("Bounty")) {
            handleBountyMenuClick(event);
        } else if (title.contains("Last Stand")) {
            handleLastStandClick(event);
        } else if (title.contains("Compass")) {
            handleCompassSettingsClick(event);
        } else if (title.contains("Sudden Death")) {
            handleSuddenDeathClick(event);
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
        }
    }

    private void handleSettingsClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || !clicked.hasItemMeta()) return;

        String buttonId = getButtonId(clicked);
        if (buttonId == null) return;

        switch (buttonId) {
            case "swap_interval":
                cycleSwapInterval(player);
                break;
            case "random_swaps":
                toggleRandomSwaps(player);
                break;
            case "safe_swaps":
                toggleSafeSwaps(player);
                break;
            case "timer_visibility":
                cycleTimerVisibility(player);
                break;
        }

        // Refresh the settings menu
        guiManager.openSettingsMenu(player);
        
        // Update game state
        plugin.getGameManager().refreshSwapSchedule();
        plugin.getGameManager().refreshActionBar();
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
        return item.getType() == Material.BARRIER && 
               item.getItemMeta().displayName() != null &&
               PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName()).contains("Back");
    }

    // Helper methods for settings actions
    private void cycleSwapInterval(Player player) {
        int current = plugin.getConfigManager().getSwapInterval();
        int[] intervals = {30, 60, 120, 180, 300}; // 30s, 1m, 2m, 3m, 5m
        
        int nextIndex = 0;
        for (int i = 0; i < intervals.length; i++) {
            if (current == intervals[i]) {
                nextIndex = (i + 1) % intervals.length;
                break;
            }
        }
        
        plugin.getConfigManager().setSwapInterval(intervals[nextIndex]);
    }

    private void toggleRandomSwaps(Player player) {
        boolean current = plugin.getConfigManager().isSwapRandomized();
        plugin.getConfigManager().setSwapRandomized(!current);
    }

    private void toggleSafeSwaps(Player player) {
        boolean current = plugin.getConfigManager().isSafeSwapEnabled();
        plugin.getConfigManager().setSafeSwapEnabled(!current);
    }

    private void cycleTimerVisibility(Player player) {
        String current = plugin.getConfigManager().getRunnerTimerVisibility();
        String next = switch (current) {
            case "always" -> "last_10";
            case "last_10" -> "never";
            default -> "always";
        };
        plugin.getConfigManager().setRunnerTimerVisibility(next);
    }

    // Other menu handlers
    private void handleTeamSelectorClick(InventoryClickEvent event) {
        // Implement team selector click handling
    }

    // removed duplicate; implemented below
    

    private void handlePowerUpsMenuClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String name = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName());

        if (name.contains("Positive Effects")) {
            guiManager.openPositiveEffectsMenu(player);
            return;
        }
        if (name.contains("Negative Effects")) {
            guiManager.openNegativeEffectsMenu(player);
            return;
        }
        // Duration UI not implemented yet; ignore clicks safely
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

        if (name.contains("Bounty") && name.contains(":")) {
            boolean enabled = plugin.getConfig().getBoolean("bounty.enabled", true);
            plugin.getConfig().set("bounty.enabled", !enabled);
            plugin.saveConfig();
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
            default -> {}
        }
    }

    private void handleSuddenDeathClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String name = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName());

        switch (name) {
            case "§e§lSchedule Sudden Death" -> plugin.getSuddenDeathManager().scheduleSuddenDeath();
            case "§c§lActivate Now" -> plugin.getSuddenDeathManager().activateSuddenDeath();
            case "§6§lActivation Delay (minutes)" -> {
                long minutes = plugin.getConfig().getLong("sudden_death.activation_delay", 120);
                if (event.isLeftClick()) minutes += 5;
                if (event.isRightClick()) minutes -= 5;
                minutes = Math.max(5, Math.min(360, minutes));
                plugin.getConfig().set("sudden_death.activation_delay", minutes);
                plugin.saveConfig();
            }
            default -> {}
        }
        // Refresh
        guiManager.openSuddenDeathMenu(player);
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
