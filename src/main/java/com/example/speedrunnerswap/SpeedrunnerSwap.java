package com.example.speedrunnerswap;

import com.example.speedrunnerswap.commands.SwapCommand;
import com.example.speedrunnerswap.config.ConfigManager;
import com.example.speedrunnerswap.game.GameManager;
import com.example.speedrunnerswap.gui.GuiManager;
import com.example.speedrunnerswap.listeners.DragonDefeatListener;
import com.example.speedrunnerswap.gui.GuiListener;
import com.example.speedrunnerswap.listeners.EventListeners;
import com.example.speedrunnerswap.tracking.TrackerManager;
import com.example.speedrunnerswap.powerups.PowerUpManager;
import com.example.speedrunnerswap.game.KitManager;
import com.example.speedrunnerswap.game.StatsManager;
import com.example.speedrunnerswap.game.WorldBorderManager;
import com.example.speedrunnerswap.game.BountyManager;
import com.example.speedrunnerswap.game.SuddenDeathManager;

import com.example.speedrunnerswap.game.KitConfigManager;
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
        
        // Validate config consistency
        validatePowerUpConfig();

        // Register commands
        getCommand("swap").setExecutor(new SwapCommand(this));
        
        // Register event listeners
        getServer().getPluginManager().registerEvents(new EventListeners(this), this);
        getServer().getPluginManager().registerEvents(new DragonDefeatListener(this), this);
        // Register GUI listener for menu interactions
        getServer().getPluginManager().registerEvents(new GuiListener(this, guiManager), this);
        
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
            org.bukkit.potion.PotionEffectType t = org.bukkit.Registry.POTION_EFFECT_TYPE.get(org.bukkit.NamespacedKey.minecraft(key));
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
}
