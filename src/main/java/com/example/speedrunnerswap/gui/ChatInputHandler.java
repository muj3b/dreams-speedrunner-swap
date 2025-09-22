package com.example.speedrunnerswap.gui;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChatInputHandler implements Listener {
    private final SpeedrunnerSwap plugin;
    private final Map<UUID, InputState> activeInputs = new HashMap<>();
    
    private static class InputState {
        enum Type { TASK_ID, TASK_DESCRIPTION, DONATION_URL, CONFIG_STRING, CONFIG_LIST_ADD }
        final Type type;
        String taskId; // Store task ID when collecting description
        String configPath; // Path for config edits
        
        InputState(Type type) {
            this.type = type;
        }
    }
    
    public ChatInputHandler(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
    }
    
    public void expectTaskId(Player player) {
        activeInputs.put(player.getUniqueId(), new InputState(InputState.Type.TASK_ID));
    }
    
    public void expectTaskDescription(Player player, String taskId) {
        InputState state = new InputState(InputState.Type.TASK_DESCRIPTION);
        state.taskId = taskId;
        activeInputs.put(player.getUniqueId(), state);
    }
    
    public void expectTaskDescription(Player player) {
        // For when we already have the task ID stored
        activeInputs.put(player.getUniqueId(), new InputState(InputState.Type.TASK_DESCRIPTION));
    }
    
    public void expectDonationUrl(Player player) {
        activeInputs.put(player.getUniqueId(), new InputState(InputState.Type.DONATION_URL));
    }
    
    public void expectConfigString(Player player, String path) {
        InputState st = new InputState(InputState.Type.CONFIG_STRING);
        st.configPath = path;
        activeInputs.put(player.getUniqueId(), st);
    }
    
    public void expectConfigListAdd(Player player, String path) {
        InputState st = new InputState(InputState.Type.CONFIG_LIST_ADD);
        st.configPath = path;
        activeInputs.put(player.getUniqueId(), st);
    }
    
@SuppressWarnings("deprecation")
@EventHandler
public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        if (!activeInputs.containsKey(uuid)) return;
        
        event.setCancelled(true);
        InputState state = activeInputs.remove(uuid);
        String input = event.getMessage().trim();
        
        if (input.equalsIgnoreCase("cancel")) {
            player.sendMessage("§c[Task Manager] Input cancelled.");
            plugin.getServer().getScheduler().runTask(plugin, () -> 
                plugin.getGuiManager().openCustomTasksMenu(player));
            return;
        }
        
        switch (state.type) {
            case TASK_ID:
                handleTaskId(player, input);
                break;
            case TASK_DESCRIPTION:
                handleTaskDescription(player, state.taskId, input);
                break;
            case DONATION_URL:
                handleDonationUrl(player, input);
                break;
            case CONFIG_STRING:
                handleConfigString(player, state.configPath, input);
                break;
            case CONFIG_LIST_ADD:
                handleConfigListAdd(player, state.configPath, input);
                break;
        }
    }
    
    private void handleTaskId(Player player, String taskId) {
        // Validate task ID
        if (taskId.isEmpty() || taskId.contains(" ")) {
            player.sendMessage("§c[Task Manager] Invalid task ID! Use underscores instead of spaces.");
            plugin.getServer().getScheduler().runTask(plugin, () -> 
                plugin.getGuiManager().openCustomTasksMenu(player));
            return;
        }
        
        // Check if ID already exists
        var taskMode = plugin.getTaskManagerMode();
        if (taskMode != null && taskMode.getTask(taskId) != null) {
            player.sendMessage("§c[Task Manager] Task ID already exists!");
            plugin.getServer().getScheduler().runTask(plugin, () -> 
                plugin.getGuiManager().openCustomTasksMenu(player));
            return;
        }
        
        // Store the ID and ask for description
        InputState newState = new InputState(InputState.Type.TASK_DESCRIPTION);
        newState.taskId = taskId;
        activeInputs.put(player.getUniqueId(), newState);
        
        player.sendMessage("§a[Task Manager] Task ID: " + taskId);
        player.sendMessage("§e[Task Manager] Now enter the task description:");
        player.sendMessage("§7Type 'cancel' to abort");
    }
    
    private void handleTaskDescription(Player player, String taskId, String description) {
        if (taskId == null || taskId.isEmpty()) {
            player.sendMessage("§c[Task Manager] Error: No task ID found!");
            plugin.getServer().getScheduler().runTask(plugin, () -> 
                plugin.getGuiManager().openCustomTasksMenu(player));
            return;
        }
        
        if (description.isEmpty()) {
            player.sendMessage("§c[Task Manager] Description cannot be empty!");
            plugin.getServer().getScheduler().runTask(plugin, () -> 
                plugin.getGuiManager().openCustomTasksMenu(player));
            return;
        }
        
        // Add the custom task
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            var taskMode = plugin.getTaskManagerMode();
            if (taskMode != null) {
                taskMode.addCustomTask(taskId, description);
                player.sendMessage("§a[Task Manager] Custom task added successfully!");
                player.sendMessage("§7Task: " + taskId + " - " + description);
            }
            plugin.getGuiManager().openCustomTasksMenu(player);
        });
    }
    
    private void handleDonationUrl(Player player, String url) {
        // Save donation URL to config
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            plugin.getConfig().set("donation.url", url);
            plugin.saveConfig();
            player.sendMessage("§a[Settings] Donation URL updated!");
            plugin.getGuiManager().openSettingsMenu(player);
        });
    }
    
    private void handleConfigString(Player player, String path, String value) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            plugin.getConfig().set(path, value);
            plugin.saveConfig();
            player.sendMessage("§a[Config] Updated " + path);
            String parent = getParent(path);
            plugin.getGuiManager().openAdvancedConfigMenu(player, parent, 0);
        });
    }
    
    private void handleConfigListAdd(Player player, String path, String value) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            java.util.List<String> list = plugin.getConfig().getStringList(path);
            list.add(value);
            plugin.getConfig().set(path, list);
            plugin.saveConfig();
            player.sendMessage("§a[Config] Added to " + path);
            plugin.getGuiManager().openConfigListEditor(player, path, 0);
        });
    }
    
    private String getParent(String path) {
        if (path == null || path.isEmpty()) return "";
        int idx = path.lastIndexOf('.');
        return idx < 0 ? "" : path.substring(0, idx);
    }
    
    public void clearInput(Player player) {
        activeInputs.remove(player.getUniqueId());
    }
}