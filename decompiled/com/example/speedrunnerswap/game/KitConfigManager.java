/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.inventory.ItemStack
 */
package com.example.speedrunnerswap.game;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.CallSite;
import java.util.ArrayList;
import java.util.logging.Level;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

public class KitConfigManager {
    private final SpeedrunnerSwap plugin;
    private FileConfiguration kitConfig;
    private File kitConfigFile;

    public KitConfigManager(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
        this.setup();
    }

    public void setup() {
        this.kitConfigFile = new File(this.plugin.getDataFolder(), "kits.yml");
        if (!this.kitConfigFile.exists()) {
            this.plugin.saveResource("kits.yml", false);
        }
        this.kitConfig = YamlConfiguration.loadConfiguration((File)this.kitConfigFile);
    }

    public FileConfiguration getConfig() {
        return this.kitConfig;
    }

    public void saveConfig() {
        try {
            this.kitConfig.save(this.kitConfigFile);
        }
        catch (IOException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Could not save kits.yml", e);
        }
    }

    public void reloadConfig() {
        this.kitConfig = YamlConfiguration.loadConfiguration((File)this.kitConfigFile);
    }

    public void saveKit(String kitType, ItemStack[] contents, ItemStack[] armor) {
        ArrayList<CallSite> itemStrings = new ArrayList<CallSite>();
        for (ItemStack item : contents) {
            if (item == null) continue;
            itemStrings.add((CallSite)((Object)(item.getType().name() + " " + item.getAmount())));
        }
        this.kitConfig.set("kits." + kitType + ".items", itemStrings);
        if (armor[3] != null) {
            this.kitConfig.set("kits." + kitType + ".armor.helmet", (Object)armor[3].getType().name());
        }
        if (armor[2] != null) {
            this.kitConfig.set("kits." + kitType + ".armor.chestplate", (Object)armor[2].getType().name());
        }
        if (armor[1] != null) {
            this.kitConfig.set("kits." + kitType + ".armor.leggings", (Object)armor[1].getType().name());
        }
        if (armor[0] != null) {
            this.kitConfig.set("kits." + kitType + ".armor.boots", (Object)armor[0].getType().name());
        }
        this.saveConfig();
    }
}

