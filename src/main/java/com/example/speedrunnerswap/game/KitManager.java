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
        ConfigurationSection kitSection = plugin.getConfig().getConfigurationSection(configPath);

        if (kitSection == null) {
            logger.warning("Kit section '" + configPath + "' not found in config!");
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

    private List<ItemStack> loadKitItems(ConfigurationSection section) {
        List<ItemStack> items = new ArrayList<>();
        
        for (String key : section.getKeys(false)) {
            if (key.equals("armor")) continue;

            if (section.isConfigurationSection(key)) {
                ConfigurationSection itemSection = section.getConfigurationSection(key);
                if (itemSection != null) {
                    try {
                        Material material = Material.valueOf(itemSection.getString("material", "").toUpperCase());
                        int amount = itemSection.getInt("amount", 1);
                        items.add(new ItemStack(material, amount));
                    } catch (IllegalArgumentException e) {
                        logger.warning("Invalid material in kit: " + itemSection.getString("material"));
                    }
                }
            }
        }

        return items;
    }

    private void giveArmor(Player player, ConfigurationSection section) {
        ConfigurationSection armorSection = section.getConfigurationSection("armor");
        if (armorSection == null) return;

        PlayerInventory inventory = player.getInventory();

        try {
            String helmet = armorSection.getString("helmet");
            if (helmet != null) {
                inventory.setHelmet(new ItemStack(Material.valueOf(helmet.toUpperCase())));
            }

            String chestplate = armorSection.getString("chestplate");
            if (chestplate != null) {
                inventory.setChestplate(new ItemStack(Material.valueOf(chestplate.toUpperCase())));
            }

            String leggings = armorSection.getString("leggings");
            if (leggings != null) {
                inventory.setLeggings(new ItemStack(Material.valueOf(leggings.toUpperCase())));
            }

            String boots = armorSection.getString("boots");
            if (boots != null) {
                inventory.setBoots(new ItemStack(Material.valueOf(boots.toUpperCase())));
            }
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid armor material in kit: " + e.getMessage());
        }
    }
}
