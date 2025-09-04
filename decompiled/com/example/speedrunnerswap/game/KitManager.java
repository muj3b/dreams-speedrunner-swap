/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.PlayerInventory
 */
package com.example.speedrunnerswap.game;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class KitManager {
    private final SpeedrunnerSwap plugin;
    private final Logger logger;

    public KitManager(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public void applyRunnerKit(Player player) {
        this.giveKit(player, "runner");
    }

    public void applyHunterKit(Player player) {
        this.giveKit(player, "hunter");
    }

    public void giveKit(Player player, String kitType) {
        boolean mainEnabled = this.plugin.getConfigManager().isKitsEnabled();
        boolean fileEnabled = this.plugin.getKitConfigManager().getConfig().getBoolean("kits.enabled", mainEnabled);
        if (!mainEnabled || !fileEnabled) {
            return;
        }
        String configPath = "kits." + kitType.toLowerCase();
        ConfigurationSection kitSection = this.plugin.getKitConfigManager().getConfig().getConfigurationSection(configPath);
        if (kitSection == null) {
            this.logger.warning("Kit section '" + configPath + "' not found in kits.yml!");
            return;
        }
        this.clearInventory(player);
        List<ItemStack> items = this.loadKitItems(kitSection);
        PlayerInventory inventory = player.getInventory();
        for (ItemStack item : items) {
            inventory.addItem(new ItemStack[]{item});
        }
        this.giveArmor(player, kitSection);
        player.sendMessage("\u00a76You have received the " + kitType + " kit!");
    }

    private void clearInventory(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
    }

    public List<ItemStack> loadKitItems(ConfigurationSection section) {
        ArrayList<ItemStack> items = new ArrayList<ItemStack>();
        List itemStrings = section.getStringList("items");
        for (String itemString : itemStrings) {
            try {
                if (itemString == null) continue;
                String[] parts = itemString.trim().split("\\s+");
                Material material = Material.valueOf((String)parts[0].toUpperCase());
                int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                items.add(new ItemStack(material, amount));
            }
            catch (Exception e) {
                this.logger.warning("Invalid item in kit: " + itemString);
            }
        }
        return items;
    }

    public ItemStack[] loadKitArmor(ConfigurationSection section) {
        ItemStack[] armor = new ItemStack[4];
        ConfigurationSection armorSection = section.getConfigurationSection("armor");
        if (armorSection == null) {
            return armor;
        }
        try {
            String helmet;
            String chestplate;
            String leggings;
            String boots = armorSection.getString("boots");
            if (boots != null) {
                armor[0] = new ItemStack(Material.valueOf((String)boots.toUpperCase()));
            }
            if ((leggings = armorSection.getString("leggings")) != null) {
                armor[1] = new ItemStack(Material.valueOf((String)leggings.toUpperCase()));
            }
            if ((chestplate = armorSection.getString("chestplate")) != null) {
                armor[2] = new ItemStack(Material.valueOf((String)chestplate.toUpperCase()));
            }
            if ((helmet = armorSection.getString("helmet")) != null) {
                armor[3] = new ItemStack(Material.valueOf((String)helmet.toUpperCase()));
            }
        }
        catch (IllegalArgumentException e) {
            this.logger.warning("Invalid armor material in kit: " + e.getMessage());
        }
        return armor;
    }

    private void giveArmor(Player player, ConfigurationSection section) {
        ConfigurationSection armorSection = section.getConfigurationSection("armor");
        if (armorSection == null) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        inventory.setArmorContents(this.loadKitArmor(section));
    }
}

