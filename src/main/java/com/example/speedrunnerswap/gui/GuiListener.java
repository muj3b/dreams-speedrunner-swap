package com.example.speedrunnerswap.gui;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class GuiListener implements Listener {
    private final SpeedrunnerSwap plugin;
    private final GuiManager guiManager;

    private void handleEffectsMenuClick(InventoryClickEvent event, String title) {
        event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        var meta = clickedItem.getItemMeta();
        if (meta == null || !meta.hasLore()) return;
        
        List<Component> loreComponents = meta.lore();
        if (loreComponents == null) return;
        
        String effectId = null;
        for (Component line : loreComponents) {
            String plainText = PlainTextComponentSerializer.plainText().serialize(line);
            if (plainText.startsWith("§7Effect ID: §f")) {
                effectId = plainText.replace("§7Effect ID: §f", "");
                break;
            }
        }

        if (effectId != null) {
            boolean isPositive = title.contains("Positive Effects");
            String configPath = isPositive ? "power_ups.good_effects" : "power_ups.bad_effects";
            List<String> effects = plugin.getConfig().getStringList(configPath);

            if (effects.contains(effectId)) {
                effects.remove(effectId);
            } else {
                effects.add(effectId);
            }

            plugin.getConfig().set(configPath, effects);
            plugin.saveConfig();

            // Refresh the menu
            Player player = (Player) event.getWhoClicked();
            if (isPositive) {
                guiManager.openPositiveEffectsMenu(player);
            } else {
                guiManager.openNegativeEffectsMenu(player);
            }
        }

        if (clickedItem.getType() == Material.BARRIER) {
            Player player = (Player) event.getWhoClicked();
            guiManager.openPowerUpsMenu(player);
        }
    }

    public GuiListener(SpeedrunnerSwap plugin, GuiManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        // Admin permission check for settings
        if (!player.hasPermission("speedrunnerswap.admin")) {
            if (title.contains("Settings") || title.contains("Kits") || title.contains("Bounty") || title.contains("Last Stand") || title.contains("Compass") || title.contains("Sudden Death")) {
                player.sendMessage("§cYou do not have permission to change these settings.");
                event.setCancelled(true);
                return;
            }
        }
        
        // Handle effect selection menus
        if (title.contains("Positive Effects") || title.contains("Negative Effects")) {
            handleEffectsMenuClick(event, title);
            return;
        }

    // For other menus
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null) return;

        // Handle Settings Menu
        if (guiManager.isSettingsMenu(event.getInventory())) {
            event.setCancelled(true);

            // Check if clicked item is valid
            if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;
            
            String buttonId = guiManager.getButtonId(event.getCurrentItem());
            if (buttonId == null) return;

            switch (buttonId) {
                case "back":
                    guiManager.openMainMenu(player);
                    break;
                case "swap_interval":
                    handleSwapIntervalClick(event);
                    break;
                case "random_swaps":
                    handleRandomizeSwapClick(event);
                    break;
                case "safe_swaps":
                    handleSafeSwapClick(event);
                    break;
                case "active_runner_timer":
                    handleActiveRunnerTimerClick(event);
                    break;
                case "waiting_runner_timer":
                    handleWaitingRunnerTimerClick(event);
                    break;
                case "hunter_timer":
                    handleHunterTimerClick(event);
                    break;
            }

            // Apply live refreshes where needed
            plugin.getGameManager().refreshSwapSchedule();
            plugin.getGameManager().refreshHunterSwapSchedule();
            plugin.getGameManager().refreshActionBar();
            plugin.getGameManager().refreshTracker();
            // Apply live refreshes where needed
            plugin.getGameManager().refreshSwapSchedule();
            plugin.getGameManager().refreshHunterSwapSchedule();
            plugin.getGameManager().refreshActionBar();
            plugin.getGameManager().refreshTracker();
        }

        // If main menu, route clicks to submenus
        if (guiManager.isMainMenu(event.getInventory())) {
            event.setCancelled(true);
            if (guiManager.isTeamSelectorButton(clickedItem)) {
                guiManager.openTeamSelector(player);
            } else if (guiManager.isSettingsButton(clickedItem)) {
                guiManager.openSettingsMenu(player);
            } else if (clickedItem.getType() == Material.POTION || clickedItem.getType() == Material.SPLASH_POTION) {
                guiManager.openPowerUpsMenu(player);
            } else if (clickedItem.getType() == Material.BARRIER && clickedItem.getItemMeta() != null && clickedItem.getItemMeta().displayName() != null && PlainTextComponentSerializer.plainText().serialize(clickedItem.getItemMeta().displayName()).contains("World Border")) {
                guiManager.openWorldBorderMenu(player);
            } else if (clickedItem.getType() == Material.DIAMOND_CHESTPLATE) {
                guiManager.openKitsMenu(player);
            } else if (clickedItem.getType() == Material.GOLDEN_APPLE) {
                guiManager.openBountyMenu(player);
            } else if (clickedItem.getType() == Material.TOTEM_OF_UNDYING) {
                guiManager.openLastStandMenu(player);
            } else if (clickedItem.getType() == Material.COMPASS) {
                guiManager.openCompassSettingsMenu(player);
            } else if (clickedItem.getType() == Material.DRAGON_HEAD) {
                guiManager.openSuddenDeathMenu(player);
            } else if (clickedItem.getType() == Material.BOOK) {
                guiManager.openStatisticsMenu(player);
            }
        }

        // Handle Kits menu clicks
        if (PlainTextComponentSerializer.plainText().serialize(event.getView().title()).contains("Kits")) {
            event.setCancelled(true);
            if (clickedItem.getType() == Material.ARROW) {
                guiManager.openMainMenu(player);
                return;
            }
            if (clickedItem.getType() == Material.LIME_DYE || clickedItem.getType() == Material.GRAY_DYE) {
                // Toggle kits
                boolean current = plugin.getConfigManager().isKitsEnabled();
                plugin.getConfigManager().setKitsEnabled(!current);
                guiManager.openKitsMenu(player);
                return;
            }
            if (clickedItem.getType() == Material.DIAMOND_BOOTS) {
                // Give runner kit to clicking player
                plugin.getKitManager().applyRunnerKit(player);
                player.sendMessage("§aRunner kit applied.");
                return;
            }
            if (clickedItem.getType() == Material.IRON_SWORD) {
                // Give hunter kit
                plugin.getKitManager().applyHunterKit(player);
                player.sendMessage("§aHunter kit applied.");
                return;
            }
            if (clickedItem.getType() == Material.CRAFTING_TABLE) {
                String displayName = PlainTextComponentSerializer.plainText().serialize(clickedItem.getItemMeta().displayName());
                if (displayName.contains("Runner")) {
                    guiManager.openKitEditor(player, "runner");
                } else if (displayName.contains("Hunter")) {
                    guiManager.openKitEditor(player, "hunter");
                }
            }
        }

        // Handle Kit Editor
        if (title.startsWith("§e§lEdit")) {
            event.setCancelled(true);
            if (clickedItem != null && clickedItem.getType() == Material.GREEN_CONCRETE) {
                String kitType = title.contains("Runner") ? "runner" : "hunter";
                ItemStack[] contents = event.getInventory().getContents();
                ItemStack[] armor = new ItemStack[]{contents[48], contents[47], contents[46], contents[45]};
                // Clear armor slots and save button from contents
                contents[45] = null;
                contents[46] = null;
                contents[47] = null;
                contents[48] = null;
                contents[53] = null;
                plugin.getKitConfigManager().saveKit(kitType, contents, armor);
                player.sendMessage("§a" + kitType + " kit saved!");
                guiManager.openKitsMenu(player);
            }
        }

        // Handle Bounty menu clicks
        if (PlainTextComponentSerializer.plainText().serialize(event.getView().title()).contains("Bounty")) {
            event.setCancelled(true);
            if (clickedItem.getType() == Material.ARROW) {
                guiManager.openMainMenu(player);
                return;
            }
            if (clickedItem.getType() == Material.GOLDEN_APPLE) {
                plugin.getBountyManager().assignNewBounty();
                player.sendMessage("§eNew bounty assigned.");
                guiManager.openBountyMenu(player);
                return;
            }
            if (clickedItem.getType() == Material.BARRIER) {
                plugin.getBountyManager().clearBounty();
                player.sendMessage("§cBounty cleared.");
                guiManager.openBountyMenu(player);
                return;
            }
        }

        // Handle Last Stand menu clicks
        if (PlainTextComponentSerializer.plainText().serialize(event.getView().title()).contains("Last Stand")) {
            event.setCancelled(true);
            if (clickedItem.getType() == Material.ARROW) {
                guiManager.openMainMenu(player);
                return;
            }
            if (clickedItem.getType() == Material.TOTEM_OF_UNDYING || clickedItem.getType() == Material.BARRIER) {
                boolean current = plugin.getConfigManager().isLastStandEnabled();
                plugin.getConfig().set("last_stand.enabled", !current);
                plugin.saveConfig();
                guiManager.openLastStandMenu(player);
                return;
            }
            if (clickedItem.getType() == Material.CLOCK) {
                // Adjust duration (left/right click)
                int current = plugin.getConfigManager().getLastStandDuration();
                if (event.isLeftClick()) plugin.getConfig().set("last_stand.duration_ticks", current + 100);
                if (event.isRightClick()) plugin.getConfig().set("last_stand.duration_ticks", Math.max(20, current - 100));
                plugin.saveConfig();
                guiManager.openLastStandMenu(player);
                return;
            }
            if (clickedItem.getType() == Material.BLAZE_POWDER) {
                int current = plugin.getConfigManager().getLastStandStrengthAmplifier();
                if (event.isLeftClick()) plugin.getConfig().set("last_stand.strength_amplifier", current + 1);
                if (event.isRightClick()) plugin.getConfig().set("last_stand.strength_amplifier", Math.max(0, current - 1));
                plugin.saveConfig();
                guiManager.openLastStandMenu(player);
                return;
            }
        }

        // Handle Compass Settings menu
        if (PlainTextComponentSerializer.plainText().serialize(event.getView().title()).contains("Compass Settings")) {
            event.setCancelled(true);
            if (clickedItem.getType() == Material.ARROW) {
                guiManager.openMainMenu(player);
                return;
            }
            if (clickedItem.getType() == Material.REDSTONE_BLOCK || clickedItem.getType() == Material.GRAY_DYE) {
                boolean current = plugin.getConfigManager().isCompassJammingEnabled();
                plugin.getConfig().set("tracker.compass_jamming.enabled", !current);
                plugin.saveConfig();
                guiManager.openCompassSettingsMenu(player);
                return;
            }
            if (clickedItem.getType() == Material.CLOCK) {
                int current = plugin.getConfigManager().getCompassJamDuration();
                if (event.isLeftClick()) plugin.getConfig().set("tracker.compass_jamming.duration_ticks", current + 20);
                if (event.isRightClick()) plugin.getConfig().set("tracker.compass_jamming.duration_ticks", Math.max(0, current - 20));
                plugin.saveConfig();
                guiManager.openCompassSettingsMenu(player);
                return;
            }
        }

        // Handle Sudden Death menu
        if (PlainTextComponentSerializer.plainText().serialize(event.getView().title()).contains("Sudden Death")) {
            event.setCancelled(true);
            if (clickedItem.getType() == Material.ARROW) {
                guiManager.openMainMenu(player);
                return;
            }
            if (clickedItem.getType() == Material.CLOCK && PlainTextComponentSerializer.plainText().serialize(clickedItem.getItemMeta().displayName()).contains("Schedule")) {
                plugin.getSuddenDeathManager().scheduleSuddenDeath();
                player.sendMessage("§eSudden Death scheduled.");
                guiManager.openSuddenDeathMenu(player);
                return;
            }
            if (clickedItem.getType() == Material.DRAGON_HEAD) {
                plugin.getSuddenDeathManager().activateSuddenDeath();
                player.sendMessage("§cSudden Death activated.");
                guiManager.openSuddenDeathMenu(player);
                return;
            }
            if (clickedItem.getType() == Material.CLOCK && PlainTextComponentSerializer.plainText().serialize(clickedItem.getItemMeta().displayName()).contains("Activation Delay")) {
                long current = plugin.getConfig().getLong("sudden_death.activation_delay", 120);
                if (event.isLeftClick()) plugin.getConfig().set("sudden_death.activation_delay", Math.max(1, current + 5));
                if (event.isRightClick()) plugin.getConfig().set("sudden_death.activation_delay", Math.max(1, current - 5));
                plugin.saveConfig();
                guiManager.openSuddenDeathMenu(player);
                return;
            }
        }

        // Handle Statistics menu
        if (PlainTextComponentSerializer.plainText().serialize(event.getView().title()).contains("Statistics")) {
            event.setCancelled(true);
            if (clickedItem.getType() == Material.ARROW) {
                guiManager.openMainMenu(player);
                return;
            }
            if (clickedItem.getType() == Material.BOOK) {
                plugin.getStatsManager().displayStats();
                player.sendMessage("§eStatistics displayed.");
                return;
            }
            if (clickedItem.getType() == Material.GREEN_CONCRETE) {
                plugin.getStatsManager().startTracking();
                player.sendMessage("§aStats tracking started.");
                return;
            }
            if (clickedItem.getType() == Material.RED_CONCRETE) {
                plugin.getStatsManager().stopTracking();
                player.sendMessage("§cStats tracking stopped and broadcasted.");
                return;
            }
        }
    }

    private void handleSwapIntervalClick(InventoryClickEvent event) {
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !guiManager.isSwapIntervalButton(clickedItem)) return;
        
        int currentInterval = plugin.getConfigManager().getSwapInterval();
        if (event.isLeftClick()) {
            if (event.isShiftClick()) {
                plugin.getConfigManager().setSwapInterval(currentInterval + 60);
            } else {
                plugin.getConfigManager().setSwapInterval(currentInterval + 30);
            }
        } else if (event.isRightClick()) {
            if (event.isShiftClick()) {
                plugin.getConfigManager().setSwapInterval(Math.max(30, currentInterval - 60));
            } else {
                plugin.getConfigManager().setSwapInterval(Math.max(30, currentInterval - 30));
            }
        }
        guiManager.openSettingsMenu((Player) event.getWhoClicked());
    }

    private void handleRandomizeSwapClick(InventoryClickEvent event) {
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !guiManager.isRandomizeSwapButton(clickedItem)) return;
        
        boolean currentValue = plugin.getConfigManager().isSwapRandomized();
        plugin.getConfigManager().setSwapRandomized(!currentValue);
        guiManager.openSettingsMenu((Player) event.getWhoClicked());
    }

    private void handleSafeSwapClick(InventoryClickEvent event) {
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !guiManager.isSafeSwapButton(clickedItem)) return;
        
        boolean currentValue = plugin.getConfigManager().isSafeSwapEnabled();
        plugin.getConfigManager().setSafeSwapEnabled(!currentValue);
        guiManager.openSettingsMenu((Player) event.getWhoClicked());
    }

    private void handleActiveRunnerTimerClick(InventoryClickEvent event) {
        String currentVisibility = plugin.getConfigManager().getRunnerTimerVisibility();
        String nextVisibility = guiManager.getNextVisibility(currentVisibility);
        plugin.getConfigManager().setRunnerTimerVisibility(nextVisibility);
        guiManager.openSettingsMenu((Player) event.getWhoClicked());
    }

    private void handleWaitingRunnerTimerClick(InventoryClickEvent event) {
        String currentVisibility = plugin.getConfigManager().getWaitingTimerVisibility();
        String nextVisibility = guiManager.getNextVisibility(currentVisibility);
        plugin.getConfigManager().setWaitingTimerVisibility(nextVisibility);
        guiManager.openSettingsMenu((Player) event.getWhoClicked());
    }

    private void handleHunterTimerClick(InventoryClickEvent event) {
        String currentVisibility = plugin.getConfigManager().getHunterTimerVisibility();
        String nextVisibility = guiManager.getNextVisibility(currentVisibility);
        plugin.getConfigManager().setHunterTimerVisibility(nextVisibility);
        guiManager.openSettingsMenu((Player) event.getWhoClicked());
    }


}