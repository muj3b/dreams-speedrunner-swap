package com.example.speedrunnerswap.gui;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ControlGui {
    private final SpeedrunnerSwap plugin;

    public ControlGui(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        int rows = Math.max(1, plugin.getConfigManager().getGuiMainMenuRows());
        int size = rows * 9;
        String title = plugin.getConfigManager().getGuiMainMenuTitle();

        Inventory inv = com.example.speedrunnerswap.utils.GuiCompat.createInventory(new ControlGuiHolder(ControlGuiHolder.Type.MAIN), size, title);

        // Filler
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        com.example.speedrunnerswap.utils.GuiCompat.setDisplayName(fm, " ");
        filler.setItemMeta(fm);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);

        boolean running = plugin.getGameManager().isGameRunning();
        boolean paused = plugin.getGameManager().isGamePaused();

        // Back to Mode Selector
        inv.setItem(0, named(Material.ARROW, "§7§lBack", List.of("§7Return to mode selector")));

        // Start/Stop
        if (!running) {
            inv.setItem(10, named(Material.LIME_WOOL, "Start Game", List.of("Begin swapping every "+plugin.getConfigManager().getSwapInterval()+"s")));
        } else {
            inv.setItem(10, named(Material.RED_WOOL, "Stop Game", List.of("End current game")));
        }

        // Pause/Resume
        if (running && !paused) {
            inv.setItem(12, named(Material.YELLOW_WOOL, "Pause", List.of("Temporarily pause swapping")));
        } else if (running && paused) {
            inv.setItem(12, named(Material.ORANGE_WOOL, "Resume", List.of("Resume swapping")));
        } else {
            inv.setItem(12, named(Material.GRAY_WOOL, "Pause", List.of("Game not running")));
        }

        // Shuffle queue
        inv.setItem(14, named(Material.NETHER_STAR, "Shuffle Queue", List.of("Keep active runner, shuffle the rest")));

        // Set runners button only shown in non-Sapnap modes
        if (plugin.getCurrentMode() != SpeedrunnerSwap.SwapMode.SAPNAP) {
            inv.setItem(16, named(Material.BOOK, "Set Runners", List.of("Open the runner selector")));
        }

        // Randomize toggle
        boolean randomize = plugin.getConfigManager().isSwapRandomized();
        inv.setItem(22, named(Material.COMPARATOR, randomize ? "Randomize: ON" : "Randomize: OFF",
                List.of("Toggle randomized intervals")));

        // Runner timer visibility (cycle FULL/LAST 10s/HIDDEN)
        String runnerVis = plugin.getConfigManager().getRunnerTimerVisibility();
        String runnerLabel = switch (runnerVis.toLowerCase()) {
            case "always" -> "FULL";
            case "never" -> "HIDDEN";
            default -> "LAST 10s";
        };
        inv.setItem(20, named(
                Material.CLOCK,
                "Runner Timer: " + runnerLabel,
                List.of("Cycle active runner timer visibility",
                        "FULL / LAST 10s / HIDDEN")));

        // Waiting timer visibility (cycle FULL/LAST 10s/HIDDEN)
        String waitingVis = plugin.getConfigManager().getWaitingTimerVisibility();
        String waitingLabel = switch (waitingVis.toLowerCase()) {
            case "last_10" -> "LAST 10s";
            case "never" -> "HIDDEN";
            default -> "FULL";
        };
        inv.setItem(21, named(
                Material.CLOCK,
                "Waiting Timer: " + waitingLabel,
                List.of("Cycle waiting runner timer visibility",
                        "FULL / LAST 10s / HIDDEN")));

        // Interval display and adjusters
        int interval = plugin.getConfigManager().getSwapInterval();
        inv.setItem(23, named(Material.PAPER, "Interval: " + interval + "s", List.of("Base swap interval")));
        inv.setItem(18, named(Material.ARROW, "-5s", List.of("Decrease interval")));
        inv.setItem(26, named(Material.ARROW, "+5s", List.of("Increase interval")));

        // Freeze mode cycle
        String freeze = plugin.getConfigManager().getFreezeMode();
        inv.setItem(4, named(Material.ARMOR_STAND, "Inactive Runner State: " + freeze, List.of(
                "EFFECTS: Blind/Dark/Slow",
                "SPECTATOR: Spectator mode",
                "LIMBO: Teleport to limbo",
                "CAGE: Bedrock cage (one shared cage)")));

        // Safe Swap toggle
        boolean safeSwap = plugin.getConfigManager().isSafeSwapEnabled();
        inv.setItem(6, named(safeSwap ? Material.SLIME_BLOCK : Material.MAGMA_BLOCK,
                safeSwap ? "Safe Swap: ON" : "Safe Swap: OFF",
                List.of("Avoid lava/cactus/fire on teleports")));

        // Single Player Sleep toggle (enabled by default for Sapnap mode)
        boolean singlePlayerSleep = plugin.getConfigManager().isSinglePlayerSleepEnabled();
        inv.setItem(8, named(singlePlayerSleep ? Material.WHITE_BED : Material.RED_BED,
                singlePlayerSleep ? "Single Player Sleep: ON" : "Single Player Sleep: OFF",
                List.of("Allow only active runner to skip night", "Useful when other players are caged")));

        // Status
        inv.setItem(24, named(Material.PAPER, "Status", List.of("Show current status in chat")));

        player.openInventory(inv);
    }

    public void openRunnerSelector(Player player) {
        int rows = Math.max(2, plugin.getConfigManager().getGuiTeamSelectorRows());
        int size = rows * 9;
        String title = plugin.getConfigManager().getGuiTeamSelectorTitle();
        Inventory inv = com.example.speedrunnerswap.utils.GuiCompat.createInventory(new ControlGuiHolder(ControlGuiHolder.Type.RUNNER_SELECTOR), size, title);

        // Filler
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        com.example.speedrunnerswap.utils.GuiCompat.setDisplayName(fm, " ");
        filler.setItemMeta(fm);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);

        // Online players as selectable entries
        java.util.List<String> selected = plugin.getConfigManager().getRunnerNames();
        int idx = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            ItemStack icon = new ItemStack(Material.PLAYER_HEAD);
            try {
                org.bukkit.inventory.meta.SkullMeta sm = (org.bukkit.inventory.meta.SkullMeta) icon.getItemMeta();
                sm.setOwningPlayer(p);
                com.example.speedrunnerswap.utils.GuiCompat.setDisplayName(sm, p.getName());
                boolean isSel = selected.contains(p.getName());
                java.util.List<String> legacy = new java.util.ArrayList<>();
                legacy.add(isSel ? "Selected: Yes" : "Selected: No");
                com.example.speedrunnerswap.utils.GuiCompat.setLore(sm, legacy);
                icon.setItemMeta(sm);
            } catch (Throwable t) {
                ItemMeta im = icon.getItemMeta();
                com.example.speedrunnerswap.utils.GuiCompat.setDisplayName(im, p.getName());
                icon.setItemMeta(im);
            }
            if (idx < size - 9) {
                inv.setItem(idx, icon);
            }
            idx++;
        }

        // Back to main
        inv.setItem(size - 8, named(Material.ARROW, "§7§lBack", List.of("§7Return to control menu")));

        // Save / Cancel buttons
        inv.setItem(size - 6, named(Material.EMERALD_BLOCK, "Save", List.of("Apply selected runners")));
        inv.setItem(size - 4, named(Material.BARRIER, "Cancel", List.of("Discard changes")));

        player.openInventory(inv);
    }

    private ItemStack named(Material mat, String name, List<String> loreText) {
        ItemStack it = new ItemStack(mat);
        ItemMeta im = it.getItemMeta();
        com.example.speedrunnerswap.utils.GuiCompat.setDisplayName(im, name);
        if (loreText != null && !loreText.isEmpty()) {
            com.example.speedrunnerswap.utils.GuiCompat.setLore(im, loreText);
        }
        it.setItemMeta(im);
        return it;
    }
}
