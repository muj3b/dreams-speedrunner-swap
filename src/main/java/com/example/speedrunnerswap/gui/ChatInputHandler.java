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
        String msg = event.getMessage();

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            switch (state.type) {
                case TASK_ID -> {
                    expectTaskDescription(player, msg.trim());
                    player.sendMessage("§6Enter a §bdescription §6for task §e" + msg.trim());
                }
                case TASK_DESCRIPTION -> {
                    String id = state.taskId != null ? state.taskId.trim() : ("custom_" + System.currentTimeMillis());
                    java.util.List<String> list = plugin.getConfig().getStringList("task_manager.custom_tasks");
                    list.add(msg.trim());
                    plugin.getConfig().set("task_manager.custom_tasks", list);
                    plugin.saveConfig();
                    player.sendMessage("§aAdded custom task §e" + id + "§a: §f" + msg.trim());
                }
                case DONATION_URL -> {
                    plugin.getConfig().set("donation.url", msg.trim());
                    plugin.saveConfig();
                    player.sendMessage("§aUpdated donation URL.");
                }
                case CONFIG_STRING -> {
                    plugin.getConfig().set(state.configPath, msg.trim());
                    plugin.saveConfig();
                    player.sendMessage("§aUpdated §e" + state.configPath + "§a.");
                }
                case CONFIG_LIST_ADD -> {
                    java.util.List<String> l = plugin.getConfig().getStringList(state.configPath);
                    l.add(msg.trim());
                    plugin.getConfig().set(state.configPath, l);
                    plugin.saveConfig();
                    player.sendMessage("§aAppended to §e" + state.configPath + "§a.");
                }
            }
        });
    }
    
    // Removed unused handler methods after inlining logic in onChat() to reduce lints.
    
    public void clearInput(Player player) {
        activeInputs.remove(player.getUniqueId());
    }
}