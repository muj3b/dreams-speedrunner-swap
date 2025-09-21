package com.example.speedrunnerswap.gui;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ControlGuiListener implements Listener {
    private final SpeedrunnerSwap plugin;

    // Temporary selections per player for runner selector GUI
    private final Map<java.util.UUID, Set<String>> pendingRunnerSelections = new HashMap<>();

    public ControlGuiListener(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
    }
    
    // Helper method to get button ID from persistent data
    private String getButtonId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        
        org.bukkit.persistence.PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "ssw_button_id");
        
        if (container.has(key, org.bukkit.persistence.PersistentDataType.STRING)) {
            return container.get(key, org.bukkit.persistence.PersistentDataType.STRING);
        }
        return null;
    }

    private boolean isMain(Inventory inv) {
        return inv != null && inv.getHolder() instanceof ControlGuiHolder holder && holder.getType() == ControlGuiHolder.Type.MAIN;
    }

    private boolean isRunnerSelector(Inventory inv) {
        return inv != null && inv.getHolder() instanceof ControlGuiHolder holder && holder.getType() == ControlGuiHolder.Type.RUNNER_SELECTOR;
    }
    
    private boolean isCoordination(Inventory inv) {
        return inv != null && inv.getHolder() instanceof ControlGuiHolder holder && holder.getType() == ControlGuiHolder.Type.COORDINATION;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory top = event.getView().getTopInventory();
        if (top == null) return;
        
        // Check if this is one of our GUI inventories
        if (!isMain(top) && !isRunnerSelector(top) && !isCoordination(top)) return;
        
        // ALWAYS cancel the event to prevent item movement
        event.setCancelled(true);
        
        // Debug logging
        plugin.getLogger().info("GUI Click detected - Slot: " + event.getSlot() + ", RawSlot: " + event.getRawSlot());
        
        // Prevent any click in player inventory when GUI is open
        if (event.getRawSlot() >= top.getSize()) {
            // Click in player inventory - block it completely
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        
        plugin.getLogger().info("Processing click on item: " + clicked.getType());

        if (isMain(top)) {
            handleMainClick(player, clicked);
        } else if (isRunnerSelector(top)) {
            handleRunnerSelectorClick(player, clicked, event.getRawSlot(), top.getSize());
        } else if (isCoordination(top)) {
            handleCoordinationClick(player, clicked);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Inventory top = event.getView().getTopInventory();
        if (top == null) return;
        
        // Check if this is one of our GUI inventories
        if (!isMain(top) && !isRunnerSelector(top) && !isCoordination(top)) return;
        
        // ALWAYS cancel drag events in our GUIs
        event.setCancelled(true);
        plugin.getLogger().info("Blocked drag attempt in GUI");
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryMoveItem(org.bukkit.event.inventory.InventoryMoveItemEvent event) {
        Inventory source = event.getSource();
        Inventory destination = event.getDestination();
        
        // Block any item movement from/to our GUIs
        if ((source != null && (isMain(source) || isRunnerSelector(source) || isCoordination(source))) ||
            (destination != null && (isMain(destination) || isRunnerSelector(destination) || isCoordination(destination)))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        Inventory top = event.getView().getTopInventory();
        if (top == null) return;
        if (isRunnerSelector(top)) {
            // Drop any temporary selection when closing runner selector without saving
            pendingRunnerSelections.remove(player.getUniqueId());
        }
    }

    private void handleMainClick(Player player, ItemStack clicked) {
        Material type = clicked.getType();
        boolean running = plugin.getGameManager().isGameRunning();
        
        // Debug logging
        String itemName = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
        plugin.getLogger().info("Main GUI click - Material: " + type + ", Display Name: " + itemName);
        
        // Check for button ID first (more reliable than material type)
        String buttonId = getButtonId(clicked);
        if (buttonId != null) {
            plugin.getLogger().info("Found button ID: " + buttonId);
            switch (buttonId) {
                case "manage_runners" -> {
                    plugin.getLogger().info("Processing manage_runners button");
                    // Initialize pending selection with current config
                    Set<String> initial = new HashSet<>(plugin.getConfigManager().getRunnerNames());
                    pendingRunnerSelections.put(player.getUniqueId(), initial);
                    new ControlGui(plugin).openRunnerSelector(player);
                    return;
                }
            }
        }

        if (type == Material.LIME_WOOL) {
            plugin.getLogger().info("Processing LIME_WOOL (Start Game) click");
            if (!running) {
                // Ensure mode is runner-only
                plugin.setCurrentMode(SpeedrunnerSwap.SwapMode.SAPNAP);
                // In Sapnap mode, always set all online players as runners
                List<String> names = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
                plugin.getConfigManager().setRunnerNames(names);
                plugin.getGameManager().setRunners(new ArrayList<>(Bukkit.getOnlinePlayers()));
                plugin.getGameManager().startGame();
                player.sendMessage("§aGame started!");
            } else {
                player.sendMessage("§cGame is already running!");
            }
            new ControlGui(plugin).openMainMenu(player);
            return;
        }

        if (type == Material.RED_WOOL) {
            plugin.getLogger().info("Processing RED_WOOL (Stop Game) click");
            if (running) {
                plugin.getGameManager().stopGame();
                player.sendMessage("§cGame stopped!");
            } else {
                player.sendMessage("§cGame is not running!");
            }
            new ControlGui(plugin).openMainMenu(player);
            return;
        }

        if (type == Material.YELLOW_WOOL) {
            plugin.getLogger().info("Processing YELLOW_WOOL (Pause Game) click");
            if (running && !plugin.getGameManager().isGamePaused()) {
                plugin.getGameManager().pauseGame();
                player.sendMessage("§eGame paused!");
            } else {
                player.sendMessage("§cCannot pause - game not running or already paused!");
            }
            new ControlGui(plugin).openMainMenu(player);
            return;
        }

        if (type == Material.ORANGE_WOOL) {
            plugin.getLogger().info("Processing ORANGE_WOOL (Resume Game) click");
            if (running && plugin.getGameManager().isGamePaused()) {
                plugin.getGameManager().resumeGame();
                player.sendMessage("§aGame resumed!");
            } else {
                player.sendMessage("§cCannot resume - game not running or not paused!");
            }
            new ControlGui(plugin).openMainMenu(player);
            return;
        }

        if (type == Material.NETHER_STAR) {
            plugin.getLogger().info("Processing NETHER_STAR (Shuffle Queue) click");
            if (plugin.getGameManager().shuffleQueue()) {
                player.sendMessage("§aShuffled runner queue successfully.");
            } else {
                player.sendMessage("§cCannot shuffle queue - need at least 2 runners.");
            }
            new ControlGui(plugin).openMainMenu(player);
            return;
        }

        if (type == Material.PLAYER_HEAD) {
            String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
            plugin.getLogger().info("Processing PLAYER_HEAD click with name: " + name);
            if (name != null && name.contains("Manage Runners")) {
                plugin.getLogger().info("Opening runner selector");
                // Initialize pending selection with current config
                Set<String> initial = new HashSet<>(plugin.getConfigManager().getRunnerNames());
                pendingRunnerSelections.put(player.getUniqueId(), initial);
                new ControlGui(plugin).openRunnerSelector(player);
                return;
            }
        }

        if (type == Material.BOOK) {
            String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
            if (name != null && name.contains("Cooperation Tips")) {
                // Show extended cooperation tips in chat
                player.sendMessage("§6=== §e§lSapnap Mode Cooperation Tips §6===");
                player.sendMessage("§b1. Communication is Key!§7 Use voice chat or text to coordinate");
                player.sendMessage("§b2. Share Everything!§7 Leave items for teammates in shared chests");
                player.sendMessage("§b3. Plan Ahead!§7 Discuss strategies while inactive");
                player.sendMessage("§b4. Stay Informed!§7 Active runner should narrate their actions");
                player.sendMessage("§b5. Resource Management!§7 Don't waste materials when time is limited");
                player.sendMessage("§b6. Safe Locations!§7 Try to swap in secure areas");
                player.sendMessage("§b7. Emergency Protocols!§7 Agree on what to do in dangerous situations");
                player.sendMessage("§b8. Goal Coordination!§7 Work together toward the Ender Dragon!");
                player.sendMessage("§6========================================");
                return;
            }
        }

        // Save current as this mode's default
        if (type == Material.WRITABLE_BOOK) {
            String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
            if ("Save as Mode Default".equals(name)) {
                int curr = plugin.getConfigManager().getSwapInterval();
                plugin.getConfigManager().setModeDefaultInterval(plugin.getCurrentMode(), curr);
                player.sendMessage("§aSaved " + curr + "s as default for this mode.");
                new ControlGui(plugin).openMainMenu(player);
                return;
            }
        }

        if (type == Material.GOLDEN_SWORD) {
            String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
            if (name != null && name.contains("Quick Actions")) {
                // Open quick actions menu or perform instant action
                handleQuickActions(player);
                return;
            }
        }
        
        if (type == Material.ENCHANTED_BOOK) {
            String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
            if (name != null && name.contains("Live Analytics")) {
                // Show detailed analytics in chat
                showDetailedAnalytics(player);
                return;
            } else if (name != null && name.contains("Smart Help")) {
                // Show context-sensitive help
                showSmartHelp(player);
                return;
            }
        }
        
        if (type == Material.BARRIER) {
            String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
            if (name != null && name.contains("Performance Alert")) {
                // Show optimization tips
                showOptimizationTips(player);
                return;
            }
        }
        
        if (type == Material.COMPASS) {
            String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
            if (name != null && name.contains("Team Coordination")) {
                new ControlGui(plugin).openTeamCoordinationMenu(player);
                return;
            }
        }
        
        if (type == Material.REDSTONE) {
            String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
            if (name != null && name.contains("Advanced Settings")) {
                // Open the comprehensive advanced settings menu
                plugin.getGuiManager().openSettingsMenu(player);
                return;
            }
        }

        if (type == Material.CLOCK) {
            String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
            if (name != null && name.contains("Game Status")) {
                // Show detailed status in chat
                player.sendMessage("§6=== §b§lSapnap Mode Status §6===");
                player.sendMessage("§eMode: §bMulti-Runner Cooperation");
                player.sendMessage("§eRunning: §f" + plugin.getGameManager().isGameRunning());
                player.sendMessage("§ePaused: §f" + plugin.getGameManager().isGamePaused());
                if (plugin.getGameManager().isGameRunning()) {
                    Player active = plugin.getGameManager().getActiveRunner();
                    player.sendMessage("§eActive Runner: §f" + (active != null ? active.getName() : "None"));
                    player.sendMessage("§eNext Swap: §f" + plugin.getGameManager().getTimeUntilNextSwap() + "s");
                    List<Player> runnersList = plugin.getGameManager().getRunners();
                    String runners = runnersList != null ? runnersList.stream().map(Player::getName).collect(Collectors.joining(", ")) : "None";
                    player.sendMessage("§eRunners: §f" + runners);
                    player.sendMessage("§eQueue Position: §f" + getQueueInfo(player));
                }
                player.sendMessage("§6===========================");
                return;
            }
        }

        if (type == Material.ARMOR_STAND || type == Material.ENDER_EYE || type == Material.ENDER_PEARL || type == Material.BEDROCK || type == Material.POTION) {
            String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
            if (name != null && name.contains("Inactive State:")) {
                // Cycle freeze mode EFFECTS -> SPECTATOR -> LIMBO -> CAGE -> EFFECTS
                String mode = plugin.getConfigManager().getFreezeMode();
                String next = switch (mode.toUpperCase()) {
                    case "EFFECTS" -> "SPECTATOR";
                    case "SPECTATOR" -> "LIMBO";
                    case "LIMBO" -> "CAGE";
                    default -> "EFFECTS";
                };
                plugin.getConfigManager().setFreezeMode(next);
                player.sendMessage("§eInactive runner state: §a" + next);
                // Re-apply to all
                plugin.getGameManager().reapplyStates();
                new ControlGui(plugin).openMainMenu(player);
                return;
            }
        }

        if (type == Material.SLIME_BLOCK || type == Material.MAGMA_BLOCK) {
            boolean enabled = plugin.getConfigManager().isSafeSwapEnabled();
            plugin.getConfigManager().setSafeSwapEnabled(!enabled);
            player.sendMessage("§eSafe Swap: " + (!enabled ? "§aEnabled" : "§cDisabled"));
            new ControlGui(plugin).openMainMenu(player);
            return;
        }

        if (type == Material.WHITE_BED || type == Material.RED_BED) {
            boolean enabled = plugin.getConfigManager().isSinglePlayerSleepEnabled();
            plugin.getConfigManager().setSinglePlayerSleepEnabled(!enabled);
            player.sendMessage("§eSingle Player Sleep: " + (!enabled ? "§aEnabled" : "§cDisabled"));
            new ControlGui(plugin).openMainMenu(player);
            return;
        }

        // Experimental intervals toggle
        if (type == Material.LEVER || type == Material.REDSTONE_TORCH) {
            String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
            if (name != null && name.startsWith("Experimental Intervals:")) {
                boolean enabled = plugin.getConfigManager().isBetaIntervalEnabled();
                plugin.getConfigManager().setBetaIntervalEnabled(!enabled);
                player.sendMessage("§eExperimental Intervals: " + (!enabled ? "§aEnabled" : "§cDisabled"));
                new ControlGui(plugin).openMainMenu(player);
                return;
            }
        }

        // Apply default on mode switch toggle
        if (type == Material.NOTE_BLOCK || type == Material.GRAY_DYE) {
            String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
            if (name != null && name.startsWith("Apply Mode Default on Switch:")) {
                boolean current = plugin.getConfigManager().getApplyDefaultOnModeSwitch();
                plugin.getConfigManager().setApplyDefaultOnModeSwitch(!current);
                player.sendMessage("§eApply Default on Switch: " + (!current ? "§aYes" : "§cNo"));
                new ControlGui(plugin).openMainMenu(player);
                return;
            }
        }

        // Reset interval to mode default
        if (type == Material.BARRIER) {
            String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
            if ("Reset Interval".equals(name)) {
                plugin.getConfigManager().applyModeDefaultInterval(plugin.getCurrentMode());
                plugin.getGameManager().refreshSwapSchedule();
                player.sendMessage("§eInterval reset to mode default.");
                new ControlGui(plugin).openMainMenu(player);
                return;
            }
        }

        if (type == Material.ARROW) {
            String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
            
            // Handle interval adjustment arrows FIRST (higher priority)
            if (name != null) {
                if (name.equals("-5s")) {
                    int current = plugin.getConfigManager().getSwapInterval();
                    boolean beta = plugin.getConfigManager().isBetaIntervalEnabled();
                    int minAllowed = beta ? 10 : plugin.getConfigManager().getMinSwapInterval();
                    int maxAllowed = plugin.getConfigManager().getSwapIntervalMax();
                    int interval = current - 5;
                    interval = beta ? Math.max(minAllowed, interval) : Math.max(minAllowed, Math.min(maxAllowed, interval));
                    plugin.getConfigManager().setSwapInterval(interval);
                    player.sendMessage("§eInterval decreased to: §a" + interval + "s");
                    if (interval < 30) {
                        player.sendMessage("§cBETA: Intervals below 30s are experimental and may be unstable.");
                        if (interval < 15) player.sendMessage("§cWarning: Intervals under 15s may impact performance.");
                    }
                    if (interval > maxAllowed) {
                        player.sendMessage("§cBETA: Intervals above configured maximum are experimental.");
                    }
                    plugin.getGameManager().refreshSwapSchedule();
                    new ControlGui(plugin).openMainMenu(player);
                    return;
                } else if (name.equals("+5s")) {
                    int current = plugin.getConfigManager().getSwapInterval();
                    boolean beta = plugin.getConfigManager().isBetaIntervalEnabled();
                    int minAllowed = beta ? 10 : plugin.getConfigManager().getMinSwapInterval();
                    int maxAllowed = plugin.getConfigManager().getSwapIntervalMax();
                    int interval = current + 5;
                    interval = beta ? Math.max(minAllowed, interval) : Math.max(minAllowed, Math.min(maxAllowed, interval));
                    plugin.getConfigManager().setSwapInterval(interval);
                    player.sendMessage("§eInterval increased to: §a" + interval + "s");
                    if (interval > maxAllowed) {
                        player.sendMessage("§cBETA: Intervals above configured maximum are experimental and may be unstable.");
                    }
                    plugin.getGameManager().refreshSwapSchedule();
                    new ControlGui(plugin).openMainMenu(player);
                    return;
                }
            }
            
            // Handle back button (lower priority)
            if ("§7§lBack".equals(name)) {
                plugin.getGuiManager().openModeSelector(player);
                return;
            }
        }

        if (type == Material.COMPARATOR) {
            boolean randomize = plugin.getConfigManager().isSwapRandomized();
            plugin.getConfigManager().setSwapRandomized(!randomize);
            player.sendMessage("§eRandomize swaps: " + (!randomize ? "§aEnabled" : "§cDisabled"));
            new ControlGui(plugin).openMainMenu(player);
            return;
        }

        if (type == Material.PAPER) {
            String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
            plugin.getLogger().info("Processing PAPER click with name: " + name);
            if ("Status".equals(name)) {
                // Print status
                player.sendMessage("§6=== Runner-Only Status ===");
                player.sendMessage("§eGame Running: §f" + plugin.getGameManager().isGameRunning());
                player.sendMessage("§eGame Paused: §f" + plugin.getGameManager().isGamePaused());
                if (plugin.getGameManager().isGameRunning()) {
                    Player active = plugin.getGameManager().getActiveRunner();
                    player.sendMessage("§eActive Runner: §f" + (active != null ? active.getName() : "None"));
                    player.sendMessage("§eTime Until Next Swap: §f" + plugin.getGameManager().getTimeUntilNextSwap() + "s");
                    List<Player> runnersList = plugin.getGameManager().getRunners();
                    String runners = runnersList != null ? runnersList.stream().map(Player::getName).collect(Collectors.joining(", ")) : "None";
                    player.sendMessage("§eRunners: §f" + runners);
                }
                return;
            }
        }
        
        // Catch-all for unhandled clicks - helps debug GUI issues
        plugin.getLogger().warning("Unhandled GUI click - Material: " + type + ", Display Name: " + itemName);
        player.sendMessage("§eClick detected but not handled. Material: " + type + ". Check console for details.");
    }

    private void handleRunnerSelectorClick(Player player, ItemStack clicked, int rawSlot, int size) {
        // In Sapnap mode, all players are runners, so return to main menu
        if (plugin.getCurrentMode() == SpeedrunnerSwap.SwapMode.SAPNAP) {
            new ControlGui(plugin).openMainMenu(player);
            return;
        }

        Material type = clicked.getType();
        // Bottom row buttons
        if (rawSlot >= size - 9) {
            if (type == Material.BARRIER) {
                // Discard
                pendingRunnerSelections.remove(player.getUniqueId());
                new ControlGui(plugin).openMainMenu(player);
                return;
            } else if (type == Material.ARROW) {
                new ControlGui(plugin).openMainMenu(player);
                return;
            } else if (type == Material.EMERALD_BLOCK) {
                // Apply
                Set<String> sel = pendingRunnerSelections.remove(player.getUniqueId());
                if (sel == null) sel = new HashSet<>();
                plugin.getConfigManager().setRunnerNames(new ArrayList<>(sel));
                List<Player> players = new ArrayList<>();
                for (String name : sel) {
                    Player p = Bukkit.getPlayerExact(name);
                    if (p != null && p.isOnline()) players.add(p);
                }
                plugin.getGameManager().setRunners(players);
                player.sendMessage("§aRunners set: §f" + String.join(", ", sel));
                new ControlGui(plugin).openMainMenu(player);
                return;
            }
        }

        // Toggle player selection based on the head name
        if (type == Material.PLAYER_HEAD) {
            String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
            if (name == null || name.isBlank()) return;
            Set<String> sel = pendingRunnerSelections.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>(plugin.getConfigManager().getRunnerNames()));
            if (sel.contains(name)) sel.remove(name); else sel.add(name);
            // Refresh the selector GUI
            new ControlGui(plugin).openRunnerSelector(player);
        }
    }

    private String getQueueInfo(Player player) {
        List<Player> runners = plugin.getGameManager().getRunners();
        Player active = plugin.getGameManager().getActiveRunner();
        
        // Handle null runners list
        if (runners == null || runners.isEmpty()) {
            return "No runners configured";
        }
        
        if (!runners.contains(player)) {
            return "Not in queue";
        }
        
        if (player == active) {
            return "Currently Active";
        }
        
        int position = runners.indexOf(player);
        int activeIndex = runners.indexOf(active);
        
        if (activeIndex == -1) {
            return "Position " + (position + 1) + " of " + runners.size();
        }
        
        // Calculate how many swaps until this player's turn
        int swapsUntilTurn;
        if (position > activeIndex) {
            swapsUntilTurn = position - activeIndex;
        } else {
            swapsUntilTurn = (runners.size() - activeIndex) + position;
        }
        
        return "Next in " + swapsUntilTurn + " swap(s)";
    }
    
    private void handleCoordinationClick(Player player, ItemStack clicked) {
        Material type = clicked.getType();
        
        if (type == Material.ARROW) {
            // Back to main menu
            new ControlGui(plugin).openMainMenu(player);
            return;
        }
        
        if (type == Material.BELL) {
            // Broadcast location with enhanced error handling
            String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
            if (name != null && name.contains("Broadcast Location")) {
                try {
                    String location = String.format("%d, %d, %d in %s", 
                        (int) player.getLocation().getX(),
                        (int) player.getLocation().getY(), 
                        (int) player.getLocation().getZ(),
                        player.getWorld().getName());
                    
                    String message = "§6[§eTeam§6] §b" + player.getName() + "§7 is at: §f" + location;
                    
                    // Broadcast to all runners with null checks
                    List<Player> runners = plugin.getGameManager().getRunners();
                    int broadcastCount = 0;
                    if (runners != null) {
                        for (Player runner : runners) {
                            if (runner != null && runner.isOnline()) {
                                runner.sendMessage(message);
                                broadcastCount++;
                            }
                        }
                    }
                    
                    player.sendMessage("§aLocation broadcasted to " + broadcastCount + " team members!");
                } catch (Exception e) {
                    player.sendMessage("§cError broadcasting location. Please try again.");
                }
                return;
            }
        }
        
        if (type == Material.CHEST) {
            // Share inventory status
            String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
            if (name != null && name.contains("Inventory Status")) {
                List<String> importantItems = new ArrayList<>();
                
                // Check for key items
                for (ItemStack item : player.getInventory().getContents()) {
                    if (item == null || item.getType() == Material.AIR) continue;
                    
                    Material mat = item.getType();
                    int amount = item.getAmount();
                    
                    // Tools, weapons, food, and important items
                    if (mat.name().contains("SWORD") || mat.name().contains("AXE") || 
                        mat.name().contains("PICKAXE") || mat.name().contains("SHOVEL") ||
                        mat == Material.BOW || mat == Material.CROSSBOW ||
                        mat == Material.BREAD || mat == Material.COOKED_BEEF ||
                        mat == Material.ENDER_PEARL || mat == Material.BLAZE_ROD ||
                        mat == Material.DIAMOND || mat == Material.EMERALD ||
                        mat == Material.IRON_INGOT || mat == Material.GOLD_INGOT) {
                        
                        String itemName = mat.name().toLowerCase().replace("_", " ");
                        importantItems.add(amount + "x " + itemName);
                    }
                }
                
                String statusMsg;
                if (importantItems.isEmpty()) {
                    statusMsg = "§6[§eTeam§6] §b" + player.getName() + "§7 has no important items";
                } else {
                    statusMsg = "§6[§eTeam§6] §b" + player.getName() + "§7 has: §f" + String.join(", ", importantItems);
                }
                
                // Broadcast to all runners
                List<Player> runners = plugin.getGameManager().getRunners();
                if (runners != null) {
                    for (Player runner : runners) {
                        if (runner != null && runner.isOnline()) {
                            runner.sendMessage(statusMsg);
                        }
                    }
                }
                
                player.sendMessage("§aInventory status shared with team!");
                return;
            }
        }
        
        if (type == Material.REDSTONE_TORCH) {
            // Emergency pause request
            String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
            if (name != null && name.contains("Emergency Pause")) {
                if (plugin.getGameManager().isGameRunning() && !plugin.getGameManager().isGamePaused()) {
                    plugin.getGameManager().pauseGame();
                    
                    String pauseMsg = "§c[§lEMERGENCY§c] §6" + player.getName() + "§e requested emergency pause!";
                    
                    // Notify all runners
                    List<Player> runners = plugin.getGameManager().getRunners();
                    if (runners != null) {
                        for (Player runner : runners) {
                            if (runner != null && runner.isOnline()) {
                                runner.sendMessage(pauseMsg);
                                // Create title with modern API
                                net.kyori.adventure.title.Title title = net.kyori.adventure.title.Title.title(
                                    net.kyori.adventure.text.Component.text("§c§lEMERGENCY PAUSE"),
                                    net.kyori.adventure.text.Component.text("§6Requested by " + player.getName()),
                                    net.kyori.adventure.title.Title.Times.times(
                                        java.time.Duration.ofMillis(500),
                                        java.time.Duration.ofMillis(2000),
                                        java.time.Duration.ofMillis(500)
                                    )
                                );
                                runner.showTitle(title);
                            }
                        }
                    }
                    
                    player.sendMessage("§aGame paused! Emergency pause activated.");
                } else {
                    player.sendMessage("§cGame is not running or already paused!");
                }
                return;
            }
        }
        
        if (type == Material.ENDER_EYE) {
            // Mark waypoint (placeholder - requires more complex implementation)
            String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
            if (name != null && name.contains("Mark Waypoint")) {
                // For now, just broadcast the location as a waypoint
                String location = String.format("%d, %d, %d", 
                    (int) player.getLocation().getX(),
                    (int) player.getLocation().getY(), 
                    (int) player.getLocation().getZ());
                
                String waypointMsg = "§d[§lWAYPOINT§d] §b" + player.getName() + "§7 marked: §f" + location + "§7 - Check your compass!";
                
                // Broadcast to all runners
                List<Player> runners = plugin.getGameManager().getRunners();
                if (runners != null) {
                    for (Player runner : runners) {
                        if (runner != null && runner.isOnline()) {
                            runner.sendMessage(waypointMsg);
                        }
                    }
                }
                
                player.sendMessage("§aWaypoint marked and shared with team!");
                return;
            }
        }
    }
    
    // Quick Actions Handler - Lightning fast team coordination with enhanced error handling
    private void handleQuickActions(Player player) {
        if (!plugin.getGameManager().isGameRunning()) {
            player.sendMessage("§cQuick actions only available during active games!");
            return;
        }
        
        try {
            // Instant location broadcast with enhanced information
            String location = String.format("%d, %d, %d in %s", 
                (int) player.getLocation().getX(),
                (int) player.getLocation().getY(), 
                (int) player.getLocation().getZ(),
                player.getWorld().getName());
            
            // Enhanced status information
            double health = Math.round(player.getHealth() * 10.0) / 10.0; // Round to 1 decimal
            int food = player.getFoodLevel();
            String gamemode = player.getGameMode().name().toLowerCase();
            
            String quickMsg = "§e[§lQUICK§e] §b" + player.getName() + "§7: §f" + location + 
                             "§7 | Health: §c" + health + "/20 §7| Food: §6" + food + "/20 §7| Mode: §d" + gamemode;
            
            // Broadcast to all runners with error handling
            List<Player> runners = plugin.getGameManager().getRunners();
            if (runners != null && !runners.isEmpty()) {
                for (Player runner : runners) {
                    if (runner != null && runner.isOnline()) {
                        runner.sendMessage(quickMsg);
                    }
                }
            }
            
            player.sendMessage("§a⚡ Quick status broadcasted to " + (runners != null ? runners.size() : 0) + " runners!");
            
            // Send title notification to active runner if different
            Player activeRunner = plugin.getGameManager().getActiveRunner();
            if (activeRunner != null && !activeRunner.equals(player) && activeRunner.isOnline()) {
                net.kyori.adventure.title.Title title = net.kyori.adventure.title.Title.title(
                    net.kyori.adventure.text.Component.text("§e§lTEAM UPDATE"),
                    net.kyori.adventure.text.Component.text("§b" + player.getName() + "§7 shared location"),
                    net.kyori.adventure.title.Title.Times.times(
                        java.time.Duration.ofMillis(250),
                        java.time.Duration.ofMillis(1500),
                        java.time.Duration.ofMillis(250)
                    )
                );
                activeRunner.showTitle(title);
            }
        } catch (Exception e) {
            player.sendMessage("§cError sending quick update. Please try again.");
            // Log error for debugging if needed
        }
    }
    
    // Detailed Analytics Display
    private void showDetailedAnalytics(Player player) {
        if (!plugin.getGameManager().isGameRunning()) {
            player.sendMessage("§cAnalytics only available during active games!");
            return;
        }
        
        player.sendMessage("§6========== §b§lDETAILED ANALYTICS §6==========");
        
        // Game state info
        Player activeRunner = plugin.getGameManager().getActiveRunner();
        List<Player> runners = plugin.getGameManager().getRunners();
        int timeLeft = plugin.getGameManager().getTimeUntilNextSwap();
        
        player.sendMessage("§e§lGame Status:");
        player.sendMessage("§7  Active Runner: §b" + (activeRunner != null ? activeRunner.getName() : "None"));
        player.sendMessage("§7  Total Runners: §e" + (runners != null ? runners.size() : 0));
        player.sendMessage("§7  Next Swap: §e" + timeLeft + "s");
        player.sendMessage("§7  Game Paused: §f" + plugin.getGameManager().isGamePaused());
        
        // Performance metrics
        player.sendMessage("");
        player.sendMessage("§e§lPerformance Metrics:");
        
        try {
            double tps = Bukkit.getTPS()[0];
            String tpsStatus = tps >= 18.0 ? "§aExcellent" : tps >= 15.0 ? "§eGood" : "§cPoor";
            player.sendMessage("§7  Server TPS: §f" + String.format("%.2f", tps) + " (" + tpsStatus + "§7)");
        } catch (Exception e) {
            player.sendMessage("§7  Server TPS: §7Unavailable");
        }
        
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        int memoryPercent = (int) ((usedMemory * 100) / maxMemory);
        String memStatus = memoryPercent < 70 ? "§aGood" : memoryPercent < 85 ? "§eModerate" : "§cHigh";
        
        player.sendMessage("§7  Memory Usage: §f" + usedMemory + "/" + maxMemory + "MB (" + memoryPercent + "% - " + memStatus + "§7)");
        
        // Team coordination insights
        player.sendMessage("");
        player.sendMessage("§e§lTeam Insights:");
        
        int runnerCount = runners != null ? runners.size() : 0;
        if (runnerCount <= 2) {
            player.sendMessage("§7  Team Size: §eSmall - Focus on efficiency");
        } else if (runnerCount >= 5) {
            player.sendMessage("§7  Team Size: §cLarge - Monitor coordination");
        } else {
            player.sendMessage("§7  Team Size: §aOptimal for cooperation");
        }
        
        // Recommendations
        player.sendMessage("");
        player.sendMessage("§e§lSmart Recommendations:");
        
        if (timeLeft <= 15) {
            player.sendMessage("§7  ➤ §cPrepare for imminent swap!");
        } else if (timeLeft >= 60) {
            player.sendMessage("§7  ➤ §bGood time for strategic planning");
        }
        
        if (memoryPercent > 80) {
            player.sendMessage("§7  ➤ §cConsider reducing view distance");
        }
        
        if (runnerCount >= 4 && plugin.getConfigManager().getSwapInterval() > 60) {
            player.sendMessage("§7  ➤ §eLarge team: Consider shorter intervals");
        }
        
        player.sendMessage("§6" + "=".repeat(45));
    }
    
    // Smart Help System - Context-aware guidance
    private void showSmartHelp(Player player) {
        boolean running = plugin.getGameManager().isGameRunning();
        boolean paused = plugin.getGameManager().isGamePaused();
        
        player.sendMessage("§6========== §e§lSMART HELP SYSTEM §6==========");
        player.sendMessage("§e§lContext-Aware Guidance for Sapnap Mode");
        player.sendMessage("");
        
        if (!running) {
            player.sendMessage("§b§lGetting Started:");
            player.sendMessage("§7  1. ➤ Select your team of runners");
            player.sendMessage("§7  2. ➤ Configure swap interval (30-120s recommended)");
            player.sendMessage("§7  3. ➤ Choose freeze mode for inactive players");
            player.sendMessage("§7  4. ➤ Set up voice communication (Discord/TeamSpeak)");
            player.sendMessage("§7  5. ➤ Click 'Start Game' to begin cooperation!");
            player.sendMessage("");
            player.sendMessage("§e§lRecommended Settings:");
            player.sendMessage("§7  • Team Size: 2-4 players (optimal)");
            player.sendMessage("§7  • Swap Interval: 45-60s (balanced)");
            player.sendMessage("§7  • Freeze Mode: SPECTATOR (most flexible)");
            player.sendMessage("§7  • Safe Swaps: ON (prevents danger)");
        } else if (paused) {
            player.sendMessage("§c§lGame Paused - Strategic Opportunities:");
            player.sendMessage("§7  ➤ Perfect time for team discussion");
            player.sendMessage("§7  ➤ Plan your next objectives");
            player.sendMessage("§7  ➤ Share inventory and resource status");
            player.sendMessage("§7  ➤ Coordinate meeting points");
            player.sendMessage("§7  ➤ Use Team Coordination tools");
            player.sendMessage("§7  ➤ Click 'Resume' when ready to continue");
        } else {
            int timeLeft = plugin.getGameManager().getTimeUntilNextSwap();
            if (timeLeft <= 15) {
                player.sendMessage("§c§lSwap Imminent - Immediate Actions:");
                player.sendMessage("§7  ⚡ Finish current objective quickly!");
                player.sendMessage("§7  ⚡ Move to safe location if possible");
                player.sendMessage("§7  ⚡ Communicate handoff plans");
                player.sendMessage("§7  ⚡ Prepare next runner via voice chat");
                player.sendMessage("§7  ⚡ Use emergency pause if in danger");
            } else {
                player.sendMessage("§a§lGame Active - Cooperation Tips:");
                player.sendMessage("§7  ✓ Use Quick Actions for instant communication");
                player.sendMessage("§7  ✓ Monitor Live Analytics for performance");
                player.sendMessage("§7  ✓ Share locations and inventory status");
                player.sendMessage("§7  ✓ Mark waypoints for important locations");
                player.sendMessage("§7  ✓ Active runner should narrate actions");
                player.sendMessage("§7  ✓ Waiting runners should plan ahead");
            }
        }
        
        player.sendMessage("");
        player.sendMessage("§d§lAdvanced Features Available:");
        player.sendMessage("§7  ➤ Team Coordination Hub - Full communication suite");
        player.sendMessage("§7  ➤ Quick Actions - Lightning-fast team updates");
        player.sendMessage("§7  ➤ Live Analytics - Real-time performance monitoring");
        player.sendMessage("§7  ➤ Smart Help - Context-aware guidance (this!)");
        player.sendMessage("§7  ➤ Performance Alerts - Optimization recommendations");
        
        player.sendMessage("§6" + "=".repeat(50));
    }
    
    // Performance Optimization Tips
    private void showOptimizationTips(Player player) {
        player.sendMessage("§c========== §c§lPERFORMANCE OPTIMIZATION §c==========");
        player.sendMessage("§c§lServer Performance Issues Detected!");
        player.sendMessage("");
        
        try {
            double tps = Bukkit.getTPS()[0];
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory() / 1024 / 1024;
            long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
            int memoryPercent = (int) ((usedMemory * 100) / maxMemory);
            
            player.sendMessage("§e§lCurrent Status:");
            player.sendMessage("§7  Server TPS: §f" + String.format("%.2f", tps) + (tps < 15.0 ? " §c(LOW)" : " §a(OK)"));
            player.sendMessage("§7  Memory Usage: §f" + memoryPercent + "% " + (memoryPercent > 85 ? "§c(HIGH)" : "§a(OK)"));
            
            player.sendMessage("");
            player.sendMessage("§c§lImmediate Actions:");
            
            if (tps < 15.0) {
                player.sendMessage("§7  ⚠ §cReduce team size (remove 1-2 runners)");
                player.sendMessage("§7  ⚠ §cIncrease swap interval (60s+ recommended)");
                player.sendMessage("§7  ⚠ §cConsider pausing temporarily");
            }
            
            if (memoryPercent > 85) {
                player.sendMessage("§7  ⚠ §cReduce render distance (8-12 chunks)");
                player.sendMessage("§7  ⚠ §cClose unnecessary applications");
                player.sendMessage("§7  ⚠ §cRestart server if possible");
            }
            
        } catch (Exception e) {
            player.sendMessage("§7  Performance metrics unavailable");
        }
        
        player.sendMessage("");
        player.sendMessage("§e§lGeneral Optimization Tips:");
        player.sendMessage("§7  1. Keep team size between 2-4 players");
        player.sendMessage("§7  2. Use 45-60s swap intervals for stability");
        player.sendMessage("§7  3. Enable Safe Swaps to prevent lag spikes");
        player.sendMessage("§7  4. Monitor Live Analytics regularly");
        player.sendMessage("§7  5. Use SPECTATOR freeze mode (least resource intensive)");
        player.sendMessage("§7  6. Restart the game session if problems persist");
        
        player.sendMessage("");
        player.sendMessage("§b§lPro Tips:");
        player.sendMessage("§7  • Large teams (5+) work best on dedicated servers");
        player.sendMessage("§7  • Random timing adds excitement but uses more resources");
        player.sendMessage("§7  • Voice chat reduces need for in-game communication");
        player.sendMessage("§7  • Regular breaks help maintain server performance");
        
        player.sendMessage("§c" + "=".repeat(55));
    }
}
