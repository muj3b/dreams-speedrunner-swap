package com.example.speedrunnerswap.utils;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Small helpers to bridge Adventure display names/lore with legacy ItemMeta getters.
 */
public final class GuiCompat {
    private GuiCompat() {}

    public static Inventory createInventory(InventoryHolder holder, int size, String title) {
        InventoryHolder effective = (holder != null) ? holder : new com.example.speedrunnerswap.gui.PluginGuiHolder();
        try {
            return Bukkit.createInventory(effective, size, Component.text(title));
        } catch (Throwable t) {
            // Fallback to legacy title if Component API is unavailable
            return createInventoryLegacy(effective, size, title);
        }
    }

    @SuppressWarnings("deprecation")
    private static Inventory createInventoryLegacy(InventoryHolder holder, int size, String title) {
        InventoryHolder effective = (holder != null) ? holder : new com.example.speedrunnerswap.gui.PluginGuiHolder();
        return Bukkit.createInventory(effective, size, title);
    }

    public static void setDisplayName(ItemMeta meta, String name) {
        try {
            meta.displayName(Component.text(name));
        } catch (Throwable t) {
            setDisplayNameLegacy(meta, name);
        }
    }

    public static String getDisplayName(ItemMeta meta) {
        try {
            Component c = meta.displayName();
            if (c != null) {
                return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(c);
            }
        } catch (Throwable ignored) {}
        return getDisplayNameLegacy(meta);
        
    }

    public static void setLore(ItemMeta meta, List<String> legacyLore) {
        try {
            List<Component> list = new ArrayList<>();
            for (String s : legacyLore) list.add(Component.text(s));
            meta.lore(list);
        } catch (Throwable t) {
            setLoreLegacy(meta, legacyLore);
        }
    }

    public static List<String> getLore(ItemMeta meta) {
        try {
            List<Component> comps = meta.lore();
            if (comps != null) {
                List<String> out = new ArrayList<>(comps.size());
                var serializer = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText();
                for (Component c : comps) {
                    out.add(serializer.serialize(c));
                }
                return out;
            }
        } catch (Throwable ignored) {}
        return getLoreLegacy(meta);
    }

    @SuppressWarnings("deprecation")
    private static void setDisplayNameLegacy(ItemMeta meta, String name) {
        try { meta.setDisplayName(name); } catch (Throwable ignored) {}
    }

    @SuppressWarnings("deprecation")
    private static String getDisplayNameLegacy(ItemMeta meta) {
        try { return meta.getDisplayName(); } catch (Throwable ignored) {}
        return "";
    }

    @SuppressWarnings("deprecation")
    private static void setLoreLegacy(ItemMeta meta, List<String> legacyLore) {
        try { meta.setLore(legacyLore); } catch (Throwable ignored) {}
    }

    @SuppressWarnings("deprecation")
    private static List<String> getLoreLegacy(ItemMeta meta) {
        try { return meta.getLore(); } catch (Throwable ignored) {}
        return null;
    }
}
