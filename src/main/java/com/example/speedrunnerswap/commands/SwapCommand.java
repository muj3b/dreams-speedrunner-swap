package com.example.speedrunnerswap.commands;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class SwapCommand implements CommandExecutor, TabCompleter {
    
    private final SpeedrunnerSwap plugin;
    
    public SwapCommand(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            if (args.length == 0) {
                return handleMainCommand(sender);
            }

            String subCommand = args[0].toLowerCase();

            switch (subCommand) {
                case "start":
                    return handleStart(sender);
                case "stop":
                    return handleStop(sender);
                case "pause":
                    return handlePause(sender);
                case "resume":
                    return handleResume(sender);
                case "status":
                    return handleStatus(sender);
                case "creator":
                    return handleCreator(sender);
                case "setrunners":
                    return handleSetRunners(sender, Arrays.copyOfRange(args, 1, args.length));
                case "sethunters":
                    return handleSetHunters(sender, Arrays.copyOfRange(args, 1, args.length));
                case "reload":
                    return handleReload(sender);
                case "gui":
                    return handleMainCommand(sender);
                case "clearteams":
                    return handleClearTeams(sender);
                default:
                    sender.sendMessage("§cUnknown subcommand. Use /swap for help.");
                    return false;
            }
        } catch (Exception e) {
            // Catch any unexpected errors so Bukkit doesn't show the generic message without a stacktrace
            sender.sendMessage("§cAn internal error occurred while executing that command. Check server logs for details.");
            plugin.getLogger().log(Level.SEVERE, "Unhandled exception while executing /swap by " + (sender == null ? "UNKNOWN" : sender.getName()), e);
            return false;
        }
    }

    private boolean handleCreator(CommandSender sender) {
        // No special permission; anyone can view credits/support
        final String donateUrl = plugin.getConfig().getString(
                "donation.url",
                "https://donate.stripe.com/8x29AT0H58K03judnR0Ba01"
        );

        net.kyori.adventure.text.Component header = net.kyori.adventure.text.Component.text("Speedrunner Swap")
                .color(net.kyori.adventure.text.format.NamedTextColor.GOLD)
                .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD);
        net.kyori.adventure.text.Component author = net.kyori.adventure.text.Component.text("Created by muj3b")
                .color(net.kyori.adventure.text.format.NamedTextColor.YELLOW);
        net.kyori.adventure.text.Component donate = net.kyori.adventure.text.Component.text("❤ Donate to support development")
                .color(net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE)
                .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD)
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                        net.kyori.adventure.text.Component.text("Open donation page", net.kyori.adventure.text.format.NamedTextColor.GOLD)))
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.openUrl(donateUrl));

        if (sender instanceof org.bukkit.entity.Player p) {
            p.sendMessage(header);
            p.sendMessage(author);
            p.sendMessage(donate);
        } else {
            sender.sendMessage("Speedrunner Swap — Created by muj3b");
            sender.sendMessage("Donate: " + donateUrl);
        }
        return true;
    }

    private boolean handleMainCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return false;
        }

        if (!sender.hasPermission("speedrunnerswap.command")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return false;
        }

        if (plugin.getGuiManager() == null) {
            sender.sendMessage("§cError: GUI Manager not initialized properly. Please report this to the plugin developer.");
            plugin.getLogger().log(Level.SEVERE, "GUI Manager is null when trying to open main menu");
            return false;
        }

        try {
            plugin.getGuiManager().openMainMenu((Player) sender);
            return true;
        } catch (Exception e) {
            sender.sendMessage("§cError opening GUI: " + e.getMessage());
            plugin.getLogger().log(Level.SEVERE, "Error opening GUI for player " + sender.getName(), e);
            return false;
        }
    }
    
    private boolean handleStart(CommandSender sender) {
        if (!sender.hasPermission("speedrunnerswap.command")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return false;
        }
        
        if (plugin.getGameManager().isGameRunning()) {
            sender.sendMessage("§cThe game is already running.");
            return false;
        }
        
        boolean success = plugin.getGameManager().startGame();
        if (success) {
            sender.sendMessage("§aGame started successfully.");
        } else {
            sender.sendMessage("§cFailed to start the game. Make sure there are runners set.");
        }
        
        return success;
    }
    
    private boolean handleStop(CommandSender sender) {
        if (!sender.hasPermission("speedrunnerswap.command")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return false;
        }
        
        if (!plugin.getGameManager().isGameRunning()) {
            sender.sendMessage("§cThe game is not running.");
            return false;
        }
        
        plugin.getGameManager().stopGame();
        sender.sendMessage("§aGame stopped.");
        
        return true;
    }
    
    private boolean handlePause(CommandSender sender) {
        if (!sender.hasPermission("speedrunnerswap.command")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return false;
        }
        
        if (!plugin.getGameManager().isGameRunning()) {
            sender.sendMessage("§cThe game is not running.");
            return false;
        }
        
        if (plugin.getGameManager().isGamePaused()) {
            sender.sendMessage("§cThe game is already paused.");
            return false;
        }
        
        boolean success = plugin.getGameManager().pauseGame();
        if (success) {
            sender.sendMessage("§aGame paused.");
        } else {
            sender.sendMessage("§cFailed to pause the game.");
        }
        
        return success;
    }
    
    private boolean handleResume(CommandSender sender) {
        if (!sender.hasPermission("speedrunnerswap.command")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return false;
        }
        
        if (!plugin.getGameManager().isGameRunning()) {
            sender.sendMessage("§cThe game is not running.");
            return false;
        }
        
        if (!plugin.getGameManager().isGamePaused()) {
            sender.sendMessage("§cThe game is not paused.");
            return false;
        }
        
        boolean success = plugin.getGameManager().resumeGame();
        if (success) {
            sender.sendMessage("§aGame resumed.");
        } else {
            sender.sendMessage("§cFailed to resume the game.");
        }
        
        return success;
    }
    
    private boolean handleStatus(CommandSender sender) {
        if (!sender.hasPermission("speedrunnerswap.command")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return false;
        }
        
        sender.sendMessage("§6=== SpeedrunnerSwap Status ===");
        sender.sendMessage("§eGame Running: §f" + plugin.getGameManager().isGameRunning());
        sender.sendMessage("§eGame Paused: §f" + plugin.getGameManager().isGamePaused());
        
        if (plugin.getGameManager().isGameRunning()) {
            Player activeRunner = plugin.getGameManager().getActiveRunner();
            sender.sendMessage("§eActive Runner: §f" + (activeRunner != null ? activeRunner.getName() : "None"));
            sender.sendMessage("§eTime Until Next Swap: §f" + plugin.getGameManager().getTimeUntilNextSwap() + "s");
            
            List<Player> runners = plugin.getGameManager().getRunners();
            List<Player> hunters = plugin.getGameManager().getHunters();
            
            sender.sendMessage("§eRunners: §f" + runners.stream()
                    .map(Player::getName)
                    .collect(Collectors.joining(", ")));
            
            sender.sendMessage("§eHunters: §f" + hunters.stream()
                    .map(Player::getName)
                    .collect(Collectors.joining(", ")));
        }
        
        return true;
    }
    
    private boolean handleSetRunners(CommandSender sender, String[] playerNames) {
        if (!sender.hasPermission("speedrunnerswap.command")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return false;
        }
        
        if (playerNames.length == 0) {
            sender.sendMessage("§cUsage: /swap setrunners <player1> [player2] [player3] ...");
            return false;
        }
        
        List<Player> players = new ArrayList<>();
        for (String name : playerNames) {
            Player player = Bukkit.getPlayerExact(name);
            if (player != null) {
                players.add(player);
            } else {
                sender.sendMessage("§cPlayer not found: " + name);
            }
        }
        
        if (players.isEmpty()) {
            sender.sendMessage("§cNo valid players specified.");
            return false;
        }
        
        plugin.getGameManager().setRunners(players);
        sender.sendMessage("§aRunners set: " + players.stream()
                .map(Player::getName)
                .collect(Collectors.joining(", ")));
        
        return true;
    }
    
    private boolean handleSetHunters(CommandSender sender, String[] playerNames) {
        if (!sender.hasPermission("speedrunnerswap.command")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return false;
        }
        
        if (playerNames.length == 0) {
            sender.sendMessage("§cUsage: /swap sethunters <player1> [player2] [player3] ...");
            return false;
        }
        
        List<Player> players = new ArrayList<>();
        for (String name : playerNames) {
            Player player = Bukkit.getPlayerExact(name);
            if (player != null) {
                players.add(player);
            } else {
                sender.sendMessage("§cPlayer not found: " + name);
            }
        }
        
        if (players.isEmpty()) {
            sender.sendMessage("§cNo valid players specified.");
            return false;
        }
        
        plugin.getGameManager().setHunters(players);
        sender.sendMessage("§aHunters set: " + players.stream()
                .map(Player::getName)
                .collect(Collectors.joining(", ")));
        
        return true;
    }
    
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("speedrunnerswap.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return false;
        }
        
        // Stop the game if it's running
        if (plugin.getGameManager().isGameRunning()) {
            plugin.getGameManager().stopGame();
        }
        
        // Reload the config
        plugin.getConfigManager().loadConfig();
        sender.sendMessage("§aConfiguration reloaded.");
        
        return true;
    }

    private boolean handleClearTeams(CommandSender sender) {
        if (!sender.hasPermission("speedrunnerswap.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return false;
        }

        // Stop the game if it's running
        if (plugin.getGameManager().isGameRunning()) {
            plugin.getGameManager().stopGame();
        }

        plugin.getGameManager().setRunners(new ArrayList<>());
        plugin.getGameManager().setHunters(new ArrayList<>());
        sender.sendMessage("§aCleared all teams (runners and hunters).");
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Subcommands
            List<String> subCommands = Arrays.asList("start", "stop", "pause", "resume", "status", "creator", "setrunners", "sethunters", "reload", "gui", "clearteams");
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length > 1) {
            // Player names for setrunners and sethunters
            if (args[0].equalsIgnoreCase("setrunners") || args[0].equalsIgnoreCase("sethunters")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    String name = player.getName();
                    if (name.toLowerCase().startsWith(args[args.length - 1].toLowerCase())) {
                        completions.add(name);
                    }
                }
            }
        }
        
        return completions;
    }
}
