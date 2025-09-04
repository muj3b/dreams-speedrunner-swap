/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.NamespacedKey
 *  org.bukkit.Registry
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 *  org.bukkit.potion.PotionEffectType
 */
package com.example.speedrunnerswap;

import com.example.speedrunnerswap.commands.SwapCommand;
import com.example.speedrunnerswap.config.ConfigManager;
import com.example.speedrunnerswap.game.BountyManager;
import com.example.speedrunnerswap.game.GameManager;
import com.example.speedrunnerswap.game.KitConfigManager;
import com.example.speedrunnerswap.game.KitManager;
import com.example.speedrunnerswap.game.StatsManager;
import com.example.speedrunnerswap.game.SuddenDeathManager;
import com.example.speedrunnerswap.game.WorldBorderManager;
import com.example.speedrunnerswap.gui.GuiListener;
import com.example.speedrunnerswap.gui.GuiManager;
import com.example.speedrunnerswap.listeners.DragonDefeatListener;
import com.example.speedrunnerswap.listeners.EventListeners;
import com.example.speedrunnerswap.powerups.PowerUpManager;
import com.example.speedrunnerswap.tracking.TrackerManager;
import java.util.ArrayList;
import java.util.Locale;
import java.util.function.Consumer;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

public final class SpeedrunnerSwap
extends JavaPlugin {
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

    public void onEnable() {
        instance = this;
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
        this.validatePowerUpConfig();
        SwapCommand swapCommand = new SwapCommand(this);
        this.getCommand("swap").setExecutor((CommandExecutor)swapCommand);
        this.getCommand("swap").setTabCompleter((TabCompleter)swapCommand);
        this.getServer().getPluginManager().registerEvents((Listener)new EventListeners(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new DragonDefeatListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new GuiListener(this, this.guiManager), (Plugin)this);
        String ver = this.getPluginMeta() != null ? this.getPluginMeta().getVersion() : "unknown";
        this.getLogger().info("SpeedrunnerSwap v" + ver + " enabled");
    }

    public void onDisable() {
        if (this.gameManager.isGameRunning()) {
            this.gameManager.stopGame();
        }
        this.configManager.saveConfig();
        this.getLogger().info("SpeedrunnerSwap disabled");
    }

    public static SpeedrunnerSwap getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return this.configManager;
    }

    public GameManager getGameManager() {
        return this.gameManager;
    }

    public GuiManager getGuiManager() {
        return this.guiManager;
    }

    public TrackerManager getTrackerManager() {
        return this.trackerManager;
    }

    public PowerUpManager getPowerUpManager() {
        return this.powerUpManager;
    }

    public KitManager getKitManager() {
        return this.kitManager;
    }

    public StatsManager getStatsManager() {
        return this.statsManager;
    }

    public WorldBorderManager getWorldBorderManager() {
        return this.worldBorderManager;
    }

    public BountyManager getBountyManager() {
        return this.bountyManager;
    }

    public SuddenDeathManager getSuddenDeathManager() {
        return this.suddenDeathManager;
    }

    public KitConfigManager getKitConfigManager() {
        return this.kitConfigManager;
    }

    private void validatePowerUpConfig() {
        ArrayList invalid = new ArrayList();
        Consumer<String> check = id -> {
            String key;
            if (id == null) {
                return;
            }
            key = switch (key = id.toLowerCase(Locale.ROOT)) {
                case "increase_damage" -> "strength";
                case "damage_resistance" -> "resistance";
                case "slow" -> "slowness";
                case "jump" -> "jump_boost";
                case "slow_digging" -> "mining_fatigue";
                case "confusion" -> "nausea";
                default -> key;
            };
            PotionEffectType t = (PotionEffectType)Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft((String)key));
            if (t == null) {
                invalid.add(id);
            }
        };
        for (String s : this.configManager.getGoodPowerUps()) {
            check.accept(s);
        }
        for (String s : this.configManager.getBadPowerUps()) {
            check.accept(s);
        }
        if (!invalid.isEmpty()) {
            this.getLogger().warning("Unknown potion effect ids in power_ups lists: " + String.join((CharSequence)", ", invalid));
        }
    }
}

