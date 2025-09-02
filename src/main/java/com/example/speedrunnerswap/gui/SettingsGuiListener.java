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

public class SettingsGuiListener implements Listener {
    private final SpeedrunnerSwap plugin;
    private final GuiManager guiManager;

    public SettingsGuiListener(SpeedrunnerSwap plugin, GuiManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        net.kyori.adventure.text.Component title = event.getView().title();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        event.setCancelled(true);

        // Handle back button clicks (arrow)
        if (clickedItem.getType() == Material.ARROW) {
            if (clickedItem.hasItemMeta() && clickedItem.getItemMeta().displayName() != null) {
                String name = PlainTextComponentSerializer.plainText().serialize(clickedItem.getItemMeta().displayName());
                if (name.contains("Back")) {
                    guiManager.openMainMenu(player);
                    return;
                }
            }
        }

        // Handle different menus (match by title contains to be more robust)
        String titleText = PlainTextComponentSerializer.plainText().serialize(title);

        if (titleText.contains("World Border")) {
            handleWorldBorderClick(event);
        } else if (titleText.contains("Power-ups")) {
            handlePowerUpsClick(event);
        }
        // Add more cases for other menus
    }

    private void handleWorldBorderClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getItemMeta() == null) return;
        Component displayName = clicked.getItemMeta().displayName();
        if (displayName == null) return;
        String itemName = PlainTextComponentSerializer.plainText().serialize(displayName);

        switch (itemName) {
            case "§e§lToggle World Border":
                boolean currentState = plugin.getConfig().getBoolean("world_border.enabled", true);
                plugin.getConfig().set("world_border.enabled", !currentState);
                plugin.saveConfig();
                if (plugin.getWorldBorderManager() != null) {
                    if (!currentState) {
                        plugin.getWorldBorderManager().startBorderShrinking();
                    } else {
                        plugin.getWorldBorderManager().stopBorderShrinking();
                    }
                }
                break;

            case "§a§lInitial Border Size":
                int initialSize = plugin.getConfig().getInt("world_border.initial_size", 2000);
                if (event.isLeftClick()) {
                    initialSize += event.isShiftClick() ? 500 : 100;
                } else if (event.isRightClick()) {
                    initialSize -= event.isShiftClick() ? 500 : 100;
                }
                initialSize = Math.max(100, Math.min(29999984, initialSize));
                plugin.getConfig().set("world_border.initial_size", initialSize);
                plugin.saveConfig();
                break;

            case "§c§lFinal Border Size":
                int finalSize = plugin.getConfig().getInt("world_border.final_size", 100);
                if (event.isLeftClick()) {
                    finalSize += event.isShiftClick() ? 100 : 50;
                } else if (event.isRightClick()) {
                    finalSize -= event.isShiftClick() ? 100 : 50;
                }
                finalSize = Math.max(50, Math.min(1000, finalSize));
                plugin.getConfig().set("world_border.final_size", finalSize);
                plugin.saveConfig();
                break;

            case "§6§lShrink Duration":
                int duration = plugin.getConfig().getInt("world_border.shrink_duration", 1800);
                if (event.isLeftClick()) {
                    duration += event.isShiftClick() ? 900 : 300; // +15 or +5 minutes
                } else if (event.isRightClick()) {
                    duration -= event.isShiftClick() ? 900 : 300;
                }
                duration = Math.max(300, Math.min(7200, duration)); // 5 minutes to 2 hours
                plugin.getConfig().set("world_border.shrink_duration", duration);
                plugin.saveConfig();
                break;
        }

        // Refresh the menu
        guiManager.openWorldBorderMenu(player);
    }

    private void handlePowerUpsClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getItemMeta() == null) return;
        Component displayName = clicked.getItemMeta().displayName();
        if (displayName == null) return;
        String itemName = PlainTextComponentSerializer.plainText().serialize(displayName);

        switch (itemName) {
            case "§e§lToggle Power-ups":
                boolean currentState = plugin.getConfig().getBoolean("power_ups.enabled", true);
                plugin.getConfig().set("power_ups.enabled", !currentState);
                plugin.saveConfig();
                break;

            case "§a§lPositive Effects":
                // Open sub-menu for selecting positive effects
                openEffectSelectionMenu(player, true);
                return;

            case "§c§lNegative Effects":
                // Open sub-menu for selecting negative effects
                openEffectSelectionMenu(player, false);
                return;
        }

        // Refresh the menu
        guiManager.openPowerUpsMenu(player);
    }

    private void openEffectSelectionMenu(Player player, boolean positive) {
        // Implementation for effect selection menu
        // This would be implemented in the GuiManager
        if (positive) {
            guiManager.openPositiveEffectsMenu(player);
        } else {
            guiManager.openNegativeEffectsMenu(player);
        }
    }
}
