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

    private boolean isMain(Inventory inv) {
        return inv != null && inv.getHolder() instanceof ControlGuiHolder holder && holder.getType() == ControlGuiHolder.Type.MAIN;
    }

    private boolean isRunnerSelector(Inventory inv) {
        return inv != null && inv.getHolder() instanceof ControlGuiHolder holder && holder.getType() == ControlGuiHolder.Type.RUNNER_SELECTOR;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory top = event.getView().getTopInventory();
        if (top == null) return;
        if (!isMain(top) && !isRunnerSelector(top)) return;
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (isMain(top)) {
            handleMainClick(player, clicked);
        } else if (isRunnerSelector(top)) {
            handleRunnerSelectorClick(player, clicked, event.getRawSlot(), top.getSize());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Inventory top = event.getView().getTopInventory();
        if (top == null) return;
        if (!isMain(top) && !isRunnerSelector(top)) return;
        // Cancel any drag that touches the top inventory
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < top.getSize()) {
                event.setCancelled(true);
                return;
            }
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


        if (type == Material.LIME_WOOL) {
            if (!running) {
                // Ensure mode is runner-only
                plugin.setCurrentMode(SpeedrunnerSwap.SwapMode.SAPNAP);
                // In Sapnap mode, always set all online players as runners
                List<String> names = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
                plugin.getConfigManager().setRunnerNames(names);
                plugin.getGameManager().setRunners(new ArrayList<>(Bukkit.getOnlinePlayers()));
                plugin.getGameManager().startGame();
            }
            new ControlGui(plugin).openMainMenu(player);
            return;
        }

        if (type == Material.RED_WOOL) {
            if (running) plugin.getGameManager().stopGame();
            new ControlGui(plugin).openMainMenu(player);
            return;
        }

        if (type == Material.YELLOW_WOOL) {
            if (running && !plugin.getGameManager().isGamePaused()) plugin.getGameManager().pauseGame();
            new ControlGui(plugin).openMainMenu(player);
            return;
        }

        if (type == Material.ORANGE_WOOL) {
            if (running && plugin.getGameManager().isGamePaused()) plugin.getGameManager().resumeGame();
            new ControlGui(plugin).openMainMenu(player);
            return;
        }

        if (type == Material.NETHER_STAR) {
            if (plugin.getGameManager().shuffleQueue()) {
                player.sendMessage("§aShuffled runner queue successfully.");
            } else {
                player.sendMessage("§cCannot shuffle queue - need at least 2 runners.");
            }
            new ControlGui(plugin).openMainMenu(player);
            return;
        }

        if (type == Material.BOOK) {
            // Initialize pending selection with current config
            Set<String> initial = new HashSet<>(plugin.getConfigManager().getRunnerNames());
            pendingRunnerSelections.put(player.getUniqueId(), initial);
            new ControlGui(plugin).openRunnerSelector(player);
            return;
        }

        if (type == Material.CLOCK) {
            // Distinguish which clock was clicked by its display name
            String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
            if (name.startsWith("Runner Timer:")) {
                String current = plugin.getConfigManager().getRunnerTimerVisibility();
                String next = switch (current.toLowerCase()) {
                    case "always" -> "last_10";
                    case "last_10" -> "never";
                    default -> "always";
                };
                plugin.getConfigManager().setRunnerTimerVisibility(next);
                player.sendMessage("§eRunner timer visibility: §a" + next);
                plugin.getGameManager().refreshActionBar();
                new ControlGui(plugin).openMainMenu(player);
                return;
            } else if (name.startsWith("Waiting Timer:")) {
                String current = plugin.getConfigManager().getWaitingTimerVisibility();
                String next = switch (current.toLowerCase()) {
                    case "always" -> "last_10";
                    case "last_10" -> "never";
                    default -> "always";
                };
                plugin.getConfigManager().setWaitingTimerVisibility(next);
                player.sendMessage("§eWaiting timer visibility: §a" + next);
                new ControlGui(plugin).openMainMenu(player);
                return;
            }
        }

        if (type == Material.ARMOR_STAND) {
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

        if (type == Material.ARROW) {
            String name = com.example.speedrunnerswap.utils.GuiCompat.getDisplayName(clicked.getItemMeta());
            
            // Handle interval adjustment arrows FIRST (higher priority)
            if (name != null) {
                if (name.equals("-5s")) {
                    int interval = Math.max(30, plugin.getConfigManager().getSwapInterval() - 5);
                    plugin.getConfigManager().setSwapInterval(interval);
                    player.sendMessage("§eInterval decreased to: §a" + interval + "s");
                    plugin.getGameManager().refreshSwapSchedule();
                    new ControlGui(plugin).openMainMenu(player);
                    return;
                } else if (name.equals("+5s")) {
                    int interval = Math.min(600, plugin.getConfigManager().getSwapInterval() + 5);
                    plugin.getConfigManager().setSwapInterval(interval);
                    player.sendMessage("§eInterval increased to: §a" + interval + "s");
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
            // Print status
            player.sendMessage("§6=== Runner-Only Status ===");
            player.sendMessage("§eGame Running: §f" + plugin.getGameManager().isGameRunning());
            player.sendMessage("§eGame Paused: §f" + plugin.getGameManager().isGamePaused());
            if (plugin.getGameManager().isGameRunning()) {
                Player active = plugin.getGameManager().getActiveRunner();
                player.sendMessage("§eActive Runner: §f" + (active != null ? active.getName() : "None"));
                player.sendMessage("§eTime Until Next Swap: §f" + plugin.getGameManager().getTimeUntilNextSwap() + "s");
                String runners = plugin.getGameManager().getRunners().stream().map(Player::getName).collect(Collectors.joining(", "));
                player.sendMessage("§eRunners: §f" + runners);
            }
            return;
        }
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
}
