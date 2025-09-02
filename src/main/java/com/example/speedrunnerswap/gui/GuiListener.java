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
        
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        // Handle effect selection menus
        if (title.contains("Positive Effects") || title.contains("Negative Effects")) {
            handleEffectsMenuClick(event, title);
            return;
        }

        // For other menus
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
        plugin.getConfigManager().setSwapRandomized(!currentValue);
         guiManager.openSettingsMenu((Player) event.getWhoClicked());
     }

    private void handleSafeSwapClick(InventoryClickEvent event) {
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