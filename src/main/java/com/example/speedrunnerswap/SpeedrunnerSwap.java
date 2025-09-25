package com.example.speedrunnerswap;

import com.example.speedrunnerswap.commands.SwapCommand;
import com.example.speedrunnerswap.config.ConfigManager;
import com.example.speedrunnerswap.game.GameManager;
import com.example.speedrunnerswap.gui.GuiManager;
import com.example.speedrunnerswap.listeners.DragonDefeatListener;
import com.example.speedrunnerswap.listeners.EventListeners;
import com.example.speedrunnerswap.tracking.TrackerManager;
import com.example.speedrunnerswap.powerups.PowerUpManager;
import com.example.speedrunnerswap.game.KitManager;
import com.example.speedrunnerswap.game.StatsManager;
import com.example.speedrunnerswap.game.WorldBorderManager;
import com.example.speedrunnerswap.game.BountyManager;
import com.example.speedrunnerswap.game.SuddenDeathManager;

import com.example.speedrunnerswap.game.KitConfigManager;
import com.example.speedrunnerswap.game.DragonManager;
import com.example.speedrunnerswap.gui.ChatInputHandler;
import com.example.speedrunnerswap.utils.BukkitCompat;
// Removed unused Bukkit import
import org.bukkit.plugin.java.JavaPlugin;

public final class SpeedrunnerSwap extends JavaPlugin {
    
    private static SpeedrunnerSwap instance;
    private ConfigManager configManager;
    private GameManager gameManager;
    private GuiManager guiManager;
    private TrackerManager trackerManager;
    private PowerUpManager powerUpManager;
    private KitManager kitManager;
    private StatsManager statsManager;
    private WorldBorderManager worldBorderManager;
    private BountyManager bountyManager;
    private SuddenDeathManager suddenDeathManager;
    private KitConfigManager kitConfigManager;
    private DragonManager dragonManager;
    private ChatInputHandler chatInputHandler;
    // Task configurations (tasks.yml)
    private com.example.speedrunnerswap.task.TaskConfigManager taskConfigManager;
    // Task Manager mode
    private com.example.speedrunnerswap.task.TaskManagerMode taskManagerMode;
    // Mode selection (Dream = runners+hunters, Sapnap = runners only, Task Manager = runners only with secret tasks)
    public enum SwapMode { DREAM, SAPNAP, TASK }
    private SwapMode currentMode = SwapMode.DREAM;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize managers
        this.configManager = new ConfigManager(this);
        this.gameManager = new GameManager(this);
        this.guiManager = new GuiManager(this);
        this.trackerManager = new TrackerManager(this);
        this.powerUpManager = new PowerUpManager(this);
        this.kitManager = new KitManager(this);
        this.statsManager = new StatsManager(this);
        this.worldBorderManager = new WorldBorderManager(this);
        this.bountyManager = new BountyManager(this);
        this.suddenDeathManager = new SuddenDeathManager(this);
        this.kitConfigManager = new KitConfigManager(this);
        this.dragonManager = new DragonManager(this);
        this.chatInputHandler = new ChatInputHandler(this);
        
        // Initialize Task config and Task Manager mode
        this.taskConfigManager = new com.example.speedrunnerswap.task.TaskConfigManager(this);
        this.taskManagerMode = new com.example.speedrunnerswap.task.TaskManagerMode(this);
        
        // Validate config consistency
        validatePowerUpConfig();

        // Apply default mode from config
        try {
            this.setCurrentMode(configManager.getDefaultMode());
            // Set appropriate sleep default for the current mode
            if (getCurrentMode() == SwapMode.SAPNAP && !getConfig().contains("single_player_sleep.enabled")) {
                configManager.setSinglePlayerSleepEnabled(true);
            }
        } catch (Throwable ignored) {}

        // Register commands
        SwapCommand swapCommand = new SwapCommand(this);
        try {
            if (getCommand("swap") != null) {
                getCommand("swap").setExecutor(swapCommand);
                getCommand("swap").setTabCompleter(swapCommand);
                getLogger().info("Successfully registered /swap command");
            } else {
                getLogger().severe("Failed to register /swap command - command not found in plugin.yml");
            }
        } catch (Exception e) {
            getLogger().severe("Error registering /swap command: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Register event listeners
        getServer().getPluginManager().registerEvents(new EventListeners(this), this);
        getServer().getPluginManager().registerEvents(new DragonDefeatListener(this), this);
        getServer().getPluginManager().registerEvents(dragonManager, this);
        // Task Manager event listeners
        getServer().getPluginManager().registerEvents(new com.example.speedrunnerswap.task.TaskEventListener(this), this);
        // Register GUI manager directly (handles all inventory interactions)
        getServer().getPluginManager().registerEvents(guiManager, this);
        // Register chat input handler for custom tasks
        getServer().getPluginManager().registerEvents(chatInputHandler, this);
        
        // Log startup with version
        String ver = getPluginMeta() != null ? getPluginMeta().getVersion() : "unknown";
        getLogger().info("SpeedrunnerSwap v" + ver + " enabled");
    }
    
    @Override
    public void onDisable() {
        // Stop the game if it's running
        if (gameManager.isGameRunning()) {
            gameManager.stopGame();
        }
        
        // Save config
        configManager.saveConfig();
        
        // Log shutdown
        getLogger().info("SpeedrunnerSwap disabled");
    }
    
    /**
     * Get the plugin instance
     * @return The plugin instance
     */
    public static SpeedrunnerSwap getInstance() {
        return instance;
    }
    
    /**
     * Get the config manager
     * @return The config manager
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    /**
     * Get the game manager
     * @return The game manager
     */
    public GameManager getGameManager() {
        return gameManager;
    }
    
    /**
     * Get the GUI manager
     * @return The GUI manager
     */
    public GuiManager getGuiManager() {
        return guiManager;
    }
    
    /**
     * Get the tracker manager
     * @return The tracker manager
     */
    public TrackerManager getTrackerManager() {
        return trackerManager;
    }

    public com.example.speedrunnerswap.task.TaskManagerMode getTaskManagerMode() {
        return taskManagerMode;
    }

    public PowerUpManager getPowerUpManager() {
        return powerUpManager;
    }
    
    public KitManager getKitManager() {
        return kitManager;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public WorldBorderManager getWorldBorderManager() {
        return worldBorderManager;
    }

    public BountyManager getBountyManager() {
        return bountyManager;
    }

    public SuddenDeathManager getSuddenDeathManager() {
        return suddenDeathManager;
    }



    public KitConfigManager getKitConfigManager() {
        return kitConfigManager;
    }
    
    public ChatInputHandler getChatInputHandler() {
        return chatInputHandler;
    }

    public DragonManager getDragonManager() {
        return dragonManager;
    }

    public com.example.speedrunnerswap.task.TaskConfigManager getTaskConfigManager() {
        return taskConfigManager;
    }

    private void validatePowerUpConfig() {
        java.util.List<String> invalid = new java.util.ArrayList<>();
        java.util.function.Consumer<String> check = (id) -> {
            if (id == null) return;
            String key = id.toLowerCase(java.util.Locale.ROOT);
            key = switch (key) {
                case "increase_damage" -> "strength";
                case "damage_resistance" -> "resistance";
                case "slow" -> "slowness";
                case "jump" -> "jump_boost";
                case "slow_digging" -> "mining_fatigue";
                case "confusion" -> "nausea";
                default -> key;
            };
            org.bukkit.potion.PotionEffectType t = BukkitCompat.resolvePotionEffect(key);
            if (t == null) {
                invalid.add(id);
            }
        };
        for (String s : configManager.getGoodPowerUps()) check.accept(s);
        for (String s : configManager.getBadPowerUps()) check.accept(s);
        if (!invalid.isEmpty()) {
            getLogger().warning("Unknown potion effect ids in power_ups lists: " + String.join(", ", invalid));
        }
    }

    public SwapMode getCurrentMode() {
        return currentMode;
    }

    public void setCurrentMode(SwapMode mode) {
        if (mode == null) mode = SwapMode.DREAM;
        this.currentMode = mode;
        // When switching to Task Manager mode, ensure tracker is disabled and hunters list is ignored
        if (this.currentMode == SwapMode.TASK) {
            try { getConfigManager().setTrackerEnabled(false); } catch (Exception ignored) {}
        }
        // Optionally apply the per-mode default interval when switching modes (if enabled)
        try {
            if (configManager != null && gameManager != null && configManager.getApplyDefaultOnModeSwitch() && !gameManager.isGameRunning()) {
                configManager.applyModeDefaultInterval(this.currentMode);
                gameManager.refreshSwapSchedule();
            }
        } catch (Throwable ignored) {}
    }
}
