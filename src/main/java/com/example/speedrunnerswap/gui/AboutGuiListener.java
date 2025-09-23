package com.example.speedrunnerswap.gui;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import com.example.speedrunnerswap.utils.ChatTitleCompat;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class AboutGuiListener implements Listener {
    private final SpeedrunnerSwap plugin;

    public AboutGuiListener(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
    }

    private boolean isAbout(org.bukkit.inventory.Inventory inv) {
        return inv != null && inv.getHolder() instanceof ControlGuiHolder holder && holder.getType() == ControlGuiHolder.Type.ABOUT;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        org.bukkit.inventory.Inventory top = event.getView().getTopInventory();
        if (!isAbout(top)) return;

        event.setCancelled(true); // purely informational

        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        // Route Back via PDC btn=back or arrow
        String btnId = null;
        try {
            btnId = item.getItemMeta().getPersistentDataContainer().get(
                new org.bukkit.NamespacedKey(plugin, "btn"),
                org.bukkit.persistence.PersistentDataType.STRING
            );
        } catch (Throwable ignored) {}
        if (item.getType() == Material.ARROW || "back".equals(btnId)) {
            new ControlGui(plugin).openMainMenu(player);
            return;
        }

        // Donation link on head click
        if (item.getType() != Material.PLAYER_HEAD) return;
        String donateUrl = plugin.getConfig().getString("donation.url", "https://donate.stripe.com/8x29AT0H58K03judnR0Ba01");
        ChatTitleCompat.sendMessage(player, "§6§lControlSwap created by muj3b");
        ChatTitleCompat.sendClickableUrl(player, "§d§l❤ Donate: §r", donateUrl);
        ChatTitleCompat.sendMessage(player, "§7(If the link isn't clickable, copy it from chat.)");
    }
}

