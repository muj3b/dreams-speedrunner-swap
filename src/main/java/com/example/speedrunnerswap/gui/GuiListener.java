package com.example.speedrunnerswap.gui;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class GuiListener implements Listener {
    private final SpeedrunnerSwap plugin;
    private final GuiManager guiManager;

    public GuiListener(SpeedrunnerSwap plugin, GuiManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null) return;

        // Handle Settings Menu
        if (guiManager.isSettingsMenu(event.getInventory())) {
            event.setCancelled(true);

            if (guiManager.isBackButton(clickedItem)) {
                guiManager.openMainMenu(player);
            }
            else if (guiManager.isSwapIntervalButton(clickedItem)) {
                handleSwapIntervalClick(event);
            }
            else if (guiManager.isRandomizeSwapButton(clickedItem)) {
                handleRandomizeSwapClick(event);
            }
            else if (guiManager.isSafeSwapButton(clickedItem)) {
                handleSafeSwapClick(event);
            }
            else if (guiManager.isActiveRunnerTimerButton(clickedItem)) {
                handleActiveRunnerTimerClick(event);
            }
            else if (guiManager.isWaitingRunnerTimerButton(clickedItem)) {
                handleWaitingRunnerTimerClick(event);
            }
            else if (guiManager.isHunterTimerButton(clickedItem)) {
                handleHunterTimerClick(event);
            }
        }
    }

    private void handleSwapIntervalClick(InventoryClickEvent event) {
        int currentInterval = plugin.getConfigManager().getSwapInterval();
        if (event.isLeftClick()) {
            plugin.getConfigManager().setSwapInterval(currentInterval + 30);
        } else if (event.isRightClick()) {
            plugin.getConfigManager().setSwapInterval(Math.max(30, currentInterval - 30));
        } else if (event.isShiftClick()) {
            plugin.getConfigManager().setSwapInterval(60); // Reset to default
        }
        guiManager.openSettingsMenu((Player) event.getWhoClicked());
    }

    private void handleRandomizeSwapClick(InventoryClickEvent event) {
        boolean currentValue = plugin.getConfigManager().isSwapRandomized();
        plugin.getConfigManager().setRandomizeSwap(!currentValue);
        guiManager.openSettingsMenu((Player) event.getWhoClicked());
    }

    private void handleSafeSwapClick(InventoryClickEvent event) {
        boolean currentValue = plugin.getConfigManager().isSafeSwapEnabled();
        plugin.getConfigManager().setSafeSwapEnabled(!currentValue);
        guiManager.openSettingsMenu((Player) event.getWhoClicked());
    }

    private void handleActiveRunnerTimerClick(InventoryClickEvent event) {
        String currentVisibility = plugin.getConfigManager().getRunnerTimerVisibility();
        String nextVisibility = getNextVisibility(currentVisibility);
        plugin.getConfigManager().setRunnerTimerVisibility(nextVisibility);
        guiManager.openSettingsMenu((Player) event.getWhoClicked());
    }

    private void handleWaitingRunnerTimerClick(InventoryClickEvent event) {
        String currentVisibility = plugin.getConfigManager().getWaitingTimerVisibility();
        String nextVisibility = getNextVisibility(currentVisibility);
        plugin.getConfigManager().setWaitingTimerVisibility(nextVisibility);
        guiManager.openSettingsMenu((Player) event.getWhoClicked());
    }

    private void handleHunterTimerClick(InventoryClickEvent event) {
        String currentVisibility = plugin.getConfigManager().getHunterTimerVisibility();
        String nextVisibility = getNextVisibility(currentVisibility);
        plugin.getConfigManager().setHunterTimerVisibility(nextVisibility);
        guiManager.openSettingsMenu((Player) event.getWhoClicked());
    }

    private String getNextVisibility(String current) {
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
}