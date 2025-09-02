package com.example.speedrunnerswap.game;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class KitConfigManager {
    private final SpeedrunnerSwap plugin;
    private FileConfiguration kitConfig;
    private File kitConfigFile;

    public KitConfigManager(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
        setup();
    }

    public void setup() {
        kitConfigFile = new File(plugin.getDataFolder(), "kits.yml");
        if (!kitConfigFile.exists()) {
            plugin.saveResource("kits.yml", false);
        }
        kitConfig = YamlConfiguration.loadConfiguration(kitConfigFile);
    }

    public FileConfiguration getConfig() {
        return kitConfig;
    }

    public void saveConfig() {
        try {
            kitConfig.save(kitConfigFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save kits.yml", e);
        }
    }

    public void reloadConfig() {
        kitConfig = YamlConfiguration.loadConfiguration(kitConfigFile);
    }

    public void saveKit(String kitType, ItemStack[] contents, ItemStack[] armor) {
        List<String> itemStrings = new ArrayList<>();
        for (ItemStack item : contents) {
            if (item != null) {
                itemStrings.add(item.getType().name() + " " + item.getAmount());
            }
        }
        kitConfig.set("kits." + kitType + ".items", itemStrings);

        if (armor[3] != null) kitConfig.set("kits." + kitType + ".armor.helmet", armor[3].getType().name());
        if (armor[2] != null) kitConfig.set("kits." + kitType + ".armor.chestplate", armor[2].getType().name());
        if (armor[1] != null) kitConfig.set("kits." + kitType + ".armor.leggings", armor[1].getType().name());
        if (armor[0] != null) kitConfig.set("kits." + kitType + ".armor.boots", armor[0].getType().name());

        saveConfig();
    }
}
