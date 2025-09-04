/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  net.kyori.adventure.text.format.NamedTextColor
 *  net.kyori.adventure.text.format.TextColor
 *  net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.NamespacedKey
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.World
 *  org.bukkit.enchantments.Enchantment
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemFlag
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.inventory.meta.SkullMeta
 *  org.bukkit.persistence.PersistentDataType
 *  org.bukkit.plugin.Plugin
 */
package com.example.speedrunnerswap.gui;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import com.example.speedrunnerswap.models.Team;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public class GuiManager {
    private final SpeedrunnerSwap plugin;
    private final String BACK_BUTTON_TITLE = "\u00a7c\u00a7lBack to Main Menu";
    private static final String BUTTON_ID_KEY = "ssw_button_id";

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
        }
        if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        }
        return String.format("%ds", seconds);
    }

    public void openPositiveEffectsMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, (int)36, (Component)Component.text((String)"\u00a7a\u00a7lPositive Effects"));
        ItemStack filler = this.createItem(Material.BLACK_STAINED_GLASS_PANE, " ", new String[0]);
        this.fillBorder(inventory, filler);
        ArrayList<ItemStack> effectItems = new ArrayList<ItemStack>();
        effectItems.add(this.createEffectItem(Material.POTION, "Speed", "SPEED"));
        effectItems.add(this.createEffectItem(Material.POTION, "Jump Boost", "JUMP"));
        effectItems.add(this.createEffectItem(Material.POTION, "Strength", "INCREASE_DAMAGE"));
        effectItems.add(this.createEffectItem(Material.POTION, "Regeneration", "REGENERATION"));
        effectItems.add(this.createEffectItem(Material.POTION, "Resistance", "DAMAGE_RESISTANCE"));
        effectItems.add(this.createEffectItem(Material.POTION, "Fire Resistance", "FIRE_RESISTANCE"));
        effectItems.add(this.createEffectItem(Material.POTION, "Water Breathing", "WATER_BREATHING"));
        effectItems.add(this.createEffectItem(Material.POTION, "Night Vision", "NIGHT_VISION"));
        for (int i = 0; i < effectItems.size(); ++i) {
            inventory.setItem(10 + i, (ItemStack)effectItems.get(i));
        }
        inventory.setItem(35, this.createItem(Material.BARRIER, "\u00a7c\u00a7lBack to Main Menu", new String[0]));
        player.openInventory(inventory);
    }

    public void openNegativeEffectsMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, (int)36, (Component)Component.text((String)"\u00a7c\u00a7lNegative Effects"));
        ItemStack filler = this.createItem(Material.BLACK_STAINED_GLASS_PANE, " ", new String[0]);
        this.fillBorder(inventory, filler);
        ArrayList<ItemStack> effectItems = new ArrayList<ItemStack>();
        effectItems.add(this.createEffectItem(Material.POTION, "Slowness", "SLOW"));
        effectItems.add(this.createEffectItem(Material.POTION, "Weakness", "WEAKNESS"));
        effectItems.add(this.createEffectItem(Material.POTION, "Poison", "POISON"));
        effectItems.add(this.createEffectItem(Material.POTION, "Blindness", "BLINDNESS"));
        effectItems.add(this.createEffectItem(Material.POTION, "Hunger", "HUNGER"));
        effectItems.add(this.createEffectItem(Material.POTION, "Mining Fatigue", "SLOW_DIGGING"));
        effectItems.add(this.createEffectItem(Material.POTION, "Nausea", "CONFUSION"));
        effectItems.add(this.createEffectItem(Material.POTION, "Glowing", "GLOWING"));
        for (int i = 0; i < effectItems.size(); ++i) {
            inventory.setItem(10 + i, (ItemStack)effectItems.get(i));
        }
        inventory.setItem(35, this.createItem(Material.BARRIER, "\u00a7c\u00a7lBack to Main Menu", new String[0]));
        player.openInventory(inventory);
    }

    public ItemStack createEffectItem(Material material, String displayName, String effectId) {
        ArrayList<String> lore = new ArrayList<String>();
        lore.add("\u00a77Effect ID: \u00a7f" + effectId);
        lore.add("\u00a77Click to toggle this effect");
        boolean isEnabled = this.plugin.getConfig().getStringList("power_ups.good_effects").contains(effectId) || this.plugin.getConfig().getStringList("power_ups.bad_effects").contains(effectId);
        lore.add(isEnabled ? "\u00a7aCurrently enabled" : "\u00a7cCurrently disabled");
        return this.createItem(material, "\u00a7e\u00a7l" + displayName, lore);
    }

    public void openPowerUpsMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, (int)36, (Component)Component.text((String)"\u00a7e\u00a7lPower-ups Menu"));
        ItemStack filler = this.createItem(Material.BLACK_STAINED_GLASS_PANE, " ", new String[0]);
        this.fillBorder(inventory, filler);
        ArrayList<String> powerUpToggleLore = new ArrayList<String>();
        boolean powerUpsEnabled = this.plugin.getConfigManager().isPowerUpsEnabled();
        powerUpToggleLore.add("\u00a77Current status: " + (powerUpsEnabled ? "\u00a7aEnabled" : "\u00a7cDisabled"));
        powerUpToggleLore.add("\u00a77Click to toggle");
        ItemStack toggleItem = this.createItem(powerUpsEnabled ? Material.LIME_DYE : Material.GRAY_DYE, "\u00a7e\u00a7lToggle Power-ups", powerUpToggleLore);
        inventory.setItem(4, toggleItem);
        ArrayList<String> positiveEffectsLore = new ArrayList<String>();
        positiveEffectsLore.add("\u00a77Current effects:");
        for (String effect : this.plugin.getConfig().getStringList("power_ups.good_effects")) {
            positiveEffectsLore.add("\u00a7a\u2022 " + effect.toLowerCase());
        }
        positiveEffectsLore.add("");
        positiveEffectsLore.add("\u00a77Click to modify");
        ItemStack goodEffectsItem = this.createItem(Material.SPLASH_POTION, "\u00a7a\u00a7lPositive Effects", positiveEffectsLore);
        inventory.setItem(11, goodEffectsItem);
        ArrayList<String> negativeEffectsLore = new ArrayList<String>();
        negativeEffectsLore.add("\u00a77Current effects:");
        for (String effect : this.plugin.getConfig().getStringList("power_ups.bad_effects")) {
            negativeEffectsLore.add("\u00a7c\u2022 " + effect.toLowerCase());
        }
        negativeEffectsLore.add("");
        negativeEffectsLore.add("\u00a77Click to modify");
        ItemStack badEffectsItem = this.createItem(Material.LINGERING_POTION, "\u00a7c\u00a7lNegative Effects", negativeEffectsLore);
        inventory.setItem(15, badEffectsItem);
        int minSec = this.plugin.getConfigManager().getPowerUpsMinSeconds();
        int maxSec = this.plugin.getConfigManager().getPowerUpsMaxSeconds();
        int minLvl = this.plugin.getConfigManager().getPowerUpsMinLevel();
        int maxLvl = this.plugin.getConfigManager().getPowerUpsMaxLevel();
        ArrayList<String> durationLore = new ArrayList<String>();
        durationLore.add("\u00a77Duration: \u00a7e" + minSec + "-" + maxSec + "s");
        durationLore.add("\u00a77Level: \u00a7e" + minLvl + "-" + maxLvl);
        durationLore.add("");
        durationLore.add("\u00a77Click to modify timings");
        ItemStack durationItem = this.createItem(Material.CLOCK, "\u00a76\u00a7lEffect Durations", durationLore);
        inventory.setItem(22, durationItem);
        inventory.setItem(31, this.createItem(Material.BARRIER, "\u00a7c\u00a7lBack to Main Menu", new String[0]));
        player.openInventory(inventory);
    }

    public void openPowerUpDurationsMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, (int)27, (Component)Component.text((String)"\u00a76\u00a7lPower-up Durations"));
        ItemStack filler = this.createItem(Material.GRAY_STAINED_GLASS_PANE, " ", new String[0]);
        this.fillBorder(inv, filler);
        ItemStack back = this.createItem(Material.ARROW, "\u00a77\u00a7lBack", List.of("\u00a77Return to power-ups"));
        inv.setItem(0, back);
        int minSec = this.plugin.getConfigManager().getPowerUpsMinSeconds();
        int maxSec = this.plugin.getConfigManager().getPowerUpsMaxSeconds();
        int minLvl = this.plugin.getConfigManager().getPowerUpsMinLevel();
        int maxLvl = this.plugin.getConfigManager().getPowerUpsMaxLevel();
        ItemStack minDur = this.createItem(Material.CLOCK, "\u00a7e\u00a7lMin Duration (s)", List.of("\u00a77Current: \u00a7f" + minSec, "\u00a77Left/Right: \u00b15"));
        ItemStack maxDur = this.createItem(Material.CLOCK, "\u00a7e\u00a7lMax Duration (s)", List.of("\u00a77Current: \u00a7f" + maxSec, "\u00a77Left/Right: \u00b15"));
        ItemStack minLvlItem = this.createItem(Material.EXPERIENCE_BOTTLE, "\u00a7e\u00a7lMin Level", List.of("\u00a77Current: \u00a7f" + minLvl, "\u00a77Left/Right: \u00b11"));
        ItemStack maxLvlItem = this.createItem(Material.EXPERIENCE_BOTTLE, "\u00a7e\u00a7lMax Level", List.of("\u00a77Current: \u00a7f" + maxLvl, "\u00a77Left/Right: \u00b11"));
        inv.setItem(10, minDur);
        inv.setItem(12, maxDur);
        inv.setItem(14, minLvlItem);
        inv.setItem(16, maxLvlItem);
        player.openInventory(inv);
    }

    public void openWorldBorderMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, (int)27, (Component)Component.text((String)"\u00a7c\u00a7lWorld Border Settings"));
        ItemStack filler = this.createItem(Material.BLACK_STAINED_GLASS_PANE, " ", new String[0]);
        this.fillBorder(inventory, filler);
        boolean isEnabled = this.plugin.getConfig().getBoolean("world_border.enabled", true);
        ArrayList<String> toggleLore = new ArrayList<String>();
        toggleLore.add("\u00a77Current status: " + (isEnabled ? "\u00a7aEnabled" : "\u00a7cDisabled"));
        toggleLore.add("\u00a77Click to toggle");
        ItemStack toggleItem = this.createItem(isEnabled ? Material.LIME_DYE : Material.GRAY_DYE, "\u00a7e\u00a7lToggle World Border", toggleLore);
        inventory.setItem(4, toggleItem);
        int initialSize = this.plugin.getConfig().getInt("world_border.initial_size", 2000);
        ArrayList<String> initialSizeLore = new ArrayList<String>();
        initialSizeLore.add("\u00a77Current size: \u00a7e" + initialSize + " blocks");
        initialSizeLore.add("\u00a77Left-click: \u00a7a+100 blocks");
        initialSizeLore.add("\u00a77Right-click: \u00a7c-100 blocks");
        initialSizeLore.add("\u00a77Shift + Left-click: \u00a7a+500 blocks");
        initialSizeLore.add("\u00a77Shift + Right-click: \u00a7c-500 blocks");
        ItemStack initialSizeItem = this.createItem(Material.GRASS_BLOCK, "\u00a7a\u00a7lInitial Border Size", initialSizeLore);
        inventory.setItem(11, initialSizeItem);
        int finalSize = this.plugin.getConfig().getInt("world_border.final_size", 100);
        ArrayList<String> finalSizeLore = new ArrayList<String>();
        finalSizeLore.add("\u00a77Current size: \u00a7e" + finalSize + " blocks");
        finalSizeLore.add("\u00a77Left-click: \u00a7a+50 blocks");
        finalSizeLore.add("\u00a77Right-click: \u00a7c-50 blocks");
        finalSizeLore.add("\u00a77Shift + Left-click: \u00a7a+100 blocks");
        finalSizeLore.add("\u00a77Shift + Right-click: \u00a7c-100 blocks");
        ItemStack finalSizeItem = this.createItem(Material.BEDROCK, "\u00a7c\u00a7lFinal Border Size", finalSizeLore);
        inventory.setItem(13, finalSizeItem);
        int shrinkDuration = this.plugin.getConfig().getInt("world_border.shrink_duration", 1800);
        ArrayList<String> durationLore = new ArrayList<String>();
        durationLore.add("\u00a77Current duration: \u00a7e" + this.formatTime(shrinkDuration));
        durationLore.add("\u00a77Left-click: \u00a7a+5 minutes");
        durationLore.add("\u00a77Right-click: \u00a7c-5 minutes");
        durationLore.add("\u00a77Shift + Left-click: \u00a7a+15 minutes");
        durationLore.add("\u00a77Shift + Right-click: \u00a7c-15 minutes");
        ItemStack durationItem = this.createItem(Material.CLOCK, "\u00a76\u00a7lShrink Duration", durationLore);
        inventory.setItem(15, durationItem);
        int warningDistance = this.plugin.getConfig().getInt("world_border.warning_distance", 50);
        ArrayList<String> warningLore = new ArrayList<String>();
        warningLore.add("\u00a77Warning distance: \u00a7e" + warningDistance + " blocks");
        warningLore.add("\u00a77Warning interval: \u00a7e" + this.plugin.getConfig().getInt("world_border.warning_interval", 300) + " seconds");
        warningLore.add("\u00a77Click to modify warnings");
        ItemStack warningItem = this.createItem(Material.BELL, "\u00a7e\u00a7lWarning Settings", warningLore);
        inventory.setItem(22, warningItem);
        inventory.setItem(26, this.createItem(Material.BARRIER, "\u00a7c\u00a7lBack to Main Menu", new String[0]));
        player.openInventory(inventory);
    }

    public void openMainMenu(Player player) {
        block8: {
            try {
                String title = this.plugin.getConfigManager().getGuiMainMenuTitle();
                int rows = this.plugin.getConfigManager().getGuiMainMenuRows();
                rows = Math.max(rows, 6);
                rows = this.clampRows(rows);
                Inventory inventory = Bukkit.createInventory(null, (int)(rows * 9), (Component)Component.text((String)title));
                ItemStack filler = this.createItem(Material.BLACK_STAINED_GLASS_PANE, " ", new String[0]);
                this.fillBorder(inventory, filler);
                ArrayList<String> teamSelectorLore = new ArrayList<String>();
                teamSelectorLore.add("\u00a77Click to open the team selection menu");
                teamSelectorLore.add("\u00a77Here you can:");
                teamSelectorLore.add("\u00a77\u2022 Assign players to teams");
                teamSelectorLore.add("\u00a77\u2022 View current team assignments");
                teamSelectorLore.add("\u00a77\u2022 Manage runner and hunter roles");
                ItemStack teamSelector = this.createGuiButton(Material.PLAYER_HEAD, "\u00a7e\u00a7lTeam Selector", teamSelectorLore, "team_selector");
                inventory.setItem(10, teamSelector);
                ArrayList<String> borderLore = new ArrayList<String>();
                borderLore.add("\u00a77Configure world border settings:");
                borderLore.add("\u00a77\u2022 Set initial and final size");
                borderLore.add("\u00a77\u2022 Adjust shrink duration");
                borderLore.add("\u00a77\u2022 Toggle border shrinking");
                ItemStack borderSettings = this.createGuiButton(Material.BARRIER, "\u00a7c\u00a7lWorld Border", borderLore, "world_border");
                inventory.setItem(12, borderSettings);
                ArrayList<String> powerupsLore = new ArrayList<String>();
                powerupsLore.add("\u00a77Configure power-up settings:");
                powerupsLore.add("\u00a77\u2022 Enable/disable power-ups");
                powerupsLore.add("\u00a77\u2022 Customize effects");
                powerupsLore.add("\u00a77\u2022 Adjust durations");
                ItemStack powerupsSettings = this.createGuiButton(Material.POTION, "\u00a7d\u00a7lPower-ups", powerupsLore, "power_ups");
                inventory.setItem(14, powerupsSettings);
                ArrayList<String> kitsLore = new ArrayList<String>();
                kitsLore.add("\u00a77Configure custom kits:");
                kitsLore.add("\u00a77\u2022 Edit runner kits");
                kitsLore.add("\u00a77\u2022 Edit hunter kits");
                kitsLore.add("\u00a77\u2022 Manage equipment");
                ItemStack kitsSettings = this.createGuiButton(Material.DIAMOND_CHESTPLATE, "\u00a7b\u00a7lKits", kitsLore, "kits");
                inventory.setItem(16, kitsSettings);
                ArrayList<String> bountyLore = new ArrayList<String>();
                bountyLore.add("\u00a77Configure bounty system:");
                bountyLore.add("\u00a77\u2022 Set bounty rewards");
                bountyLore.add("\u00a77\u2022 Adjust durations");
                bountyLore.add("\u00a77\u2022 Manage effects");
                ItemStack bountySettings = this.createGuiButton(Material.GOLDEN_APPLE, "\u00a76\u00a7lBounty System", bountyLore, "bounty");
                inventory.setItem(28, bountySettings);
                ArrayList<String> lastStandLore = new ArrayList<String>();
                lastStandLore.add("\u00a77Configure Last Stand mode:");
                lastStandLore.add("\u00a77\u2022 Set buff strengths");
                lastStandLore.add("\u00a77\u2022 Adjust duration");
                lastStandLore.add("\u00a77\u2022 Toggle feature");
                ItemStack lastStandSettings = this.createGuiButton(Material.TOTEM_OF_UNDYING, "\u00a7e\u00a7lLast Stand", lastStandLore, "last_stand");
                inventory.setItem(30, lastStandSettings);
                ArrayList<String> compassLore = new ArrayList<String>();
                compassLore.add("\u00a77Configure compass settings:");
                compassLore.add("\u00a77\u2022 Set jamming duration");
                compassLore.add("\u00a77\u2022 Adjust tracking");
                compassLore.add("\u00a77\u2022 Toggle features");
                ItemStack compassSettings = this.createGuiButton(Material.COMPASS, "\u00a79\u00a7lCompass Settings", compassLore, "compass");
                inventory.setItem(32, compassSettings);
                ArrayList<String> suddenDeathLore = new ArrayList<String>();
                suddenDeathLore.add("\u00a77Configure Sudden Death mode:");
                suddenDeathLore.add("\u00a77\u2022 Set activation time");
                suddenDeathLore.add("\u00a77\u2022 Customize effects");
                suddenDeathLore.add("\u00a77\u2022 Set arena location");
                ItemStack suddenDeathSettings = this.createGuiButton(Material.DRAGON_HEAD, "\u00a74\u00a7lSudden Death", suddenDeathLore, "sudden_death");
                inventory.setItem(34, suddenDeathSettings);
                ArrayList<String> statsLore = new ArrayList<String>();
                statsLore.add("\u00a77Configure statistics:");
                statsLore.add("\u00a77\u2022 Toggle stat tracking");
                statsLore.add("\u00a77\u2022 Set display options");
                statsLore.add("\u00a77\u2022 View current stats");
                ItemStack statsSettings = this.createGuiButton(Material.BOOK, "\u00a7a\u00a7lStatistics", statsLore, "statistics");
                inventory.setItem(48, statsSettings);
                ArrayList<String> settingsLore = new ArrayList<String>();
                settingsLore.add("\u00a77Click to configure plugin settings");
                settingsLore.add("\u00a77Manage:");
                settingsLore.add("\u00a77\u2022 Swap intervals and type");
                settingsLore.add("\u00a77\u2022 Safety features");
                settingsLore.add("\u00a77\u2022 Tracking options");
                settingsLore.add("\u00a77\u2022 Game mechanics");
                ItemStack settings = this.createGuiButton(Material.COMPARATOR, "\u00a7a\u00a7lSettings", settingsLore, "settings");
                inventory.setItem(15, settings);
                if (this.plugin.getGameManager().isGameRunning()) {
                    if (this.plugin.getGameManager().isGamePaused()) {
                        ArrayList<String> resumeLore = new ArrayList<String>();
                        resumeLore.add("\u00a77Click to resume the game");
                        resumeLore.add("\u00a77This will:");
                        resumeLore.add("\u00a77\u2022 Reactivate the swap timer");
                        resumeLore.add("\u00a77\u2022 Allow runner movement");
                        resumeLore.add("\u00a77\u2022 Resume all game mechanics");
                        ItemStack resume = this.createItem(Material.LIME_CONCRETE, "\u00a7a\u00a7lResume Game", resumeLore);
                        inventory.setItem(22, this.createGlowingItem(resume));
                    } else {
                        ArrayList<String> pauseLore = new ArrayList<String>();
                        pauseLore.add("\u00a77Click to pause the game");
                        pauseLore.add("\u00a77This will:");
                        pauseLore.add("\u00a77\u2022 Stop the swap timer");
                        pauseLore.add("\u00a77\u2022 Freeze current positions");
                        pauseLore.add("\u00a77\u2022 Maintain current game state");
                        ItemStack pause = this.createItem(Material.YELLOW_CONCRETE, "\u00a7e\u00a7lPause Game", pauseLore);
                        inventory.setItem(22, this.createGlowingItem(pause));
                    }
                    ArrayList<String> stopLore = new ArrayList<String>();
                    stopLore.add("\u00a7c\u00a7lWARNING: This will end the current game!");
                    stopLore.add("\u00a77This action:");
                    stopLore.add("\u00a77\u2022 Ends the current game session");
                    stopLore.add("\u00a77\u2022 Resets all player states");
                    stopLore.add("\u00a77\u2022 Returns players to spawn");
                    ItemStack stop = this.createItem(Material.RED_CONCRETE, "\u00a7c\u00a7lStop Game", stopLore);
                    inventory.setItem(31, this.createGlowingItem(stop));
                } else {
                    ArrayList<String> startLore = new ArrayList<String>();
                    boolean canStart = this.plugin.getGameManager().canStartGame();
                    if (canStart) {
                        startLore.add("\u00a77Click to start the game");
                        startLore.add("\u00a77Requirements met:");
                        startLore.add("\u00a7a\u2714 Sufficient players");
                        startLore.add("\u00a7a\u2714 Teams configured");
                        startLore.add("");
                        startLore.add("\u00a77Game will begin immediately!");
                    } else {
                        startLore.add("\u00a7cCannot start game:");
                        startLore.add("\u00a77Requirements:");
                        startLore.add(this.plugin.getGameManager().getRunners().size() > 0 ? "\u00a7a\u2714" : "\u00a7c\u2718 At least one runner");
                        startLore.add(this.plugin.getGameManager().getHunters().size() > 0 ? "\u00a7a\u2714" : "\u00a7c\u2718 At least one hunter");
                        startLore.add("");
                        startLore.add("\u00a77Configure teams to start!");
                    }
                    ItemStack start = this.createItem(Material.GREEN_CONCRETE, "\u00a7a\u00a7lStart Game", startLore);
                    inventory.setItem(22, canStart ? this.createGlowingItem(start) : start);
                }
                ItemStack status = this.createItem(Material.COMPASS, "\u00a7b\u00a7lGame Status", this.getStatusLore());
                inventory.setItem(40, status);
                player.openInventory(inventory);
            }
            catch (Exception e) {
                this.plugin.getLogger().log(Level.SEVERE, "Failed to open main GUI for player " + (player == null ? "UNKNOWN" : player.getName()), e);
                if (player == null || !player.isOnline()) break block8;
                player.sendMessage("\u00a7cFailed to open menu due to an internal error. Check server logs for details.");
            }
        }
    }

    public void openTeamSelector(Player player) {
        String title = this.plugin.getConfigManager().getGuiTeamSelectorTitle();
        int rows = this.plugin.getConfigManager().getGuiTeamSelectorRows();
        rows = Math.max(rows, 4);
        rows = this.clampRows(rows);
        Inventory inventory = Bukkit.createInventory(null, (int)(rows * 9), (Component)Component.text((String)title));
        ItemStack filler = this.createItem(Material.BLACK_STAINED_GLASS_PANE, " ", new String[0]);
        this.fillBorder(inventory, filler);
        ArrayList<String> backLore = new ArrayList<String>();
        backLore.add("\u00a77Return to main menu");
        backLore.add("\u00a77Current selections will be saved");
        ItemStack back = this.createItem(Material.ARROW, "\u00a77\u00a7lBack", backLore);
        inventory.setItem(0, back);
        ItemStack clearTeams = this.createGuiButton(Material.BARRIER, "\u00a7c\u00a7lClear All Teams", List.of("\u00a77Remove all existing team assignments"), "clear_teams");
        inventory.setItem(8, clearTeams);
        ArrayList<String> runnerLore = new ArrayList<String>();
        runnerLore.add("\u00a77Click to select players as runners");
        runnerLore.add("");
        runnerLore.add("\u00a77Current Runners: \u00a7b" + this.plugin.getGameManager().getRunners().size());
        runnerLore.add("\u00a77Role:");
        runnerLore.add("\u00a77\u2022 Complete objectives");
        runnerLore.add("\u00a77\u2022 Avoid hunters");
        runnerLore.add("\u00a77\u2022 Swap between players");
        ItemStack runnerTeam = this.createItem(Material.DIAMOND_BOOTS, "\u00a7b\u00a7lRunners", runnerLore);
        inventory.setItem(2, this.plugin.getGameManager().getPlayerState(player).getSelectedTeam() == Team.RUNNER ? this.createGlowingItem(runnerTeam) : this.createNormalItem(runnerTeam));
        ArrayList<String> hunterLore = new ArrayList<String>();
        hunterLore.add("\u00a77Click to select players as hunters");
        hunterLore.add("");
        hunterLore.add("\u00a77Current Hunters: \u00a7c" + this.plugin.getGameManager().getHunters().size());
        hunterLore.add("\u00a77Role:");
        hunterLore.add("\u00a77\u2022 Track runners");
        hunterLore.add("\u00a77\u2022 Eliminate runners");
        hunterLore.add("\u00a77\u2022 Prevent objectives");
        ItemStack hunterTeam = this.createItem(Material.IRON_SWORD, "\u00a7c\u00a7lHunters", hunterLore);
        inventory.setItem(6, this.plugin.getGameManager().getPlayerState(player).getSelectedTeam() == Team.HUNTER ? this.createGlowingItem(hunterTeam) : this.createNormalItem(hunterTeam));
        ArrayList<String> instructionsLore = new ArrayList<String>();
        instructionsLore.add("\u00a77How to assign teams:");
        instructionsLore.add("\u00a771. Select a team above");
        instructionsLore.add("\u00a772. Click player heads below");
        instructionsLore.add("\u00a773. Confirm your selections");
        ItemStack instructions = this.createItem(Material.BOOK, "\u00a7e\u00a7lInstructions", instructionsLore);
        inventory.setItem(4, instructions);
        int slot = 18;
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (slot >= inventory.getSize()) break;
            ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta)playerHead.getItemMeta();
            meta.setOwningPlayer((OfflinePlayer)onlinePlayer);
            Team currentTeam = this.plugin.getGameManager().getPlayerState(onlinePlayer).getSelectedTeam();
            NamedTextColor nameColor = currentTeam == Team.RUNNER ? NamedTextColor.AQUA : (currentTeam == Team.HUNTER ? NamedTextColor.RED : NamedTextColor.WHITE);
            meta.displayName(Component.text((String)onlinePlayer.getName()).color((TextColor)nameColor));
            ArrayList<String> lore = new ArrayList<String>();
            if (this.plugin.getGameManager().isRunner(onlinePlayer)) {
                lore.add("\u00a7bCurrently a Runner");
                lore.add("\u00a77Click while Hunter team is selected");
                lore.add("\u00a77to change to Hunter");
            } else if (this.plugin.getGameManager().isHunter(onlinePlayer)) {
                lore.add("\u00a7cCurrently a Hunter");
                lore.add("\u00a77Click while Runner team is selected");
                lore.add("\u00a77to change to Runner");
            } else {
                lore.add("\u00a77No team assigned");
                lore.add("\u00a77Select a team above first");
                lore.add("\u00a77then click to assign");
            }
            lore.add("");
            lore.add(onlinePlayer.isOnline() ? "\u00a7a\u2714 Online" : "\u00a7c\u2718 Offline");
            ArrayList<TextComponent> componentLore = new ArrayList<TextComponent>();
            for (String line : lore) {
                componentLore.add(Component.text((String)line));
            }
            meta.lore(componentLore);
            playerHead.setItemMeta((ItemMeta)meta);
            if (currentTeam != Team.NONE) {
                playerHead = this.createGlowingItem(playerHead);
            }
            inventory.setItem(slot++, playerHead);
        }
        player.openInventory(inventory);
    }

    public void openSettingsMenu(Player player) {
        String title = this.plugin.getConfigManager().getGuiSettingsTitle();
        int rows = this.plugin.getConfigManager().getGuiSettingsRows();
        rows = Math.max(rows, 6);
        rows = this.clampRows(rows);
        Inventory inventory = Bukkit.createInventory(null, (int)(rows * 9), (Component)Component.text((String)title));
        ItemStack filler = this.createItem(Material.BLACK_STAINED_GLASS_PANE, " ", new String[0]);
        this.fillBorder(inventory, filler);
        ArrayList<String> backLore = new ArrayList<String>();
        backLore.add("\u00a77Return to main menu");
        backLore.add("\u00a77Settings will be saved automatically");
        ItemStack back = this.createItem(Material.ARROW, "\u00a77\u00a7lBack", backLore);
        ArrayList<String> trackerHeaderLore = new ArrayList<String>();
        trackerHeaderLore.add("\u00a77Configure compass tracking:");
        trackerHeaderLore.add("\u00a77\u2022 Hunters receive a compass to track active runner");
        ItemStack trackerHeader = this.createItem(Material.BOOK, "\u00a76\u00a7lTracker Settings", trackerHeaderLore);
        inventory.setItem(26, trackerHeader);
        inventory.setItem(0, back);
        ArrayList<String> swapHeaderLore = new ArrayList<String>();
        swapHeaderLore.add("\u00a77Configure swap mechanics:");
        swapHeaderLore.add("\u00a77\u2022 Intervals and timing");
        swapHeaderLore.add("\u00a77\u2022 Randomization options");
        swapHeaderLore.add("\u00a77\u2022 Safety features");
        swapHeaderLore.add("\u00a77\u2022 Hover over options for details");
        ItemStack swapHeader = this.createItem(Material.BOOK, "\u00a76\u00a7lSwap Settings", swapHeaderLore);
        inventory.setItem(9, swapHeader);
        boolean isRandomized = this.plugin.getConfigManager().isSwapRandomized();
        ArrayList<String> randomSwapLore = new ArrayList<String>();
        randomSwapLore.add("\u00a77Current: " + (isRandomized ? "\u00a7aRandom" : "\u00a7bFixed"));
        randomSwapLore.add("");
        randomSwapLore.add("\u00a77Random Mode:");
        randomSwapLore.add("\u00a77\u2022 Varies interval within range");
        randomSwapLore.add("\u00a77\u2022 Adds unpredictability");
        randomSwapLore.add("");
        randomSwapLore.add("\u00a77Fixed Mode:");
        randomSwapLore.add("\u00a77\u2022 Consistent timing");
        randomSwapLore.add("\u00a77\u2022 Predictable swaps");
        ItemStack swapTypeButton = this.createGuiButton(isRandomized ? Material.CLOCK : Material.REPEATER, "\u00a7e\u00a7lSwap Type: " + (isRandomized ? "\u00a7aRandom" : "\u00a7bFixed"), randomSwapLore, "random_swaps");
        inventory.setItem(10, swapTypeButton);
        int currentInterval = this.plugin.getConfigManager().getSwapInterval();
        ArrayList<String> swapIntervalLore = new ArrayList<String>();
        swapIntervalLore.add("\u00a77Current: \u00a7e" + currentInterval + " seconds");
        swapIntervalLore.add("");
        swapIntervalLore.add("\u00a77Click to change");
        swapIntervalLore.add("\u00a77\u2022 Left-click: +30s");
        swapIntervalLore.add("\u00a77\u2022 Right-click: -30s");
        swapIntervalLore.add("\u00a77\u2022 Shift+click: \u00b160s");
        ItemStack intervalButton = this.createGuiButton(Material.CLOCK, "\u00a7e\u00a7lSwap Interval", swapIntervalLore, "swap_interval");
        inventory.setItem(11, intervalButton);
        ArrayList<String> safeSwapLore = new ArrayList<String>();
        boolean isSafeSwapsEnabled = this.plugin.getConfigManager().isSafeSwapEnabled();
        safeSwapLore.add("\u00a77Current: " + (isSafeSwapsEnabled ? "\u00a7aEnabled" : "\u00a7cDisabled"));
        safeSwapLore.add("");
        safeSwapLore.add("\u00a77When enabled:");
        safeSwapLore.add("\u00a77\u2022 Checks for safe landing");
        safeSwapLore.add("\u00a77\u2022 Prevents void/lava deaths");
        safeSwapLore.add("\u00a77\u2022 May delay swaps slightly");
        ItemStack safeButton = this.createGuiButton(isSafeSwapsEnabled ? Material.TOTEM_OF_UNDYING : Material.BARRIER, "\u00a7e\u00a7lSafe Swaps: " + (isSafeSwapsEnabled ? "\u00a7aEnabled" : "\u00a7cDisabled"), safeSwapLore, "safe_swaps");
        inventory.setItem(12, safeButton);
        boolean pauseOnDisconnect = this.plugin.getConfigManager().isPauseOnDisconnect();
        ItemStack pauseToggle = this.createGuiButton(pauseOnDisconnect ? Material.REDSTONE_TORCH : Material.LEVER, "\u00a7e\u00a7lPause On Disconnect: " + (pauseOnDisconnect ? "\u00a7aEnabled" : "\u00a7cDisabled"), List.of("\u00a77Pause game if active runner disconnects"), "pause_on_disconnect");
        inventory.setItem(16, pauseToggle);
        boolean trackerEnabled = this.plugin.getConfigManager().isTrackerEnabled();
        ArrayList<String> trackerToggleLore = new ArrayList<String>();
        trackerToggleLore.add("\u00a77Current: " + (trackerEnabled ? "\u00a7aEnabled" : "\u00a7cDisabled"));
        trackerToggleLore.add("\u00a77Hunters receive tracking compasses");
        ItemStack trackerToggle = this.createGuiButton(trackerEnabled ? Material.COMPASS : Material.GRAY_DYE, "\u00a7e\u00a7lTracker: " + (trackerEnabled ? "\u00a7aEnabled" : "\u00a7cDisabled"), trackerToggleLore, "tracker_toggle");
        inventory.setItem(13, trackerToggle);
        boolean hunterSwapEnabled = this.plugin.getConfigManager().isHunterSwapEnabled();
        ItemStack hunterSwapToggle = this.createGuiButton(hunterSwapEnabled ? Material.CROSSBOW : Material.GRAY_DYE, "\u00a7e\u00a7lHunter Swap: " + (hunterSwapEnabled ? "\u00a7aEnabled" : "\u00a7cDisabled"), List.of("\u00a77Shuffle hunters on a timer"), "hunter_swap_toggle");
        inventory.setItem(14, hunterSwapToggle);
        int hunterSwapInterval = this.plugin.getConfigManager().getHunterSwapInterval();
        ItemStack hunterSwapIntervalItem = this.createGuiButton(Material.CLOCK, "\u00a7e\u00a7lHunter Swap Interval", List.of("\u00a77Current: \u00a7e" + hunterSwapInterval + " seconds", "\u00a77Left/Right: \u00b130s", "\u00a77Shift: \u00b160s"), "hunter_swap_interval");
        inventory.setItem(15, hunterSwapIntervalItem);
        boolean hotPotato = this.plugin.getConfigManager().isHotPotatoModeEnabled();
        ItemStack hotPotatoToggle = this.createGuiButton(hotPotato ? Material.BLAZE_POWDER : Material.GRAY_DYE, "\u00a7e\u00a7lHot Potato Mode: " + (hotPotato ? "\u00a7aEnabled" : "\u00a7cDisabled"), List.of("\u00a77Experimental runner swap variant"), "hot_potato_toggle");
        inventory.setItem(17, hotPotatoToggle);
        ItemStack adminHeader = this.createItem(Material.BOOK, "\u00a76\u00a7lAdmin Tools", List.of("\u00a77Operator utilities: force actions"));
        inventory.setItem(33, adminHeader);
        ItemStack forceSwap = this.createGuiButton(Material.ENDER_PEARL, "\u00a7d\u00a7lForce Runner Swap", List.of("\u00a77Trigger immediate runner swap"), "force_swap");
        inventory.setItem(28, forceSwap);
        ItemStack forceHunterShuffle = this.createGuiButton(Material.CROSSBOW, "\u00a7c\u00a7lShuffle Hunters", List.of("\u00a77Shuffle hunter order now"), "force_hunter_shuffle");
        inventory.setItem(29, forceHunterShuffle);
        ItemStack updateCompasses = this.createGuiButton(Material.LODESTONE, "\u00a7b\u00a7lUpdate Compasses", List.of("\u00a77Refresh all hunter compasses"), "update_compasses");
        inventory.setItem(30, updateCompasses);
        ArrayList<String> timerHeaderLore = new ArrayList<String>();
        timerHeaderLore.add("\u00a77Configure timer visibility:");
        timerHeaderLore.add("\u00a77\u2022 Active runner settings");
        timerHeaderLore.add("\u00a77\u2022 Waiting runner settings");
        timerHeaderLore.add("\u00a77\u2022 Hunter settings");
        ItemStack timerHeader = this.createItem(Material.BOOK, "\u00a76\u00a7lTimer Settings", timerHeaderLore);
        inventory.setItem(18, timerHeader);
        String runnerVisibility = this.plugin.getConfigManager().getRunnerTimerVisibility();
        ArrayList<String> runnerTimerLore = new ArrayList<String>();
        runnerTimerLore.add("\u00a77Current: " + this.getVisibilityDisplay(runnerVisibility));
        runnerTimerLore.add("");
        runnerTimerLore.add("\u00a77Options:");
        runnerTimerLore.add("\u00a77\u2022 Always show timer");
        runnerTimerLore.add("\u00a77\u2022 Show last 10 seconds");
        runnerTimerLore.add("\u00a77\u2022 Never show timer");
        runnerTimerLore.add("");
        runnerTimerLore.add("\u00a77Click to change");
        ItemStack runnerTimer = this.createItem(Material.CLOCK, "\u00a7e\u00a7lActive Runner Timer", runnerTimerLore);
        inventory.setItem(19, runnerTimer);
        String waitingVisibility = this.plugin.getConfigManager().getWaitingTimerVisibility();
        ArrayList<String> waitingTimerLore = new ArrayList<String>();
        waitingTimerLore.add("\u00a77Current: " + this.getVisibilityDisplay(waitingVisibility));
        waitingTimerLore.add("");
        waitingTimerLore.add("\u00a77Options:");
        waitingTimerLore.add("\u00a77\u2022 Always show timer");
        waitingTimerLore.add("\u00a77\u2022 Show last 10 seconds");
        waitingTimerLore.add("\u00a77\u2022 Never show timer");
        waitingTimerLore.add("");
        waitingTimerLore.add("\u00a77Click to change");
        ItemStack waitingTimer = this.createItem(Material.CLOCK, "\u00a7e\u00a7lWaiting Runner Timer", waitingTimerLore);
        inventory.setItem(20, waitingTimer);
        String hunterVisibility = this.plugin.getConfigManager().getHunterTimerVisibility();
        ArrayList<String> hunterTimerLore = new ArrayList<String>();
        hunterTimerLore.add("\u00a77Current: " + this.getVisibilityDisplay(hunterVisibility));
        hunterTimerLore.add("");
        hunterTimerLore.add("\u00a77Options:");
        hunterTimerLore.add("\u00a77\u2022 Always show timer");
        hunterTimerLore.add("\u00a77\u2022 Show last 10 seconds");
        hunterTimerLore.add("\u00a77\u2022 Never show timer");
        hunterTimerLore.add("");
        hunterTimerLore.add("\u00a77Click to change");
        ItemStack hunterTimer = this.createItem(Material.CLOCK, "\u00a7e\u00a7lHunter Timer", hunterTimerLore);
        inventory.setItem(21, hunterTimer);
        ArrayList<String> freezeHeaderLore = new ArrayList<String>();
        freezeHeaderLore.add("\u00a77Freeze/slow hunters near the runner");
        ItemStack freezeHeader = this.createItem(Material.BOOK, "\u00a76\u00a7lFreeze Mechanic", freezeHeaderLore);
        inventory.setItem(36, freezeHeader);
        boolean freezeEnabled = this.plugin.getConfigManager().isFreezeMechanicEnabled();
        ItemStack freezeToggle = this.createGuiButton(freezeEnabled ? Material.BLUE_ICE : Material.GRAY_DYE, "\u00a7e\u00a7lFreeze Mechanic: " + (freezeEnabled ? "\u00a7aEnabled" : "\u00a7cDisabled"), List.of("\u00a77Toggle freeze mechanic"), "freeze_toggle");
        inventory.setItem(37, freezeToggle);
        String mode = this.plugin.getConfigManager().getFreezeMode();
        ItemStack freezeMode = this.createGuiButton(Material.SNOWBALL, "\u00a7e\u00a7lFreeze Mode: \u00a7b" + mode, List.of("\u00a77Cycle: EFFECTS \u2192 SPECTATOR \u2192 LIMBO"), "freeze_mode");
        inventory.setItem(38, freezeMode);
        int freezeDuration = this.plugin.getConfigManager().getFreezeDurationTicks();
        ItemStack freezeDurationItem = this.createGuiButton(Material.CLOCK, "\u00a7e\u00a7lFreeze Duration", List.of("\u00a77Current: \u00a7e" + freezeDuration + " ticks", "\u00a77Left/Right: \u00b120", "\u00a77Shift: \u00b1100"), "freeze_duration");
        inventory.setItem(39, freezeDurationItem);
        int freezeInterval = this.plugin.getConfigManager().getFreezeCheckIntervalTicks();
        ItemStack freezeIntervalItem = this.createGuiButton(Material.REPEATER, "\u00a7e\u00a7lFreeze Check Interval", List.of("\u00a77Current: \u00a7e" + freezeInterval + " ticks", "\u00a77Left/Right: \u00b15", "\u00a77Shift: \u00b120"), "freeze_check_interval");
        inventory.setItem(40, freezeIntervalItem);
        int freezeDistance = (int)this.plugin.getConfigManager().getFreezeMaxDistance();
        ItemStack freezeDistanceItem = this.createGuiButton(Material.SPYGLASS, "\u00a7e\u00a7lFreeze Max Distance", List.of("\u00a77Current: \u00a7e" + freezeDistance + " blocks", "\u00a77Left/Right: \u00b15", "\u00a77Shift: \u00b120"), "freeze_max_distance");
        inventory.setItem(41, freezeDistanceItem);
        player.openInventory(inventory);
    }

    public void openKitsMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, (int)27, (Component)Component.text((String)"\u00a7b\u00a7lKits"));
        ItemStack filler = this.createItem(Material.BLACK_STAINED_GLASS_PANE, " ", new String[0]);
        this.fillBorder(inv, filler);
        ItemStack back = this.createItem(Material.ARROW, "\u00a77\u00a7lBack", List.of("\u00a77Return to main menu"));
        inv.setItem(0, back);
        boolean enabled = this.plugin.getConfigManager().isKitsEnabled();
        ItemStack toggle = this.createItem(enabled ? Material.LIME_DYE : Material.GRAY_DYE, "\u00a7e\u00a7lKits: " + (enabled ? "\u00a7aEnabled" : "\u00a7cDisabled"), List.of("\u00a77Click to toggle kit system"));
        inv.setItem(4, toggle);
        ItemStack applyRunner = this.createItem(Material.DIAMOND_BOOTS, "\u00a7b\u00a7lGive Runner Kit", List.of("\u00a77Click to receive the runner kit"));
        inv.setItem(11, applyRunner);
        ItemStack applyHunter = this.createItem(Material.IRON_SWORD, "\u00a7c\u00a7lGive Hunter Kit", List.of("\u00a77Click to receive the hunter kit"));
        inv.setItem(15, applyHunter);
        ItemStack editRunner = this.createItem(Material.CRAFTING_TABLE, "\u00a7b\u00a7lEdit Runner Kit", List.of("\u00a77Click to edit the runner kit"));
        inv.setItem(20, editRunner);
        ItemStack editHunter = this.createItem(Material.CRAFTING_TABLE, "\u00a7c\u00a7lEdit Hunter Kit", List.of("\u00a77Click to edit the hunter kit"));
        inv.setItem(24, editHunter);
        player.openInventory(inv);
    }

    public void openKitEditor(Player player, String kitType) {
        Inventory inv = Bukkit.createInventory(null, (int)54, (Component)Component.text((String)("\u00a7e\u00a7lEdit " + kitType + " Kit")));
        List<ItemStack> items = this.plugin.getKitManager().loadKitItems(this.plugin.getKitConfigManager().getConfig().getConfigurationSection("kits." + kitType));
        ItemStack[] armor = this.plugin.getKitManager().loadKitArmor(this.plugin.getKitConfigManager().getConfig().getConfigurationSection("kits." + kitType));
        for (ItemStack item : items) {
            inv.addItem(new ItemStack[]{item});
        }
        inv.setItem(45, armor[3]);
        inv.setItem(46, armor[2]);
        inv.setItem(47, armor[1]);
        inv.setItem(48, armor[0]);
        ItemStack save = this.createItem(Material.GREEN_CONCRETE, "\u00a7a\u00a7lSave Kit", List.of("\u00a77Save the current inventory as the " + kitType + " kit"));
        inv.setItem(53, save);
        player.openInventory(inv);
    }

    public void openBountyMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, (int)27, (Component)Component.text((String)"\u00a76\u00a7lBounty System"));
        ItemStack filler = this.createItem(Material.BLACK_STAINED_GLASS_PANE, " ", new String[0]);
        this.fillBorder(inv, filler);
        ItemStack back = this.createItem(Material.ARROW, "\u00a77\u00a7lBack", List.of("\u00a77Return to main menu"));
        inv.setItem(0, back);
        boolean active = this.plugin.getBountyManager().isActive();
        ItemStack status = this.createItem(active ? Material.GOLD_BLOCK : Material.COAL_BLOCK, "\u00a76\u00a7lBounty Status: " + (active ? "\u00a7aActive" : "\u00a7cInactive"), List.of("\u00a77Assign or clear the bounty target"));
        inv.setItem(4, status);
        ItemStack assign = this.createItem(Material.GOLDEN_APPLE, "\u00a7e\u00a7lAssign New Bounty", List.of("\u00a77Click to randomly choose a runner as bounty"));
        inv.setItem(11, assign);
        ItemStack clear = this.createItem(Material.BARRIER, "\u00a7c\u00a7lClear Bounty", List.of("\u00a77Click to clear current bounty"));
        inv.setItem(15, clear);
        player.openInventory(inv);
    }

    public void openLastStandMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, (int)27, (Component)Component.text((String)"\u00a7e\u00a7lLast Stand"));
        ItemStack filler = this.createItem(Material.BLACK_STAINED_GLASS_PANE, " ", new String[0]);
        this.fillBorder(inv, filler);
        ItemStack back = this.createItem(Material.ARROW, "\u00a77\u00a7lBack", List.of("\u00a77Return to main menu"));
        inv.setItem(0, back);
        boolean enabled = this.plugin.getConfigManager().isLastStandEnabled();
        ItemStack toggle = this.createItem(enabled ? Material.TOTEM_OF_UNDYING : Material.BARRIER, "\u00a7e\u00a7lLast Stand: " + (enabled ? "\u00a7aEnabled" : "\u00a7cDisabled"), List.of("\u00a77Click to toggle Last Stand feature"));
        inv.setItem(4, toggle);
        int duration = this.plugin.getConfigManager().getLastStandDuration();
        ItemStack durationItem = this.createItem(Material.CLOCK, "\u00a76\u00a7lLast Stand Duration", List.of("\u00a77Current: \u00a7f" + duration + " ticks", "\u00a77Left/Right click to adjust by 100 ticks"));
        inv.setItem(11, durationItem);
        int strength = this.plugin.getConfigManager().getLastStandStrengthAmplifier();
        ItemStack strengthItem = this.createItem(Material.BLAZE_POWDER, "\u00a7e\u00a7lStrength Amplifier", List.of("\u00a77Current: \u00a7f" + strength, "\u00a77Left/Right click to adjust"));
        inv.setItem(15, strengthItem);
        player.openInventory(inv);
    }

    public void openCompassSettingsMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, (int)27, (Component)Component.text((String)"\u00a79\u00a7lCompass Settings"));
        ItemStack filler = this.createItem(Material.BLACK_STAINED_GLASS_PANE, " ", new String[0]);
        this.fillBorder(inv, filler);
        ItemStack back = this.createItem(Material.ARROW, "\u00a77\u00a7lBack", List.of("\u00a77Return to main menu"));
        inv.setItem(0, back);
        boolean enabled = this.plugin.getConfigManager().isCompassJammingEnabled();
        ItemStack jamToggle = this.createItem(enabled ? Material.REDSTONE_BLOCK : Material.GRAY_DYE, "\u00a7e\u00a7lCompass Jamming: " + (enabled ? "\u00a7aEnabled" : "\u00a7cDisabled"), List.of("\u00a77Toggle compass jamming feature"));
        inv.setItem(11, jamToggle);
        int jamDuration = this.plugin.getConfigManager().getCompassJamDuration();
        ItemStack jamDurationItem = this.createItem(Material.CLOCK, "\u00a76\u00a7lJam Duration (ticks)", List.of("\u00a77Current: \u00a7f" + jamDuration, "\u00a77Left/Right click to +/- 20 ticks"));
        inv.setItem(15, jamDurationItem);
        World world = player.getWorld();
        Location hint = this.plugin.getConfigManager().getEndPortalHint(world);
        String hintLine = hint != null ? "\u00a7aSet at \u00a7f" + hint.getBlockX() + ", " + hint.getBlockY() + ", " + hint.getBlockZ() : "\u00a7cNot set";
        ItemStack setHint = this.createItem(Material.LODESTONE, "\u00a7e\u00a7lSet End Portal Hint (this world)", List.of("\u00a77Used when target is in The End", hintLine));
        ItemStack clearHint = this.createItem(Material.BARRIER, "\u00a7c\u00a7lClear End Portal Hint (this world)", List.of("\u00a77Remove stored hint for this world"));
        inv.setItem(20, setHint);
        inv.setItem(22, clearHint);
        player.openInventory(inv);
    }

    public void openSuddenDeathMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, (int)27, (Component)Component.text((String)"\u00a74\u00a7lSudden Death"));
        ItemStack filler = this.createItem(Material.BLACK_STAINED_GLASS_PANE, " ", new String[0]);
        this.fillBorder(inv, filler);
        ItemStack back = this.createItem(Material.ARROW, "\u00a77\u00a7lBack", List.of("\u00a77Return to main menu"));
        inv.setItem(0, back);
        boolean active = this.plugin.getSuddenDeathManager().isActive();
        ItemStack status = this.createItem(active ? Material.DRAGON_EGG : Material.END_STONE, "\u00a74\u00a7lSudden Death: " + (active ? "\u00a7aActive" : "\u00a7cInactive"), List.of("\u00a77Control sudden death behavior"));
        inv.setItem(4, status);
        boolean scheduled = this.plugin.getSuddenDeathManager().isScheduled();
        ItemStack schedule = this.createItem(Material.CLOCK, "\u00a7e\u00a7lSchedule Sudden Death", List.of("\u00a77Left-click to schedule using config delay", "\u00a77Click to schedule now"));
        inv.setItem(11, schedule);
        ItemStack activate = this.createItem(Material.DRAGON_HEAD, "\u00a7c\u00a7lActivate Now", List.of("\u00a77Activate sudden death immediately"));
        inv.setItem(15, activate);
        ItemStack cancel = this.createItem(Material.BARRIER, "\u00a7e\u00a7lCancel Scheduled Sudden Death", List.of(scheduled ? "\u00a77A schedule is pending" : "\u00a77No schedule pending"));
        inv.setItem(13, cancel);
        long delaySeconds = this.plugin.getConfig().getLong("sudden_death.activation_delay", 1200L);
        long delayMinutes = Math.max(1L, delaySeconds) / 60L;
        ItemStack delayItem = this.createItem(Material.CLOCK, "\u00a76\u00a7lActivation Delay (minutes)", List.of("\u00a77Current: \u00a7f" + delayMinutes + " minutes", "\u00a77Left/Right to +/- 5 minutes"));
        inv.setItem(22, delayItem);
        player.openInventory(inv);
    }

    public void openStatisticsMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, (int)27, (Component)Component.text((String)"\u00a7a\u00a7lStatistics"));
        ItemStack filler = this.createItem(Material.BLACK_STAINED_GLASS_PANE, " ", new String[0]);
        this.fillBorder(inv, filler);
        ItemStack back = this.createItem(Material.ARROW, "\u00a77\u00a7lBack", List.of("\u00a77Return to main menu"));
        inv.setItem(0, back);
        ItemStack display = this.createItem(Material.BOOK, "\u00a7e\u00a7lDisplay Statistics", List.of("\u00a77Click to broadcast current game statistics"));
        inv.setItem(11, display);
        ItemStack start = this.createItem(Material.GREEN_CONCRETE, "\u00a7a\u00a7lStart Tracking", List.of("\u00a77Click to start stats tracking for current game"));
        inv.setItem(15, start);
        ItemStack stop = this.createItem(Material.RED_CONCRETE, "\u00a7c\u00a7lStop Tracking", List.of("\u00a77Click to stop and broadcast stats"));
        inv.setItem(22, stop);
        player.openInventory(inv);
    }

    private int clampRows(int rows) {
        if (rows < 1) {
            return 1;
        }
        if (rows > 6) {
            return 6;
        }
        return rows;
    }

    public ItemStack createItem(Material material, String name, String ... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName((Component)Component.text((String)name));
        if (lore.length > 0) {
            ArrayList<TextComponent> loreList = new ArrayList<TextComponent>();
            for (String line : lore) {
                loreList.add(Component.text((String)line));
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
        meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
        glowingItem.setItemMeta(meta);
        return glowingItem;
    }

    public ItemStack createNormalItem(ItemStack item) {
        ItemStack normalItem = item.clone();
        ItemMeta meta = normalItem.getItemMeta();
        meta.removeEnchant(Enchantment.UNBREAKING);
        meta.removeItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
        normalItem.setItemMeta(meta);
        return normalItem;
    }

    public ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName((Component)Component.text((String)name));
        if (lore != null && !lore.isEmpty()) {
            ArrayList<TextComponent> componentLore = new ArrayList<TextComponent>();
            for (String line : lore) {
                componentLore.add(Component.text((String)line));
            }
            meta.lore(componentLore);
        }
        item.setItemMeta(meta);
        return item;
    }

    private void fillBorder(Inventory inv, ItemStack filler) {
        if (inv == null || filler == null) {
            return;
        }
        int size = inv.getSize();
        if (size < 9) {
            return;
        }
        int rows = size / 9;
        for (int c = 0; c < 9; ++c) {
            inv.setItem(c, filler);
            inv.setItem((rows - 1) * 9 + c, filler);
        }
        for (int r = 0; r < rows; ++r) {
            inv.setItem(r * 9, filler);
            inv.setItem(r * 9 + 8, filler);
        }
    }

    private List<String> getStatusLore() {
        ArrayList<String> lore = new ArrayList<String>();
        lore.add("\u00a77Game Running: " + (this.plugin.getGameManager().isGameRunning() ? "\u00a7aYes" : "\u00a7cNo"));
        lore.add("\u00a77Game Paused: " + (this.plugin.getGameManager().isGamePaused() ? "\u00a7eYes" : "\u00a7aNo"));
        if (this.plugin.getGameManager().isGameRunning()) {
            Player activeRunner = this.plugin.getGameManager().getActiveRunner();
            lore.add("\u00a77Active Runner: \u00a7f" + (activeRunner != null ? activeRunner.getName() : "None"));
            lore.add("\u00a77Next Swap: \u00a7f" + this.plugin.getGameManager().getTimeUntilNextSwap() + "s");
            lore.add("");
            lore.add("\u00a7bRunners:");
            for (Player runner : this.plugin.getGameManager().getRunners()) {
                lore.add("\u00a77- \u00a7f" + runner.getName());
            }
            lore.add("");
            lore.add("\u00a7cHunters:");
            for (Player hunter : this.plugin.getGameManager().getHunters()) {
                lore.add("\u00a77- \u00a7f" + hunter.getName());
            }
        }
        return lore;
    }

    private boolean isItem(ItemStack item, Material material, String name) {
        if (item == null || item.getType() != material || !item.hasItemMeta()) {
            return false;
        }
        String displayName = PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
        return name.equals(displayName);
    }

    public boolean isMainMenu(Inventory inventory) {
        if (inventory == null || inventory.getHolder() != null || inventory.getViewers().isEmpty()) {
            return false;
        }
        String title = PlainTextComponentSerializer.plainText().serialize(((HumanEntity)inventory.getViewers().get(0)).getOpenInventory().title());
        return this.plugin.getConfigManager().getGuiMainMenuTitle().equals(title);
    }

    public boolean isTeamSelector(Inventory inventory) {
        if (inventory == null || inventory.getHolder() != null || inventory.getViewers().isEmpty()) {
            return false;
        }
        String title = PlainTextComponentSerializer.plainText().serialize(((HumanEntity)inventory.getViewers().get(0)).getOpenInventory().title());
        return this.plugin.getConfigManager().getGuiTeamSelectorTitle().equals(title);
    }

    public boolean isSettingsMenu(Inventory inventory) {
        if (inventory == null || inventory.getHolder() != null || inventory.getViewers().isEmpty()) {
            return false;
        }
        String title = PlainTextComponentSerializer.plainText().serialize(((HumanEntity)inventory.getViewers().get(0)).getOpenInventory().title());
        return this.plugin.getConfigManager().getGuiSettingsTitle().equals(title);
    }

    public boolean isBackButton(ItemStack item) {
        return this.isItem(item, Material.ARROW, "\u00a77\u00a7lBack");
    }

    public boolean isStartButton(ItemStack item) {
        return this.isItem(item, Material.GREEN_CONCRETE, "\u00a7a\u00a7lStart Game");
    }

    public boolean isStopButton(ItemStack item) {
        return this.isItem(item, Material.RED_CONCRETE, "\u00a7c\u00a7lStop Game");
    }

    public boolean isPauseButton(ItemStack item) {
        return this.isItem(item, Material.YELLOW_CONCRETE, "\u00a7e\u00a7lPause Game");
    }

    public boolean isResumeButton(ItemStack item) {
        return this.isItem(item, Material.LIME_CONCRETE, "\u00a7a\u00a7lResume Game");
    }

    public boolean isTeamSelectorButton(ItemStack item) {
        return this.isItem(item, Material.PLAYER_HEAD, "\u00a7e\u00a7lTeam Selector");
    }

    public boolean isSettingsButton(ItemStack item) {
        return this.isItem(item, Material.COMPARATOR, "\u00a7a\u00a7lSettings");
    }

    public boolean isRunnerTeamButton(ItemStack item) {
        return this.isItem(item, Material.DIAMOND_BOOTS, "\u00a7b\u00a7lRunners");
    }

    public boolean isHunterTeamButton(ItemStack item) {
        return this.isItem(item, Material.IRON_SWORD, "\u00a7c\u00a7lHunters");
    }

    public ItemStack createGuiButton(Material material, String name, List<String> lore, String buttonId) {
        ItemStack item = this.createItem(material, name, lore);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(new NamespacedKey((Plugin)this.plugin, BUTTON_ID_KEY), PersistentDataType.STRING, (Object)buttonId);
            item.setItemMeta(meta);
        }
        return item;
    }

    public String getButtonId(ItemStack item) {
        if (item != null && item.hasItemMeta()) {
            return (String)item.getItemMeta().getPersistentDataContainer().get(new NamespacedKey((Plugin)this.plugin, BUTTON_ID_KEY), PersistentDataType.STRING);
        }
        return null;
    }

    public boolean isSwapIntervalButton(ItemStack item) {
        String buttonId = this.getButtonId(item);
        return "swap_interval".equals(buttonId) && item != null && item.getType() == Material.CLOCK;
    }

    public boolean isRandomizeSwapButton(ItemStack item) {
        String buttonId = this.getButtonId(item);
        return "random_swaps".equals(buttonId) && item != null && (item.getType() == Material.CLOCK || item.getType() == Material.REPEATER);
    }

    public boolean isSafeSwapButton(ItemStack item) {
        String buttonId = this.getButtonId(item);
        return "safe_swaps".equals(buttonId) && item != null && (item.getType() == Material.TOTEM_OF_UNDYING || item.getType() == Material.BARRIER);
    }

    public boolean isActiveRunnerTimerButton(ItemStack item) {
        return this.isItem(item, Material.CLOCK, "\u00a7e\u00a7lActive Runner Timer");
    }

    public boolean isWaitingRunnerTimerButton(ItemStack item) {
        return this.isItem(item, Material.CLOCK, "\u00a7e\u00a7lWaiting Runner Timer");
    }

    public boolean isHunterTimerButton(ItemStack item) {
        return this.isItem(item, Material.CLOCK, "\u00a7e\u00a7lHunter Timer");
    }

    private String getVisibilityDisplay(String visibility) {
        switch (visibility) {
            case "always": {
                return "\u00a7aAlways Show";
            }
            case "last_10": {
                return "\u00a7eLast 10 Seconds";
            }
            case "never": {
                return "\u00a7cNever Show";
            }
        }
        return "\u00a77Unknown";
    }

    public String getNextVisibility(String current) {
        switch (current) {
            case "always": {
                return "last_10";
            }
            case "last_10": {
                return "never";
            }
            case "never": {
                return "always";
            }
        }
        return "last_10";
    }

    public void setPlayerTeam(Player player, Team team) {
        this.plugin.getGameManager().getPlayerState(player).setSelectedTeam(team);
        this.updateTeamSelectors();
    }

    public void updateTeamSelectors() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!this.isTeamSelector(player.getOpenInventory().getTopInventory())) continue;
            this.openTeamSelector(player);
        }
    }

    public void updateMainMenus() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!this.isMainMenu(player.getOpenInventory().getTopInventory())) continue;
            this.openMainMenu(player);
        }
    }

    public void updateSettingsMenus() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!this.isSettingsMenu(player.getOpenInventory().getTopInventory())) continue;
            this.openSettingsMenu(player);
        }
    }

    public Team getSelectedTeam(Player player) {
        return this.plugin.getGameManager().getPlayerState(player).getSelectedTeam();
    }

    public boolean isSwapRandomized() {
        return this.plugin.getConfigManager().isSwapRandomized();
    }
}

