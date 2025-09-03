package com.example.speedrunnerswap.gui;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import com.example.speedrunnerswap.models.Team;
import java.util.logging.Level;
import org.bukkit.enchantments.Enchantment;

import java.util.ArrayList;
import java.util.List;

public class GuiManager {
    
    private final SpeedrunnerSwap plugin;
    private final String BACK_BUTTON_TITLE = "§c§lBack to Main Menu";
    
    public GuiManager(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
    }

    public String formatTime(int seconds) {
        int minutes = seconds / 60;
        int hours = minutes / 60;
        minutes %= 60;
        seconds %= 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    public void openPositiveEffectsMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 36, Component.text("§a§lPositive Effects"));
        
        // Border-only filler for cleaner look
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        fillBorder(inventory, filler);

        // Add all available positive effects
        List<ItemStack> effectItems = new ArrayList<>();
        effectItems.add(createEffectItem(Material.POTION, "Speed", "SPEED"));
        effectItems.add(createEffectItem(Material.POTION, "Jump Boost", "JUMP"));
        effectItems.add(createEffectItem(Material.POTION, "Strength", "INCREASE_DAMAGE"));
        effectItems.add(createEffectItem(Material.POTION, "Regeneration", "REGENERATION"));
        effectItems.add(createEffectItem(Material.POTION, "Resistance", "DAMAGE_RESISTANCE"));
        effectItems.add(createEffectItem(Material.POTION, "Fire Resistance", "FIRE_RESISTANCE"));
        effectItems.add(createEffectItem(Material.POTION, "Water Breathing", "WATER_BREATHING"));
        effectItems.add(createEffectItem(Material.POTION, "Night Vision", "NIGHT_VISION"));

        for (int i = 0; i < effectItems.size(); i++) {
            inventory.setItem(10 + i, effectItems.get(i));
        }

        // Back button
        inventory.setItem(35, createItem(Material.BARRIER, BACK_BUTTON_TITLE));

        player.openInventory(inventory);
    }

    public void openNegativeEffectsMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 36, Component.text("§c§lNegative Effects"));
        
        // Border-only filler for cleaner look
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        fillBorder(inventory, filler);

        // Add all available negative effects
        List<ItemStack> effectItems = new ArrayList<>();
        effectItems.add(createEffectItem(Material.POTION, "Slowness", "SLOW"));
        effectItems.add(createEffectItem(Material.POTION, "Weakness", "WEAKNESS"));
        effectItems.add(createEffectItem(Material.POTION, "Poison", "POISON"));
        effectItems.add(createEffectItem(Material.POTION, "Blindness", "BLINDNESS"));
        effectItems.add(createEffectItem(Material.POTION, "Hunger", "HUNGER"));
        effectItems.add(createEffectItem(Material.POTION, "Mining Fatigue", "SLOW_DIGGING"));
        effectItems.add(createEffectItem(Material.POTION, "Nausea", "CONFUSION"));
        effectItems.add(createEffectItem(Material.POTION, "Glowing", "GLOWING"));

        for (int i = 0; i < effectItems.size(); i++) {
            inventory.setItem(10 + i, effectItems.get(i));
        }

        // Back button
        inventory.setItem(35, createItem(Material.BARRIER, BACK_BUTTON_TITLE));

        player.openInventory(inventory);
    }

    public ItemStack createEffectItem(Material material, String displayName, String effectId) {
        List<String> lore = new ArrayList<>();
        lore.add("§7Effect ID: §f" + effectId);
        lore.add("§7Click to toggle this effect");
        boolean isEnabled = plugin.getConfig().getStringList("power_ups.good_effects").contains(effectId) ||
                          plugin.getConfig().getStringList("power_ups.bad_effects").contains(effectId);
        lore.add(isEnabled ? "§aCurrently enabled" : "§cCurrently disabled");
        
        return createItem(material, "§e§l" + displayName, lore);
    }

    public void openPowerUpsMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 36, Component.text("§e§lPower-ups Menu"));
        
        // Border-only filler for cleaner look
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        fillBorder(inventory, filler);

        // Toggle button
        List<String> powerUpToggleLore = new ArrayList<>();
        boolean powerUpsEnabled = plugin.getConfigManager().isPowerUpsEnabled();
        powerUpToggleLore.add("§7Current status: " + (powerUpsEnabled ? "§aEnabled" : "§cDisabled"));
        powerUpToggleLore.add("§7Click to toggle");
        ItemStack toggleItem = createItem(
            powerUpsEnabled ? Material.LIME_DYE : Material.GRAY_DYE,
            "§e§lToggle Power-ups",
            powerUpToggleLore
        );
        inventory.setItem(4, toggleItem);

        // Positive effects section (detailed list)
        List<String> positiveEffectsLore = new ArrayList<>();
        positiveEffectsLore.add("§7Current effects:");
        for (String effect : plugin.getConfig().getStringList("power_ups.good_effects")) {
            positiveEffectsLore.add("§a• " + effect.toLowerCase());
        }
        positiveEffectsLore.add("");
        positiveEffectsLore.add("§7Click to modify");
        ItemStack goodEffectsItem = createItem(Material.SPLASH_POTION, "§a§lPositive Effects", positiveEffectsLore);
        inventory.setItem(11, goodEffectsItem);

        // Negative effects section (detailed list)
        List<String> negativeEffectsLore = new ArrayList<>();
        negativeEffectsLore.add("§7Current effects:");
        for (String effect : plugin.getConfig().getStringList("power_ups.bad_effects")) {
            negativeEffectsLore.add("§c• " + effect.toLowerCase());
        }
        negativeEffectsLore.add("");
        negativeEffectsLore.add("§7Click to modify");
        ItemStack badEffectsItem = createItem(Material.LINGERING_POTION, "§c§lNegative Effects", negativeEffectsLore);
        inventory.setItem(15, badEffectsItem);

        // Duration settings
        int minSec = plugin.getConfigManager().getPowerUpsMinSeconds();
        int maxSec = plugin.getConfigManager().getPowerUpsMaxSeconds();
        int minLvl = plugin.getConfigManager().getPowerUpsMinLevel();
        int maxLvl = plugin.getConfigManager().getPowerUpsMaxLevel();
        List<String> durationLore = new ArrayList<>();
        durationLore.add("§7Duration: §e" + minSec + "-" + maxSec + "s");
        durationLore.add("§7Level: §e" + minLvl + "-" + maxLvl);
        durationLore.add("");
        durationLore.add("§7Click to modify timings");
        ItemStack durationItem = createItem(Material.CLOCK, "§6§lEffect Durations", durationLore);
        inventory.setItem(22, durationItem);

        // Back button
        inventory.setItem(31, createItem(Material.BARRIER, BACK_BUTTON_TITLE));

        player.openInventory(inventory);
    }

    public void openPowerUpDurationsMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("§6§lPower-up Durations"));
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        fillBorder(inv, filler);

        ItemStack back = createItem(Material.ARROW, "§7§lBack", List.of("§7Return to power-ups"));
        inv.setItem(0, back);

        int minSec = plugin.getConfigManager().getPowerUpsMinSeconds();
        int maxSec = plugin.getConfigManager().getPowerUpsMaxSeconds();
        int minLvl = plugin.getConfigManager().getPowerUpsMinLevel();
        int maxLvl = plugin.getConfigManager().getPowerUpsMaxLevel();

        ItemStack minDur = createItem(Material.CLOCK, "§e§lMin Duration (s)", List.of("§7Current: §f" + minSec, "§7Left/Right: ±5"));
        ItemStack maxDur = createItem(Material.CLOCK, "§e§lMax Duration (s)", List.of("§7Current: §f" + maxSec, "§7Left/Right: ±5"));
        ItemStack minLvlItem = createItem(Material.EXPERIENCE_BOTTLE, "§e§lMin Level", List.of("§7Current: §f" + minLvl, "§7Left/Right: ±1"));
        ItemStack maxLvlItem = createItem(Material.EXPERIENCE_BOTTLE, "§e§lMax Level", List.of("§7Current: §f" + maxLvl, "§7Left/Right: ±1"));

        inv.setItem(10, minDur);
        inv.setItem(12, maxDur);
        inv.setItem(14, minLvlItem);
        inv.setItem(16, maxLvlItem);

        player.openInventory(inv);
    }

    public void openWorldBorderMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, Component.text("§c§lWorld Border Settings"));
        
        // Border-only filler for cleaner look
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        fillBorder(inventory, filler);

        // Toggle button
        boolean isEnabled = plugin.getConfig().getBoolean("world_border.enabled", true);
        List<String> toggleLore = new ArrayList<>();
        toggleLore.add("§7Current status: " + (isEnabled ? "§aEnabled" : "§cDisabled"));
        toggleLore.add("§7Click to toggle");
        ItemStack toggleItem = createItem(
            isEnabled ? Material.LIME_DYE : Material.GRAY_DYE,
            "§e§lToggle World Border",
            toggleLore
        );
        inventory.setItem(4, toggleItem);

        // Initial size setting
        int initialSize = plugin.getConfig().getInt("world_border.initial_size", 2000);
        List<String> initialSizeLore = new ArrayList<>();
        initialSizeLore.add("§7Current size: §e" + initialSize + " blocks");
        initialSizeLore.add("§7Left-click: §a+100 blocks");
        initialSizeLore.add("§7Right-click: §c-100 blocks");
        initialSizeLore.add("§7Shift + Left-click: §a+500 blocks");
        initialSizeLore.add("§7Shift + Right-click: §c-500 blocks");
        ItemStack initialSizeItem = createItem(Material.GRASS_BLOCK, "§a§lInitial Border Size", initialSizeLore);
        inventory.setItem(11, initialSizeItem);

        // Final size setting
        int finalSize = plugin.getConfig().getInt("world_border.final_size", 100);
        List<String> finalSizeLore = new ArrayList<>();
        finalSizeLore.add("§7Current size: §e" + finalSize + " blocks");
        finalSizeLore.add("§7Left-click: §a+50 blocks");
        finalSizeLore.add("§7Right-click: §c-50 blocks");
        finalSizeLore.add("§7Shift + Left-click: §a+100 blocks");
        finalSizeLore.add("§7Shift + Right-click: §c-100 blocks");
        ItemStack finalSizeItem = createItem(Material.BEDROCK, "§c§lFinal Border Size", finalSizeLore);
        inventory.setItem(13, finalSizeItem);

        // Shrink duration setting
        int shrinkDuration = plugin.getConfig().getInt("world_border.shrink_duration", 1800);
        List<String> durationLore = new ArrayList<>();
        durationLore.add("§7Current duration: §e" + formatTime(shrinkDuration));
        durationLore.add("§7Left-click: §a+5 minutes");
        durationLore.add("§7Right-click: §c-5 minutes");
        durationLore.add("§7Shift + Left-click: §a+15 minutes");
        durationLore.add("§7Shift + Right-click: §c-15 minutes");
        ItemStack durationItem = createItem(Material.CLOCK, "§6§lShrink Duration", durationLore);
        inventory.setItem(15, durationItem);

        // Warning settings
        int warningDistance = plugin.getConfig().getInt("world_border.warning_distance", 50);
        List<String> warningLore = new ArrayList<>();
        warningLore.add("§7Warning distance: §e" + warningDistance + " blocks");
        warningLore.add("§7Warning interval: §e" + 
            plugin.getConfig().getInt("world_border.warning_interval", 300) + " seconds");
        warningLore.add("§7Click to modify warnings");
        ItemStack warningItem = createItem(Material.BELL, "§e§lWarning Settings", warningLore);
        inventory.setItem(22, warningItem);

        // Back button
        inventory.setItem(26, createItem(Material.BARRIER, BACK_BUTTON_TITLE));

        player.openInventory(inventory);
    }

    public void openMainMenu(Player player) {
        try {
            String title = plugin.getConfigManager().getGuiMainMenuTitle();
        int rows = plugin.getConfigManager().getGuiMainMenuRows();
        rows = Math.max(rows, 6); // ensure enough space for layout (needs at least 54 slots)
            rows = clampRows(rows);

            Inventory inventory = Bukkit.createInventory(null, rows * 9, Component.text(title));

            // Border-only filler for better visual organization
            ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
            fillBorder(inventory, filler);

            // Team selector button (Top row)
            List<String> teamSelectorLore = new ArrayList<>();
            teamSelectorLore.add("§7Click to open the team selection menu");
            teamSelectorLore.add("§7Here you can:");
            teamSelectorLore.add("§7• Assign players to teams");
            teamSelectorLore.add("§7• View current team assignments");
            teamSelectorLore.add("§7• Manage runner and hunter roles");
            ItemStack teamSelector = createGuiButton(Material.PLAYER_HEAD, "§e§lTeam Selector", teamSelectorLore, "team_selector");
            inventory.setItem(10, teamSelector);

    // World Border Settings (Top row)
        List<String> borderLore = new ArrayList<>();
        borderLore.add("§7Configure world border settings:");
        borderLore.add("§7• Set initial and final size");
        borderLore.add("§7• Adjust shrink duration");
        borderLore.add("§7• Toggle border shrinking");
        ItemStack borderSettings = createGuiButton(Material.BARRIER, "§c§lWorld Border", borderLore, "world_border");
        inventory.setItem(12, borderSettings);

        // Power-ups Settings (Top row)
        List<String> powerupsLore = new ArrayList<>();
        powerupsLore.add("§7Configure power-up settings:");
        powerupsLore.add("§7• Enable/disable power-ups");
        powerupsLore.add("§7• Customize effects");
        powerupsLore.add("§7• Adjust durations");
        ItemStack powerupsSettings = createGuiButton(Material.POTION, "§d§lPower-ups", powerupsLore, "power_ups");
        inventory.setItem(14, powerupsSettings);

        // Kit Settings (Top row)
        List<String> kitsLore = new ArrayList<>();
        kitsLore.add("§7Configure custom kits:");
        kitsLore.add("§7• Edit runner kits");
        kitsLore.add("§7• Edit hunter kits");
        kitsLore.add("§7• Manage equipment");
        ItemStack kitsSettings = createGuiButton(Material.DIAMOND_CHESTPLATE, "§b§lKits", kitsLore, "kits");
        inventory.setItem(16, kitsSettings);

        // Bounty System (Middle row)
        List<String> bountyLore = new ArrayList<>();
        bountyLore.add("§7Configure bounty system:");
        bountyLore.add("§7• Set bounty rewards");
        bountyLore.add("§7• Adjust durations");
        bountyLore.add("§7• Manage effects");
        ItemStack bountySettings = createGuiButton(Material.GOLDEN_APPLE, "§6§lBounty System", bountyLore, "bounty");
        inventory.setItem(28, bountySettings);

        // Last Stand Settings (Middle row)
        List<String> lastStandLore = new ArrayList<>();
        lastStandLore.add("§7Configure Last Stand mode:");
        lastStandLore.add("§7• Set buff strengths");
        lastStandLore.add("§7• Adjust duration");
        lastStandLore.add("§7• Toggle feature");
        ItemStack lastStandSettings = createGuiButton(Material.TOTEM_OF_UNDYING, "§e§lLast Stand", lastStandLore, "last_stand");
        inventory.setItem(30, lastStandSettings);

        // Compass Settings (Middle row)
        List<String> compassLore = new ArrayList<>();
        compassLore.add("§7Configure compass settings:");
        compassLore.add("§7• Set jamming duration");
        compassLore.add("§7• Adjust tracking");
        compassLore.add("§7• Toggle features");
        ItemStack compassSettings = createGuiButton(Material.COMPASS, "§9§lCompass Settings", compassLore, "compass");
        inventory.setItem(32, compassSettings);

        // Sudden Death Settings (Middle row)
        List<String> suddenDeathLore = new ArrayList<>();
        suddenDeathLore.add("§7Configure Sudden Death mode:");
        suddenDeathLore.add("§7• Set activation time");
        suddenDeathLore.add("§7• Customize effects");
        suddenDeathLore.add("§7• Set arena location");
        ItemStack suddenDeathSettings = createGuiButton(Material.DRAGON_HEAD, "§4§lSudden Death", suddenDeathLore, "sudden_death");
        inventory.setItem(34, suddenDeathSettings);

        // Stats Settings (Bottom row)
        List<String> statsLore = new ArrayList<>();
        statsLore.add("§7Configure statistics:");
        statsLore.add("§7• Toggle stat tracking");
        statsLore.add("§7• Set display options");
        statsLore.add("§7• View current stats");
        ItemStack statsSettings = createGuiButton(Material.BOOK, "§a§lStatistics", statsLore, "statistics");
        inventory.setItem(48, statsSettings);
        
        // Settings button
        List<String> settingsLore = new ArrayList<>();
        settingsLore.add("§7Click to configure plugin settings");
        settingsLore.add("§7Manage:");
        settingsLore.add("§7• Swap intervals and type");
        settingsLore.add("§7• Safety features");
        settingsLore.add("§7• Tracking options");
        settingsLore.add("§7• Game mechanics");
        ItemStack settings = createGuiButton(Material.COMPARATOR, "§a§lSettings", settingsLore, "settings");
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
        } catch (Exception e) {
            // Log and notify player so we can see full stacktrace in server logs
            plugin.getLogger().log(Level.SEVERE, "Failed to open main GUI for player " + (player == null ? "UNKNOWN" : player.getName()), e);
            if (player != null && player.isOnline()) {
                player.sendMessage("§cFailed to open menu due to an internal error. Check server logs for details.");
            }
        }
    }
    
    public void openTeamSelector(Player player) {
        String title = plugin.getConfigManager().getGuiTeamSelectorTitle();
        int rows = plugin.getConfigManager().getGuiTeamSelectorRows();
        rows = Math.max(rows, 4); // ensure space for selector + player heads row
        rows = clampRows(rows);
        
        Inventory inventory = Bukkit.createInventory(null, rows * 9, Component.text(title));
        
        // Border-only filler for better visual organization
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        fillBorder(inventory, filler);
        
        // Back button with enhanced tooltip
        List<String> backLore = new ArrayList<>();
        backLore.add("§7Return to main menu");
        backLore.add("§7Current selections will be saved");
        ItemStack back = createItem(Material.ARROW, "§7§lBack", backLore);
        inventory.setItem(0, back);
        
        // Clear teams quick action
        ItemStack clearTeams = createGuiButton(Material.BARRIER, "§c§lClear All Teams", List.of(
            "§7Remove all existing team assignments"
        ), "clear_teams");
        inventory.setItem(8, clearTeams);
        
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
        inventory.setItem(2, plugin.getGameManager().getPlayerState(player).getSelectedTeam() == Team.RUNNER ? 
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
        inventory.setItem(6, plugin.getGameManager().getPlayerState(player).getSelectedTeam() == Team.HUNTER ? 
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
            
            Team currentTeam = plugin.getGameManager().getPlayerState(onlinePlayer).getSelectedTeam();
            // Apply team color to player name
            net.kyori.adventure.text.format.TextColor nameColor = currentTeam == Team.RUNNER ?
                net.kyori.adventure.text.format.NamedTextColor.AQUA :
                currentTeam == Team.HUNTER ?
                net.kyori.adventure.text.format.NamedTextColor.RED :
                net.kyori.adventure.text.format.NamedTextColor.WHITE;
            meta.displayName(net.kyori.adventure.text.Component.text(onlinePlayer.getName()).color(nameColor));
            
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
            
            List<net.kyori.adventure.text.Component> componentLore = new ArrayList<>();
            for (String line : lore) {
                componentLore.add(net.kyori.adventure.text.Component.text(line));
            }
            meta.lore(componentLore);
            playerHead.setItemMeta(meta);
            
            // Add glow effect for current team members
            if (currentTeam != Team.NONE) {
                playerHead = createGlowingItem(playerHead);
            }
            
            inventory.setItem(slot++, playerHead);
        }
        
        player.openInventory(inventory);
    }
    
    public void openSettingsMenu(Player player) {
        String title = plugin.getConfigManager().getGuiSettingsTitle();
        int rows = plugin.getConfigManager().getGuiSettingsRows();
        rows = Math.max(rows, 6); // ensure enough slots for layout
        rows = clampRows(rows);
        
        Inventory inventory = Bukkit.createInventory(null, rows * 9, Component.text(title));
        
        // Border-only filler for cleaner layout
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        fillBorder(inventory, filler);
        
        // Back button with enhanced tooltip
        List<String> backLore = new ArrayList<>();
        backLore.add("§7Return to main menu");
        backLore.add("§7Settings will be saved automatically");
        ItemStack back = createItem(Material.ARROW, "§7§lBack", backLore);

    // Tracker Settings Header
    List<String> trackerHeaderLore = new ArrayList<>();
    trackerHeaderLore.add("§7Configure compass tracking:");
    trackerHeaderLore.add("§7• Hunters receive a compass to track active runner");
    ItemStack trackerHeader = createItem(Material.BOOK, "§6§lTracker Settings", trackerHeaderLore);
    inventory.setItem(26, trackerHeader);
    inventory.setItem(0, back);
        
        // Swap Settings Section
        List<String> swapHeaderLore = new ArrayList<>();
        swapHeaderLore.add("§7Configure swap mechanics:");
        swapHeaderLore.add("§7• Intervals and timing");
        swapHeaderLore.add("§7• Randomization options");
        swapHeaderLore.add("§7• Safety features");
        swapHeaderLore.add("§7• Hover over options for details");
        ItemStack swapHeader = createItem(Material.BOOK, "§6§lSwap Settings", swapHeaderLore);
        inventory.setItem(9, swapHeader);
        
        // Enhanced swap interval settings with unique button IDs
        boolean isRandomized = plugin.getConfigManager().isSwapRandomized();
        List<String> randomSwapLore = new ArrayList<>();
        randomSwapLore.add("§7Current: " + (isRandomized ? "§aRandom" : "§bFixed"));
        randomSwapLore.add("");
        randomSwapLore.add("§7Random Mode:");
        randomSwapLore.add("§7• Varies interval within range");
        randomSwapLore.add("§7• Adds unpredictability");
        randomSwapLore.add("");
        randomSwapLore.add("§7Fixed Mode:");
        randomSwapLore.add("§7• Consistent timing");
        randomSwapLore.add("§7• Predictable swaps");
        ItemStack swapTypeButton = createGuiButton(
                isRandomized ? Material.CLOCK : Material.REPEATER,
                "§e§lSwap Type: " + (isRandomized ? "§aRandom" : "§bFixed"),
                randomSwapLore,
                "random_swaps");
        inventory.setItem(10, swapTypeButton);

        // Swap interval button
        int currentInterval = plugin.getConfigManager().getSwapInterval();
        List<String> swapIntervalLore = new ArrayList<>();
        swapIntervalLore.add("§7Current: §e" + currentInterval + " seconds");
        swapIntervalLore.add("");
        swapIntervalLore.add("§7Click to change");
        swapIntervalLore.add("§7• Left-click: +30s");
        swapIntervalLore.add("§7• Right-click: -30s");
        swapIntervalLore.add("§7• Shift+click: ±60s");
        ItemStack intervalButton = createGuiButton(
                Material.CLOCK,
                "§e§lSwap Interval",
                swapIntervalLore,
                "swap_interval");
        inventory.setItem(11, intervalButton);

        // Safe swap button
        List<String> safeSwapLore = new ArrayList<>();
        boolean isSafeSwapsEnabled = plugin.getConfigManager().isSafeSwapEnabled();
        safeSwapLore.add("§7Current: " + (isSafeSwapsEnabled ? "§aEnabled" : "§cDisabled"));
        safeSwapLore.add("");
        safeSwapLore.add("§7When enabled:");
        safeSwapLore.add("§7• Checks for safe landing");
        safeSwapLore.add("§7• Prevents void/lava deaths");
        safeSwapLore.add("§7• May delay swaps slightly");
        ItemStack safeButton = createGuiButton(
                isSafeSwapsEnabled ? Material.TOTEM_OF_UNDYING : Material.BARRIER,
                "§e§lSafe Swaps: " + (isSafeSwapsEnabled ? "§aEnabled" : "§cDisabled"),
                safeSwapLore,
                "safe_swaps");
        inventory.setItem(12, safeButton);
        
        // Pause on disconnect toggle
        boolean pauseOnDisconnect = plugin.getConfigManager().isPauseOnDisconnect();
        ItemStack pauseToggle = createGuiButton(
                pauseOnDisconnect ? Material.REDSTONE_TORCH : Material.LEVER,
                "§e§lPause On Disconnect: " + (pauseOnDisconnect ? "§aEnabled" : "§cDisabled"),
                List.of("§7Pause game if active runner disconnects"),
                "pause_on_disconnect");
        inventory.setItem(16, pauseToggle);

        // Tracker toggle
        boolean trackerEnabled = plugin.getConfigManager().isTrackerEnabled();
        List<String> trackerToggleLore = new ArrayList<>();
        trackerToggleLore.add("§7Current: " + (trackerEnabled ? "§aEnabled" : "§cDisabled"));
        trackerToggleLore.add("§7Hunters receive tracking compasses");
        ItemStack trackerToggle = createGuiButton(
                trackerEnabled ? Material.COMPASS : Material.GRAY_DYE,
                "§e§lTracker: " + (trackerEnabled ? "§aEnabled" : "§cDisabled"),
                trackerToggleLore,
                "tracker_toggle");
        inventory.setItem(13, trackerToggle);

        // Hunter Swap toggle and interval
        boolean hunterSwapEnabled = plugin.getConfigManager().isHunterSwapEnabled();
        ItemStack hunterSwapToggle = createGuiButton(
                hunterSwapEnabled ? Material.CROSSBOW : Material.GRAY_DYE,
                "§e§lHunter Swap: " + (hunterSwapEnabled ? "§aEnabled" : "§cDisabled"),
                List.of("§7Shuffle hunters on a timer"),
                "hunter_swap_toggle");
        inventory.setItem(14, hunterSwapToggle);

        int hunterSwapInterval = plugin.getConfigManager().getHunterSwapInterval();
        ItemStack hunterSwapIntervalItem = createGuiButton(
                Material.CLOCK,
                "§e§lHunter Swap Interval",
                List.of("§7Current: §e" + hunterSwapInterval + " seconds", "§7Left/Right: ±30s", "§7Shift: ±60s"),
                "hunter_swap_interval");
        inventory.setItem(15, hunterSwapIntervalItem);

        // Hot Potato mode toggle
        boolean hotPotato = plugin.getConfigManager().isHotPotatoModeEnabled();
        ItemStack hotPotatoToggle = createGuiButton(
                hotPotato ? Material.BLAZE_POWDER : Material.GRAY_DYE,
                "§e§lHot Potato Mode: " + (hotPotato ? "§aEnabled" : "§cDisabled"),
                List.of("§7Experimental runner swap variant"),
                "hot_potato_toggle");
        inventory.setItem(17, hotPotatoToggle);

        // Admin tools
        ItemStack adminHeader = createItem(Material.BOOK, "§6§lAdmin Tools", List.of(
            "§7Operator utilities: force actions"
        ));
        inventory.setItem(33, adminHeader);

        ItemStack forceSwap = createGuiButton(Material.ENDER_PEARL, "§d§lForce Runner Swap", List.of(
            "§7Trigger immediate runner swap"
        ), "force_swap");
        inventory.setItem(28, forceSwap);

        ItemStack forceHunterShuffle = createGuiButton(Material.CROSSBOW, "§c§lShuffle Hunters", List.of(
            "§7Shuffle hunter order now"
        ), "force_hunter_shuffle");
        inventory.setItem(29, forceHunterShuffle);

        ItemStack updateCompasses = createGuiButton(Material.LODESTONE, "§b§lUpdate Compasses", List.of(
            "§7Refresh all hunter compasses"
        ), "update_compasses");
        inventory.setItem(30, updateCompasses);
        
        // Removed duplicate swap interval and safe swap UI blocks

        // Timer Visibility Section
        List<String> timerHeaderLore = new ArrayList<>();
        timerHeaderLore.add("§7Configure timer visibility:");
        timerHeaderLore.add("§7• Active runner settings");
        timerHeaderLore.add("§7• Waiting runner settings");
        timerHeaderLore.add("§7• Hunter settings");
        ItemStack timerHeader = createItem(Material.BOOK, "§6§lTimer Settings", timerHeaderLore);
        inventory.setItem(18, timerHeader);

        // Active Runner Timer Settings
        String runnerVisibility = plugin.getConfigManager().getRunnerTimerVisibility();
        List<String> runnerTimerLore = new ArrayList<>();
        runnerTimerLore.add("§7Current: " + getVisibilityDisplay(runnerVisibility));
        runnerTimerLore.add("");
        runnerTimerLore.add("§7Options:");
        runnerTimerLore.add("§7• Always show timer");
        runnerTimerLore.add("§7• Show last 10 seconds");
        runnerTimerLore.add("§7• Never show timer");
        runnerTimerLore.add("");
        runnerTimerLore.add("§7Click to change");
        ItemStack runnerTimer = createItem(Material.CLOCK, "§e§lActive Runner Timer", runnerTimerLore);
        inventory.setItem(19, runnerTimer);

        // Waiting Runner Timer Settings
        String waitingVisibility = plugin.getConfigManager().getWaitingTimerVisibility();
        List<String> waitingTimerLore = new ArrayList<>();
        waitingTimerLore.add("§7Current: " + getVisibilityDisplay(waitingVisibility));
        waitingTimerLore.add("");
        waitingTimerLore.add("§7Options:");
        waitingTimerLore.add("§7• Always show timer");
        waitingTimerLore.add("§7• Show last 10 seconds");
        waitingTimerLore.add("§7• Never show timer");
        waitingTimerLore.add("");
        waitingTimerLore.add("§7Click to change");
        ItemStack waitingTimer = createItem(Material.CLOCK, "§e§lWaiting Runner Timer", waitingTimerLore);
        inventory.setItem(20, waitingTimer);

        // Hunter Timer Settings
        String hunterVisibility = plugin.getConfigManager().getHunterTimerVisibility();
        List<String> hunterTimerLore = new ArrayList<>();
        hunterTimerLore.add("§7Current: " + getVisibilityDisplay(hunterVisibility));
        hunterTimerLore.add("");
        hunterTimerLore.add("§7Options:");
        hunterTimerLore.add("§7• Always show timer");
        hunterTimerLore.add("§7• Show last 10 seconds");
        hunterTimerLore.add("§7• Never show timer");
        hunterTimerLore.add("");
        hunterTimerLore.add("§7Click to change");
        ItemStack hunterTimer = createItem(Material.CLOCK, "§e§lHunter Timer", hunterTimerLore);
        inventory.setItem(21, hunterTimer);

        // Freeze Mechanic Section
        List<String> freezeHeaderLore = new ArrayList<>();
        freezeHeaderLore.add("§7Freeze/slow hunters near the runner");
        ItemStack freezeHeader = createItem(Material.BOOK, "§6§lFreeze Mechanic", freezeHeaderLore);
        inventory.setItem(36, freezeHeader);

        boolean freezeEnabled = plugin.getConfigManager().isFreezeMechanicEnabled();
        ItemStack freezeToggle = createGuiButton(
                freezeEnabled ? Material.BLUE_ICE : Material.GRAY_DYE,
                "§e§lFreeze Mechanic: " + (freezeEnabled ? "§aEnabled" : "§cDisabled"),
                List.of("§7Toggle freeze mechanic"),
                "freeze_toggle");
        inventory.setItem(37, freezeToggle);

        String mode = plugin.getConfigManager().getFreezeMode();
        ItemStack freezeMode = createGuiButton(
                Material.SNOWBALL,
                "§e§lFreeze Mode: §b" + mode,
                List.of("§7Cycle: EFFECTS → SPECTATOR → LIMBO"),
                "freeze_mode");
        inventory.setItem(38, freezeMode);

        int freezeDuration = plugin.getConfigManager().getFreezeDurationTicks();
        ItemStack freezeDurationItem = createGuiButton(
                Material.CLOCK,
                "§e§lFreeze Duration",
                List.of("§7Current: §e" + freezeDuration + " ticks", "§7Left/Right: ±20", "§7Shift: ±100"),
                "freeze_duration");
        inventory.setItem(39, freezeDurationItem);

        int freezeInterval = plugin.getConfigManager().getFreezeCheckIntervalTicks();
        ItemStack freezeIntervalItem = createGuiButton(
                Material.REPEATER,
                "§e§lFreeze Check Interval",
                List.of("§7Current: §e" + freezeInterval + " ticks", "§7Left/Right: ±5", "§7Shift: ±20"),
                "freeze_check_interval");
        inventory.setItem(40, freezeIntervalItem);

        int freezeDistance = (int) plugin.getConfigManager().getFreezeMaxDistance();
        ItemStack freezeDistanceItem = createGuiButton(
                Material.SPYGLASS,
                "§e§lFreeze Max Distance",
                List.of("§7Current: §e" + freezeDistance + " blocks", "§7Left/Right: ±5", "§7Shift: ±20"),
                "freeze_max_distance");
        inventory.setItem(41, freezeDistanceItem);
        
        player.openInventory(inventory);
    }

    // Placeholder menus for items referenced in main menu. These should be expanded later.
    public void openKitsMenu(Player player) {
    Inventory inv = Bukkit.createInventory(null, 27, Component.text("§b§lKits"));
    // Border-only filler
    ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
    fillBorder(inv, filler);

    ItemStack back = createItem(Material.ARROW, "§7§lBack", List.of("§7Return to main menu"));
    inv.setItem(0, back);

    // Toggle kits enabled
    boolean enabled = plugin.getConfigManager().isKitsEnabled();
    ItemStack toggle = createItem(enabled ? Material.LIME_DYE : Material.GRAY_DYE,
        "§e§lKits: " + (enabled ? "§aEnabled" : "§cDisabled"),
        List.of("§7Click to toggle kit system"));
    inv.setItem(4, toggle);

    // Apply runner kit to yourself
    ItemStack applyRunner = createItem(Material.DIAMOND_BOOTS, "§b§lGive Runner Kit", List.of("§7Click to receive the runner kit"));
    inv.setItem(11, applyRunner);

    // Apply hunter kit to yourself
    ItemStack applyHunter = createItem(Material.IRON_SWORD, "§c§lGive Hunter Kit", List.of("§7Click to receive the hunter kit"));
    inv.setItem(15, applyHunter);

    // Edit Runner Kit
    ItemStack editRunner = createItem(Material.CRAFTING_TABLE, "§b§lEdit Runner Kit", List.of("§7Click to edit the runner kit"));
    inv.setItem(20, editRunner);

    // Edit Hunter Kit
    ItemStack editHunter = createItem(Material.CRAFTING_TABLE, "§c§lEdit Hunter Kit", List.of("§7Click to edit the hunter kit"));
    inv.setItem(24, editHunter);

    player.openInventory(inv);
    }

    public void openKitEditor(Player player, String kitType) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("§e§lEdit " + kitType + " Kit"));

        // Load current kit
        List<ItemStack> items = plugin.getKitManager().loadKitItems(plugin.getKitConfigManager().getConfig().getConfigurationSection("kits." + kitType));
        ItemStack[] armor = plugin.getKitManager().loadKitArmor(plugin.getKitConfigManager().getConfig().getConfigurationSection("kits." + kitType));

        for (ItemStack item : items) {
            inv.addItem(item);
        }

        inv.setItem(45, armor[3]); // Helmet
        inv.setItem(46, armor[2]); // Chestplate
        inv.setItem(47, armor[1]); // Leggings
        inv.setItem(48, armor[0]); // Boots

        // Save button
        ItemStack save = createItem(Material.GREEN_CONCRETE, "§a§lSave Kit", List.of("§7Save the current inventory as the " + kitType + " kit"));
        inv.setItem(53, save);

        player.openInventory(inv);
    }

    public void openBountyMenu(Player player) {
    Inventory inv = Bukkit.createInventory(null, 27, Component.text("§6§lBounty System"));
    ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
    fillBorder(inv, filler);

    ItemStack back = createItem(Material.ARROW, "§7§lBack", List.of("§7Return to main menu"));
    inv.setItem(0, back);

    // Status
    boolean active = plugin.getBountyManager().isActive();
    ItemStack status = createItem(active ? Material.GOLD_BLOCK : Material.COAL_BLOCK,
        "§6§lBounty Status: " + (active ? "§aActive" : "§cInactive"),
        List.of("§7Assign or clear the bounty target"));
    inv.setItem(4, status);

    // Assign new bounty
    ItemStack assign = createItem(Material.GOLDEN_APPLE, "§e§lAssign New Bounty", List.of("§7Click to randomly choose a runner as bounty"));
    inv.setItem(11, assign);

    // Clear bounty
    ItemStack clear = createItem(Material.BARRIER, "§c§lClear Bounty", List.of("§7Click to clear current bounty"));
    inv.setItem(15, clear);

    player.openInventory(inv);
    }

    public void openLastStandMenu(Player player) {
    Inventory inv = Bukkit.createInventory(null, 27, Component.text("§e§lLast Stand"));
    ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
    fillBorder(inv, filler);

    ItemStack back = createItem(Material.ARROW, "§7§lBack", List.of("§7Return to main menu"));
    inv.setItem(0, back);

    boolean enabled = plugin.getConfigManager().isLastStandEnabled();
    ItemStack toggle = createItem(enabled ? Material.TOTEM_OF_UNDYING : Material.BARRIER,
        "§e§lLast Stand: " + (enabled ? "§aEnabled" : "§cDisabled"),
        List.of("§7Click to toggle Last Stand feature"));
    inv.setItem(4, toggle);

    // Duration and amplifiers display (click to increase/decrease)
    int duration = plugin.getConfigManager().getLastStandDuration();
    ItemStack durationItem = createItem(Material.CLOCK, "§6§lLast Stand Duration", List.of("§7Current: §f" + duration + " ticks", "§7Left/Right click to adjust by 100 ticks"));
    inv.setItem(11, durationItem);

    int strength = plugin.getConfigManager().getLastStandStrengthAmplifier();
    ItemStack strengthItem = createItem(Material.BLAZE_POWDER, "§e§lStrength Amplifier", List.of("§7Current: §f" + strength, "§7Left/Right click to adjust"));
    inv.setItem(15, strengthItem);

    player.openInventory(inv);
    }

    public void openCompassSettingsMenu(Player player) {
    Inventory inv = Bukkit.createInventory(null, 27, Component.text("§9§lCompass Settings"));
    ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
    fillBorder(inv, filler);

    ItemStack back = createItem(Material.ARROW, "§7§lBack", List.of("§7Return to main menu"));
    inv.setItem(0, back);

    boolean enabled = plugin.getConfigManager().isCompassJammingEnabled();
    ItemStack jamToggle = createItem(enabled ? Material.REDSTONE_BLOCK : Material.GRAY_DYE,
        "§e§lCompass Jamming: " + (enabled ? "§aEnabled" : "§cDisabled"),
        List.of("§7Toggle compass jamming feature"));
    inv.setItem(11, jamToggle);

    int jamDuration = plugin.getConfigManager().getCompassJamDuration();
    ItemStack jamDurationItem = createItem(Material.CLOCK, "§6§lJam Duration (ticks)", List.of("§7Current: §f" + jamDuration, "§7Left/Right click to +/- 20 ticks"));
    inv.setItem(15, jamDurationItem);

    // End Portal hint controls (per-world)
    org.bukkit.World world = player.getWorld();
    org.bukkit.Location hint = plugin.getConfigManager().getEndPortalHint(world);
    String hintLine = hint != null ? ("§aSet at §f" + hint.getBlockX() + ", " + hint.getBlockY() + ", " + hint.getBlockZ()) : "§cNot set";
    ItemStack setHint = createItem(Material.LODESTONE, "§e§lSet End Portal Hint (this world)", List.of("§7Used when target is in The End", hintLine));
    ItemStack clearHint = createItem(Material.BARRIER, "§c§lClear End Portal Hint (this world)", List.of("§7Remove stored hint for this world"));
    inv.setItem(20, setHint);
    inv.setItem(22, clearHint);

    player.openInventory(inv);
    }

    public void openSuddenDeathMenu(Player player) {
    Inventory inv = Bukkit.createInventory(null, 27, Component.text("§4§lSudden Death"));
    ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
    fillBorder(inv, filler);

    ItemStack back = createItem(Material.ARROW, "§7§lBack", List.of("§7Return to main menu"));
    inv.setItem(0, back);

    boolean active = plugin.getSuddenDeathManager().isActive();
    ItemStack status = createItem(active ? Material.DRAGON_EGG : Material.END_STONE,
        "§4§lSudden Death: " + (active ? "§aActive" : "§cInactive"), List.of("§7Control sudden death behavior"));
    inv.setItem(4, status);

    boolean scheduled = plugin.getSuddenDeathManager().isScheduled();
    ItemStack schedule = createItem(Material.CLOCK, "§e§lSchedule Sudden Death", List.of("§7Left-click to schedule using config delay", "§7Click to schedule now"));
    inv.setItem(11, schedule);

    ItemStack activate = createItem(Material.DRAGON_HEAD, "§c§lActivate Now", List.of("§7Activate sudden death immediately"));
    inv.setItem(15, activate);

    // Cancel schedule button (shown regardless; will no-op when none)
    ItemStack cancel = createItem(Material.BARRIER, "§e§lCancel Scheduled Sudden Death", List.of(scheduled ? "§7A schedule is pending" : "§7No schedule pending"));
    inv.setItem(13, cancel);

    // Activation delay display (in minutes)
    long delaySeconds = plugin.getConfig().getLong("sudden_death.activation_delay", 1200);
    long delayMinutes = Math.max(1L, delaySeconds) / 60L;
    ItemStack delayItem = createItem(Material.CLOCK, "§6§lActivation Delay (minutes)", List.of("§7Current: §f" + delayMinutes + " minutes", "§7Left/Right to +/- 5 minutes"));
    inv.setItem(22, delayItem);

    player.openInventory(inv);
    }

    public void openStatisticsMenu(Player player) {
    Inventory inv = Bukkit.createInventory(null, 27, Component.text("§a§lStatistics"));
    ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
    fillBorder(inv, filler);

    ItemStack back = createItem(Material.ARROW, "§7§lBack", List.of("§7Return to main menu"));
    inv.setItem(0, back);

    ItemStack display = createItem(Material.BOOK, "§e§lDisplay Statistics", List.of("§7Click to broadcast current game statistics"));
    inv.setItem(11, display);

    ItemStack start = createItem(Material.GREEN_CONCRETE, "§a§lStart Tracking", List.of("§7Click to start stats tracking for current game"));
    inv.setItem(15, start);

    ItemStack stop = createItem(Material.RED_CONCRETE, "§c§lStop Tracking", List.of("§7Click to stop and broadcast stats"));
    inv.setItem(22, stop);

    player.openInventory(inv);
    }
    
    private int clampRows(int rows) {
        if (rows < 1) return 1;
        if (rows > 6) return 6;
        return rows;
    }

    public ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));

        if (lore.length > 0) {
            List<Component> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(Component.text(line));
            }
            meta.lore(loreList);
        }

        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createGlowingItem(ItemStack item) {
        ItemStack glowingItem = item.clone();
        ItemMeta meta = glowingItem.getItemMeta();
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        glowingItem.setItemMeta(meta);
        return glowingItem;
    }

    public ItemStack createNormalItem(ItemStack item) {
        ItemStack normalItem = item.clone();
        ItemMeta meta = normalItem.getItemMeta();
        meta.removeEnchant(Enchantment.UNBREAKING);
        meta.removeItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        normalItem.setItemMeta(meta);
        return normalItem;
    }
    
    public ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        
        if (lore != null && !lore.isEmpty()) {
            List<Component> componentLore = new ArrayList<>();
            for (String line : lore) {
                componentLore.add(Component.text(line));
            }
            meta.lore(componentLore);
        }
        
        item.setItemMeta(meta);
        return item;
    }

    // Visual utility: fill only the outer border of an inventory with a filler item
    private void fillBorder(Inventory inv, ItemStack filler) {
        if (inv == null || filler == null) return;
        int size = inv.getSize();
        if (size < 9) return;
        int rows = size / 9;
        // Top and bottom rows
        for (int c = 0; c < 9; c++) {
            inv.setItem(c, filler);
            inv.setItem((rows - 1) * 9 + c, filler);
        }
        // Left and right columns
        for (int r = 0; r < rows; r++) {
            inv.setItem(r * 9, filler);
            inv.setItem(r * 9 + 8, filler);
        }
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
    private boolean isItem(ItemStack item, Material material, String name) {
        if (item == null || item.getType() != material || !item.hasItemMeta()) {
            return false;
        }
        String displayName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
        return name.equals(displayName);
    }
    
    public boolean isMainMenu(Inventory inventory) {
        if (inventory == null || inventory.getHolder() != null || inventory.getViewers().isEmpty()) {
            return false;
        }
        String title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(inventory.getViewers().get(0).getOpenInventory().title());
        return plugin.getConfigManager().getGuiMainMenuTitle().equals(title);
    }
    
    public boolean isTeamSelector(Inventory inventory) {
        if (inventory == null || inventory.getHolder() != null || inventory.getViewers().isEmpty()) {
            return false;
        }
        String title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(inventory.getViewers().get(0).getOpenInventory().title());
        return plugin.getConfigManager().getGuiTeamSelectorTitle().equals(title);
    }
    
    public boolean isSettingsMenu(Inventory inventory) {
        if (inventory == null || inventory.getHolder() != null || inventory.getViewers().isEmpty()) {
            return false;
        }
        String title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(inventory.getViewers().get(0).getOpenInventory().title());
        return plugin.getConfigManager().getGuiSettingsTitle().equals(title);
    }
    
    // Helper methods for item identification
    public boolean isBackButton(ItemStack item) {
        return isItem(item, Material.ARROW, "§7§lBack");
    }
    
    public boolean isStartButton(ItemStack item) {
        return isItem(item, Material.GREEN_CONCRETE, "§a§lStart Game");
    }
    
    public boolean isStopButton(ItemStack item) {
        return isItem(item, Material.RED_CONCRETE, "§c§lStop Game");
    }
    
    public boolean isPauseButton(ItemStack item) {
        return isItem(item, Material.YELLOW_CONCRETE, "§e§lPause Game");
    }
    
    public boolean isResumeButton(ItemStack item) {
        return isItem(item, Material.LIME_CONCRETE, "§a§lResume Game");
    }
    
    public boolean isTeamSelectorButton(ItemStack item) {
        return isItem(item, Material.PLAYER_HEAD, "§e§lTeam Selector");
    }
    
    public boolean isSettingsButton(ItemStack item) {
        return isItem(item, Material.COMPARATOR, "§a§lSettings");
    }
    
    public boolean isRunnerTeamButton(ItemStack item) {
        return isItem(item, Material.DIAMOND_BOOTS, "§b§lRunners");
    }
    
    public boolean isHunterTeamButton(ItemStack item) {
        return isItem(item, Material.IRON_SWORD, "§c§lHunters");
    }
    
    private static final String BUTTON_ID_KEY = "ssw_button_id";

    public ItemStack createGuiButton(Material material, String name, List<String> lore, String buttonId) {
        ItemStack item = createItem(material, name, lore);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, BUTTON_ID_KEY),
                org.bukkit.persistence.PersistentDataType.STRING,
                buttonId
            );
            item.setItemMeta(meta);
        }
        return item;
    }

    public String getButtonId(ItemStack item) {
        if (item != null && item.hasItemMeta()) {
            return item.getItemMeta().getPersistentDataContainer().get(
                new org.bukkit.NamespacedKey(plugin, BUTTON_ID_KEY),
                org.bukkit.persistence.PersistentDataType.STRING
            );
        }
        return null;
    }

    public boolean isSwapIntervalButton(ItemStack item) {
        String buttonId = getButtonId(item);
        return "swap_interval".equals(buttonId) && item != null && item.getType() == Material.CLOCK;
    }

    public boolean isRandomizeSwapButton(ItemStack item) {
        String buttonId = getButtonId(item);
        return "random_swaps".equals(buttonId) && item != null && 
               (item.getType() == Material.CLOCK || item.getType() == Material.REPEATER);
    }

    public boolean isSafeSwapButton(ItemStack item) {
        String buttonId = getButtonId(item);
        return "safe_swaps".equals(buttonId) && item != null && 
               (item.getType() == Material.TOTEM_OF_UNDYING || item.getType() == Material.BARRIER);
    }

    public boolean isActiveRunnerTimerButton(ItemStack item) {
        return isItem(item, Material.CLOCK, "§e§lActive Runner Timer");
    }

    public boolean isWaitingRunnerTimerButton(ItemStack item) {
        return isItem(item, Material.CLOCK, "§e§lWaiting Runner Timer");
    }

    public boolean isHunterTimerButton(ItemStack item) {
        return isItem(item, Material.CLOCK, "§e§lHunter Timer");
    }

    private String getVisibilityDisplay(String visibility) {
        switch (visibility) {
            case "always":
                return "§aAlways Show";
            case "last_10":
                return "§eLast 10 Seconds";
            case "never":
                return "§cNever Show";
            default:
                return "§7Unknown";
        }
    }

    public String getNextVisibility(String current) {
        switch (current) {
            case "always":
                return "last_10";
            case "last_10":
                return "never";
            case "never":
                return "always";
            default:
                return "last_10";
        }
    }
    
    // Helper methods for team management
    public void setPlayerTeam(Player player, Team team) {
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
    
    
    // Fix for missing getSelectedTeam method
    public Team getSelectedTeam(Player player) {
        return plugin.getGameManager().getPlayerState(player).getSelectedTeam();
    }
    
    // Fix for missing isSwapRandomized method
    public boolean isSwapRandomized() {
        return plugin.getConfigManager().isSwapRandomized();
    }
}
