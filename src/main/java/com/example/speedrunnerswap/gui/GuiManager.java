package com.example.speedrunnerswap.gui;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import com.example.speedrunnerswap.config.ConfigManager;
import com.example.speedrunnerswap.game.GameManager;
import com.example.speedrunnerswap.models.Team;
import com.example.speedrunnerswap.task.TaskDefinition;
import com.example.speedrunnerswap.task.TaskManagerMode;
import com.example.speedrunnerswap.task.TaskDifficulty;
import com.example.speedrunnerswap.utils.GuiCompat;
import com.example.speedrunnerswap.utils.Msg;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Rebuilt GUI system that exposes the entire plugin configuration and runtime controls.
 * Every screen is described using Menu definitions to keep navigation predictable.
 */
public final class GuiManager implements Listener {

    private static final ItemStack FILLER_PRIMARY;
    private static final ItemStack FILLER_ACCENT;
    private static final ItemStack FILLER_BORDER;
    private static final NamespacedKey BUTTON_KEY;
    private static final List<String> PARTICLE_TYPES = List.of("DUST", "END_ROD", "FLAME", "CRIT", "HEART", "CLOUD", "SMOKE");

    static {
        FILLER_PRIMARY = pane(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        FILLER_ACCENT = pane(Material.WHITE_STAINED_GLASS_PANE);
        FILLER_BORDER = pane(Material.BLUE_STAINED_GLASS_PANE);
        BUTTON_KEY = new NamespacedKey(SpeedrunnerSwap.getInstance(), "menu_button");
    }

    private static ItemStack pane(Material material) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        GuiCompat.setDisplayName(meta, " ");
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.values());
        pane.setItemMeta(meta);
        return pane;
    }

    private final SpeedrunnerSwap plugin;
    private final Map<MenuKey, MenuBuilder> builders = new EnumMap<>(MenuKey.class);
    private final Map<UUID, MenuSession> sessions = new HashMap<>();
    private final Map<UUID, Deque<MenuRequest>> history = new HashMap<>();
    private final Map<UUID, Team> teamFocus = new HashMap<>();
    private final Map<UUID, StatsParent> statsParents = new HashMap<>();

    public GuiManager(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
        registerBuilders();
    }

    // -----------------------------------------------------------------
    // Public entry points

    public void openMainMenu(Player player) {
        open(player, MenuKey.MAIN, null, false);
    }

    public void openDirectGamemodeSelector(Player player) {
        if (player == null) return;
        resetNavigation(player);
        open(player, MenuKey.MODE_SELECT_DIRECT, null, false);
    }

    public void openModeSelector(Player player) {
        open(player, MenuKey.MODE_SELECT, null, false);
    }

    public void openTeamSelector(Player player) {
        open(player, MenuKey.TEAM_MANAGEMENT, null, false);
    }

    public void openSettingsMenu(Player player) {
        open(player, MenuKey.SETTINGS_HOME, null, false);
    }

    public void openPowerUpsMenu(Player player) {
        open(player, MenuKey.POWERUPS_ROOT, null, false);
    }

    public void openDangerousBlocksMenu(Player player) {
        open(player, MenuKey.DANGEROUS_BLOCKS, null, false);
    }

    public void openTaskManagerMenu(Player player) {
        open(player, MenuKey.TASK_HOME, null, false);
    }

    public void openStatisticsMenu(Player player, StatsParent parent) {
        statsParents.put(player.getUniqueId(), parent);
        open(player, MenuKey.STATS_ROOT, null, false);
    }

    public void openStatisticsMenu(Player player) {
        openStatisticsMenu(player, StatsParent.SETTINGS);
    }

    // -----------------------------------------------------------------
    // Menu engine

    private void open(Player player, MenuKey key, Object data, boolean replaceHistory) {
        if (player == null) return;
        MenuBuilder builder = builders.get(key);
        if (builder == null) {
            player.closeInventory();
            player.sendMessage("§cMenu not implemented: " + key.name());
            return;
        }

        MenuRequest request = new MenuRequest(key, data);
        MenuContext context = new MenuContext(this, player, request);
        MenuScreen screen = builder.build(context);

        Inventory inventory = GuiCompat.createInventory(null, screen.size(), screen.title());
        fill(inventory, screen, context);

        sessions.put(player.getUniqueId(), new MenuSession(request, screen, inventory));

        Deque<MenuRequest> stack = history.computeIfAbsent(player.getUniqueId(), id -> new ArrayDeque<>());
        if (replaceHistory && !stack.isEmpty()) {
            stack.pop();
        }
        if (stack.isEmpty() || !stack.peek().equals(request)) {
            stack.push(request);
        }

        player.openInventory(inventory);
    }

    void reopen(Player player) {
        MenuSession session = sessions.get(player.getUniqueId());
        if (session == null) return;
        open(player, session.request.key(), session.request.data(), true);
    }

    void open(Player player, MenuKey key) {
        open(player, key, null, false);
    }

    void open(Player player, MenuKey key, Object data) {
        open(player, key, data, false);
    }

    void openPrevious(Player player) {
        Deque<MenuRequest> stack = history.get(player.getUniqueId());
        if (stack == null || stack.isEmpty()) {
            player.closeInventory();
            return;
        }
        stack.pop(); // current
        if (stack.isEmpty()) {
            player.closeInventory();
            return;
        }
        MenuRequest previous = stack.peek();
        open(player, previous.key(), previous.data(), true);
    }

    private void fill(Inventory inventory, MenuScreen screen, MenuContext context) {
        int rows = inventory.getSize() / 9;
        for (int i = 0; i < inventory.getSize(); i++) {
            int row = i / 9;
            int col = i % 9;
            ItemStack filler;
            if (row == 0 || row == rows - 1 || col == 0 || col == 8) {
                filler = FILLER_BORDER.clone();
            } else {
                boolean usePrimary = ((row + col) % 2) == 0;
                filler = (usePrimary ? FILLER_PRIMARY : FILLER_ACCENT).clone();
            }
            inventory.setItem(i, filler);
        }
        for (MenuItem item : screen.items()) {
            if (item.slot() < 0 || item.slot() >= inventory.getSize()) continue;
            ItemStack icon = item.icon().apply(context);
            ItemMeta meta = icon.getItemMeta();
            meta.getPersistentDataContainer().set(BUTTON_KEY, PersistentDataType.STRING, item.id());
            icon.setItemMeta(meta);
            inventory.setItem(item.slot(), icon);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        MenuSession session = sessions.get(player.getUniqueId());
        if (session == null) return;
        if (!Objects.equals(event.getView().getTopInventory(), session.inventory())) return;

        event.setCancelled(true);
        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType() == Material.AIR) return;
        ItemMeta meta = current.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String id = container.get(BUTTON_KEY, PersistentDataType.STRING);
        if (id == null) return;

        MenuItem item = session.screen.button(id);
        if (item == null || item.action() == null) return;

        MenuClickContext ctx = new MenuClickContext(this, player, session.request, event.isShiftClick(), event.getClick());
        item.action().accept(ctx);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        MenuSession session = sessions.get(player.getUniqueId());
        if (session == null) return;
        if (Objects.equals(event.getView().getTopInventory(), session.inventory())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        MenuSession session = sessions.get(player.getUniqueId());
        if (session != null && Objects.equals(session.inventory(), event.getInventory())) {
            sessions.remove(player.getUniqueId());
        }
    }

    // -----------------------------------------------------------------
    // Menu registrations

    private void registerBuilders() {
        builders.put(MenuKey.MAIN, ctx -> buildMainMenu(ctx));
        builders.put(MenuKey.MODE_SELECT, ctx -> buildModeSelect(ctx, false));
        builders.put(MenuKey.MODE_SELECT_DIRECT, ctx -> buildModeSelect(ctx, true));
        builders.put(MenuKey.TEAM_MANAGEMENT, this::buildTeamMenu);
        builders.put(MenuKey.SETTINGS_HOME, this::buildSettingsHome);
        builders.put(MenuKey.SETTINGS_SWAP, this::buildSwapSettings);
        builders.put(MenuKey.SETTINGS_SAFETY, this::buildSafetySettings);
        builders.put(MenuKey.SETTINGS_HUNTER, this::buildHunterSettings);
        builders.put(MenuKey.POWERUPS_ROOT, this::buildPowerUpsRoot);
        builders.put(MenuKey.POWERUPS_EFFECTS, this::buildPowerUpEffects);
        builders.put(MenuKey.POWERUPS_DURATION, this::buildPowerUpDurations);
        builders.put(MenuKey.DANGEROUS_BLOCKS, this::buildDangerousBlocks);
        builders.put(MenuKey.SETTINGS_WORLD_BORDER, this::buildWorldBorder);
        builders.put(MenuKey.SETTINGS_BOUNTY, this::buildBounty);
        builders.put(MenuKey.SETTINGS_LAST_STAND, this::buildLastStand);
        builders.put(MenuKey.SETTINGS_SUDDEN_DEATH, this::buildSuddenDeath);
        builders.put(MenuKey.STATS_ROOT, this::buildStatsRoot);
        builders.put(MenuKey.STATS_ADVANCED, this::buildStatsAdvanced);
        builders.put(MenuKey.SETTINGS_TASK, this::buildTaskSettings);
        builders.put(MenuKey.TASK_HOME, this::buildTaskHome);
        builders.put(MenuKey.TASK_CUSTOM, this::buildTaskCustom);
        builders.put(MenuKey.TASK_POOL, this::buildTaskPool);
        builders.put(MenuKey.TASK_ASSIGNMENTS, this::buildTaskAssignments);
        builders.put(MenuKey.SETTINGS_VOICE_CHAT, this::buildVoiceChat);
        builders.put(MenuKey.SETTINGS_BROADCAST, this::buildBroadcast);
        builders.put(MenuKey.SETTINGS_UI, this::buildUiSettings);
        builders.put(MenuKey.KIT_MANAGER, this::buildKitManager);
    }

    // -----------------------------------------------------------------
    // Menu builders

    private void resetNavigation(Player player) {
        UUID id = player.getUniqueId();
        sessions.remove(id);
        history.remove(id);
    }

    private MenuScreen buildMainMenu(MenuContext ctx) {
        List<MenuItem> items = new ArrayList<>();
        GameManager gm = plugin.getGameManager();
        ConfigManager cfg = plugin.getConfigManager();
        boolean running = gm.isGameRunning();
        boolean paused = gm.isGamePaused();
        SpeedrunnerSwap.SwapMode mode = plugin.getCurrentMode();

        items.add(backButton(0, "§7§lBack", null, null, null));

        List<String> statusLore = new ArrayList<>();
        statusLore.add("§7Mode: §f" + mode.name());
        statusLore.add("§7Running: " + (running ? "§aYes" : "§cNo"));
        statusLore.add("§7Paused: " + (paused ? "§eYes" : "§cNo"));
        statusLore.add("§7Speed Owners: §b" + gm.getRunners().size());
        if (mode == SpeedrunnerSwap.SwapMode.DREAM) {
            statusLore.add("§7Hunters: §c" + gm.getHunters().size());
        } else {
            statusLore.add("§7Hunters: §8Not used in this mode");
        }
        items.add(simpleItem(4, () -> icon(Material.CLOCK, "§6§lGame Status", statusLore)));

        if (!running) {
            items.add(clickItem(10, () -> icon(Material.LIME_CONCRETE, "§a§lStart Game",
                    List.of("§7Swap interval: §f" + cfg.getSwapInterval() + "s")), ctxClick -> {
                if (gm.startGame()) {
                    Msg.send(ctxClick.player(), "§aGame started!");
                } else {
                    Msg.send(ctxClick.player(), "§cCannot start. Check team assignments.");
                }
                ctxClick.reopen();
            }));
        } else {
            items.add(clickItem(10, () -> icon(Material.RED_CONCRETE, "§c§lStop Game", List.of("§7End the current game")), ctxClick -> {
                gm.stopGame();
                Msg.send(ctxClick.player(), "§cGame stopped.");
                ctxClick.reopen();
            }));
            if (paused) {
                items.add(clickItem(12, () -> icon(Material.ORANGE_CONCRETE, "§a§lResume", List.of("§7Resume swaps")), ctxClick -> {
                    gm.resumeGame();
                    ctxClick.reopen();
                }));
            } else {
                items.add(clickItem(12, () -> icon(Material.YELLOW_CONCRETE, "§e§lPause", List.of("§7Pause swaps")), ctxClick -> {
                    gm.pauseGame();
                    ctxClick.reopen();
                }));
            }
            items.add(clickItem(14, () -> icon(Material.NETHER_STAR, "§e§lForce Swap", List.of("§7Trigger immediate swap")), ctxClick -> {
                gm.triggerImmediateSwap();
                Msg.send(ctxClick.player(), "§eSwap triggered.");
            }));
        }

        items.add(clickItem(18, () -> icon(Material.NETHER_STAR, "§d§lAbout muj3b",
                List.of(
                        "§7Click to show support info",
                        "§7and share the donation link.")), ctxClick -> {
            plugin.getGameManager().sendDonationMessage(ctxClick.player());
            Msg.send(ctxClick.player(), "§dThanks for supporting muj3b!");
        }));

        items.add(clickItem(20, () -> icon(Material.PLAYER_HEAD, "§b§lTeam Management", List.of("§7Assign runners & hunters")),
                ctxClick -> open(ctxClick.player(), MenuKey.TEAM_MANAGEMENT, null, false)));
        items.add(clickItem(21, () -> icon(Material.ENDER_EYE, "§d§lChoose Mode", List.of("§7Switch Dream/Sapnap/Task")),
                ctxClick -> open(ctxClick.player(), MenuKey.MODE_SELECT, null, false)));
        items.add(clickItem(22, () -> icon(Material.COMPARATOR, "§6§lSettings", List.of("§7Configure every mechanic")),
                ctxClick -> open(ctxClick.player(), MenuKey.SETTINGS_HOME, null, false)));
        items.add(clickItem(23, () -> icon(Material.BOOK, "§b§lStatistics", List.of("§7Adjust stat tracking")),
                ctxClick -> openStatisticsMenu(ctxClick.player(), StatsParent.MAIN)));
        items.add(clickItem(24, () -> icon(Material.TARGET, "§6§lTask Manager", List.of("§7Manage secret tasks")),
                ctxClick -> open(ctxClick.player(), MenuKey.TASK_HOME, null, false)));

        items.add(clickItem(30, () -> icon(Material.POTION, "§d§lPower-ups", List.of("§7Configure swap effects")),
                ctxClick -> open(ctxClick.player(), MenuKey.POWERUPS_ROOT, null, false)));
        items.add(clickItem(31, () -> icon(Material.BARRIER, "§c§lDangerous Blocks", List.of("§7Safe swap blacklist")),
                ctxClick -> open(ctxClick.player(), MenuKey.DANGEROUS_BLOCKS, null, false)));
        items.add(clickItem(32, () -> icon(Material.GOLD_INGOT, "§6§lBounty System", List.of("§7Hunter rewards")),
                ctxClick -> open(ctxClick.player(), MenuKey.SETTINGS_BOUNTY, null, false)));
        items.add(clickItem(33, () -> icon(Material.DRAGON_HEAD, "§4§lSudden Death", List.of("§7Endgame showdown")),
                ctxClick -> open(ctxClick.player(), MenuKey.SETTINGS_SUDDEN_DEATH, null, false)));

        if (mode == SpeedrunnerSwap.SwapMode.SAPNAP) {
            items.add(clickItem(40, () -> icon(Material.CLOCK, "§b§lQueue Shuffle", List.of("§7Randomize runner order")),
                    ctxClick -> {
                        if (plugin.getGameManager().shuffleQueue()) {
                            Msg.send(ctxClick.player(), "§aRunner queue shuffled.");
                        } else {
                            Msg.send(ctxClick.player(), "§cNot enough runners to shuffle.");
                        }
                    }));
        }

        return new MenuScreen(plugin.getConfigManager().getGuiMainMenuTitle(), 54, items);
    }

    private MenuScreen buildModeSelect(MenuContext ctx, boolean direct) {
        List<MenuItem> items = new ArrayList<>();
        SpeedrunnerSwap.SwapMode current = plugin.getCurrentMode();

        if (direct) {
            items.add(simpleItem(4, () -> icon(Material.NETHER_STAR, "§e§lWelcome to Speedrunner Swap",
                    List.of("§7Pick the challenge you want to run","§7and jump straight into setup."))));
            items.add(modeItem(10, SpeedrunnerSwap.SwapMode.DREAM, true, current));
            items.add(modeItem(13, SpeedrunnerSwap.SwapMode.SAPNAP, true, current));
            items.add(modeItem(16, SpeedrunnerSwap.SwapMode.TASK, true, current));
            items.add(simpleItem(22, () -> icon(Material.MAP, "§b§lCurrent Mode",
                    List.of("§7Active: §f" + current.name(), "", "§7Select another icon to switch."))));
            items.add(clickItem(29, () -> icon(Material.PLAYER_HEAD, "§a§lTeam Manager",
                    List.of("§7Assign runners & hunters")),
                    context -> open(context.player(), MenuKey.TEAM_MANAGEMENT, null, false)));
            items.add(clickItem(31, () -> icon(Material.EMERALD, "§a§lOpen Control Hub",
                    List.of("§7Go straight to the main dashboard")),
                    context -> open(context.player(), MenuKey.MAIN, null, false)));
            items.add(clickItem(33, () -> icon(Material.COMPARATOR, "§6§lQuick Settings",
                    List.of("§7Tweak core mechanics instantly")),
                    context -> open(context.player(), MenuKey.SETTINGS_HOME, null, false)));
            items.add(backButton(35, "§7§lBack", null, null, null));
        } else {
            items.add(modeItem(11, SpeedrunnerSwap.SwapMode.DREAM, false, current));
            items.add(modeItem(13, SpeedrunnerSwap.SwapMode.SAPNAP, false, current));
            items.add(modeItem(15, SpeedrunnerSwap.SwapMode.TASK, false, current));
            items.add(backButton(22, "§7§lBack", MenuKey.MAIN, null,
                    player -> openPrevious(player)));
        }

        int size = direct ? 36 : 27;
        String title = direct ? "§9§lSpeedrunner Swap Hub" : "§6§lMode Selector";
        return new MenuScreen(title, size, items);
    }

    private MenuItem modeItem(int slot, SpeedrunnerSwap.SwapMode mode, boolean direct, SpeedrunnerSwap.SwapMode current) {
        boolean selected = mode == current;
        Material mat = switch (mode) {
            case DREAM -> Material.DIAMOND_SWORD;
            case SAPNAP -> Material.DIAMOND_BOOTS;
            case TASK -> Material.TARGET;
        };
        List<String> lore = new ArrayList<>();
        lore.add("§8────────────────");
        switch (mode) {
            case DREAM -> lore.addAll(List.of("§e§lSpeedrunners vs Hunters", "§7Classic chase experience"));
            case SAPNAP -> lore.addAll(List.of("§b§lMulti-runner Control", "§7Share one body cooperatively"));
            case TASK -> lore.addAll(List.of("§6§lTask Master", "§7Secret objectives and deception"));
        }
        if (direct) {
            lore.add("");
            switch (mode) {
                case DREAM -> lore.addAll(List.of("§7Recommended: §f3+ players", "§7(1 runner, 2+ hunters)"));
                case SAPNAP -> lore.addAll(List.of("§7Recommended: §f2-4 players", "§7Perfect for co-op runs"));
                case TASK -> lore.addAll(List.of("§7Recommended: §f3+ players", "§7For strategic chaos"));
            }
        }
        lore.add("");
        lore.add(selected ? "§aCurrently active" : "§eClick to switch");

        return clickItem(slot, () -> {
            ItemStack icon = icon(mat, (selected ? "§a§l" : "§e§l") + switch (mode) {
                case DREAM -> "Dream";
                case SAPNAP -> "Sapnap";
                case TASK -> "Task Master";
            }, lore);
            if (selected) {
                ItemMeta meta = icon.getItemMeta();
                meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                icon.setItemMeta(meta);
            }
            return icon;
        }, ctx -> {
            if (selected) {
                Msg.send(ctx.player(), "§eAlready using that mode.");
                return;
            }
            if (plugin.getGameManager().isGameRunning()) {
                Msg.send(ctx.player(), "§cStop the current game before switching modes.");
                return;
            }
            plugin.setCurrentMode(mode);
            Msg.send(ctx.player(), "§aSwitched to §f" + mode.name() + "§a mode.");
            if (direct) {
                open(ctx.player(), MenuKey.MAIN, null, false);
            } else {
                ctx.reopen();
            }
        });
    }

    private MenuScreen buildTeamMenu(MenuContext ctx) {
        Player viewer = ctx.player();
        GameManager gm = plugin.getGameManager();
        Team initialFocus = teamFocus.computeIfAbsent(viewer.getUniqueId(), uuid -> Team.RUNNER);

        boolean huntersAvailable = plugin.getCurrentMode() == SpeedrunnerSwap.SwapMode.DREAM;
        if (!huntersAvailable && initialFocus == Team.HUNTER) {
            initialFocus = Team.RUNNER;
            teamFocus.put(viewer.getUniqueId(), Team.RUNNER);
        }
        final Team focus = initialFocus;

        List<MenuItem> items = new ArrayList<>();

        items.add(backButton(0, "§7§lBack", MenuKey.MAIN, null, this::openMainMenu));

        items.add(clickItem(2, () -> icon(Material.DIAMOND_BOOTS,
                focus == Team.RUNNER ? "§a§lAssigning Runners" : "§b§lAssign Runners",
                List.of("§7Click to set focus")), ctxClick -> {
            teamFocus.put(ctxClick.player().getUniqueId(), Team.RUNNER);
            ctxClick.reopen();
        }));

        List<String> instructionLore = new ArrayList<>();
        instructionLore.add("§71. Select runner/hunter focus");
        instructionLore.add("§72. Click player head to assign");
        instructionLore.add("§73. Shift-click to remove");
        if (plugin.getCurrentMode() != SpeedrunnerSwap.SwapMode.DREAM) {
            instructionLore.add("§cThis mode uses speed owners only.");
            instructionLore.add("§7Assigning hunters is disabled.");
        }
        items.add(simpleItem(4, () -> icon(Material.BOOK, "§e§lInstructions", instructionLore)));

        items.add(clickItem(6, () -> icon(Material.IRON_SWORD,
                huntersAvailable
                        ? (focus == Team.HUNTER ? "§a§lAssigning Hunters" : "§c§lAssign Hunters")
                        : "§7§lHunters Disabled",
                huntersAvailable
                        ? List.of("§7Click to set focus")
                        : List.of("§7Dream mode only")), ctxClick -> {
            if (!huntersAvailable) {
                Msg.send(ctxClick.player(), "§eHunters are only available in Dream mode.");
                return;
            }
            teamFocus.put(ctxClick.player().getUniqueId(), Team.HUNTER);
            ctxClick.reopen();
        }));

        items.add(clickItem(8, () -> icon(Material.BARRIER, "§c§lClear All", List.of("§7Remove all assignments")), ctxClick -> {
            Set<Player> affected = new HashSet<>();
            affected.addAll(gm.getRunners());
            affected.addAll(gm.getHunters());
            gm.clearAllTeams();
            Msg.send(ctxClick.player(), "§cCleared all teams.");
            for (Player p : affected) {
                if (p != null && p.isOnline() && p != ctxClick.player()) {
                    Msg.send(p, "§eYour team assignment was cleared by §f" + ctxClick.player().getName());
                }
            }
            ctxClick.reopen();
        }));

        int slot = 9;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (slot >= 54) break;
            Team assigned = gm.isRunner(online) ? Team.RUNNER : gm.isHunter(online) ? Team.HUNTER : Team.NONE;
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(online);
            String prefix = switch (assigned) {
                case RUNNER -> "§b";
                case HUNTER -> "§c";
                case NONE -> "§7";
            };
            GuiCompat.setDisplayName(meta, prefix + online.getName());
            Team currentFocus = huntersAvailable ? focus : Team.RUNNER;
            List<String> lore = new ArrayList<>();
            lore.add("§7Team: " + switch (assigned) {
                case RUNNER -> "§bRunner";
                case HUNTER -> "§cHunter";
                case NONE -> "§7Unassigned";
            });
            lore.add("§7Focus: " + (currentFocus == Team.RUNNER ? "§bSpeed Owners" : "§cHunters"));
            lore.add("§7Click to assign, shift-click to clear");
            GuiCompat.setLore(meta, lore);
            head.setItemMeta(meta);

            items.add(clickItem(slot, () -> head, ctxClick -> {
                Team targetTeam = ctxClick.shift() ? Team.NONE : teamFocus.getOrDefault(ctxClick.player().getUniqueId(), Team.RUNNER);
                if (!huntersAvailable && targetTeam == Team.HUNTER) {
                    teamFocus.put(ctxClick.player().getUniqueId(), Team.RUNNER);
                    Msg.send(ctxClick.player(), "§eAssign hunters only when Dream mode is active.");
                    ctxClick.reopen();
                    return;
                }
                if (targetTeam == Team.HUNTER && !huntersAvailable) {
                    Msg.send(ctxClick.player(), "§eAssign hunters only when Dream mode is active.");
                    return;
                }
                boolean changed = gm.assignPlayerToTeam(online, targetTeam);
                if (!changed) {
                    Msg.send(ctxClick.player(), "§eNo change for §f" + online.getName());
                } else if (targetTeam == Team.NONE) {
                    Msg.send(ctxClick.player(), "§eRemoved §f" + online.getName() + "§e from teams.");
                    if (online != ctxClick.player()) Msg.send(online, "§eYou were removed from all teams by §f" + ctxClick.player().getName());
                } else {
                    String label = targetTeam == Team.RUNNER ? "§bSpeed Owners" : "§cHunters";
                    Msg.send(ctxClick.player(), "§aAdded §f" + online.getName() + "§a to " + label + "§a.");
                    if (online != ctxClick.player()) Msg.send(online, "§eYou were assigned to " + label + " §eby §f" + ctxClick.player().getName());
                }
                ctxClick.reopen();
            }));

            slot++;
            if ((slot + 1) % 9 == 0) slot += 2;
        }

        return new MenuScreen(plugin.getConfigManager().getGuiTeamSelectorTitle(), 54, items);
    }

    private MenuScreen buildSettingsHome(MenuContext ctx) {
        List<MenuItem> items = new ArrayList<>();
        items.add(backButton(0, "§7§lBack", MenuKey.MAIN, null, this::openMainMenu));

        items.add(navigateItem(10, Material.CLOCK, "§e§lSwap & Timing", MenuKey.SETTINGS_SWAP,
                "§7Intervals, randomness, hunter swap"));
        items.add(navigateItem(11, Material.SHIELD, "§c§lSafety & Freeze", MenuKey.SETTINGS_SAFETY,
                "§7Safe swap, freeze modes, sleep"));
        items.add(navigateItem(12, Material.COMPASS, "§c§lHunter Tools", MenuKey.SETTINGS_HUNTER,
                "§7Tracker, compass jamming"));
        items.add(navigateItem(13, Material.POTION, "§d§lPower-ups", MenuKey.POWERUPS_ROOT,
                "§7Manage effects"));
        items.add(navigateItem(14, Material.BARRIER, "§4§lWorld Border", MenuKey.SETTINGS_WORLD_BORDER,
                "§7Shrink timing"));
        items.add(navigateItem(15, Material.GOLD_INGOT, "§6§lBounty", MenuKey.SETTINGS_BOUNTY,
                "§7Hunter reward system"));
        items.add(navigateItem(16, Material.TOTEM_OF_UNDYING, "§6§lLast Stand", MenuKey.SETTINGS_LAST_STAND,
                "§7Final runner boost"));

        items.add(navigateItem(19, Material.DRAGON_HEAD, "§4§lSudden Death", MenuKey.SETTINGS_SUDDEN_DEATH,
                "§7Endgame showdown"));
        items.add(navigateItem(20, Material.BOOK, "§b§lStatistics", MenuKey.STATS_ROOT,
                "§7Tracking toggles"));
        items.add(navigateItem(21, Material.TARGET, "§6§lTask Master", MenuKey.SETTINGS_TASK,
                "§7Competition rules"));
        items.add(navigateItem(22, Material.NOTE_BLOCK, "§d§lVoice Chat", MenuKey.SETTINGS_VOICE_CHAT,
                "§7Simple Voice Chat integration"));
        items.add(navigateItem(23, Material.BELL, "§e§lBroadcasts", MenuKey.SETTINGS_BROADCAST,
                "§7Announcement settings"));
        items.add(navigateItem(24, Material.COMPARATOR, "§b§lUI & Timers", MenuKey.SETTINGS_UI,
                "§7Actionbars, titles, visibility"));

        items.add(navigateItem(28, Material.CHEST, "§a§lKits", MenuKey.KIT_MANAGER,
                "§7Toggle kits and quick actions"));
        items.add(navigateItem(29, Material.MAGMA_BLOCK, "§c§lDangerous Blocks", MenuKey.DANGEROUS_BLOCKS,
                "§7Edit safe-swap blacklist"));

        return new MenuScreen(plugin.getConfigManager().getGuiSettingsTitle(), 54, items);
    }

    private MenuScreen buildSwapSettings(MenuContext ctx) {
        ConfigManager cfg = plugin.getConfigManager();
        List<MenuItem> items = new ArrayList<>();
        items.add(backButton(0, "§7§lBack", MenuKey.SETTINGS_HOME, null, this::openSettingsMenu));

        items.add(toggleItem(10, Material.REPEATER, "§e§lRandomized Swaps",
                cfg::isSwapRandomized,
                value -> cfg.setSwapRandomized(value),
                "§7Gaussian distribution around interval"));

        items.add(adjustItem(11, Material.CLOCK, "§e§lBase Interval",
                cfg::getSwapInterval,
                value -> cfg.setSwapInterval(value),
                5, 15, 5, 600,
                "§7Swap every X seconds"));

        items.add(toggleItem(12, Material.REDSTONE_TORCH, "§e§lExperimental Intervals",
                cfg::isBetaIntervalEnabled,
                value -> cfg.setBetaIntervalEnabled(value),
                "§7Allow intervals <30s or above max"));

        items.add(adjustItem(13, Material.COMPASS, "§6§lMin Interval",
                cfg::getMinSwapInterval,
                value -> plugin.getConfig().set("swap.min_interval", value),
                5, 15, 5, 600,
                "§7Minimum random interval"));

        items.add(adjustItem(14, Material.COMPASS, "§6§lMax Interval",
                cfg::getSwapIntervalMax,
                value -> plugin.getConfig().set("swap.max_interval", value),
                5, 15, 5, 1800,
                "§7Maximum random interval"));

        items.add(adjustItem(15, Material.SPYGLASS, "§6§lJitter Std Dev",
                () -> (int) Math.round(cfg.getJitterStdDev()),
                value -> plugin.getConfig().set("swap.jitter.stddev", (double) value),
                1, 5, 0, 600,
                "§7Standard deviation for random interval"));

        items.add(toggleItem(16, Material.LEVER, "§e§lClamp Jitter",
                cfg::isClampJitter,
                value -> plugin.getConfig().set("swap.jitter.clamp", value),
                "§7Restrict random interval within min/max"));

        items.add(toggleItem(19, Material.PISTON, "§e§lHunter Swap",
                cfg::isHunterSwapEnabled,
                value -> plugin.getConfig().set("swap.hunter_swap.enabled", value),
                "§7Rotate which hunter chases"));

        items.add(adjustItem(20, Material.ARROW, "§6§lHunter Swap Interval",
                cfg::getHunterSwapInterval,
                value -> plugin.getConfig().set("swap.hunter_swap.interval", value),
                10, 30, 10, 600,
                "§7Seconds between hunter rotations"));

        items.add(toggleItem(21, Material.BLAZE_POWDER, "§e§lHot Potato Mode",
                cfg::isHotPotatoModeEnabled,
                value -> plugin.getConfig().set("swap.hot_potato_mode.enabled", value),
                "§7Swap to damaged runner immediately"));

        items.add(toggleItem(22, Material.REDSTONE_TORCH, "§e§lPause on Disconnect",
                cfg::isPauseOnDisconnect,
                value -> plugin.getConfig().set("swap.pause_on_disconnect", value),
                "§7Auto-pause when active runner leaves"));

        items.add(toggleItem(23, Material.BOOK, "§e§lApply Mode Defaults",
                cfg::getApplyDefaultOnModeSwitch,
                value -> plugin.getConfig().set("swap.apply_default_on_mode_switch", value),
                "§7Reset interval when switching mode"));

        items.add(adjustItem(24, Material.DIAMOND_SWORD, "§6§lDream Default",
                () -> cfg.getModeDefaultInterval(SpeedrunnerSwap.SwapMode.DREAM),
                value -> cfg.setModeDefaultInterval(SpeedrunnerSwap.SwapMode.DREAM, value),
                5, 15, 5, 600,
                "§7Default interval for Dream mode"));
        items.add(adjustItem(25, Material.DIAMOND_BOOTS, "§6§lSapnap Default",
                () -> cfg.getModeDefaultInterval(SpeedrunnerSwap.SwapMode.SAPNAP),
                value -> cfg.setModeDefaultInterval(SpeedrunnerSwap.SwapMode.SAPNAP, value),
                5, 15, 5, 600,
                "§7Default interval for Sapnap mode"));
        items.add(adjustItem(26, Material.TARGET, "§6§lTask Default",
                () -> cfg.getModeDefaultInterval(SpeedrunnerSwap.SwapMode.TASK),
                value -> cfg.setModeDefaultInterval(SpeedrunnerSwap.SwapMode.TASK, value),
                5, 15, 5, 600,
                "§7Default interval for Task mode"));

        items.add(adjustItem(28, Material.SHIELD, "§6§lGrace Period (s)",
                () -> (int) Math.round(plugin.getConfig().getInt("swap.grace_period_ticks", 40) / 20.0),
                value -> plugin.getConfig().set("swap.grace_period_ticks", Math.max(0, value) * 20),
                1, 5, 0, 600,
                "§7Seconds of invulnerability after swapping"));

        items.add(toggleConfigItem(30, Material.PLAYER_HEAD, "§e§lPreserve Runner Progress",
                "swap.preserve_runner_progress_on_end", false,
                "§7Copy final runner inventory to everyone"));

        items.add(backButton(44, "§7§lBack", MenuKey.SETTINGS_HOME, null, this::openSettingsMenu));

        return new MenuScreen("§e§lSwap & Timing", 54, items);
    }

    private MenuScreen buildSafetySettings(MenuContext ctx) {
        ConfigManager cfg = plugin.getConfigManager();
        List<MenuItem> items = new ArrayList<>();
        items.add(backButton(0, "§7§lBack", MenuKey.SETTINGS_HOME, null, this::openSettingsMenu));

        items.add(toggleItem(10, Material.SLIME_BLOCK, "§e§lSafe Swaps",
                cfg::isSafeSwapEnabled,
                value -> cfg.setSafeSwapEnabled(value),
                "§7Scan area before swapping"));

        items.add(adjustItem(11, Material.MAP, "§6§lHorizontal Radius",
                cfg::getSafeSwapHorizontalRadius,
                value -> plugin.getConfig().set("safe_swap.horizontal_radius", value),
                1, 5, 1, 32,
                "§7Scan radius in blocks"));

        items.add(adjustItem(12, Material.LADDER, "§6§lVertical Distance",
                cfg::getSafeSwapVerticalDistance,
                value -> plugin.getConfig().set("safe_swap.vertical_distance", value),
                1, 5, 1, 32,
                "§7Vertical search range"));

        items.add(clickItem(13, () -> icon(Material.MAGMA_BLOCK, "§c§lDangerous Blocks", List.of("§7Edit blacklist")),
                ctxClick -> open(ctxClick.player(), MenuKey.DANGEROUS_BLOCKS, null, false)));

        items.add(cycleItem(19, Material.ICE, "§6§lFreeze Mode", cfg::getFreezeMode,
                value -> {
                    String next = switch (value.toUpperCase(Locale.ROOT)) {
                        case "EFFECTS" -> "SPECTATOR";
                        case "SPECTATOR" -> "LIMBO";
                        case "LIMBO" -> "CAGE";
                        default -> "EFFECTS";
                    };
                    plugin.getConfigManager().setFreezeMode(next);
                    plugin.getGameManager().refreshFreezeMechanic();
                    return next;
                },
                List.of("§7How inactive runners are handled",
                        "§bEFFECTS §7- heavy slowness",
                        "§bSPECTATOR §7- spectator mode",
                        "§bLIMBO §7- teleport to limbo",
                        "§bCAGE §7- trap in cage")));

        items.add(toggleItem(20, Material.REDSTONE_TORCH, "§e§lFreeze Mechanic",
                cfg::isFreezeMechanicEnabled,
                value -> {
                    plugin.getConfig().set("freeze_mechanic.enabled", value);
                    plugin.getGameManager().refreshFreezeMechanic();
                },
                "§7Force inactive runners near active one"));

        items.add(adjustItem(21, Material.CLOCK, "§6§lFreeze Duration",
                cfg::getFreezeDurationTicks,
                value -> {
                    plugin.getConfig().set("freeze_mechanic.duration_ticks", value);
                    plugin.getGameManager().refreshFreezeMechanic();
                },
                20, 100, 20, 20 * 60 * 5,
                "§7Ticks freeze persists"));

        items.add(adjustItem(22, Material.REPEATER, "§6§lCheck Interval",
                cfg::getFreezeCheckIntervalTicks,
                value -> {
                    plugin.getConfig().set("freeze_mechanic.check_interval_ticks", value);
                    plugin.getGameManager().refreshFreezeMechanic();
                },
                5, 20, 5, 200,
                "§7Ticks between freeze checks"));

        items.add(adjustItem(23, Material.COMPASS, "§6§lMax Distance",
                () -> (int) Math.round(cfg.getFreezeMaxDistance()),
                value -> {
                    plugin.getConfig().set("freeze_mechanic.max_distance", value);
                    plugin.getGameManager().refreshFreezeMechanic();
                },
                5, 20, 5, 256,
                "§7Maximum distance before freeze"));

        items.add(toggleItem(30, Material.BARRIER, "§e§lCancel Movement",
                cfg::isCancelMovement,
                value -> plugin.getConfig().set("cancel.movement", value),
                "§7Block inactive runner movement"));

        items.add(toggleItem(31, Material.STICK, "§e§lCancel Interactions",
                cfg::isCancelInteractions,
                value -> plugin.getConfig().set("cancel.interactions", value),
                "§7Block inactive runner interactions"));

        items.add(toggleItem(32, Material.WHITE_BED, "§e§lSingle Player Sleep",
                cfg::isSinglePlayerSleepEnabled,
                value -> cfg.setSinglePlayerSleepEnabled(value),
                "§7Only active runner must sleep"));

        items.add(toggleConfigItem(33, Material.RESPAWN_ANCHOR, "§e§lForce Global Spawn",
                "spawn.force_global", true,
                "§7Override personal beds"));

        items.add(clickItem(34, () -> {
            org.bukkit.Location spawn = plugin.getConfigManager().getSpawnLocation();
            String worldName = spawn.getWorld() != null ? spawn.getWorld().getName() : "unknown";
            return icon(Material.COMPASS, "§b§lSet Spawn",
                    List.of("§7World: §f" + worldName,
                            String.format(Locale.ROOT, "§7Coords: §f%.1f / %.1f / %.1f", spawn.getX(), spawn.getY(), spawn.getZ()),
                            "",
                            "§eClick to use your position"));
        }, ctxClick -> {
            plugin.getConfigManager().setGlobalSpawn(ctxClick.player().getLocation(), true);
            Msg.send(ctxClick.player(), "§aGlobal spawn updated to your current position.");
            ctxClick.reopen();
        }));

        items.add(clickItem(35, () -> {
            org.bukkit.Location limbo = cfg.getLimboLocation();
            String world = limbo.getWorld() != null ? limbo.getWorld().getName() : "unknown";
            String coords = String.format(Locale.ROOT, "§f%.1f §7/ §f%.1f §7/ §f%.1f", limbo.getX(), limbo.getY(), limbo.getZ());
            return icon(Material.ENDER_PEARL, "§b§lSet Limbo Location",
                    List.of("§7World: §f" + world, "§7Coords: " + coords, "", "§eClick to use your position"));
        }, ctxClick -> {
            org.bukkit.Location loc = ctxClick.player().getLocation();
            plugin.getConfig().set("limbo.world", loc.getWorld() != null ? loc.getWorld().getName() : "world");
            plugin.getConfig().set("limbo.x", loc.getX());
            plugin.getConfig().set("limbo.y", loc.getY());
            plugin.getConfig().set("limbo.z", loc.getZ());
            plugin.saveConfig();
            Msg.send(ctxClick.player(), "§aLimbo location updated to your current position.");
            ctxClick.reopen();
        }));

        items.add(backButton(44, "§7§lBack", MenuKey.SETTINGS_HOME, null, this::openSettingsMenu));

        return new MenuScreen("§c§lSafety & Freeze", 54, items);
    }

    private MenuScreen buildHunterSettings(MenuContext ctx) {
        ConfigManager cfg = plugin.getConfigManager();
        List<MenuItem> items = new ArrayList<>();
        items.add(backButton(0, "§7§lBack", MenuKey.SETTINGS_HOME, null, this::openSettingsMenu));

        items.add(toggleItem(10, Material.COMPASS, "§e§lTracker",
                cfg::isTrackerEnabled,
                value -> {
                    cfg.setTrackerEnabled(value);
                    if (plugin.getGameManager().isGameRunning()) {
                        if (value) {
                            plugin.getTrackerManager().startTracking();
                            plugin.getTrackerManager().updateAllHunterCompasses();
                        } else {
                            plugin.getTrackerManager().stopTracking();
                        }
                    }
                },
                "§7Give hunters tracking compasses"));

        items.add(adjustItem(11, Material.REPEATER, "§6§lUpdate Ticks",
                cfg::getTrackerUpdateTicks,
                value -> {
                    plugin.getConfig().set("tracker.update_ticks", value);
                    if (plugin.getGameManager().isGameRunning() && cfg.isTrackerEnabled()) {
                        plugin.getTrackerManager().startTracking();
                    }
                },
                5, 20, 1, 200,
                "§7Compass update frequency"));

        items.add(toggleItem(12, Material.BLAZE_ROD, "§e§lCompass Jamming",
                cfg::isCompassJammingEnabled,
                value -> plugin.getConfig().set("tracker.compass_jamming.enabled", value),
                "§7Scramble compass after swap"));

        items.add(adjustItem(13, Material.CLOCK, "§6§lJam Duration",
                cfg::getCompassJamDuration,
                value -> plugin.getConfig().set("tracker.compass_jamming.duration_ticks", value),
                20, 100, 20, 20 * 60,
                "§7Ticks compasses stay jammed"));

        items.add(adjustItem(14, Material.SPYGLASS, "§6§lJam Distance",
                cfg::getCompassJamMaxDistance,
                value -> cfg.setCompassJamMaxDistance(value),
                10, 50, 0, 5000,
                "§7Maximum random offset"));

        items.add(clickItem(44, () -> icon(Material.BARRIER, "§7§lBack", Collections.emptyList()),
                ctxClick -> open(ctxClick.player(), MenuKey.SETTINGS_HOME, null, false)));

        return new MenuScreen("§c§lHunter Tools", 27, items);
    }

    private MenuScreen buildPowerUpsRoot(MenuContext ctx) {
        ConfigManager cfg = plugin.getConfigManager();
        List<MenuItem> items = new ArrayList<>();
        items.add(backButton(0, "§7§lBack", MenuKey.SETTINGS_HOME, null, this::openSettingsMenu));

        items.add(toggleItem(11, Material.LIME_DYE, "§e§lPower-ups",
                cfg::isPowerUpsEnabled,
                value -> cfg.setPowerUpsEnabled(value),
                "§7Enable random swap effects"));

        items.add(navigateItem(13, Material.SPLASH_POTION, "§a§lPositive Effects", MenuKey.POWERUPS_EFFECTS,
                "§7Configure buffs", "positive"));
        items.add(navigateItem(15, Material.POISONOUS_POTATO, "§c§lNegative Effects", MenuKey.POWERUPS_EFFECTS,
                "§7Configure debuffs", "negative"));

        items.add(navigateItem(22, Material.CLOCK, "§6§lDurations & Levels", MenuKey.POWERUPS_DURATION,
                "§7Modify duration range"));

        items.add(clickItem(44, () -> icon(Material.BARRIER, "§7§lBack", Collections.emptyList()),
                ctxClick -> open(ctxClick.player(), MenuKey.SETTINGS_HOME, null, false)));

        return new MenuScreen("§d§lPower-ups", 27, items);
    }

    private MenuScreen buildPowerUpEffects(MenuContext ctx) {
        boolean positive = "positive".equalsIgnoreCase(String.valueOf(ctx.request().data()));
        List<String> list = positive ? plugin.getConfigManager().getGoodPowerUps() : plugin.getConfigManager().getBadPowerUps();
        Set<String> enabled = new HashSet<>();
        for (String id : list) {
            enabled.add(id.toUpperCase(Locale.ROOT));
        }

        List<MenuItem> items = new ArrayList<>();
        items.add(backButton(0, "§7§lBack", MenuKey.POWERUPS_ROOT, null, this::openPowerUpsMenu));

        int slot = 9;
        @SuppressWarnings("deprecation")
        PotionEffectType[] effectTypes = PotionEffectType.values();
        for (PotionEffectType type : effectTypes) {
            if (type == null || type.getKey() == null) continue;
            String id = type.getKey().getKey().toUpperCase(Locale.ROOT);
            String prefix = positive ? "§a" : "§c";
            Material material = positive ? Material.HONEY_BOTTLE : Material.SPIDER_EYE;
            items.add(toggleItem(slot, material, prefix + id,
                    () -> enabled.contains(id), value -> {
                        List<String> editable = positive ? plugin.getConfig().getStringList("power_ups.good_effects") : plugin.getConfig().getStringList("power_ups.bad_effects");
                        if (value && !editable.contains(id)) editable.add(id);
                        if (!value) editable.remove(id);
                        if (positive) plugin.getConfig().set("power_ups.good_effects", editable);
                        else plugin.getConfig().set("power_ups.bad_effects", editable);
                        plugin.saveConfig();
                        Msg.send(ctx.player(), "§e" + id + ": " + (value ? "§aEnabled" : "§cDisabled"));
                    }, "§7Click to toggle"));

            slot++;
            if ((slot + 1) % 9 == 0) slot += 2;
            if (slot >= 54) break;
        }

        return new MenuScreen(positive ? "§a§lPositive Effects" : "§c§lNegative Effects", 54, items);
    }

    private MenuScreen buildPowerUpDurations(MenuContext ctx) {
        ConfigManager cfg = plugin.getConfigManager();
        List<MenuItem> items = new ArrayList<>();
        items.add(backButton(0, "§7§lBack", MenuKey.POWERUPS_ROOT, null, this::openPowerUpsMenu));

        items.add(adjustItem(12, Material.CLOCK, "§6§lMin Duration", cfg::getPowerUpsMinSeconds,
                value -> cfg.setPowerUpsMinSeconds(value), 5, 20, 1, 1800,
                "§7Seconds minimum"));
        items.add(adjustItem(14, Material.CLOCK, "§6§lMax Duration", cfg::getPowerUpsMaxSeconds,
                value -> cfg.setPowerUpsMaxSeconds(value), 5, 20, 1, 3600,
                "§7Seconds maximum"));
        items.add(adjustItem(21, Material.EXPERIENCE_BOTTLE, "§6§lMin Level", cfg::getPowerUpsMinLevel,
                value -> cfg.setPowerUpsMinLevel(value), 1, 1, 1, 5,
                "§7Potion amplifier minimum"));
        items.add(adjustItem(23, Material.EXPERIENCE_BOTTLE, "§6§lMax Level", cfg::getPowerUpsMaxLevel,
                value -> cfg.setPowerUpsMaxLevel(value), 1, 1, 1, 5,
                "§7Potion amplifier maximum"));

        return new MenuScreen("§6§lDuration & Level", 45, items);
    }

    private MenuScreen buildDangerousBlocks(MenuContext ctx) {
        List<MenuItem> items = new ArrayList<>();
        items.add(backButton(0, "§7§lBack", MenuKey.SETTINGS_HOME, null, this::openSettingsMenu));

        Set<Material> blocks = new HashSet<>(plugin.getConfigManager().getDangerousBlocks());
        List<Material> sorted = new ArrayList<>(blocks);
        sorted.sort(java.util.Comparator.comparing(Enum::name));

        int slot = 9;
        for (Material material : sorted) {
            items.add(clickItem(slot, () -> icon(material, "§e" + material.name(), List.of("§cClick to remove")), ctxClick -> {
                Set<Material> set = plugin.getConfigManager().getDangerousBlocks();
                set.remove(material);
                List<String> updated = new ArrayList<>();
                for (Material m : set) updated.add(m.name());
                plugin.getConfig().set("safe_swap.dangerous_blocks", updated);
                plugin.saveConfig();
                Msg.send(ctxClick.player(), "§eRemoved §f" + material.name());
                ctxClick.reopen();
            }));
            slot++;
            if ((slot + 1) % 9 == 0) slot += 2;
        }

        items.add(clickItem(44, () -> icon(Material.EMERALD_BLOCK, "§a§lAdd Block", List.of("§7Type block ID in chat")), ctxClick -> {
            plugin.getChatInputHandler().expectConfigListAdd(ctxClick.player(), "safe_swap.dangerous_blocks");
            ctxClick.player().closeInventory();
            Msg.send(ctxClick.player(), "§eType a block ID (or 'cancel').");
        }));

        return new MenuScreen("§c§lDangerous Blocks", 54, items);
    }

    private MenuScreen buildWorldBorder(MenuContext ctx) {
        List<MenuItem> items = new ArrayList<>();
        items.add(backButton(0, "§7§lBack", MenuKey.SETTINGS_HOME, null, this::openSettingsMenu));

        items.add(toggleConfigItem(10, Material.BARRIER, "§e§lWorld Border", "world_border.enabled", false,
                "§7Enable shrinking border"));

        items.add(adjustConfigItem(12, Material.GRASS_BLOCK, "§6§lInitial Size",
                "world_border.initial_size", 2000,
                50, 100, 50, 100000,
                "§7Blocks at start"));
        items.add(adjustConfigItem(14, Material.BEDROCK, "§6§lFinal Size",
                "world_border.final_size", 100,
                25, 100, 25, 5000,
                "§7Blocks at end"));
        items.add(adjustConfigItem(16, Material.CLOCK, "§6§lShrink Duration",
                "world_border.shrink_duration", 1800,
                60, 300, 60, 21600,
                "§7Seconds to shrink"));

        items.add(adjustConfigItem(20, Material.SPYGLASS, "§6§lWarning Distance",
                "world_border.warning_distance", 50,
                5, 25, 0, 5000,
                "§7Blocks before warning"));

        items.add(adjustConfigItem(22, Material.BELL, "§6§lWarning Interval (s)",
                "world_border.warning_interval", 300,
                30, 60, 30, 3600,
                "§7Seconds between alerts"));

        return new MenuScreen("§4§lWorld Border", 45, items);
    }

    private MenuScreen buildBounty(MenuContext ctx) {
        List<MenuItem> items = new ArrayList<>();
        items.add(backButton(0, "§7§lBack", MenuKey.SETTINGS_HOME, null, this::openSettingsMenu));

        items.add(toggleItem(10, Material.GOLD_INGOT, "§6§lBounty Enabled",
                () -> plugin.getConfig().getBoolean("bounty.enabled", false),
                value -> {
                    plugin.getConfig().set("bounty.enabled", value);
                    plugin.saveConfig();
                    if (!value) plugin.getBountyManager().clearBounty();
                },
                "§7Enable hunter bounty challenges"));

        items.add(adjustItem(12, Material.CLOCK, "§6§lCooldown (s)",
                () -> plugin.getConfig().getInt("bounty.cooldown", 300),
                value -> plugin.getConfig().set("bounty.cooldown", Math.max(0, value)),
                30, 60, 0, 3600,
                "§7Minimum seconds between bounties"));

        items.add(adjustItem(14, Material.GLOWSTONE_DUST, "§6§lGlow Duration (s)",
                () -> plugin.getConfig().getInt("bounty.glow_duration", 300),
                value -> plugin.getConfig().set("bounty.glow_duration", Math.max(10, value)),
                30, 60, 10, 3600,
                "§7Seconds the target glows"));

        items.add(adjustItem(21, Material.SUGAR, "§6§lSpeed Reward (s)",
                () -> plugin.getConfig().getInt("bounty.rewards.speed_duration", 300),
                value -> plugin.getConfig().set("bounty.rewards.speed_duration", Math.max(10, value)),
                30, 60, 10, 6000,
                "§7Speed effect duration for killer"));

        items.add(adjustItem(23, Material.BLAZE_POWDER, "§6§lStrength Reward (s)",
                () -> plugin.getConfig().getInt("bounty.rewards.strength_duration", 300),
                value -> plugin.getConfig().set("bounty.rewards.strength_duration", Math.max(10, value)),
                30, 60, 10, 6000,
                "§7Strength effect duration for killer"));

        items.add(clickItem(30, () -> icon(Material.TARGET, "§a§lAssign New Bounty", List.of("§7Pick a new target")), ctxClick -> {
            plugin.getBountyManager().assignNewBounty();
            Msg.send(ctxClick.player(), "§aNew bounty assigned.");
        }));
        items.add(clickItem(32, () -> icon(Material.BARRIER, "§c§lClear Bounty", List.of("§7Remove current target")), ctxClick -> {
            plugin.getBountyManager().clearBounty();
            Msg.send(ctxClick.player(), "§cBounty cleared.");
        }));

        return new MenuScreen("§6§lBounty System", 45, items);
    }

    private MenuScreen buildLastStand(MenuContext ctx) {
        ConfigManager cfg = plugin.getConfigManager();
        List<MenuItem> items = new ArrayList<>();
        items.add(backButton(0, "§7§lBack", MenuKey.SETTINGS_HOME, null, this::openSettingsMenu));

        items.add(toggleItem(10, Material.TOTEM_OF_UNDYING, "§6§lLast Stand",
                cfg::isLastStandEnabled,
                value -> plugin.getConfig().set("last_stand.enabled", value),
                "§7Boost final runner"));

        items.add(adjustItem(12, Material.CLOCK, "§6§lDuration",
                cfg::getLastStandDuration,
                value -> plugin.getConfig().set("last_stand.duration_ticks", value),
                20, 100, 20, 20 * 60 * 5,
                "§7Ticks boost lasts"));

        items.add(adjustItem(14, Material.IRON_SWORD, "§6§lStrength Level",
                cfg::getLastStandStrengthAmplifier,
                value -> plugin.getConfig().set("last_stand.strength_amplifier", value),
                1, 1, 0, 5,
                "§7Amplifier (level-1)"));

        items.add(adjustItem(16, Material.SUGAR, "§6§lSpeed Level",
                cfg::getLastStandSpeedAmplifier,
                value -> plugin.getConfig().set("last_stand.speed_amplifier", value),
                1, 1, 0, 5,
                "§7Amplifier (level-1)"));

        return new MenuScreen("§6§lLast Stand", 45, items);
    }

    private MenuScreen buildSuddenDeath(MenuContext ctx) {
        List<MenuItem> items = new ArrayList<>();
        items.add(backButton(0, "§7§lBack", MenuKey.SETTINGS_HOME, null, this::openSettingsMenu));

        items.add(toggleConfigItem(10, Material.DRAGON_HEAD, "§4§lSudden Death",
                "sudden_death.enabled", false,
                "§7Enable end dimension showdown"));

        items.add(adjustItem(12, Material.CLOCK, "§6§lActivation Delay (s)",
                () -> plugin.getConfig().getInt("sudden_death.activation_delay", 1200),
                value -> plugin.getConfig().set("sudden_death.activation_delay", Math.max(30, value)),
                60, 300, 30, 36000,
                "§7Seconds before sudden death begins"));

        items.add(adjustItem(14, Material.SHIELD, "§6§lResistance (s)",
                () -> (int) Math.round(plugin.getConfig().getInt("sudden_death.effects.resistance_duration", 200) / 20.0),
                value -> plugin.getConfig().set("sudden_death.effects.resistance_duration", Math.max(1, value) * 20),
                5, 20, 1, 600,
                "§7Duration of Resistance IV"));

        items.add(adjustItem(16, Material.GOLDEN_APPLE, "§6§lRegeneration (s)",
                () -> (int) Math.round(plugin.getConfig().getInt("sudden_death.effects.regeneration_duration", 200) / 20.0),
                value -> plugin.getConfig().set("sudden_death.effects.regeneration_duration", Math.max(1, value) * 20),
                5, 20, 1, 600,
                "§7Duration of Regeneration III"));

        items.add(adjustItem(20, Material.SPYGLASS, "§6§lMax Jam Distance",
                () -> plugin.getConfig().getInt("sudden_death.arena.max_jam_distance", 100),
                value -> plugin.getConfig().set("sudden_death.arena.max_jam_distance", Math.max(0, value)),
                10, 50, 0, 10000,
                "§7Blocks for random compass offsets"));

        items.add(clickItem(22, () -> {
            double x = plugin.getConfig().getDouble("sudden_death.arena.x", 100.0);
            double y = plugin.getConfig().getDouble("sudden_death.arena.y", 50.0);
            double z = plugin.getConfig().getDouble("sudden_death.arena.z", 0.0);
            String worldName = plugin.getConfig().getString("sudden_death.arena.world", "world_the_end");
            return icon(Material.ENDER_EYE, "§b§lSet Arena Location",
                    List.of("§7Current: §f" + x + ", " + y + ", " + z,
                            "§7World: §f" + worldName,
                            "", "§eClick to use your position"));
        }, ctxClick -> {
            org.bukkit.Location loc = ctxClick.player().getLocation();
            plugin.getConfig().set("sudden_death.arena.x", loc.getX());
            plugin.getConfig().set("sudden_death.arena.y", loc.getY());
            plugin.getConfig().set("sudden_death.arena.z", loc.getZ());
            plugin.getConfig().set("sudden_death.arena.world", loc.getWorld() != null ? loc.getWorld().getName() : "world_the_end");
            plugin.saveConfig();
            Msg.send(ctxClick.player(), "§aArena position updated.");
            ctxClick.reopen();
        }));

        items.add(clickItem(30, () -> icon(Material.CLOCK, "§e§lSchedule", List.of("§7Start countdown")), ctxClick -> {
            plugin.getSuddenDeathManager().scheduleSuddenDeath();
            Msg.send(ctxClick.player(), "§eSudden death scheduled.");
        }));
        items.add(clickItem(32, () -> icon(Material.BARRIER, "§c§lCancel Schedule", Collections.emptyList()), ctxClick -> {
            plugin.getSuddenDeathManager().cancelSchedule();
            Msg.send(ctxClick.player(), "§cSchedule cancelled.");
        }));
        items.add(clickItem(34, () -> icon(Material.TNT, "§4§lActivate Now", Collections.emptyList()), ctxClick -> {
            plugin.getSuddenDeathManager().activateSuddenDeath();
            Msg.send(ctxClick.player(), "§4Sudden death activated!");
        }));

        return new MenuScreen("§4§lSudden Death", 45, items);
    }

    private MenuScreen buildStatsRoot(MenuContext ctx) {
        StatsParent parent = statsParents.getOrDefault(ctx.player().getUniqueId(), StatsParent.SETTINGS);
        List<MenuItem> items = new ArrayList<>();
        items.add(backButton(0, "§7§lBack", parent == StatsParent.MAIN ? MenuKey.MAIN : MenuKey.SETTINGS_HOME, null,
                player -> {
                    if (parent == StatsParent.MAIN) open(player, MenuKey.MAIN, null, false);
                    else openSettingsMenu(player);
                }));

        items.add(toggleItem(11, Material.LIME_DYE, "§e§lStatistics",
                () -> plugin.getConfig().getBoolean("stats.enabled", true),
                value -> {
                    plugin.getConfig().set("stats.enabled", value);
                    plugin.saveConfig();
                    if (!value) plugin.getStatsManager().stopTracking();
                    else if (plugin.getGameManager().isGameRunning()) plugin.getStatsManager().startTracking();
                },
                "§7Toggle server-side tracking"));

        items.add(toggleItem(13, Material.COMPASS, "§6§lDistance Tracking",
                () -> plugin.getConfig().getBoolean("stats.distance_tracking", true),
                value -> {
                    plugin.getConfig().set("stats.distance_tracking", value);
                    plugin.saveConfig();
                }, "§7Enable runner-hunter distance metric"));

        items.add(adjustItem(15, Material.REPEATER, "§6§lDistance Update",
                () -> plugin.getConfig().getInt("stats.distance_update_ticks", 20),
                value -> {
                    plugin.getConfig().set("stats.distance_update_ticks", value);
                    plugin.saveConfig();
                }, 5, 20, 1, 200,
                "§7Ticks between distance updates"));

        items.add(toggleConfigItem(17, Material.CLOCK, "§e§lPeriodic Display",
                "stats.periodic_display", false,
                "§7Announce stats automatically"));

        items.add(adjustConfigItem(26, Material.CLOCK, "§6§lDisplay Interval (s)",
                "stats.periodic_display_interval", 300,
                30, 60, 30, 3600,
                "§7Seconds between stat announcements"));

        items.add(clickItem(22, () -> icon(Material.PAPER, "§b§lBroadcast Snapshot", List.of("§7Send stats to chat")), ctxClick -> plugin.getStatsManager().displayStats()));

        items.add(navigateItem(24, Material.SPYGLASS, "§6§lAdvanced", MenuKey.STATS_ADVANCED, "§7Additional settings"));

        return new MenuScreen("§b§lStatistics", 45, items);
    }

    private MenuScreen buildStatsAdvanced(MenuContext ctx) {
        ConfigManager cfg = plugin.getConfigManager();
        List<MenuItem> items = new ArrayList<>();
        items.add(backButton(0, "§7§lBack", MenuKey.STATS_ROOT, null, player -> open(player, MenuKey.STATS_ROOT, null, false)));

        items.add(adjustItem(11, Material.CLOCK, "§6§lActionbar Update",
                cfg::getActionBarUpdateTicks,
                value -> plugin.getConfig().set("ui.update_ticks.actionbar", value),
                5, 20, 1, 200,
                "§7Ticks between actionbar refresh"));

        items.add(adjustItem(13, Material.EXPERIENCE_BOTTLE, "§6§lTitle Update",
                cfg::getTitleUpdateTicks,
                value -> plugin.getConfig().set("ui.update_ticks.title", value),
                1, 5, 1, 200,
                "§7Ticks between title refresh"));

        items.add(adjustItem(15, Material.COMPASS, "§6§lTracker Update",
                cfg::getTrackerUpdateTicks,
                value -> plugin.getConfig().set("tracker.update_ticks", value),
                5, 20, 1, 200,
                "§7Compass refresh interval"));

        items.add(cycleItem(20, Material.CLOCK, "§6§lRunner Timer", cfg::getRunnerTimerVisibility,
                current -> {
                    String next = nextVisibility(current);
                    cfg.setRunnerTimerVisibility(next);
                    return next;
                }, timerLore("Active runner")));
        items.add(cycleItem(22, Material.CLOCK, "§6§lWaiting Timer", cfg::getWaitingTimerVisibility,
                current -> {
                    String next = nextVisibility(current);
                    cfg.setWaitingTimerVisibility(next);
                    return next;
                }, timerLore("Waiting runners")));
        items.add(cycleItem(24, Material.CLOCK, "§6§lHunter Timer", cfg::getHunterTimerVisibility,
                current -> {
                    String next = nextVisibility(current);
                    cfg.setHunterTimerVisibility(next);
                    return next;
                }, timerLore("Hunters")));

        return new MenuScreen("§b§lStats - Advanced", 45, items);
    }

    private MenuScreen buildTaskHome(MenuContext ctx) {
        TaskManagerMode mode = plugin.getTaskManagerMode();
        int assignments = mode != null ? mode.getAssignments().size() : 0;
        List<MenuItem> items = new ArrayList<>();
        items.add(backButton(0, "§7§lBack", MenuKey.MAIN, null, this::openMainMenu));

        items.add(navigateItem(10, Material.WRITABLE_BOOK, "§6§lCompetition Rules", MenuKey.SETTINGS_TASK,
                "§7Pause behaviour, grace timers"));
        items.add(navigateItem(12, Material.EMERALD, "§a§lCustom Tasks", MenuKey.TASK_CUSTOM,
                "§7Manage custom entries"));
        items.add(clickItem(14, () -> icon(Material.FEATHER, "§b§lReroll Tasks", List.of("§7Assign new secret tasks")), ctxClick -> {
            if (plugin.getGameManager().isGameRunning()) {
                Msg.send(ctxClick.player(), "§cStop the game before rerolling tasks.");
                return;
            }
            if (mode != null) {
                mode.assignAndAnnounceTasks(plugin.getGameManager().getRunners());
                Msg.send(ctxClick.player(), "§aTasks rerolled for current runners.");
            }
        }));
        items.add(navigateItem(16, Material.PAPER, "§e§lCurrent Assignments", MenuKey.TASK_ASSIGNMENTS,
                "§7Active runner tasks", assignments));
        items.add(navigateItem(18, Material.BOOKSHELF, "§6§lTask Pool", MenuKey.TASK_POOL,
                "§7Enable/disable individual tasks", 0));

        return new MenuScreen("§6§lTask Master", 36, items);
    }

    private MenuScreen buildTaskSettings(MenuContext ctx) {
        List<MenuItem> items = new ArrayList<>();
        items.add(backButton(0, "§7§lBack", MenuKey.TASK_HOME, null, this::openTaskManagerMenu));

        items.add(toggleConfigItem(10, Material.REDSTONE_TORCH, "§e§lPause on Disconnect",
                "task_manager.pause_on_disconnect", true,
                "§7Pause when active runner disconnects"));

        items.add(toggleConfigItem(11, Material.BARRIER, "§e§lRemove On Timeout",
                "task_manager.remove_on_timeout", true,
                "§7Drop players who exceed rejoin grace"));

        items.add(toggleConfigItem(12, Material.HOPPER, "§e§lAllow Late Joiners",
                "task_manager.allow_late_joiners", false,
                "§7Permit players to join mid-game"));

        items.add(toggleConfigItem(13, Material.BOOK, "§e§lInclude Default Tasks",
                "task_manager.include_default_tasks", true,
                "§7Keep built-in objectives in the pool"));

        items.add(adjustConfigItem(14, Material.CLOCK, "§6§lRejoin Grace (s)",
                "task_manager.rejoin_grace_seconds", 180, 10, 30, 10, 3600,
                "§7Seconds allowed to reconnect"));

        items.add(adjustConfigItem(15, Material.CLOCK, "§6§lMax Game Length (min)",
                "task_manager.max_game_duration", 0,
                5, 15, 0, 360,
                "§70 = unlimited duration"));

        items.add(toggleConfigItem(16, Material.NAME_TAG, "§e§lEnd When One Left",
                "task_manager.end_when_one_left", false,
                "§7Automatically finish when one runner remains"));

        return new MenuScreen("§6§lTask Settings", 36, items);
    }

    private MenuScreen buildTaskCustom(MenuContext ctx) {
        TaskManagerMode mode = plugin.getTaskManagerMode();
        List<MenuItem> items = new ArrayList<>();
        items.add(backButton(0, "§7§lBack", MenuKey.TASK_HOME, null, this::openTaskManagerMenu));

        items.add(simpleItem(2, () -> icon(Material.BOOK, "§7Default Pool",
                List.of("§7Included: " + (plugin.getConfig().getBoolean("task_manager.include_default_tasks", true) ? "§aYes" : "§cNo"),
                        "§7Toggle in Task Settings"))));

        items.add(clickItem(8, () -> icon(Material.EMERALD_BLOCK, "§a§lAdd Custom Task", List.of("§7Enter ID via chat")), ctxClick -> {
            plugin.getChatInputHandler().expectTaskId(ctxClick.player());
            ctxClick.player().closeInventory();
            Msg.send(ctxClick.player(), "§eEnter a unique task ID in chat.");
        }));

        if (mode != null) {
            List<String> ids = mode.getCustomTaskIds();
            int slot = 9;
            for (String id : ids) {
                TaskDefinition def = mode.getTask(id);
                String description = def != null ? def.description() : "";
                items.add(clickItem(slot, () -> icon(Material.PAPER, "§e" + id,
                        List.of("§7" + description, "", "§cClick to remove")), ctxClick -> {
                    if (mode.removeCustomTask(id)) {
                        Msg.send(ctxClick.player(), "§cRemoved custom task §f" + id);
                    }
                    ctxClick.reopen();
                }));
                slot++;
                if ((slot + 1) % 9 == 0) slot += 2;
                if (slot >= 54) break;
            }
        }

        return new MenuScreen("§6§lCustom Tasks", 54, items);
    }

    private MenuScreen buildTaskPool(MenuContext ctx) {
        List<MenuItem> items = new ArrayList<>();
        items.add(backButton(0, "§7§lBack", MenuKey.TASK_HOME, null, this::openTaskManagerMenu));

        TaskManagerMode mode = plugin.getTaskManagerMode();
        if (mode == null) {
            items.add(simpleItem(22, () -> icon(Material.BARRIER, "§cTask Manager unavailable",
                    List.of("§7Task mode not initialised."))));
            return new MenuScreen("§6§lTask Pool", 54, items);
        }

        TaskDefinition[] defs = mode.getAllDefinitions().values().toArray(TaskDefinition[]::new);
        java.util.Arrays.sort(defs, java.util.Comparator.comparing(TaskDefinition::id, String.CASE_INSENSITIVE_ORDER));

        int perPage = 36;
        int totalPages = Math.max(1, (int) Math.ceil(defs.length / (double) perPage));
        int pageIndex = 0;
        if (ctx.request().data() instanceof Integer p) {
            pageIndex = Math.max(0, Math.min(p, totalPages - 1));
        }
        final int page = pageIndex;

        items.add(cycleItem(2, Material.NETHERITE_SWORD, "§6§lDifficulty",
                () -> mode.getDifficultyFilter().name(), current -> {
                    TaskDifficulty cur = mode.getDifficultyFilter();
                    TaskDifficulty next = switch (cur) {
                        case EASY -> TaskDifficulty.MEDIUM;
                        case MEDIUM -> TaskDifficulty.HARD;
                        case HARD -> TaskDifficulty.EASY;
                    };
                    mode.setDifficultyFilter(next);
                    return next.name();
                }, List.of("§7Filter used when assigning", "§7Cycles EASY → MEDIUM → HARD")));

        items.add(simpleItem(4, () -> icon(Material.PAPER, "§7Eligible Tasks",
                List.of("§a" + mode.getCandidateCount() + " §7available for selection"))));

        items.add(clickItem(6, () -> icon(Material.ENDER_CHEST, "§e§lReload tasks.yml",
                        List.of("§7Re-read task definitions")), ctxClick -> {
                    mode.reloadTasksFromFile();
                    Msg.send(ctxClick.player(), "§aReloaded tasks.yml.");
                    open(ctxClick.player(), MenuKey.TASK_POOL, page, true);
                }));

        int start = page * perPage;
        int end = Math.min(defs.length, start + perPage);
        for (int i = start; i < end; i++) {
            TaskDefinition def = defs[i];
            int displayIndex = i - start;
            int row = displayIndex / 9;
            int col = displayIndex % 9;
            int slot = 9 + row * 9 + col;
            String id = def.id();
            TaskDefinition current = mode.getTask(id);
            boolean enabled = current == null || current.enabled();
            Material mat = enabled ? Material.WRITABLE_BOOK : Material.GRAY_DYE;
            List<String> lore = new ArrayList<>();
            lore.add("§7" + Optional.ofNullable(def.description()).orElse("No description"));
            lore.add("§7Difficulty: §f" + (def.difficulty() != null ? def.difficulty().name() : "MEDIUM"));
            if (def.categories() != null && !def.categories().isEmpty()) {
                lore.add("§7Tags: §f" + String.join(", ", def.categories()));
            }
            lore.add("");
            lore.add(enabled ? "§aEnabled" : "§cDisabled");
            lore.add("§7Click to toggle");
            items.add(clickItem(slot, () -> icon(mat, (enabled ? "§a" : "§c") + id, lore), ctxClick -> {
                TaskDefinition cur = mode.getTask(id);
                boolean next = cur == null || !cur.enabled();
                mode.setTaskEnabled(id, next);
                Msg.send(ctxClick.player(), "§eTask §f" + id + "§e is now " + (next ? "§aenabled" : "§cdisabled"));
                open(ctxClick.player(), MenuKey.TASK_POOL, page, true);
            }));
        }

        if (page > 0) {
            items.add(clickItem(45, () -> icon(Material.ARROW, "§7§lPrevious Page",
                    List.of("§7Page " + page + " of " + totalPages)), ctxClick -> open(ctxClick.player(), MenuKey.TASK_POOL, page - 1, false)));
        }
        if (page < totalPages - 1) {
            items.add(clickItem(53, () -> icon(Material.ARROW, "§7§lNext Page",
                    List.of("§7Page " + (page + 2) + " of " + totalPages)), ctxClick -> open(ctxClick.player(), MenuKey.TASK_POOL, page + 1, false)));
        }

        items.add(simpleItem(49, () -> icon(Material.NAME_TAG, "§7Page Info",
                List.of("§7Page §f" + (page + 1) + "§7 of §f" + totalPages,
                        "§7Tasks total: §f" + defs.length))));

        return new MenuScreen("§6§lTask Pool", 54, items);
    }

    private MenuItem buildParticleTypeCycler(int slot) {
        return cycleItem(slot, Material.FIREWORK_STAR, "§6§lParticle Type",
                plugin.getConfigManager()::getParticleTrailType,
                current -> {
                    int idx = PARTICLE_TYPES.indexOf(current == null ? "" : current.toUpperCase(Locale.ROOT));
                    int nextIndex = (idx + 1) % PARTICLE_TYPES.size();
                    String type = PARTICLE_TYPES.get(nextIndex);
                    plugin.getConfig().set("particle_trail.type", type);
                    return type;
                },
                List.of("§7Cycle between allowed particle IDs"));
    }

    private MenuScreen buildTaskAssignments(MenuContext ctx) {
        TaskManagerMode mode = plugin.getTaskManagerMode();
        List<MenuItem> items = new ArrayList<>();
        items.add(backButton(0, "§7§lBack", MenuKey.TASK_HOME, null, this::openTaskManagerMenu));

        if (mode != null) {
            int slot = 9;
            for (Map.Entry<UUID, String> entry : mode.getAssignments().entrySet()) {
                UUID uuid = entry.getKey();
                String taskId = entry.getValue();
                String name = Optional.ofNullable(Bukkit.getOfflinePlayer(uuid).getName()).orElse(uuid.toString().substring(0, 8));
                TaskDefinition def = mode.getTask(taskId);
                String desc = def != null ? def.description() : "Unknown task";
                items.add(simpleItem(slot, () -> icon(Material.PAPER, "§e" + name,
                        List.of("§7Task: §f" + taskId, "§7" + desc))));
                slot++;
                if ((slot + 1) % 9 == 0) slot += 2;
                if (slot >= 54) break;
            }
        }

        return new MenuScreen("§6§lTask Assignments", 54, items);
    }

    private MenuScreen buildVoiceChat(MenuContext ctx) {
        List<MenuItem> items = new ArrayList<>();
        items.add(backButton(0, "§7§lBack", MenuKey.SETTINGS_HOME, null, this::openSettingsMenu));
        items.add(toggleConfigItem(11, Material.NOTE_BLOCK, "§e§lVoice Chat Integration",
                "voice_chat.enabled", false,
                "§7Integrate with Simple Voice Chat"));
        items.add(toggleConfigItem(13, Material.LEVER, "§e§lMute Inactive Runners",
                "voice_chat.mute_inactive_runners", true,
                "§7Automatically mute inactive players"));
        return new MenuScreen("§d§lVoice Chat", 27, items);
    }

    private MenuScreen buildBroadcast(MenuContext ctx) {
        List<MenuItem> items = new ArrayList<>();
        items.add(backButton(0, "§7§lBack", MenuKey.SETTINGS_HOME, null, this::openSettingsMenu));
        items.add(toggleConfigItem(11, Material.BELL, "§e§lBroadcasts", "broadcasts.enabled", true,
                "§7Enable general announcements"));
        items.add(toggleConfigItem(13, Material.MAP, "§e§lGame Events", "broadcasts.game_events", true,
                "§7Announce start/stop"));
        items.add(toggleConfigItem(15, Material.PAPER, "§e§lTeam Changes", "broadcasts.team_changes", true,
                "§7Announce team assignment changes"));
        return new MenuScreen("§e§lBroadcast Settings", 27, items);
    }

    private MenuScreen buildUiSettings(MenuContext ctx) {
        ConfigManager cfg = plugin.getConfigManager();
        List<MenuItem> items = new ArrayList<>();
        items.add(backButton(0, "§7§lBack", MenuKey.SETTINGS_HOME, null, this::openSettingsMenu));

        items.add(adjustItem(11, Material.CLOCK, "§6§lActionbar Update",
                cfg::getActionBarUpdateTicks,
                value -> plugin.getConfig().set("ui.update_ticks.actionbar", value),
                5, 20, 1, 200,
                "§7Ticks between actionbar updates"));

        items.add(adjustItem(13, Material.EXPERIENCE_BOTTLE, "§6§lTitle Update",
                cfg::getTitleUpdateTicks,
                value -> plugin.getConfig().set("ui.update_ticks.title", value),
                1, 5, 1, 200,
                "§7Ticks between title updates"));

        items.add(cycleItem(20, Material.CLOCK, "§6§lRunner Timer", cfg::getRunnerTimerVisibility,
                current -> {
                    String next = nextVisibility(current);
                    cfg.setRunnerTimerVisibility(next);
                    return next;
                }, timerLore("Active runner visibility")));
        items.add(cycleItem(22, Material.CLOCK, "§6§lWaiting Timer", cfg::getWaitingTimerVisibility,
                current -> {
                    String next = nextVisibility(current);
                    cfg.setWaitingTimerVisibility(next);
                    return next;
                }, timerLore("Waiting runner visibility")));
        items.add(cycleItem(24, Material.CLOCK, "§6§lHunter Timer", cfg::getHunterTimerVisibility,
                current -> {
                    String next = nextVisibility(current);
                    cfg.setHunterTimerVisibility(next);
                    return next;
                }, timerLore("Hunter visibility")));

        items.add(toggleItem(29, Material.BLAZE_POWDER, "§e§lParticle Trail",
                plugin.getConfigManager()::isParticleTrailEnabled,
                value -> plugin.getConfig().set("particle_trail.enabled", value),
                "§7Toggle runner particle trail"));

        items.add(adjustItem(31, Material.REDSTONE, "§6§lSpawn Interval",
                plugin.getConfigManager()::getParticleSpawnInterval,
                value -> plugin.getConfig().set("particle_trail.spawn_interval", Math.max(1, value)),
                1, 5, 1, 200,
                "§7Ticks between trail spawns"));

        items.add(buildParticleTypeCycler(33));

        items.add(adjustItem(39, Material.RED_DYE, "§cRed Channel",
                () -> plugin.getConfigManager().getParticleTrailColor()[0],
                value -> updateParticleColorChannel(0, value),
                5, 20, 0, 255,
                "§7Adjust red intensity"));
        items.add(adjustItem(40, Material.GREEN_DYE, "§aGreen Channel",
                () -> plugin.getConfigManager().getParticleTrailColor()[1],
                value -> updateParticleColorChannel(1, value),
                5, 20, 0, 255,
                "§7Adjust green intensity"));
        items.add(adjustItem(41, Material.LAPIS_LAZULI, "§9Blue Channel",
                () -> plugin.getConfigManager().getParticleTrailColor()[2],
                value -> updateParticleColorChannel(2, value),
                5, 20, 0, 255,
                "§7Adjust blue intensity"));

        return new MenuScreen("§b§lUI & Timers", 45, items);
    }

    private MenuScreen buildKitManager(MenuContext ctx) {
        List<MenuItem> items = new ArrayList<>();
        items.add(backButton(0, "§7§lBack", MenuKey.SETTINGS_HOME, null, this::openSettingsMenu));

        items.add(toggleItem(11, Material.CHEST, "§e§lKits Enabled",
                plugin.getConfigManager()::isKitsEnabled,
                value -> plugin.getConfigManager().setKitsEnabled(value),
                "§7Toggle kit distribution on start"));

        items.add(clickItem(13, () -> icon(Material.DIAMOND_SWORD, "§a§lGive Runner Kit", List.of("§7Equip configured runner kit")), ctxClick -> plugin.getKitManager().applyRunnerKit(ctxClick.player())));

        items.add(clickItem(15, () -> icon(Material.IRON_SWORD, "§c§lGive Hunter Kit", List.of("§7Equip configured hunter kit")), ctxClick -> plugin.getKitManager().applyHunterKit(ctxClick.player())));

        items.add(simpleItem(31, () -> icon(Material.PAPER, "§7Editing Kits",
                List.of("§7Edit contents in kits.yml", "§7or use /swap kits commands"))));

        return new MenuScreen("§a§lKit Manager", 36, items);
    }

    // -----------------------------------------------------------------
    // Helper item factories

    private MenuItem simpleItem(int slot, Supplier<ItemStack> icon) {
        return new MenuItem("static-" + slot, slot, ctx -> icon.get(), null);
    }

    private MenuItem clickItem(int slot, Supplier<ItemStack> icon, Consumer<MenuClickContext> action) {
        return new MenuItem("click-" + slot + "-" + UUID.randomUUID(), slot, ctx -> icon.get(), action);
    }

    private MenuItem backButton(int slot, String label, MenuKey target, Object data, Consumer<Player> handler) {
        return clickItem(slot, () -> icon(Material.ARROW, label, List.of("§7Go back")), ctx -> {
            if (handler != null) handler.accept(ctx.player());
            else openPrevious(ctx.player());
        });
    }

    private MenuItem navigateItem(int slot, Material material, String name, MenuKey target, String description) {
        return navigateItem(slot, material, name, target, description, null);
    }

    private MenuItem navigateItem(int slot, Material material, String name, MenuKey target, String description, Object data) {
        return clickItem(slot, () -> icon(material, name, List.of("§7" + description)), ctx -> open(ctx.player(), target, data, false));
    }

    private MenuItem toggleItem(int slot, Material material, String label, BooleanSupplier getter, Consumer<Boolean> setter, String description) {
        return clickItem(slot, () -> {
            boolean enabled = getter.getAsBoolean();
            String status = enabled ? "§aEnabled" : "§cDisabled";
            return icon(material, label + ": " + status, description == null ? List.of("§7Click to toggle") : List.of("§7" + description, "§7Click to toggle"));
        }, ctx -> {
            boolean next = !getter.getAsBoolean();
            setter.accept(next);
            plugin.saveConfig();
            Msg.send(ctx.player(), "§e" + label.replace("§", "") + ": " + (next ? "§aEnabled" : "§cDisabled"));
            ctx.reopen();
        });
    }

    private MenuItem toggleConfigItem(int slot, Material material, String label, String path, boolean def, String description) {
        return toggleItem(slot, material, label,
                () -> plugin.getConfig().getBoolean(path, def),
                value -> {
                    plugin.getConfig().set(path, value);
                },
                description);
    }

    private MenuItem adjustItem(int slot, Material material, String label, IntSupplier getter, Consumer<Integer> setter,
                                int step, int shiftStep, int min, int max, String description) {
        return clickItem(slot, () -> icon(material, label + " §f" + getter.getAsInt(),
                List.of("§7" + description,
                        "§7Left/right: ±" + step,
                        "§7Shift: ±" + shiftStep)), ctx -> {
            int value = getter.getAsInt();
            int delta = ctx.shift() ? shiftStep : step;
            if (ctx.click() == ClickType.LEFT) value += delta;
            else if (ctx.click() == ClickType.RIGHT) value -= delta;
            value = Math.max(min, Math.min(max, value));
            setter.accept(value);
            plugin.saveConfig();
            Msg.send(ctx.player(), "§e" + label.replace("§", "") + ": §f" + value);
            ctx.reopen();
        });
    }

    private MenuItem adjustConfigItem(int slot, Material material, String label, String path, int def,
                                      int step, int shiftStep, int min, int max, String description) {
        return adjustItem(slot, material, label,
                () -> plugin.getConfig().getInt(path, def),
                value -> plugin.getConfig().set(path, value),
                step, shiftStep, min, max, description);
    }

    private void updateParticleColorChannel(int channel, int value) {
        int[] rgb = plugin.getConfigManager().getParticleTrailColor();
        if (channel < 0 || channel >= rgb.length) {
            return;
        }
        int clamped = Math.max(0, Math.min(255, value));
        rgb[channel] = clamped;
        plugin.getConfig().set("particle_trail.color", Arrays.asList(rgb[0], rgb[1], rgb[2]));
    }

    private MenuItem cycleItem(int slot, Material material, String label, Supplier<String> getter,
                               Function<String, String> cycler, List<String> description) {
        return clickItem(slot, () -> icon(material, label + ": §f" + getter.get(), description), ctx -> {
            String next = cycler.apply(getter.get());
            plugin.saveConfig();
            Msg.send(ctx.player(), "§e" + label.replace("§", "") + ": §f" + next);
            ctx.reopen();
        });
    }

    private String nextVisibility(String current) {
        if (current == null) return "always";
        return switch (current.toLowerCase(Locale.ROOT)) {
            case "always" -> "last_10";
            case "last_10" -> "never";
            default -> "always";
        };
    }

    private List<String> timerLore(String title) {
        return List.of("§7Visibility: always, last_10, never", "§7Currently adjusting: §f" + title);
    }

    private ItemStack icon(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        GuiCompat.setDisplayName(meta, name);
        if (lore != null && !lore.isEmpty()) GuiCompat.setLore(meta, lore);
        item.setItemMeta(meta);
        return item;
    }

    // -----------------------------------------------------------------
    // Supporting records and enums

    private enum MenuKey {
        MAIN,
        MODE_SELECT,
        MODE_SELECT_DIRECT,
        TEAM_MANAGEMENT,
        SETTINGS_HOME,
        SETTINGS_SWAP,
        SETTINGS_SAFETY,
        SETTINGS_HUNTER,
        POWERUPS_ROOT,
        POWERUPS_EFFECTS,
        POWERUPS_DURATION,
        DANGEROUS_BLOCKS,
        SETTINGS_WORLD_BORDER,
        SETTINGS_BOUNTY,
        SETTINGS_LAST_STAND,
        SETTINGS_SUDDEN_DEATH,
        SETTINGS_TASK,
        TASK_HOME,
        TASK_CUSTOM,
        TASK_POOL,
        TASK_ASSIGNMENTS,
        STATS_ROOT,
        STATS_ADVANCED,
        SETTINGS_VOICE_CHAT,
        SETTINGS_BROADCAST,
        SETTINGS_UI,
        KIT_MANAGER
    }

    public enum StatsParent {
        MAIN,
        SETTINGS
    }

    private interface MenuBuilder {
        MenuScreen build(MenuContext context);
    }

    private record MenuRequest(MenuKey key, Object data) {}

    private record MenuSession(MenuRequest request, MenuScreen screen, Inventory inventory) {}

    private record MenuScreen(String title, int size, List<MenuItem> items) {
        MenuItem button(String id) {
            for (MenuItem item : items) {
                if (item.id().equals(id)) return item;
            }
            return null;
        }
    }

    private record MenuItem(String id, int slot, Function<MenuContext, ItemStack> icon, Consumer<MenuClickContext> action) {}

    private static class MenuContext {
        private final GuiManager manager;
        private final Player player;
        private final MenuRequest request;

        MenuContext(GuiManager manager, Player player, MenuRequest request) {
            this.manager = manager;
            this.player = player;
            this.request = request;
        }

        public GuiManager manager() { return manager; }
        public Player player() { return player; }
        public MenuRequest request() { return request; }
    }

    private static final class MenuClickContext extends MenuContext {
        private final boolean shift;
        private final ClickType click;

        MenuClickContext(GuiManager manager, Player player, MenuRequest request, boolean shift, ClickType click) {
            super(manager, player, request);
            this.shift = shift;
            this.click = click;
        }

        public boolean shift() { return shift; }
        public ClickType click() { return click; }

        public void reopen() { manager().reopen(player()); }
    }

}
