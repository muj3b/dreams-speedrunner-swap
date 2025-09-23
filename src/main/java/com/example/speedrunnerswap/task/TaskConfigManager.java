package com.example.speedrunnerswap.task;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class TaskConfigManager {
    private final SpeedrunnerSwap plugin;
    private FileConfiguration taskConfig;
    private File taskConfigFile;

    public TaskConfigManager(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
        setup();
    }

    public void setup() {
        taskConfigFile = new File(plugin.getDataFolder(), "tasks.yml");
        if (!taskConfigFile.exists()) {
            try {
                plugin.saveResource("tasks.yml", false);
            } catch (IllegalArgumentException ignored) {
                // No bundled resource; create a minimal default file
                try {
                    if (taskConfigFile.getParentFile() != null) taskConfigFile.getParentFile().mkdirs();
                    taskConfigFile.createNewFile();
                    taskConfig = new YamlConfiguration();
                    taskConfig.set("tasks", new java.util.ArrayList<>());
                    taskConfig.save(taskConfigFile);
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "Could not create tasks.yml", e);
                }
            }
        }
        taskConfig = YamlConfiguration.loadConfiguration(taskConfigFile);
    }

    public FileConfiguration getConfig() {
        return taskConfig;
    }

    public void saveConfig() {
        try {
            taskConfig.save(taskConfigFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save tasks.yml", e);
        }
    }

    public void reloadConfig() {
        taskConfig = YamlConfiguration.loadConfiguration(taskConfigFile);
    }
}
