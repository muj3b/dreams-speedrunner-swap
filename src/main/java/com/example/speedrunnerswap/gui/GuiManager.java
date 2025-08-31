package com.example.speedrunnerswap.gui;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import com.example.speedrunnerswap.models.PlayerState;
import org.bukkit.enchantments.Enchantment;

import java.util.ArrayList;
import java.util.List;

public class GuiManager {
    
    private final SpeedrunnerSwap plugin;
    
    public GuiManager(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
    }
    
    public void openMainMenu(Player player) {
        String title = plugin.getConfigManager().getGuiMainMenuTitle();
        int rows = plugin.getConfigManager().getGuiMainMenuRows();
        rows = Math.max(rows, 5); // ensure enough space for layout
        rows = clampRows(rows);
        
        Inventory inventory = Bukkit.createInventory(null, rows * 9, title);
        
        // Fill with glass panes for better visual organization
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
        
        // Team selector button
        List<String> teamSelectorLore = new ArrayList<>();
        teamSelectorLore.add("§7Click to open the team selection menu");
        teamSelectorLore.add("§7Here you can:");
        teamSelectorLore.add("§7• Assign players to teams");
        teamSelectorLore.add("§7• View current team assignments");
        teamSelectorLore.add("§7• Manage runner and hunter roles");
        ItemStack teamSelector = createItem(Material.PLAYER_HEAD, "§e§lTeam Selector", teamSelectorLore);
        inventory.setItem(11, teamSelector);
        
        // Settings button
        List<String> settingsLore = new ArrayList<>();
        settingsLore.add("§7Click to configure plugin settings");
        settingsLore.add("§7Manage:");
        settingsLore.add("§7• Swap intervals and type");
        settingsLore.add("§7• Safety features");
        settingsLore.add("§7• Tracking options");
        settingsLore.add("§7• Game mechanics");
        ItemStack settings = createItem(Material.COMPARATOR, "§a§lSettings", settingsLore);
        inventory.setItem(15, settings);
        
        // Game control buttons
        if (plugin.getGameManager().isGameRunning()) {
            if (plugin.getGameManager().isGamePaused()) {
                List<String> resumeLore = new ArrayList<>();
                resumeLore.add("§7Click to resume the game");
                resumeLore.add("§7This will:");
                resumeLore.add("§7• Reactivate the swap timer");
                resumeLore.add("§7• Allow runner movement");
                resumeLore.add("§7• Resume all game mechanics");
                ItemStack resume = createItem(Material.LIME_CONCRETE, "§a§lResume Game", resumeLore);
                inventory.setItem(22, createGlowingItem(resume));
            } else {
                List<String> pauseLore = new ArrayList<>();
                pauseLore.add("§7Click to pause the game");
                pauseLore.add("§7This will:");
                pauseLore.add("§7• Stop the swap timer");
                pauseLore.add("§7• Freeze current positions");
                pauseLore.add("§7• Maintain current game state");
                ItemStack pause = createItem(Material.YELLOW_CONCRETE, "§e§lPause Game", pauseLore);
                inventory.setItem(22, createGlowingItem(pause));
            }
            
            List<String> stopLore = new ArrayList<>();
            stopLore.add("§c§lWARNING: This will end the current game!");
            stopLore.add("§7This action:");
            stopLore.add("§7• Ends the current game session");
            stopLore.add("§7• Resets all player states");
            stopLore.add("§7• Returns players to spawn");
            ItemStack stop = createItem(Material.RED_CONCRETE, "§c§lStop Game", stopLore);
            inventory.setItem(31, createGlowingItem(stop));
        } else {
            List<String> startLore = new ArrayList<>();
            boolean canStart = plugin.getGameManager().canStartGame();
            if (canStart) {
                startLore.add("§7Click to start the game");
                startLore.add("§7Requirements met:");
                startLore.add("§a✔ Sufficient players");
                startLore.add("§a✔ Teams configured");
                startLore.add("");
                startLore.add("§7Game will begin immediately!");
            } else {
                startLore.add("§cCannot start game:");
                startLore.add("§7Requirements:");
                startLore.add(plugin.getGameManager().getRunners().size() > 0 ? "§a✔" : "§c✘" + " At least one runner");
                startLore.add(plugin.getGameManager().getHunters().size() > 0 ? "§a✔" : "§c✘" + " At least one hunter");
                startLore.add("");
                startLore.add("§7Configure teams to start!");
            }
            ItemStack start = createItem(Material.GREEN_CONCRETE, "§a§lStart Game", startLore);
            inventory.setItem(22, canStart ? createGlowingItem(start) : start);
        }
        
        // Status button with enhanced information
        ItemStack status = createItem(Material.COMPASS, "§b§lGame Status", getStatusLore());
        inventory.setItem(40, status);
        
        player.openInventory(inventory);
    }
    
    public void openTeamSelector(Player player) {
        String title = plugin.getConfigManager().getGuiTeamSelectorTitle();
        int rows = plugin.getConfigManager().getGuiTeamSelectorRows();
        rows = Math.max(rows, 4); // ensure space for selector + player heads row
        rows = clampRows(rows);
        
        Inventory inventory = Bukkit.createInventory(null, rows * 9, title);
        
        // Fill with glass panes for better visual organization
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
        
        // Back button with enhanced tooltip
        List<String> backLore = new ArrayList<>();
        backLore.add("§7Return to main menu");
        backLore.add("§7Current selections will be saved");
        ItemStack back = createItem(Material.ARROW, "§7§lBack", backLore);
        inventory.setItem(0, back);
        
        // Runner team button with enhanced information
        List<String> runnerLore = new ArrayList<>();
        runnerLore.add("§7Click to select players as runners");
        runnerLore.add("");
        runnerLore.add("§7Current Runners: §b" + plugin.getGameManager().getRunners().size());
        runnerLore.add("§7Role:");
        runnerLore.add("§7• Complete objectives");
        runnerLore.add("§7• Avoid hunters");
        runnerLore.add("§7• Swap between players");
        ItemStack runnerTeam = createItem(Material.DIAMOND_BOOTS, "§b§lRunners", runnerLore);
        inventory.setItem(2, plugin.getGameManager().getPlayerState(player).getSelectedTeam() == PlayerState.Team.RUNNER ? 
            createGlowingItem(runnerTeam) : createNormalItem(runnerTeam));
        
        // Hunter team button with enhanced information
        List<String> hunterLore = new ArrayList<>();
        hunterLore.add("§7Click to select players as hunters");
        hunterLore.add("");
        hunterLore.add("§7Current Hunters: §c" + plugin.getGameManager().getHunters().size());
        hunterLore.add("§7Role:");
        hunterLore.add("§7• Track runners");
        hunterLore.add("§7• Eliminate runners");
        hunterLore.add("§7• Prevent objectives");
        ItemStack hunterTeam = createItem(Material.IRON_SWORD, "§c§lHunters", hunterLore);
        inventory.setItem(6, plugin.getGameManager().getPlayerState(player).getSelectedTeam() == PlayerState.Team.HUNTER ? 
            createGlowingItem(hunterTeam) : createNormalItem(hunterTeam));
        
        // Team selection instructions
        List<String> instructionsLore = new ArrayList<>();
        instructionsLore.add("§7How to assign teams:");
        instructionsLore.add("§71. Select a team above");
        instructionsLore.add("§72. Click player heads below");
        instructionsLore.add("§73. Confirm your selections");
        ItemStack instructions = createItem(Material.BOOK, "§e§lInstructions", instructionsLore);
        inventory.setItem(4, instructions);
        
        // Player heads with enhanced information
        int slot = 18;
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (slot >= inventory.getSize()) break;
            
            ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) playerHead.getItemMeta();
            meta.setOwningPlayer(onlinePlayer);
            
            PlayerState.Team currentTeam = plugin.getGameManager().getPlayerState(onlinePlayer).getSelectedTeam();
            String teamColor = currentTeam == PlayerState.Team.RUNNER ? "§b" : 
                             currentTeam == PlayerState.Team.HUNTER ? "§c" : "§f";
            meta.setDisplayName(teamColor + onlinePlayer.getName());
            
            List<String> lore = new ArrayList<>();
            if (plugin.getGameManager().isRunner(onlinePlayer)) {
                lore.add("§bCurrently a Runner");
                lore.add("§7Click while Hunter team is selected");
                lore.add("§7to change to Hunter");
            } else if (plugin.getGameManager().isHunter(onlinePlayer)) {
                lore.add("§cCurrently a Hunter");
                lore.add("§7Click while Runner team is selected");
                lore.add("§7to change to Runner");
            } else {
                lore.add("§7No team assigned");
                lore.add("§7Select a team above first");
                lore.add("§7then click to assign");
            }
            
            // Add online status
            lore.add("");
            lore.add(onlinePlayer.isOnline() ? "§a✔ Online" : "§c✘ Offline");
            
            meta.setLore(lore);
            playerHead.setItemMeta(meta);
            
            // Add glow effect for current team members
            if (currentTeam != PlayerState.Team.NONE) {
                playerHead = createGlowingItem(playerHead);
            }
            
            inventory.setItem(slot++, playerHead);
        }
        
        player.openInventory(inventory);
    }
    
    public void openSettingsMenu(Player player) {
        String title = plugin.getConfigManager().getGuiSettingsTitle();
        int rows = plugin.getConfigManager().getGuiSettingsRows();
        rows = Math.max(rows, 4); // ensure enough slots for layout
        rows = clampRows(rows);
        
        Inventory inventory = Bukkit.createInventory(null, rows * 9, title);
        
        // Fill with glass panes for better organization
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
        
        // Back button with enhanced tooltip
        List<String> backLore = new ArrayList<>();
        backLore.add("§7Return to main menu");
        backLore.add("§7Settings will be saved automatically");
        ItemStack back = createItem(Material.ARROW, "§7§lBack", backLore);
        inventory.setItem(0, back);
        
        // Swap Settings Section
        List<String> swapHeaderLore = new ArrayList<>();
        swapHeaderLore.add("§7Configure swap mechanics:");
        swapHeaderLore.add("§7• Intervals and timing");
        swapHeaderLore.add("§7• Randomization options");
        swapHeaderLore.add("§7• Safety features");
        ItemStack swapHeader = createItem(Material.BOOK, "§6§lSwap Settings", swapHeaderLore);
        inventory.setItem(9, swapHeader);
        
        // Enhanced swap interval settings
        boolean randomize = plugin.getConfigManager().isSwapRandomized();
        List<String> intervalTypeLore = new ArrayList<>();
        intervalTypeLore.add("§7Current: " + (randomize ? "§aRandom" : "§bFixed"));
        intervalTypeLore.add("");
        intervalTypeLore.add("§7Random Mode:");
        intervalTypeLore.add("§7• Varies interval within range");
        intervalTypeLore.add("§7• Adds unpredictability");
        intervalTypeLore.add("");
        intervalTypeLore.add("§7Fixed Mode:");
        intervalTypeLore.add("§7• Consistent timing");
        intervalTypeLore.add("§7• Predictable swaps");
        ItemStack swapIntervalType = createItem(
                randomize ? Material.CLOCK : Material.REPEATER,
                "§e§lSwap Type: " + (randomize ? "§aRandom" : "§bFixed"),
                intervalTypeLore);
        inventory.setItem(10, swapIntervalType);
        
        int interval = plugin.getConfigManager().getSwapInterval();
        List<String> intervalLore = new ArrayList<>();
        intervalLore.add("§7Current: §f" + interval + "s");
        intervalLore.add("");
        intervalLore.add("§7Adjust using:");
        intervalLore.add("§a▲ §7Left-click: +30s");
        intervalLore.add("§c▼ §7Right-click: -30s");
        intervalLore.add("§e⟲ §7Shift-click: Reset");
        ItemStack swapInterval = createItem(Material.CLOCK, "§e§lSwap Interval", intervalLore);
        inventory.setItem(11, swapInterval);
        
        // Enhanced safe swap settings
        boolean safeSwap = plugin.getConfigManager().isSafeSwapEnabled();
        List<String> safeSwapLore = new ArrayList<>();
        safeSwapLore.add("§7Current: " + (safeSwap ? "§aEnabled" : "§cDisabled"));
        safeSwapLore.add("");
        safeSwapLore.add("§7Safety Features:");
        safeSwapLore.add("§7• Combat protection");
        safeSwapLore.add("§7• Fall damage prevention");
        safeSwapLore.add("§7• Void protection");
        ItemStack safeSwapToggle = createItem(
                safeSwap ? Material.TOTEM_OF_UNDYING : Material.BARRIER,
                "§e§lSafe Swap: " + (safeSwap ? "§aEnabled" : "§cDisabled"),
                safeSwapLore);
        inventory.setItem(12, safeSwapToggle);
        
        player.openInventory(inventory);
    }
    
    private int clampRows(int rows) {
        if (rows < 1) return 1;
        if (rows > 6) return 6;
        return rows;
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);

        if (lore.length > 0) {
            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(line);
            }
            meta.setLore(loreList);
        }

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createGlowingItem(ItemStack item) {
        ItemStack glowingItem = item.clone();
        ItemMeta meta = glowingItem.getItemMeta();
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        glowingItem.setItemMeta(meta);
        return glowingItem;
    }

    private ItemStack createNormalItem(ItemStack item) {
        ItemStack normalItem = item.clone();
        ItemMeta meta = normalItem.getItemMeta();
        meta.removeEnchant(Enchantment.UNBREAKING);
        meta.removeItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        normalItem.setItemMeta(meta);
        return normalItem;
    }
    
    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        
        if (lore != null && !lore.isEmpty()) {
            meta.setLore(lore);
        }
        
        item.setItemMeta(meta);
        return item;
    }
    
    private List<String> getStatusLore() {
        List<String> lore = new ArrayList<>();
        
        lore.add("§7Game Running: " + (plugin.getGameManager().isGameRunning() ? "§aYes" : "§cNo"));
        lore.add("§7Game Paused: " + (plugin.getGameManager().isGamePaused() ? "§eYes" : "§aNo"));
        
        if (plugin.getGameManager().isGameRunning()) {
            Player activeRunner = plugin.getGameManager().getActiveRunner();
            lore.add("§7Active Runner: §f" + (activeRunner != null ? activeRunner.getName() : "None"));
            lore.add("§7Next Swap: §f" + plugin.getGameManager().getTimeUntilNextSwap() + "s");
            
            lore.add("");
            lore.add("§bRunners:");
            for (Player runner : plugin.getGameManager().getRunners()) {
                lore.add("§7- §f" + runner.getName());
            }
            
            lore.add("");
            lore.add("§cHunters:");
            for (Player hunter : plugin.getGameManager().getHunters()) {
                lore.add("§7- §f" + hunter.getName());
            }
        }
        
        return lore;
    }
    
    // Helper methods for menu identification
    public boolean isMainMenu(Inventory inventory) {
        return inventory != null && inventory.getHolder() == null && 
               inventory.equals(inventory.getViewers().get(0).getOpenInventory().getTopInventory()) &&
               plugin.getConfigManager().getGuiMainMenuTitle().equals(inventory.getViewers().get(0).getOpenInventory().getTitle());
    }
    
    public boolean isTeamSelector(Inventory inventory) {
        return inventory != null && inventory.getHolder() == null && 
               inventory.equals(inventory.getViewers().get(0).getOpenInventory().getTopInventory()) &&
               plugin.getConfigManager().getGuiTeamSelectorTitle().equals(inventory.getViewers().get(0).getOpenInventory().getTitle());
    }
    
    public boolean isSettingsMenu(Inventory inventory) {
        return inventory != null && inventory.getHolder() == null && 
               inventory.equals(inventory.getViewers().get(0).getOpenInventory().getTopInventory()) &&
               plugin.getConfigManager().getGuiSettingsTitle().equals(inventory.getViewers().get(0).getOpenInventory().getTitle());
    }
    
    // Helper methods for item identification
    public boolean isBackButton(ItemStack item) {
        return item != null && item.getType() == Material.ARROW && 
               item.getItemMeta().getDisplayName().equals("§7§lBack");
    }
    
    public boolean isStartButton(ItemStack item) {
        return item != null && item.getType() == Material.GREEN_CONCRETE && 
               item.getItemMeta().getDisplayName().equals("§a§lStart Game");
    }
    
    public boolean isStopButton(ItemStack item) {
        return item != null && item.getType() == Material.RED_CONCRETE && 
               item.getItemMeta().getDisplayName().equals("§c§lStop Game");
    }
    
    public boolean isPauseButton(ItemStack item) {
        return item != null && item.getType() == Material.YELLOW_CONCRETE && 
               item.getItemMeta().getDisplayName().equals("§e§lPause Game");
    }
    
    public boolean isResumeButton(ItemStack item) {
        return item != null && item.getType() == Material.LIME_CONCRETE && 
               item.getItemMeta().getDisplayName().equals("§a§lResume Game");
    }
    
    public boolean isTeamSelectorButton(ItemStack item) {
        return item != null && item.getType() == Material.PLAYER_HEAD && 
               item.getItemMeta().getDisplayName().equals("§e§lTeam Selector");
    }
    
    public boolean isSettingsButton(ItemStack item) {
        return item != null && item.getType() == Material.COMPARATOR && 
               item.getItemMeta().getDisplayName().equals("§a§lSettings");
    }
    
    public boolean isRunnerTeamButton(ItemStack item) {
        return item != null && item.getType() == Material.DIAMOND_BOOTS && 
               item.getItemMeta().getDisplayName().equals("§b§lRunners");
    }
    
    public boolean isHunterTeamButton(ItemStack item) {
        return item != null && item.getType() == Material.IRON_SWORD && 
               item.getItemMeta().getDisplayName().equals("§c§lHunters");
    }
    
    public boolean isPlayerHead(ItemStack item) {
        return item != null && item.getType() == Material.PLAYER_HEAD;
    }
    
    // Helper methods for settings menu
    public boolean isSwapIntervalButton(ItemStack item) {
        return item != null && item.getType() == Material.CLOCK && 
               item.getItemMeta().getDisplayName().startsWith("§e§lSwap Interval");
    }
    
    public boolean isRandomizeSwapButton(ItemStack item) {
        return item != null && (item.getType() == Material.CLOCK || item.getType() == Material.REPEATER) && 
               item.getItemMeta().getDisplayName().startsWith("§e§lSwap Type");
    }
    
    public boolean isSafeSwapButton(ItemStack item) {
        return item != null && (item.getType() == Material.TOTEM_OF_UNDYING || item.getType() == Material.BARRIER) && 
               item.getItemMeta().getDisplayName().startsWith("§e§lSafe Swap");
    }
    
    // Helper methods for team management
    public void setPlayerTeam(Player player, PlayerState.Team team) {
        plugin.getGameManager().getPlayerState(player).setSelectedTeam(team);
        updateTeamSelectors();
    }
    
    public void updateTeamSelectors() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isTeamSelector(player.getOpenInventory().getTopInventory())) {
                openTeamSelector(player);
            }
        }
    }
    
    // Helper methods for game state
    public void updateMainMenus() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isMainMenu(player.getOpenInventory().getTopInventory())) {
                openMainMenu(player);
            }
        }
    }
    
    public void updateSettingsMenus() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isSettingsMenu(player.getOpenInventory().getTopInventory())) {
                openSettingsMenu(player);
            }
        }
    }
    
    // Helper method for time formatting
    private String formatTime(long seconds) {
        if (seconds < 0) return "0:00";
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }
    
    // Fix for missing getSelectedTeam method
    public PlayerState.Team getSelectedTeam(Player player) {
        return plugin.getGameManager().getPlayerState(player).getSelectedTeam();
    }
    
    // Fix for missing isSwapRandomized method
    public boolean isSwapRandomized() {
        return plugin.getConfigManager().isSwapRandomized();
    }
}