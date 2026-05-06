package com.example.speedrunnerswap.task;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    public boolean isTaskListEmpty() {
        List<?> tasks = taskConfig.getList("tasks");
        return tasks == null || tasks.isEmpty();
    }

    public void saveTasks(List<TaskDefinition> definitions) {
        List<Map<String, Object>> serialized = new ArrayList<>();
        if (definitions != null) {
            for (TaskDefinition definition : definitions) {
                serialized.add(serialize(definition));
            }
        }
        taskConfig.set("tasks", serialized);
        saveConfig();
    }

    public void upsertTask(TaskDefinition definition) {
        if (definition == null || definition.id() == null || definition.id().isBlank()) {
            return;
        }

        List<Map<String, Object>> serialized = new ArrayList<>();
        boolean updated = false;
        List<?> existing = taskConfig.getList("tasks");
        if (existing != null) {
            for (Object entry : existing) {
                if (entry instanceof Map<?, ?> rawMap) {
                    Map<String, Object> taskMap = new LinkedHashMap<>();
                    for (Map.Entry<?, ?> rawEntry : rawMap.entrySet()) {
                        taskMap.put(String.valueOf(rawEntry.getKey()), rawEntry.getValue());
                    }
                    if (definition.id().equals(String.valueOf(taskMap.get("id")))) {
                        serialized.add(serialize(definition));
                        updated = true;
                    } else {
                        serialized.add(taskMap);
                    }
                }
            }
        }

        if (!updated) {
            serialized.add(serialize(definition));
        }
        taskConfig.set("tasks", serialized);
        saveConfig();
    }

    public boolean removeTask(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        List<Map<String, Object>> serialized = new ArrayList<>();
        boolean removed = false;
        List<?> existing = taskConfig.getList("tasks");
        if (existing != null) {
            for (Object entry : existing) {
                if (entry instanceof Map<?, ?> rawMap) {
                    Map<String, Object> taskMap = new LinkedHashMap<>();
                    for (Map.Entry<?, ?> rawEntry : rawMap.entrySet()) {
                        taskMap.put(String.valueOf(rawEntry.getKey()), rawEntry.getValue());
                    }
                    if (id.equals(String.valueOf(taskMap.get("id")))) {
                        removed = true;
                    } else {
                        serialized.add(taskMap);
                    }
                }
            }
        }
        if (removed) {
            taskConfig.set("tasks", serialized);
            saveConfig();
        }
        return removed;
    }

    private Map<String, Object> serialize(TaskDefinition definition) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", definition.id());
        map.put("description", definition.description());
        map.put("type", definition.type().name());
        map.put("difficulty", (definition.difficulty() != null ? definition.difficulty() : TaskDifficulty.MEDIUM).name());
        map.put("categories", definition.categories() != null ? new ArrayList<>(definition.categories()) : List.of());
        map.put("enabled", definition.enabled());
        if (definition.params() != null && !definition.params().isEmpty()) {
            map.put("params", new ArrayList<>(definition.params()));
        }
        return map;
    }
}
