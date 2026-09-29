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
        // Respect both config.yml and kits.yml toggles; both must allow kits
        boolean mainEnabled = plugin.getConfigManager().isKitsEnabled();
        boolean fileEnabled = plugin.getKitConfigManager().getConfig().getBoolean("kits.enabled", mainEnabled);
        if (!(mainEnabled && fileEnabled)) {
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
                if (itemString == null) continue;
                String[] parts = itemString.trim().split("\\s+");
                Material material = Material.matchMaterial(parts[0]);
                if (material == null || !material.isItem() || material.isAir()) throw new IllegalArgumentException("Unsupported item");
                int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                if (amount < 1 || amount > material.getMaxStackSize()) throw new IllegalArgumentException("Invalid stack amount");
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

        String[] slots = {"boots", "leggings", "chestplate", "helmet"};
        for (int i = 0; i < slots.length; i++) {
            String name = armorSection.getString(slots[i]);
            if (name == null) continue;
            Material material = Material.matchMaterial(name);
            if (material == null || !material.isItem() || material.isAir()) {
                logger.warning("Unsupported armor material in kit " + slots[i] + ": " + name);
            } else armor[i] = new ItemStack(material);
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
