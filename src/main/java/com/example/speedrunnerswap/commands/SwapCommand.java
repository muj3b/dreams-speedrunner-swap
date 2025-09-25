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
import java.util.Locale;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class SwapCommand implements CommandExecutor, TabCompleter {
    
    private final SpeedrunnerSwap plugin;
    
    public SwapCommand(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
    }

    private boolean handleInterval(CommandSender sender, String[] rest) {
        if (!sender.hasPermission("speedrunnerswap.admin")) { sender.sendMessage("§cYou do not have permission to run this."); return true; }
        if (rest.length < 1) { sender.sendMessage("§cUsage: /swap interval <seconds>"); return false; }
        try {
            int sec = Integer.parseInt(rest[0]);
            plugin.getConfigManager().setSwapInterval(sec);
            plugin.getGameManager().refreshSwapSchedule();
            sender.sendMessage("§aSwap interval set to §f"+plugin.getConfigManager().getSwapInterval()+"s");
            return true;
        } catch (NumberFormatException nfe) {
            sender.sendMessage("§cInvalid number: " + rest[0]);
            return false;
        }
    }

    private boolean handleRandomize(CommandSender sender, String[] rest) {
        if (!sender.hasPermission("speedrunnerswap.admin")) { sender.sendMessage("§cYou do not have permission to run this."); return true; }
        if (rest.length < 1) { sender.sendMessage("§cUsage: /swap randomize <on|off>"); return false; }
        String opt = rest[0].toLowerCase();
        boolean val = opt.startsWith("on") || opt.equals("true");
        if (!(opt.equals("on") || opt.equals("off") || opt.equals("true") || opt.equals("false"))) {
            sender.sendMessage("§cUsage: /swap randomize <on|off>");
            return false;
        }
        plugin.getConfigManager().setSwapRandomized(val);
        plugin.getGameManager().refreshSwapSchedule();
        sender.sendMessage("§eRandomized swaps: " + (val ? "§aON" : "§cOFF"));
        return true;
    }

    private boolean handleHelp(CommandSender sender) {
        sender.sendMessage("§6§lSpeedrunnerSwap Help");
        sender.sendMessage("§e/swap gui §7Open menu");
        sender.sendMessage("§e/swap start|stop|pause|resume §7Control game");
        sender.sendMessage("§e/swap interval <seconds> §7Set base swap interval");
        sender.sendMessage("§e/swap randomize <on|off> §7Toggle randomized swaps");
        sender.sendMessage("§e/swap mode <dream|sapnap|task> §7Set mode");
        sender.sendMessage("§e/swap tasks list §7List tasks with difficulty + enabled");
        sender.sendMessage("§e/swap tasks enable|disable <id> §7Toggle a task");
        sender.sendMessage("§e/swap tasks difficulty <easy|medium|hard> §7Set difficulty pool");
        sender.sendMessage("§e/swap tasks reload §7Reload tasks.yml");
        return true;
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
                case "mode":
                    return handleMode(sender, Arrays.copyOfRange(args, 1, args.length));
                case "clearteams":
                    return handleClearTeams(sender);
                case "tasks":
                    return handleTasks(sender, Arrays.copyOfRange(args, 1, args.length));
                case "complete":
                    return handleTaskComplete(sender, Arrays.copyOfRange(args, 1, args.length));
                case "interval":
                    return handleInterval(sender, Arrays.copyOfRange(args, 1, args.length));
                case "randomize":
                    return handleRandomize(sender, Arrays.copyOfRange(args, 1, args.length));
                case "help":
                    return handleHelp(sender);
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
            return true;
        }

        boolean force = false;
        String targetArg = null;
        for (String token : rest) {
            if ("--force".equalsIgnoreCase(token) || "-f".equalsIgnoreCase(token) || "force".equalsIgnoreCase(token)) {
                force = true;
            } else if (targetArg == null) {
                targetArg = token;
            }
        }

        if (targetArg == null) {
            sender.sendMessage("§cSpecify a mode to switch to (dream, sapnap, task).");
            return false;
        }

        String mode = targetArg.toLowerCase(Locale.ROOT);

        if ("default".equals(mode)) {
            sender.sendMessage("§eSetting a startup default is now handled in config.yml (game.default_mode).");
            return true;
        }

        com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode target = switch (mode) {
            case "dream", "hunters", "manhunt" -> com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.DREAM;
            case "sapnap", "control", "multi", "multirunner", "runners" -> com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.SAPNAP;
            case "task", "taskmaster", "task-manager", "taskmanager" -> com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.TASK;
            default -> null;
        };

        if (target == null) {
            sender.sendMessage("§cUnknown mode: " + mode + ". Use dream|sapnap|task.");
            return false;
        }

        if (plugin.getGameManager().isGameRunning() && !force) {
            sender.sendMessage("§cStop the current game before switching modes. Add --force to end it and switch now.");
            return false;
        }

        if (force && plugin.getGameManager().isGameRunning()) {
            plugin.getGameManager().stopGame();
        }

        plugin.setCurrentMode(target);

        String confirmation = switch (target) {
            case DREAM -> "§aMode set to §fDream§a (runners + hunters).";
            case SAPNAP -> "§aMode set to §fSapnap§a (multi-runner control).";
            case TASK -> "§aMode set to §6Task Master§a (secret objectives).";
        };
        sender.sendMessage(confirmation);

        if (sender instanceof Player player) {
            plugin.getGuiManager().openMainMenu(player);
        }

        return true;
    }

    private boolean handleCreator(CommandSender sender) {
        // No special permission; anyone can view credits/support
        final String donateUrl = plugin.getConfig().getString(
                "donation.url",
                "https://donate.stripe.com/8x29AT0H58K03judnR0Ba01"
        );

        sender.sendMessage("§6§lSpeedrunner Swap");
        sender.sendMessage("§eCreated by §f m u j 3 b");
        sender.sendMessage("§d❤ Donate to support development");
        sender.sendMessage("§b" + donateUrl);
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
            // Open direct gamemode selector - allows access to each gamemode's main menu
            plugin.getGuiManager().openDirectGamemodeSelector((Player) sender);
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
            // Provide clearer guidance depending on current mode
            var mode = plugin.getCurrentMode();
            if (mode == com.example.speedrunnerswap.SpeedrunnerSwap.SwapMode.DREAM) {
                sender.sendMessage("§cFailed to start. Dream mode requires at least §e1 runner§c and §e1 hunter§c.");
            } else {
                sender.sendMessage("§cFailed to start. You must set at least §e1 runner§c.");
            }
        }
        
        return success;
    }
    
    private boolean handleStop(CommandSender sender) {
        if (!sender.hasPermission("speedrunnerswap.admin")) {
            sender.sendMessage("§cYou do not have permission to run this.");
            return true;
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
        if (!sender.hasPermission("speedrunnerswap.admin")) {
            sender.sendMessage("§cYou do not have permission to run this.");
            return true;
        }
        plugin.getGameManager().pauseGame();
        sender.sendMessage("§eGame paused.");
        return true;
    }
    
    private boolean handleResume(CommandSender sender) {
        if (!sender.hasPermission("speedrunnerswap.admin")) {
            sender.sendMessage("§cYou do not have permission to run this.");
            return true;
        }
        plugin.getGameManager().resumeGame();
        sender.sendMessage("§aGame resumed.");
        return true;
    }
    
    private boolean handleStatus(CommandSender sender) {
        if (!sender.hasPermission("speedrunnerswap.admin")) {
            sender.sendMessage("§cYou do not have permission to run this.");
            return true;
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
            sender.sendMessage("§eUsage: /swap tasks <list|enable <id>|disable <id>|difficulty <easy|medium|hard>|reload|reroll|endwhenoneleft <on|off|toggle>>");
            return true;
        }
        String sub = rest[0].toLowerCase();
        switch (sub) {
            case "list": {
                var tmm = plugin.getTaskManagerMode();
                if (tmm == null) { sender.sendMessage("§cTask Manager not initialized."); return false; }
                var defs = tmm.getAllDefinitions();
                if (defs.isEmpty()) { sender.sendMessage("§7No tasks defined."); return true; }
                sender.sendMessage("§6Tasks (id §7|§f difficulty §7|§f enabled):");
                int shown = 0;
                for (var e : defs.entrySet()) {
                    var d = e.getValue();
                    sender.sendMessage("§e"+d.id()+" §7| §f"+(d.difficulty()!=null?d.difficulty().name():"MEDIUM")+" §7| §f"+(d.enabled()?"true":"false"));
                    if (++shown >= 50) { sender.sendMessage("§7… (showing first 50)"); break; }
                }
                sender.sendMessage("§7Difficulty filter: §f"+tmm.getDifficultyFilter().name()+"§7 | Eligible now: §a"+tmm.getCandidateCount());
                return true;
            }
            case "enable":
            case "disable": {
                if (rest.length < 2) { sender.sendMessage("§cUsage: /swap tasks "+sub+" <id>"); return false; }
                String id = rest[1];
                var tmm = plugin.getTaskManagerMode();
                if (tmm == null) { sender.sendMessage("§cTask Manager not initialized."); return false; }
                boolean ok = tmm.setTaskEnabled(id, sub.equals("enable"));
                if (!ok) { sender.sendMessage("§cUnknown task id: "+id); return false; }
                sender.sendMessage("§aTask '"+id+"' " + (sub.equals("enable")?"enabled":"disabled") + ".");
                return true;
            }
            case "difficulty": {
                if (rest.length < 2) { sender.sendMessage("§cUsage: /swap tasks difficulty <easy|medium|hard>"); return false; }
                String lvl = rest[1].toLowerCase();
                com.example.speedrunnerswap.task.TaskDifficulty d;
                switch (lvl) {
                    case "easy" -> d = com.example.speedrunnerswap.task.TaskDifficulty.EASY;
                    case "hard" -> d = com.example.speedrunnerswap.task.TaskDifficulty.HARD;
                    default -> d = com.example.speedrunnerswap.task.TaskDifficulty.MEDIUM;
                }
                var tmm = plugin.getTaskManagerMode();
                if (tmm == null) { sender.sendMessage("§cTask Manager not initialized."); return false; }
                tmm.setDifficultyFilter(d);
                sender.sendMessage("§aTask difficulty filter set to §f"+d.name());
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
                try { plugin.getTaskConfigManager().reloadConfig(); } catch (Throwable ignored) {}
                tmm.reloadTasksFromFile();
                sender.sendMessage("§a[Task Manager] tasks.yml reloaded without restart!");
                return true;
            }
            default:
                sender.sendMessage("§cUnknown tasks subcommand. Use list|enable|disable|difficulty|reroll|endwhenoneleft|reload");
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

        java.util.LinkedHashSet<Player> affected = new java.util.LinkedHashSet<>();
        affected.addAll(plugin.getGameManager().getRunners());
        affected.addAll(plugin.getGameManager().getHunters());

        plugin.getGameManager().clearAllTeams();
        sender.sendMessage("§aCleared all teams (runners and hunters).");

        for (Player target : affected) {
            if (target != null && target.isOnline() && target != sender) {
                target.sendMessage("§eYour team assignment was cleared by §f" + sender.getName() + "§e.");
            }
        }
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Subcommands (canonical names only)
            List<String> subCommands = Arrays.asList("start", "stop", "pause", "resume", "status", "creator", "setrunners", "sethunters", "reload", "gui", "mode", "clearteams", "tasks", "complete", "interval", "randomize", "help");
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
            } else if (args[0].equalsIgnoreCase("complete") && args.length == 2) {
                if ("confirm".startsWith(args[1].toLowerCase())) {
                    completions.add("confirm");
                }
            } else if (args[0].equalsIgnoreCase("mode") && args.length == 2) {
                for (String opt : new String[]{"dream", "sapnap", "task"}) {
                    if (opt.startsWith(args[1].toLowerCase())) completions.add(opt);
                }
                if ("--force".startsWith(args[1].toLowerCase())) {
                    completions.add("--force");
                }
            } else if (args[0].equalsIgnoreCase("mode") && args.length >= 3) {
                String current = args[args.length - 1].toLowerCase();
                if ("--force".startsWith(current)) {
                    completions.add("--force");
                } else if ("-f".startsWith(current)) {
                    completions.add("-f");
                }
            } else if (args[0].equalsIgnoreCase("randomize") && args.length == 2) {
                for (String opt : new String[]{"on","off"}) if (opt.startsWith(args[1].toLowerCase())) completions.add(opt);
            } else if (args[0].equalsIgnoreCase("tasks")) {
                if (args.length == 2) {
                    for (String opt : new String[]{"list","enable","disable","difficulty","reload","reroll","endwhenoneleft"}) if (opt.startsWith(args[1].toLowerCase())) completions.add(opt);
                } else if (args.length == 3 && (args[1].equalsIgnoreCase("enable") || args[1].equalsIgnoreCase("disable"))) {
                    try {
                        var defs = plugin.getTaskManagerMode().getAllDefinitions();
                        for (String id : defs.keySet()) if (id.toLowerCase().startsWith(args[2].toLowerCase())) completions.add(id);
                    } catch (Throwable ignored) {}
                } else if (args.length == 3 && args[1].equalsIgnoreCase("difficulty")) {
                    for (String lvl : new String[]{"easy","medium","hard"}) if (lvl.startsWith(args[2].toLowerCase())) completions.add(lvl);
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
