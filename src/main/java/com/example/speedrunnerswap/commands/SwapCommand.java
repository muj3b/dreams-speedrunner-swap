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
                case "setrunner":
                    return handleSetRunners(sender, Arrays.copyOfRange(args, 1, args.length));
                case "sethunters":
                case "sethunter":
                    return handleSetHunters(sender, Arrays.copyOfRange(args, 1, args.length));
                case "reload":
                    return handleReload(sender);
                case "gui":
                    return handleMainCommand(sender);
                case "mode":
                    return handleMode(sender, Arrays.copyOfRange(args, 1, args.length));
                case "clearteams":
                    return handleClearTeams(sender);
                case "tasks":
                    return handleTasks(sender, Arrays.copyOfRange(args, 1, args.length));
                case "complete":
                    return handleTaskComplete(sender, Arrays.copyOfRange(args, 1, args.length));
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

    private boolean handleMode(CommandSender sender, String[] rest) {
        if (!sender.hasPermission("speedrunnerswap.admin")) {
            sender.sendMessage("§cYou don't have permission to change mode.");
            return false;
        }

        if (rest.length == 0) {
            sender.sendMessage("§eCurrent mode: §f" + plugin.getCurrentMode().name().toLowerCase());
            sender.sendMessage("§7Usage: /swap mode <dream|sapnap|task> [--force]");
            sender.sendMessage("§7       /swap mode default <dream|sapnap|task>");
            return true;
        }

        String mode = rest[0].toLowerCase();

        if ("default".equals(mode)) {
            if (rest.length < 2) {
                sender.sendMessage("§eDefault mode: §f" + plugin.getConfigManager().getDefaultMode().name().toLowerCase());
                sender.sendMessage("§7Usage: /swap mode default <dream|sapnap>");
                return true;
            }
            String val = rest[1].toLowerCase();
            if (!val.equals("dream") && !val.equals("sapnap") && !val.equals("task")) {
                sender.sendMessage("§cUnknown mode: " + val);
                return false;
            }
            com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode m = switch (val) {
                case "sapnap" -> com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.SAPNAP;
                case "task" -> com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.TASK;
                default -> com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.DREAM;
            };
            plugin.getConfigManager().setDefaultMode(m);
            sender.sendMessage("§aDefault mode set to §f" + val + "§a.");
            return true;
        }
        boolean force = rest.length > 1 && ("--force".equalsIgnoreCase(rest[1]) || "-f".equalsIgnoreCase(rest[1]) || "force".equalsIgnoreCase(rest[1]));
        if (plugin.getGameManager().isGameRunning() && !force) {
            sender.sendMessage("§cStop the current game before switching modes. Add --force to end it and switch now.");
            return false;
        }
        if (force && plugin.getGameManager().isGameRunning()) {
            plugin.getGameManager().stopGame();
        }
        switch (mode) {
            case "dream":
                plugin.setCurrentMode(com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.DREAM);
                sender.sendMessage("§aMode set to §fDream§a (runners + hunters)");
                // Open main GUI if a player
                if (sender instanceof Player p) plugin.getGuiManager().openMainMenu(p);
                return true;
            case "sapnap":
            case "sapnaps":
            case "runner":
            case "runners":
                plugin.setCurrentMode(com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.SAPNAP);
                sender.sendMessage("§aMode set to §fSapnap§a (runners only)");
                if (sender instanceof Player p) {
                    try { new com.example.speedrunnerswap.gui.ControlGui(plugin).openMainMenu(p); } catch (Throwable ignored) {}
                }
                return true;
            case "task":
                plugin.setCurrentMode(com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.TASK);
                sender.sendMessage("§aMode set to §6Task Manager§a (runners only, secret tasks)");
                if (sender instanceof Player p) plugin.getGuiManager().openMainMenu(p);
                return true;
            default:
                sender.sendMessage("§cUnknown mode: " + mode + ". Use dream|sapnap|task");
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
            // Open 2-button mode selector first
            plugin.getGuiManager().openModeSelector((Player) sender);
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

    private boolean handleTasks(CommandSender sender, String[] rest) {
        if (!sender.hasPermission("speedrunnerswap.admin")) {
            sender.sendMessage("§cYou don't have permission to manage tasks.");
            return false;
        }
        if (rest.length == 0) {
            sender.sendMessage("§eUsage: /swap tasks <list|reroll|endwhenoneleft <on|off|toggle>|reload>");
            return true;
        }
        String sub = rest[0].toLowerCase();
        switch (sub) {
            case "list": {
                var tmm = plugin.getTaskManagerMode();
                if (tmm == null) { sender.sendMessage("§cTask Manager not initialized."); return false; }
                var map = tmm.getAssignments();
                if (map.isEmpty()) {
                    sender.sendMessage("§7No task assignments.");
                    return true;
                }
                sender.sendMessage("§6Task Assignments:");
                for (var e : map.entrySet()) {
                    java.util.UUID uuid = e.getKey();
                    String taskId = e.getValue();
                    String name = plugin.getServer().getOfflinePlayer(uuid).getName();
                    if (name == null) name = uuid.toString().substring(0, 8);
                    var def = tmm.getTask(taskId);
                    String desc = def != null ? def.description() : taskId;
                    sender.sendMessage("§e" + name + "§7: §f" + desc + " (§8"+taskId+"§7)");
                }
                return true;
            }
            case "reroll": {
                if (plugin.getGameManager().isGameRunning()) {
                    sender.sendMessage("§cYou can only reroll before the game starts.");
                    return false;
                }
                if (plugin.getCurrentMode() != com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.TASK) {
                    sender.sendMessage("§cSwitch to Task Manager mode first: /swap mode task");
                    return false;
                }
                var tmm = plugin.getTaskManagerMode();
                if (tmm == null) { sender.sendMessage("§cTask Manager not initialized."); return false; }
                // Build runner list from selected team assignments
                java.util.List<Player> selectedRunners = new java.util.ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    var st = plugin.getGameManager().getPlayerState(p);
                    if (st != null && st.getSelectedTeam() == com.example.speedrunnerswap.models.Team.RUNNER) selectedRunners.add(p);
                }
                if (selectedRunners.isEmpty()) {
                    sender.sendMessage("§cNo selected runners found. Use the Team Selector first.");
                    return false;
                }
                tmm.assignAndAnnounceTasks(selectedRunners);
                sender.sendMessage("§aRerolled tasks for §f"+selectedRunners.size()+"§a selected runners.");
                return true;
            }
            case "endwhenoneleft": {
                boolean cur = plugin.getConfig().getBoolean("task_manager.end_when_one_left", false);
                if (rest.length >= 2) {
                    String opt = rest[1].toLowerCase();
                    if (opt.equals("on") || opt.equals("true")) cur = true; else if (opt.equals("off") || opt.equals("false")) cur = false; else cur = !cur;
                } else { cur = !cur; }
                plugin.getConfig().set("task_manager.end_when_one_left", cur);
                plugin.saveConfig();
                sender.sendMessage("§eEnd When One Runner Left: " + (cur ? "§aON" : "§cOFF"));
                return true;
            }
            case "reload": {
                var tmm = plugin.getTaskManagerMode();
                if (tmm == null) { sender.sendMessage("§cTask Manager not initialized."); return false; }
                tmm.reloadTasks();
                sender.sendMessage("§a[Task Manager] Tasks reloaded from config without restarting!");
                return true;
            }
            default:
                sender.sendMessage("§cUnknown tasks subcommand. Use list|reroll|endwhenoneleft|reload");
                return false;
        }
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
            List<String> subCommands = Arrays.asList("start", "stop", "pause", "resume", "status", "creator", "setrunners", "setrunner", "sethunters", "sethunter", "reload", "gui", "mode", "clearteams", "tasks", "complete");
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length > 1) {
            // Player names for setrunners and sethunters (both singular and plural)
            if (args[0].equalsIgnoreCase("setrunners") || args[0].equalsIgnoreCase("setrunner") || 
                args[0].equalsIgnoreCase("sethunters") || args[0].equalsIgnoreCase("sethunter")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    String name = player.getName();
                    if (name.toLowerCase().startsWith(args[args.length - 1].toLowerCase())) {
                        completions.add(name);
                    }
                }
            } else if (args[0].equalsIgnoreCase("complete") && args.length == 2) {
                if ("confirm".startsWith(args[1].toLowerCase())) {
                    completions.add("confirm");
                }
            } else if (args[0].equalsIgnoreCase("mode") && args.length == 2) {
                for (String opt : new String[]{"dream", "sapnap", "default"}) {
                    if (opt.startsWith(args[1].toLowerCase())) completions.add(opt);
                }
            } else if (args[0].equalsIgnoreCase("mode") && args.length == 3 && "default".startsWith(args[1].toLowerCase())) {
                for (String opt : new String[]{"dream", "sapnap"}) {
                    if (opt.startsWith(args[2].toLowerCase())) completions.add(opt);
                }
            }
        }

        return completions;
    }
    
    private boolean handleTaskComplete(CommandSender sender, String[] rest) {
        // Allow any player to manually complete their task
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can complete tasks.");
            return false;
        }
        
        if (plugin.getCurrentMode() != com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.TASK) {
            sender.sendMessage("§cTask completion is only available in Task Manager mode.");
            return false;
        }
        
        if (!plugin.getGameManager().isGameRunning()) {
            sender.sendMessage("§cTasks can only be completed during an active game.");
            return false;
        }
        
        var taskMode = plugin.getTaskManagerMode();
        if (taskMode == null) {
            sender.sendMessage("§cTask Manager not initialized.");
            return false;
        }
        
        String assignedTask = taskMode.getAssignedTask(player);
        if (assignedTask == null) {
            sender.sendMessage("§cYou don't have a task assigned. Join the game as a runner first.");
            return false;
        }
        
        // Get task description for confirmation
        var taskDef = taskMode.getTask(assignedTask);
        String description = taskDef != null ? taskDef.description() : assignedTask;
        
        // Check if player wants to see their task or complete it
        if (rest.length == 0) {
            // Show current task and instructions
            player.sendMessage("§6========== §e§lYOUR TASK §6==========");
            player.sendMessage("§f" + description);
            player.sendMessage("");
            player.sendMessage("§a§lTo complete your task:");
            player.sendMessage("§e/swap complete confirm");
            player.sendMessage("");
            player.sendMessage("§7When you use this command, you will win the game!");
            player.sendMessage("§7Only use it when you have actually finished your task.");
            player.sendMessage("§6" + "=".repeat(35));
            return true;
        }
        
        String action = rest[0].toLowerCase();
        if (!"confirm".equals(action)) {
            sender.sendMessage("§cUse '/swap complete confirm' to complete your task, or '/swap complete' to see your task.");
            return false;
        }
        
        // Confirm completion
        player.sendMessage("§a§lCongratulations! You completed your task:");
        player.sendMessage("§f" + description);
        
        // Complete the task
        taskMode.complete(player);
        
        return true;
    }
}
