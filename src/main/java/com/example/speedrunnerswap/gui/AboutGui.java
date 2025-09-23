package com.example.speedrunnerswap.gui;
import com.example.speedrunnerswap.SpeedrunnerSwap;
import com.example.speedrunnerswap.utils.GuiCompat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class AboutGui {
    private static final String TITLE = "ControlSwap — About (1.21 UI)";

    public AboutGui() {}

    public void openFor(Player player) {
        Inventory inv = com.example.speedrunnerswap.utils.GuiCompat.createInventory(new ControlGuiHolder(ControlGuiHolder.Type.ABOUT), 9, TITLE);

        // Filler panes
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        GuiCompat.setDisplayName(fm, " ");
        filler.setItemMeta(fm);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);

        // Creator head in the top-right corner
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        try {
            OfflinePlayer creator = Bukkit.getOfflinePlayer("muj3b");
            meta.setOwningPlayer(creator);
        } catch (Throwable ignored) {}
        GuiCompat.setDisplayName(meta, "Creator: muj3b");
        List<String> lore = new ArrayList<>();
        lore.add("Click to open donation link");
        GuiCompat.setLore(meta, lore);
        head.setItemMeta(meta);
        inv.setItem(8, head);

        // Real Back arrow (slot 0), PDC-tagged
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bm = back.getItemMeta();
        GuiCompat.setDisplayName(bm, "§7§lBack");
        java.util.List<String> blore = new java.util.ArrayList<>();
        blore.add("§7Return to control menu");
        GuiCompat.setLore(bm, blore);
        try {
            bm.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(SpeedrunnerSwap.getInstance(), "btn"),
                org.bukkit.persistence.PersistentDataType.STRING,
                "back"
            );
        } catch (Throwable ignored) {}
        back.setItemMeta(bm);
        inv.setItem(0, back);

        // Use centralized opener to avoid dead-GUI edge cases
        SpeedrunnerSwap.getInstance().getGuiManager().openInventorySoon(player, inv);
    }

    public static String getTitle() {
        return TITLE;
    }
}

