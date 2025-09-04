/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  net.kyori.adventure.text.event.ClickEvent
 *  net.kyori.adventure.text.event.HoverEvent
 *  net.kyori.adventure.text.event.HoverEventSource
 *  net.kyori.adventure.text.format.NamedTextColor
 *  net.kyori.adventure.text.format.TextColor
 *  net.kyori.adventure.text.format.TextDecoration
 *  net.kyori.adventure.title.Title
 *  net.kyori.adventure.title.Title$Times
 *  org.bukkit.Bukkit
 *  org.bukkit.GameMode
 *  org.bukkit.Location
 *  org.bukkit.NamespacedKey
 *  org.bukkit.Registry
 *  org.bukkit.World$Environment
 *  org.bukkit.attribute.Attribute
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 *  org.bukkit.scheduler.BukkitRunnable
 *  org.bukkit.scheduler.BukkitTask
 */
package com.example.speedrunnerswap.game;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import com.example.speedrunnerswap.models.PlayerState;
import com.example.speedrunnerswap.models.Team;
import com.example.speedrunnerswap.utils.PlayerStateUtil;
import com.example.speedrunnerswap.utils.SafeLocationFinder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class GameManager {
    private final SpeedrunnerSwap plugin;
    private boolean gameRunning;
    private boolean gamePaused;
    private Player activeRunner;
    private int activeRunnerIndex;
    private List<Player> runners;
    private List<Player> hunters;
    private BukkitTask swapTask;
    private BukkitTask hunterSwapTask;
    private BukkitTask actionBarTask;
    private BukkitTask freezeCheckTask;
    private long nextSwapTime;
    private final Map<UUID, PlayerState> playerStates;

    public GameManager(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
        this.gameRunning = false;
        this.gamePaused = false;
        this.activeRunnerIndex = 0;
        this.runners = new ArrayList<Player>();
        this.hunters = new ArrayList<Player>();
        this.playerStates = new HashMap<UUID, PlayerState>();
    }

    public boolean startGame() {
        if (this.gameRunning) {
            return false;
        }
        if (!this.canStartGame()) {
            Bukkit.broadcast((Component)Component.text((String)"\u00a7cGame cannot start: At least one runner and one hunter are required."), (String)"bukkit.broadcast.user");
            return false;
        }
        new BukkitRunnable(){
            int count = 3;

            public void run() {
                if (this.count > 0) {
                    Title title = Title.title((Component)((TextComponent)Component.text((String)("Starting in " + this.count)).color((TextColor)NamedTextColor.GREEN)).decorate(TextDecoration.BOLD), (Component)Component.text((String)"Made by muj3b").color((TextColor)NamedTextColor.GRAY), (Title.Times)Title.Times.times((Duration)Duration.ofMillis(500L), (Duration)Duration.ofMillis(3500L), (Duration)Duration.ofMillis(500L)));
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.showTitle(title);
                    }
                    --this.count;
                } else {
                    this.cancel();
                    GameManager.this.gameRunning = true;
                    GameManager.this.gamePaused = false;
                    GameManager.this.activeRunnerIndex = 0;
                    GameManager.this.activeRunner = GameManager.this.runners.get(GameManager.this.activeRunnerIndex);
                    GameManager.this.saveAllPlayerStates();
                    if (GameManager.this.plugin.getConfigManager().isKitsEnabled()) {
                        for (Player player : GameManager.this.runners) {
                            GameManager.this.plugin.getKitManager().giveKit(player, "runner");
                        }
                        for (Player hunter : GameManager.this.hunters) {
                            GameManager.this.plugin.getKitManager().giveKit(hunter, "hunter");
                        }
                    }
                    GameManager.this.applyInactiveEffects();
                    GameManager.this.scheduleNextSwap();
                    GameManager.this.scheduleNextHunterSwap();
                    GameManager.this.startActionBarUpdates();
                    if (GameManager.this.plugin.getConfigManager().isTrackerEnabled()) {
                        GameManager.this.plugin.getTrackerManager().startTracking();
                        for (Player hunter : GameManager.this.hunters) {
                            if (!hunter.isOnline()) continue;
                            GameManager.this.plugin.getTrackerManager().giveTrackingCompass(hunter);
                        }
                    }
                    if (GameManager.this.plugin.getConfig().getBoolean("stats.enabled", true)) {
                        GameManager.this.plugin.getStatsManager().startTracking();
                    }
                    if (GameManager.this.plugin.getConfigManager().isFreezeMechanicEnabled()) {
                        GameManager.this.startFreezeChecking();
                    }
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 20L);
        return true;
    }

    public void endGame(final Team winner) {
        TextComponent titleText;
        if (!this.gameRunning) {
            return;
        }
        String runnerSubtitle = "";
        String hunterSubtitle = "";
        if (winner == Team.RUNNER) {
            titleText = Component.text((String)"RUNNERS WIN!", (TextColor)NamedTextColor.GREEN, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD});
            runnerSubtitle = "bro y'all are locked in, good stuff";
            hunterSubtitle = "bro y'all are locked in, good stuff";
        } else if (winner == Team.HUNTER) {
            titleText = Component.text((String)"HUNTERS WIN!", (TextColor)NamedTextColor.RED, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD});
            runnerSubtitle = "you ain't the main character unc";
            hunterSubtitle = "bro those speedrunners are trash asf";
        } else {
            titleText = Component.text((String)"GAME OVER", (TextColor)NamedTextColor.RED, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD});
            runnerSubtitle = "No winner declared.";
            hunterSubtitle = "No winner declared.";
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            TextComponent subtitleText = this.isRunner(player) ? Component.text((String)runnerSubtitle, (TextColor)NamedTextColor.YELLOW) : Component.text((String)hunterSubtitle, (TextColor)NamedTextColor.YELLOW);
            Title endTitle = Title.title((Component)titleText, (Component)subtitleText, (Title.Times)Title.Times.times((Duration)Duration.ofMillis(500L), (Duration)Duration.ofMillis(5000L), (Duration)Duration.ofMillis(500L)));
            player.showTitle(endTitle);
        }
        if (this.swapTask != null) {
            this.swapTask.cancel();
        }
        if (this.hunterSwapTask != null) {
            this.hunterSwapTask.cancel();
        }
        if (this.actionBarTask != null) {
            this.actionBarTask.cancel();
        }
        if (this.freezeCheckTask != null) {
            this.freezeCheckTask.cancel();
        }
        this.plugin.getTrackerManager().stopTracking();
        try {
            this.plugin.getStatsManager().stopTracking();
        }
        catch (Exception exception) {
            // empty catch block
        }
        new BukkitRunnable(this){
            final /* synthetic */ GameManager this$0;
            {
                this.this$0 = this$0;
            }

            public void run() {
                if (this.this$0.plugin.getConfig().getBoolean("swap.preserve_runner_progress_on_end", false)) {
                    try {
                        if (this.this$0.activeRunner != null && this.this$0.activeRunner.isOnline() && !this.this$0.runners.isEmpty()) {
                            PlayerState finalState = PlayerStateUtil.capturePlayerState(this.this$0.activeRunner);
                            for (Player r : this.this$0.runners) {
                                this.this$0.playerStates.put(r.getUniqueId(), finalState);
                            }
                        }
                    }
                    catch (Exception ex) {
                        this.this$0.plugin.getLogger().warning("Failed to capture/apply final runner state: " + ex.getMessage());
                    }
                }
                this.this$0.restoreAllPlayerStates();
                this.this$0.gameRunning = false;
                this.this$0.gamePaused = false;
                this.this$0.activeRunner = null;
                if (this.this$0.plugin.getConfigManager().isBroadcastGameEvents()) {
                    String winnerMessage = winner != null ? winner.name() + " team won!" : "Game ended!";
                    Bukkit.broadcast((Component)Component.text((String)("\u00a7a[SpeedrunnerSwap] Game ended! " + winnerMessage)), (String)"bukkit.broadcast.user");
                }
                this.this$0.broadcastDonationMessage();
            }
        }.runTaskLater((Plugin)this.plugin, 200L);
    }

    private void broadcastDonationMessage() {
        Bukkit.broadcast((Component)Component.text((String)"\n"), (String)"bukkit.broadcast.user");
        Bukkit.broadcast((Component)((TextComponent)Component.text((String)"=== Support the Creator ===").color((TextColor)NamedTextColor.GOLD)).decorate(TextDecoration.BOLD), (String)"bukkit.broadcast.user");
        Bukkit.broadcast((Component)Component.text((String)"Enjoy the plugin? Consider supporting the creator (muj3b)!").color((TextColor)NamedTextColor.YELLOW), (String)"bukkit.broadcast.user");
        Component donateMessage = ((TextComponent)((TextComponent)((TextComponent)Component.text((String)"[Click here to donate]").color((TextColor)NamedTextColor.GREEN)).decorate(TextDecoration.BOLD)).clickEvent(ClickEvent.openUrl((String)"https://donate.stripe.com/cNicN5gG3f8ocU4cjN0Ba00"))).hoverEvent((HoverEventSource)HoverEvent.showText((Component)Component.text((String)"Click to support the creator!").color((TextColor)NamedTextColor.YELLOW)));
        Bukkit.broadcast((Component)donateMessage, (String)"bukkit.broadcast.user");
        Bukkit.broadcast((Component)Component.text((String)"\n"), (String)"bukkit.broadcast.user");
    }

    public void stopGame() {
        this.endGame(null);
    }

    public boolean isHunter(Player player) {
        return this.hunters.contains(player);
    }

    public boolean isRunner(Player player) {
        return this.runners.contains(player);
    }

    public boolean isGameRunning() {
        return this.gameRunning;
    }

    public Player getActiveRunner() {
        return this.activeRunner;
    }

    public List<Player> getRunners() {
        return this.runners;
    }

    public List<Player> getHunters() {
        return this.hunters;
    }

    public void refreshSwapSchedule() {
        if (this.gameRunning && !this.gamePaused) {
            this.scheduleNextSwap();
        }
    }

    public void refreshActionBar() {
        if (this.gameRunning && !this.gamePaused) {
            this.updateActionBar();
        }
    }

    public PlayerState getPlayerState(Player player) {
        if (player == null) {
            return null;
        }
        return this.playerStates.computeIfAbsent(player.getUniqueId(), id -> PlayerStateUtil.capturePlayerState(player));
    }

    public boolean canStartGame() {
        if (this.gameRunning) {
            return false;
        }
        this.loadTeams();
        return !this.runners.isEmpty() && !this.hunters.isEmpty();
    }

    public void updateTeams() {
        ArrayList<Player> newRunners = new ArrayList<Player>();
        ArrayList<Player> newHunters = new ArrayList<Player>();
        for (Player runner : this.runners) {
            if (!runner.isOnline()) continue;
            newRunners.add(runner);
        }
        for (Player hunter : this.hunters) {
            if (!hunter.isOnline()) continue;
            newHunters.add(hunter);
        }
        this.runners = newRunners;
        this.hunters = newHunters;
        if (this.gameRunning && (this.runners.isEmpty() || this.hunters.isEmpty())) {
            if (this.plugin.getConfigManager().isPauseOnDisconnect()) {
                this.pauseGame();
                if (this.plugin.getConfigManager().isBroadcastGameEvents()) {
                    Bukkit.broadcast((Component)Component.text((String)"\u00a7e[SpeedrunnerSwap] Game paused: waiting for players to return."), (String)"bukkit.broadcast.user");
                }
            } else {
                this.plugin.getLogger().warning("A team is empty; game continues (pause_on_disconnect=false)");
            }
        }
    }

    public void handlePlayerQuit(Player player) {
        if (!this.gameRunning) {
            return;
        }
        if (player.equals((Object)this.activeRunner)) {
            if (this.plugin.getConfigManager().isPauseOnDisconnect()) {
                this.pauseGame();
            } else {
                this.performSwap();
            }
        }
        this.savePlayerState(player);
    }

    public int getTimeUntilNextSwap() {
        return (int)((this.nextSwapTime - System.currentTimeMillis()) / 1000L);
    }

    private void saveAllPlayerStates() {
        for (Player runner : this.runners) {
            this.savePlayerState(runner);
        }
    }

    private void savePlayerState(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        PlayerState state = PlayerStateUtil.capturePlayerState(player);
        this.playerStates.put(player.getUniqueId(), state);
    }

    private void restoreAllPlayerStates() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (this.playerStates.containsKey(player.getUniqueId())) {
                this.restorePlayerState(player);
            }
            player.removePotionEffect(PotionEffectType.BLINDNESS);
            player.removePotionEffect(PotionEffectType.DARKNESS);
            player.removePotionEffect(PotionEffectType.WEAKNESS);
            player.removePotionEffect(PotionEffectType.SLOW_FALLING);
            player.removePotionEffect(PotionEffectType.SLOWNESS);
            player.removePotionEffect(PotionEffectType.JUMP_BOOST);
            if (player.getGameMode() != GameMode.SPECTATOR || !this.runners.contains(player)) continue;
            player.setGameMode(GameMode.SURVIVAL);
        }
    }

    private void restorePlayerState(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        PlayerState state = this.playerStates.get(player.getUniqueId());
        if (state != null) {
            try {
                player.getInventory().clear();
                for (PotionEffect effect : player.getActivePotionEffects()) {
                    player.removePotionEffect(effect.getType());
                }
                player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
                player.setFoodLevel(20);
                player.setFireTicks(0);
                player.setFallDistance(0.0f);
                player.setInvulnerable(false);
                PlayerStateUtil.applyPlayerState(player, state);
                Location loc = state.getLocation();
                if (loc != null && !loc.getBlock().getType().isSolid()) {
                    player.teleport(loc);
                } else {
                    player.teleport(this.plugin.getConfigManager().getSpawnLocation());
                    player.setGameMode(GameMode.SURVIVAL);
                    player.getInventory().clear();
                }
            }
            catch (Exception e) {
                this.plugin.getLogger().warning("Failed to restore state for player " + player.getName() + ": " + e.getMessage());
                player.teleport(this.plugin.getConfigManager().getSpawnLocation());
                player.setGameMode(GameMode.SURVIVAL);
                player.getInventory().clear();
            }
        }
    }

    private void loadTeams() {
        Player player;
        this.runners.clear();
        this.hunters.clear();
        for (String name : this.plugin.getConfigManager().getRunnerNames()) {
            player = Bukkit.getPlayerExact((String)name);
            if (player == null || !player.isOnline()) continue;
            this.runners.add(player);
        }
        for (String name : this.plugin.getConfigManager().getHunterNames()) {
            player = Bukkit.getPlayerExact((String)name);
            if (player == null || !player.isOnline()) continue;
            this.hunters.add(player);
        }
    }

    private void scheduleNextSwap() {
        long intervalSeconds;
        if (this.swapTask != null) {
            this.swapTask.cancel();
        }
        if (this.plugin.getConfigManager().isSwapRandomized()) {
            double mean = this.plugin.getConfigManager().getSwapInterval();
            double stdDev = this.plugin.getConfigManager().getJitterStdDev();
            double jitteredInterval = ThreadLocalRandom.current().nextGaussian() * stdDev + mean;
            if (this.plugin.getConfigManager().isClampJitter()) {
                int min = this.plugin.getConfigManager().getMinSwapInterval();
                int max = this.plugin.getConfigManager().getMaxSwapInterval();
                jitteredInterval = Math.max((double)min, Math.min((double)max, jitteredInterval));
            }
            intervalSeconds = Math.round(jitteredInterval);
        } else {
            intervalSeconds = this.plugin.getConfigManager().getSwapInterval();
        }
        long intervalTicks = intervalSeconds * 20L;
        this.nextSwapTime = System.currentTimeMillis() + intervalSeconds * 1000L;
        this.swapTask = Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, this::performSwap, intervalTicks);
    }

    private void scheduleNextHunterSwap() {
        if (this.hunterSwapTask != null) {
            this.hunterSwapTask.cancel();
        }
        if (!this.plugin.getConfigManager().isHunterSwapEnabled()) {
            return;
        }
        long intervalTicks = (long)this.plugin.getConfigManager().getHunterSwapInterval() * 20L;
        this.hunterSwapTask = Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, this::performHunterSwap, intervalTicks);
    }

    private void startActionBarUpdates() {
        if (this.actionBarTask != null) {
            this.actionBarTask.cancel();
        }
        this.actionBarTask = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, () -> {
            if (!this.gameRunning) {
                return;
            }
            this.updateActionBar();
        }, 0L, 20L);
    }

    private void updateActionBar() {
        if (!this.gameRunning || this.gamePaused) {
            return;
        }
        int timeLeft = this.getTimeUntilNextSwap();
        for (Player player : Bukkit.getOnlinePlayers()) {
            String message;
            boolean showTimer;
            String visibility;
            boolean isWaitingRunner = false;
            if (player.equals((Object)this.activeRunner)) {
                visibility = this.plugin.getConfigManager().getRunnerTimerVisibility();
            } else if (this.runners.contains(player)) {
                visibility = this.plugin.getConfigManager().getWaitingTimerVisibility();
                isWaitingRunner = true;
            } else {
                visibility = this.plugin.getConfigManager().getHunterTimerVisibility();
            }
            switch (visibility) {
                case "always": {
                    boolean bl = true;
                    break;
                }
                case "last_10": {
                    boolean bl;
                    if (timeLeft <= 10) {
                        bl = true;
                        break;
                    }
                    bl = false;
                    break;
                }
                case "never": {
                    boolean bl = false;
                    break;
                }
                default: {
                    boolean bl = showTimer = false;
                }
            }
            if (!showTimer) {
                player.sendActionBar((Component)Component.text((String)""));
                continue;
            }
            if (isWaitingRunner && this.activeRunner != null) {
                String dim = switch (this.activeRunner.getWorld().getEnvironment()) {
                    case World.Environment.NETHER -> "Nether";
                    case World.Environment.THE_END -> "End";
                    default -> "Overworld";
                };
                message = String.format("\u00a7eActive: \u00a7b%s \u00a77in \u00a7d%s \u00a77| \u00a7eSwap in: \u00a7c%ds", this.activeRunner.getName(), dim, timeLeft);
            } else {
                message = String.format("\u00a7eTime until next swap: \u00a7c%ds", timeLeft);
            }
            player.sendActionBar((Component)Component.text((String)message));
        }
    }

    private void applyInactiveEffects() {
        String freezeMode = this.plugin.getConfigManager().getFreezeMode();
        for (Player runner : this.runners) {
            if (runner.equals((Object)this.activeRunner)) {
                runner.removePotionEffect(PotionEffectType.BLINDNESS);
                runner.removePotionEffect(PotionEffectType.DARKNESS);
                runner.removePotionEffect(PotionEffectType.SLOWNESS);
                runner.removePotionEffect(PotionEffectType.SLOW_FALLING);
                runner.setGameMode(GameMode.SURVIVAL);
                for (Player viewer : Bukkit.getOnlinePlayers()) {
                    viewer.showPlayer((Plugin)this.plugin, runner);
                }
                continue;
            }
            if (freezeMode.equalsIgnoreCase("EFFECTS")) {
                runner.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, false));
                runner.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, Integer.MAX_VALUE, 1, false, false));
                runner.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 255, false, false));
                runner.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, Integer.MAX_VALUE, 128, false, false));
            } else if (freezeMode.equalsIgnoreCase("SPECTATOR")) {
                runner.setGameMode(GameMode.SPECTATOR);
            } else if (freezeMode.equalsIgnoreCase("LIMBO")) {
                Location limboLocation = this.plugin.getConfigManager().getLimboLocation();
                Location safe = SafeLocationFinder.findSafeLocation(limboLocation, this.plugin.getConfigManager().getSafeSwapHorizontalRadius(), this.plugin.getConfigManager().getSafeSwapVerticalDistance(), this.plugin.getConfigManager().getDangerousBlocks());
                runner.teleport(safe != null ? safe : limboLocation);
                runner.setGameMode(GameMode.ADVENTURE);
                runner.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, false));
            }
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (viewer.equals((Object)runner)) continue;
                viewer.hidePlayer((Plugin)this.plugin, runner);
            }
        }
    }

    private void startFreezeChecking() {
        int interval = this.plugin.getConfigManager().getFreezeCheckIntervalTicks();
        this.freezeCheckTask = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, () -> {
            Player hunter;
            if (!this.gameRunning || this.gamePaused || this.activeRunner == null) {
                return;
            }
            int maxDistance = (int)this.plugin.getConfigManager().getFreezeMaxDistance();
            Entity target = this.activeRunner.getTargetEntity(maxDistance, false);
            if (target instanceof Player && this.isHunter(hunter = (Player)target)) {
                int duration = this.plugin.getConfigManager().getFreezeDurationTicks();
                hunter.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 255, false, false));
                hunter.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, duration, 128, false, false));
                if (this.plugin.getConfigManager().isBroadcastGameEvents()) {
                    hunter.sendMessage((Component)Component.text((String)"\u00a7cYou have been frozen by the runner!"));
                }
            }
        }, 0L, (long)interval);
    }

    public void refreshFreezeMechanic() {
        if (this.freezeCheckTask != null) {
            this.freezeCheckTask.cancel();
            this.freezeCheckTask = null;
        }
        if (this.gameRunning && this.plugin.getConfigManager().isFreezeMechanicEnabled()) {
            this.startFreezeChecking();
        }
        if (this.gameRunning) {
            this.applyInactiveEffects();
        }
    }

    private void performSwap() {
        long duration;
        if (!this.gameRunning || this.gamePaused || this.runners.isEmpty()) {
            return;
        }
        if (this.activeRunner != null && this.activeRunner.isOnline()) {
            this.savePlayerState(this.activeRunner);
        }
        int attempts = 0;
        do {
            this.activeRunnerIndex = (this.activeRunnerIndex + 1) % this.runners.size();
            if (++attempts < this.runners.size()) continue;
            this.plugin.getLogger().warning("No online runners found during swap - pausing game");
            this.pauseGame();
            return;
        } while (!this.runners.get(this.activeRunnerIndex).isOnline());
        Player nextRunner = this.runners.get(this.activeRunnerIndex);
        Player previousRunner = this.activeRunner;
        if (previousRunner != null && previousRunner.equals((Object)nextRunner)) {
            this.scheduleNextSwap();
            if (this.plugin.getConfigManager().isPowerUpsEnabled()) {
                this.applyRandomPowerUp(nextRunner);
            }
            return;
        }
        this.activeRunner = nextRunner;
        int gracePeriodTicks = this.plugin.getConfigManager().getGracePeriodTicks();
        if (gracePeriodTicks > 0) {
            nextRunner.setInvulnerable(true);
            Player finalNextRunner = nextRunner;
            Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
                if (finalNextRunner.isOnline()) {
                    finalNextRunner.setInvulnerable(false);
                }
            }, (long)gracePeriodTicks);
        }
        if (previousRunner != null && previousRunner.isOnline()) {
            Location swapLocation;
            Location safeLocation;
            PlayerState prevState = PlayerStateUtil.capturePlayerState(previousRunner);
            PlayerStateUtil.applyPlayerState(nextRunner, prevState);
            if (this.plugin.getConfigManager().isSafeSwapEnabled() && (safeLocation = SafeLocationFinder.findSafeLocation(swapLocation = previousRunner.getLocation(), this.plugin.getConfigManager().getSafeSwapHorizontalRadius(), this.plugin.getConfigManager().getSafeSwapVerticalDistance(), this.plugin.getConfigManager().getDangerousBlocks())) != null) {
                nextRunner.teleport(safeLocation);
            }
            for (PotionEffect effect : previousRunner.getActivePotionEffects()) {
                previousRunner.removePotionEffect(effect.getType());
            }
            previousRunner.getInventory().clear();
            previousRunner.getInventory().setArmorContents(new ItemStack[0]);
            previousRunner.getInventory().setItemInOffHand(null);
            previousRunner.updateInventory();
        } else if (this.plugin.getConfigManager().isKitsEnabled()) {
            nextRunner.getInventory().clear();
            this.plugin.getKitManager().giveKit(nextRunner, "runner");
        }
        this.applyInactiveEffects();
        this.scheduleNextSwap();
        if (this.plugin.getConfigManager().isBroadcastsEnabled() && previousRunner != null) {
            Bukkit.broadcast((Component)Component.text((String)("\u00a76[SpeedrunnerSwap] Swapped from " + previousRunner.getName() + " to " + nextRunner.getName() + "!")), (String)"bukkit.broadcast.user");
        }
        if (this.plugin.getConfigManager().isPowerUpsEnabled()) {
            this.applyRandomPowerUp(nextRunner);
        }
        if (this.plugin.getConfigManager().isCompassJammingEnabled() && (duration = (long)this.plugin.getConfigManager().getCompassJamDuration()) > 0L) {
            this.plugin.getTrackerManager().jamCompasses(duration);
        }
    }

    public void triggerImmediateSwap() {
        if (!this.gameRunning || this.gamePaused) {
            return;
        }
        Bukkit.getScheduler().runTask((Plugin)this.plugin, this::performSwap);
    }

    public void triggerImmediateHunterSwap() {
        if (!this.gameRunning || this.gamePaused) {
            return;
        }
        Bukkit.getScheduler().runTask((Plugin)this.plugin, this::performHunterSwap);
    }

    private void performHunterSwap() {
        if (!this.gameRunning || this.gamePaused || this.hunters.size() < 2) {
            return;
        }
        Collections.shuffle(this.hunters);
        this.plugin.getTrackerManager().updateAllHunterCompasses();
        if (this.plugin.getConfigManager().isBroadcastsEnabled()) {
            Bukkit.broadcast((Component)Component.text((String)"\u00a7c[SpeedrunnerSwap] Hunters have been swapped!"), (String)"bukkit.broadcast.user");
        }
    }

    private void applyRandomPowerUp(Player player) {
        boolean isGoodEffect;
        PotionEffectType t;
        List<String> good = this.plugin.getConfigManager().getGoodPowerUps();
        List<String> bad = this.plugin.getConfigManager().getBadPowerUps();
        List<PotionEffectType> goodTypes = new ArrayList<PotionEffectType>();
        List<PotionEffectType> badTypes = new ArrayList<PotionEffectType>();
        for (String id : good) {
            t = this.resolveEffect(id);
            if (t == null) continue;
            goodTypes.add(t);
        }
        for (String id : bad) {
            t = this.resolveEffect(id);
            if (t == null) continue;
            badTypes.add(t);
        }
        if (goodTypes.isEmpty()) {
            goodTypes = Arrays.asList(PotionEffectType.SPEED, PotionEffectType.REGENERATION, PotionEffectType.RESISTANCE, PotionEffectType.NIGHT_VISION, PotionEffectType.DOLPHINS_GRACE);
        }
        if (badTypes.isEmpty()) {
            badTypes = Arrays.asList(PotionEffectType.SLOWNESS, PotionEffectType.WEAKNESS, PotionEffectType.HUNGER, PotionEffectType.DARKNESS, PotionEffectType.GLOWING);
        }
        ArrayList<PotionEffectType> effectPool = (isGoodEffect = ThreadLocalRandom.current().nextBoolean()) ? goodTypes : badTypes;
        PotionEffectType effectType = (PotionEffectType)effectPool.get(ThreadLocalRandom.current().nextInt(effectPool.size()));
        int duration = ThreadLocalRandom.current().nextInt(10, 21) * 20;
        int amplifier = ThreadLocalRandom.current().nextInt(2);
        player.addPotionEffect(new PotionEffect(effectType, duration, amplifier));
        String effectName = effectType.getKey().getKey().replace("_", " ").toLowerCase();
        String effectLevel = amplifier == 0 ? "I" : "II";
        player.sendMessage((Component)Component.text((String)String.format("\u00a7%sYou received a %s power-up: %s %s!", isGoodEffect ? "a" : "c", isGoodEffect ? "good" : "bad", effectName, effectLevel)));
    }

    private PotionEffectType resolveEffect(String id) {
        String key;
        if (id == null) {
            return null;
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
        PotionEffectType type = (PotionEffectType)Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft((String)key));
        return type;
    }

    public boolean pauseGame() {
        if (!this.gameRunning || this.gamePaused) {
            return false;
        }
        this.gamePaused = true;
        if (this.swapTask != null) {
            this.swapTask.cancel();
        }
        if (this.hunterSwapTask != null) {
            this.hunterSwapTask.cancel();
        }
        if (this.actionBarTask != null) {
            this.actionBarTask.cancel();
        }
        return true;
    }

    public boolean resumeGame() {
        if (!this.gameRunning || !this.gamePaused) {
            return false;
        }
        this.gamePaused = false;
        this.scheduleNextSwap();
        this.scheduleNextHunterSwap();
        this.startActionBarUpdates();
        return true;
    }

    public boolean isGamePaused() {
        return this.gamePaused;
    }

    public void setRunners(List<Player> players) {
        ArrayList<String> names = new ArrayList<String>();
        for (Player p : players) {
            names.add(p.getName());
        }
        this.plugin.getConfigManager().setRunnerNames(names);
        List<String> currentHunters = this.plugin.getConfigManager().getHunterNames();
        currentHunters.removeAll(names);
        this.plugin.getConfigManager().setHunterNames(currentHunters);
        this.runners = new ArrayList<Player>(players);
    }

    public void setHunters(List<Player> players) {
        ArrayList<String> names = new ArrayList<String>();
        for (Player p : players) {
            names.add(p.getName());
        }
        this.plugin.getConfigManager().setHunterNames(names);
        List<String> currentRunners = this.plugin.getConfigManager().getRunnerNames();
        currentRunners.removeAll(names);
        this.plugin.getConfigManager().setRunnerNames(currentRunners);
        this.hunters = new ArrayList<Player>(players);
    }
}

