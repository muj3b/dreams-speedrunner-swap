/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.entity.Player
 */
package com.example.speedrunnerswap.commands;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class SwapCommand
implements CommandExecutor,
TabCompleter {
    private final SpeedrunnerSwap plugin;

    public SwapCommand(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            String subCommand;
            if (args.length == 0) {
                return this.handleMainCommand(sender);
            }
            switch (subCommand = args[0].toLowerCase()) {
                case "start": {
                    return this.handleStart(sender);
                }
                case "stop": {
                    return this.handleStop(sender);
                }
                case "pause": {
                    return this.handlePause(sender);
                }
                case "resume": {
                    return this.handleResume(sender);
                }
                case "status": {
                    return this.handleStatus(sender);
                }
                case "setrunners": {
                    return this.handleSetRunners(sender, Arrays.copyOfRange(args, 1, args.length));
                }
                case "sethunters": {
                    return this.handleSetHunters(sender, Arrays.copyOfRange(args, 1, args.length));
                }
                case "reload": {
                    return this.handleReload(sender);
                }
                case "gui": {
                    return this.handleMainCommand(sender);
                }
                case "clearteams": {
                    return this.handleClearTeams(sender);
                }
            }
            sender.sendMessage("\u00a7cUnknown subcommand. Use /swap for help.");
            return false;
        }
        catch (Exception e) {
            sender.sendMessage("\u00a7cAn internal error occurred while executing that command. Check server logs for details.");
            this.plugin.getLogger().log(Level.SEVERE, "Unhandled exception while executing /swap by " + (sender == null ? "UNKNOWN" : sender.getName()), e);
            return false;
        }
    }

    private boolean handleMainCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("\u00a7cThis command can only be used by players.");
            return false;
        }
        if (!sender.hasPermission("speedrunnerswap.command")) {
            sender.sendMessage("\u00a7cYou don't have permission to use this command.");
            return false;
        }
        if (this.plugin.getGuiManager() == null) {
            sender.sendMessage("\u00a7cError: GUI Manager not initialized properly. Please report this to the plugin developer.");
            this.plugin.getLogger().log(Level.SEVERE, "GUI Manager is null when trying to open main menu");
            return false;
        }
        try {
            this.plugin.getGuiManager().openMainMenu((Player)sender);
            return true;
        }
        catch (Exception e) {
            sender.sendMessage("\u00a7cError opening GUI: " + e.getMessage());
            this.plugin.getLogger().log(Level.SEVERE, "Error opening GUI for player " + sender.getName(), e);
            return false;
        }
    }

    private boolean handleStart(CommandSender sender) {
        if (!sender.hasPermission("speedrunnerswap.command")) {
            sender.sendMessage("\u00a7cYou don't have permission to use this command.");
            return false;
        }
        if (this.plugin.getGameManager().isGameRunning()) {
            sender.sendMessage("\u00a7cThe game is already running.");
            return false;
        }
        boolean success = this.plugin.getGameManager().startGame();
        if (success) {
            sender.sendMessage("\u00a7aGame started successfully.");
        } else {
            sender.sendMessage("\u00a7cFailed to start the game. Make sure there are runners set.");
        }
        return success;
    }

    private boolean handleStop(CommandSender sender) {
        if (!sender.hasPermission("speedrunnerswap.command")) {
            sender.sendMessage("\u00a7cYou don't have permission to use this command.");
            return false;
        }
        if (!this.plugin.getGameManager().isGameRunning()) {
            sender.sendMessage("\u00a7cThe game is not running.");
            return false;
        }
        this.plugin.getGameManager().stopGame();
        sender.sendMessage("\u00a7aGame stopped.");
        return true;
    }

    private boolean handlePause(CommandSender sender) {
        if (!sender.hasPermission("speedrunnerswap.command")) {
            sender.sendMessage("\u00a7cYou don't have permission to use this command.");
            return false;
        }
        if (!this.plugin.getGameManager().isGameRunning()) {
            sender.sendMessage("\u00a7cThe game is not running.");
            return false;
        }
        if (this.plugin.getGameManager().isGamePaused()) {
            sender.sendMessage("\u00a7cThe game is already paused.");
            return false;
        }
        boolean success = this.plugin.getGameManager().pauseGame();
        if (success) {
            sender.sendMessage("\u00a7aGame paused.");
        } else {
            sender.sendMessage("\u00a7cFailed to pause the game.");
        }
        return success;
    }

    private boolean handleResume(CommandSender sender) {
        if (!sender.hasPermission("speedrunnerswap.command")) {
            sender.sendMessage("\u00a7cYou don't have permission to use this command.");
            return false;
        }
        if (!this.plugin.getGameManager().isGameRunning()) {
            sender.sendMessage("\u00a7cThe game is not running.");
            return false;
        }
        if (!this.plugin.getGameManager().isGamePaused()) {
            sender.sendMessage("\u00a7cThe game is not paused.");
            return false;
        }
        boolean success = this.plugin.getGameManager().resumeGame();
        if (success) {
            sender.sendMessage("\u00a7aGame resumed.");
        } else {
            sender.sendMessage("\u00a7cFailed to resume the game.");
        }
        return success;
    }

    private boolean handleStatus(CommandSender sender) {
        if (!sender.hasPermission("speedrunnerswap.command")) {
            sender.sendMessage("\u00a7cYou don't have permission to use this command.");
            return false;
        }
        sender.sendMessage("\u00a76=== SpeedrunnerSwap Status ===");
        sender.sendMessage("\u00a7eGame Running: \u00a7f" + this.plugin.getGameManager().isGameRunning());
        sender.sendMessage("\u00a7eGame Paused: \u00a7f" + this.plugin.getGameManager().isGamePaused());
        if (this.plugin.getGameManager().isGameRunning()) {
            Player activeRunner = this.plugin.getGameManager().getActiveRunner();
            sender.sendMessage("\u00a7eActive Runner: \u00a7f" + (activeRunner != null ? activeRunner.getName() : "None"));
            sender.sendMessage("\u00a7eTime Until Next Swap: \u00a7f" + this.plugin.getGameManager().getTimeUntilNextSwap() + "s");
            List<Player> runners = this.plugin.getGameManager().getRunners();
            List<Player> hunters = this.plugin.getGameManager().getHunters();
            sender.sendMessage("\u00a7eRunners: \u00a7f" + runners.stream().map(Player::getName).collect(Collectors.joining(", ")));
            sender.sendMessage("\u00a7eHunters: \u00a7f" + hunters.stream().map(Player::getName).collect(Collectors.joining(", ")));
        }
        return true;
    }

    private boolean handleSetRunners(CommandSender sender, String[] playerNames) {
        if (!sender.hasPermission("speedrunnerswap.command")) {
            sender.sendMessage("\u00a7cYou don't have permission to use this command.");
            return false;
        }
        if (playerNames.length == 0) {
            sender.sendMessage("\u00a7cUsage: /swap setrunners <player1> [player2] [player3] ...");
            return false;
        }
        ArrayList<Player> players = new ArrayList<Player>();
        for (String name : playerNames) {
            Player player = Bukkit.getPlayerExact((String)name);
            if (player != null) {
                players.add(player);
                continue;
            }
            sender.sendMessage("\u00a7cPlayer not found: " + name);
        }
        if (players.isEmpty()) {
            sender.sendMessage("\u00a7cNo valid players specified.");
            return false;
        }
        this.plugin.getGameManager().setRunners(players);
        sender.sendMessage("\u00a7aRunners set: " + players.stream().map(Player::getName).collect(Collectors.joining(", ")));
        return true;
    }

    private boolean handleSetHunters(CommandSender sender, String[] playerNames) {
        if (!sender.hasPermission("speedrunnerswap.command")) {
            sender.sendMessage("\u00a7cYou don't have permission to use this command.");
            return false;
        }
        if (playerNames.length == 0) {
            sender.sendMessage("\u00a7cUsage: /swap sethunters <player1> [player2] [player3] ...");
            return false;
        }
        ArrayList<Player> players = new ArrayList<Player>();
        for (String name : playerNames) {
            Player player = Bukkit.getPlayerExact((String)name);
            if (player != null) {
                players.add(player);
                continue;
            }
            sender.sendMessage("\u00a7cPlayer not found: " + name);
        }
        if (players.isEmpty()) {
            sender.sendMessage("\u00a7cNo valid players specified.");
            return false;
        }
        this.plugin.getGameManager().setHunters(players);
        sender.sendMessage("\u00a7aHunters set: " + players.stream().map(Player::getName).collect(Collectors.joining(", ")));
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("speedrunnerswap.admin")) {
            sender.sendMessage("\u00a7cYou don't have permission to use this command.");
            return false;
        }
        if (this.plugin.getGameManager().isGameRunning()) {
            this.plugin.getGameManager().stopGame();
        }
        this.plugin.getConfigManager().loadConfig();
        sender.sendMessage("\u00a7aConfiguration reloaded.");
        return true;
    }

    private boolean handleClearTeams(CommandSender sender) {
        if (!sender.hasPermission("speedrunnerswap.admin")) {
            sender.sendMessage("\u00a7cYou don't have permission to use this command.");
            return false;
        }
        if (this.plugin.getGameManager().isGameRunning()) {
            this.plugin.getGameManager().stopGame();
        }
        this.plugin.getGameManager().setRunners(new ArrayList<Player>());
        this.plugin.getGameManager().setHunters(new ArrayList<Player>());
        sender.sendMessage("\u00a7aCleared all teams (runners and hunters).");
        return true;
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        ArrayList<String> completions;
        block3: {
            block2: {
                completions = new ArrayList<String>();
                if (args.length != 1) break block2;
                List<String> subCommands = Arrays.asList("start", "stop", "pause", "resume", "status", "setrunners", "sethunters", "reload", "gui", "clearteams");
                for (String subCommand : subCommands) {
                    if (!subCommand.startsWith(args[0].toLowerCase())) continue;
                    completions.add(subCommand);
                }
                break block3;
            }
            if (args.length <= 1 || !args[0].equalsIgnoreCase("setrunners") && !args[0].equalsIgnoreCase("sethunters")) break block3;
            for (Player player : Bukkit.getOnlinePlayers()) {
                String name = player.getName();
                if (!name.toLowerCase().startsWith(args[args.length - 1].toLowerCase())) continue;
                completions.add(name);
            }
        }
        return completions;
    }
}

