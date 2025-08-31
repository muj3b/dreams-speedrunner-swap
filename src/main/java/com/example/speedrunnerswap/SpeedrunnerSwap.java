package com.example.speedrunnerswap;

import com.example.speedrunnerswap.commands.SwapCommand;
import com.example.speedrunnerswap.config.ConfigManager;
import com.example.speedrunnerswap.game.GameManager;
import com.example.speedrunnerswap.gui.GuiManager;
import com.example.speedrunnerswap.listeners.EventListeners;
import com.example.speedrunnerswap.tracking.TrackerManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpeedrunnerSwap extends JavaPlugin {
    
    private static SpeedrunnerSwap instance;
    private ConfigManager configManager;
    private GameManager gameManager;
    private GuiManager guiManager;
    private TrackerManager trackerManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize managers
        this.configManager = new ConfigManager(this);
        this.gameManager = new GameManager(this);
        this.guiManager = new GuiManager(this);
        this.trackerManager = new TrackerManager(this);
        
        // Register commands
        getCommand("swap").setExecutor(new SwapCommand(this));
        
        // Register event listeners
        getServer().getPluginManager().registerEvents(new EventListeners(this), this);
        
        // Log startup
        getLogger().info("SpeedrunnerSwap v" + getDescription().getVersion() + " has been enabled!");
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
}