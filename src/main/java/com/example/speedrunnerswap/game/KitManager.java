package com.example.speedrunnerswap.game;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class KitManager {
    private final SpeedrunnerSwap plugin;
    private final Logger logger;

    public KitManager(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public void applyRunnerKit(Player player) {
        giveKit(player, "runner");
    }

    public void applyHunterKit(Player player) {
        giveKit(player, "hunter");
    }

    public void giveKit(Player player, String kitType) {
        if (!plugin.getConfig().getBoolean("kits.enabled", true)) {
            return;
        }

        String configPath = "kits." + kitType.toLowerCase();
        ConfigurationSection kitSection = plugin.getKitConfigManager().getConfig().getConfigurationSection(configPath);

        if (kitSection == null) {
            logger.warning("Kit section '" + configPath + "' not found in kits.yml!");
            return;
        }

        clearInventory(player);

        // Load and give items
        List<ItemStack> items = loadKitItems(kitSection);
        PlayerInventory inventory = player.getInventory();
        
        for (ItemStack item : items) {
            inventory.addItem(item);
        }

        // Give armor if specified
        giveArmor(player, kitSection);

        player.sendMessage("§6You have received the " + kitType + " kit!");
    }

    private void clearInventory(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
    }

    public List<ItemStack> loadKitItems(ConfigurationSection section) {
        List<ItemStack> items = new ArrayList<>();
        List<String> itemStrings = section.getStringList("items");
        for (String itemString : itemStrings) {
            try {
                String[] parts = itemString.split(" ");
                Material material = Material.valueOf(parts[0].toUpperCase());
                int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                items.add(new ItemStack(material, amount));
            } catch (Exception e) {
                logger.warning("Invalid item in kit: " + itemString);
            }
        }
        return items;
    }

    public ItemStack[] loadKitArmor(ConfigurationSection section) {
        ItemStack[] armor = new ItemStack[4];
        ConfigurationSection armorSection = section.getConfigurationSection("armor");
        if (armorSection == null) return armor;

        try {
            String boots = armorSection.getString("boots");
            if (boots != null) armor[0] = new ItemStack(Material.valueOf(boots.toUpperCase()));
            String leggings = armorSection.getString("leggings");
            if (leggings != null) armor[1] = new ItemStack(Material.valueOf(leggings.toUpperCase()));
            String chestplate = armorSection.getString("chestplate");
            if (chestplate != null) armor[2] = new ItemStack(Material.valueOf(chestplate.toUpperCase()));
            String helmet = armorSection.getString("helmet");
            if (helmet != null) armor[3] = new ItemStack(Material.valueOf(helmet.toUpperCase()));
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid armor material in kit: " + e.getMessage());
        }
        return armor;
    }

    private void giveArmor(Player player, ConfigurationSection section) {
        ConfigurationSection armorSection = section.getConfigurationSection("armor");
        if (armorSection == null) return;

        PlayerInventory inventory = player.getInventory();
        inventory.setArmorContents(loadKitArmor(section));
    }
}
