package com.example.speedrunnerswap.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Marker holder for plugin-managed GUIs created via GuiCompat when a specific holder is not supplied.
 * Using a custom holder is more reliable than title-based detection across server versions.
 */
public final class PluginGuiHolder implements InventoryHolder {
    @Override
    public Inventory getInventory() {
        return null; // Not used; Bukkit will manage inventory instance separately
    }
}
