/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 */
package com.example.speedrunnerswap.integration;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import org.bukkit.entity.Player;

public class VoiceChatIntegration {
    private final SpeedrunnerSwap plugin;
    private boolean enabled;
    private boolean pluginDetected;

    public VoiceChatIntegration(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfigManager().isVoiceChatIntegrationEnabled();
        this.pluginDetected = this.checkForVoiceChatPlugin();
    }

    private boolean checkForVoiceChatPlugin() {
        return this.plugin.getServer().getPluginManager().getPlugin("SimpleVoiceChat") != null;
    }

    public void mutePlayer(Player player) {
        if (!this.enabled || !this.pluginDetected) {
            return;
        }
        this.plugin.getLogger().info("VoiceChat: Would mute player " + player.getName());
    }

    public void unmutePlayer(Player player) {
        if (!this.enabled || !this.pluginDetected) {
            return;
        }
        this.plugin.getLogger().info("VoiceChat: Would unmute player " + player.getName());
    }

    public void updateRunnerMuteStatus() {
        if (!this.enabled || !this.pluginDetected) {
            return;
        }
        Player activeRunner = this.plugin.getGameManager().getActiveRunner();
        for (Player runner : this.plugin.getGameManager().getRunners()) {
            if (runner.equals((Object)activeRunner)) {
                this.unmutePlayer(runner);
                continue;
            }
            this.mutePlayer(runner);
        }
    }

    public void resetAllPlayerMuteStatus() {
        if (!this.enabled || !this.pluginDetected) {
            return;
        }
        for (Player runner : this.plugin.getGameManager().getRunners()) {
            this.unmutePlayer(runner);
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            this.resetAllPlayerMuteStatus();
        } else if (this.plugin.getGameManager().isGameRunning()) {
            this.updateRunnerMuteStatus();
        }
    }

    public boolean isEnabled() {
        return this.enabled && this.pluginDetected;
    }
}

