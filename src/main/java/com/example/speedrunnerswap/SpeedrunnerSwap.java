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
import com.example.speedrunnerswap.game.CompassManager;
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
    private CompassManager compassManager;
    
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
        this.compassManager = new CompassManager(this);
        
        // Register commands
        getCommand("swap").setExecutor(new SwapCommand(this));
        
        // Register event listeners
        getServer().getPluginManager().registerEvents(new EventListeners(this), this);
        getServer().getPluginManager().registerEvents(new DragonDefeatListener(this), this);
        // Register GUI listener for menu interactions
        getServer().getPluginManager().registerEvents(new GuiListener(this, guiManager), this);
        
        // Log startup
        getLogger().info("SpeedrunnerSwap v" + this.getName() + " has been enabled!");
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
        getLogger().info("SpeedrunnerSwap has been disabled!");
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

    public CompassManager getCompassManager() {
        return compassManager;
    }
}