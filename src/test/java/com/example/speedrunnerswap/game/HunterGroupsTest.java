package com.example.speedrunnerswap.game;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import org.bukkit.*;
import org.bukkit.inventory.ItemStack;
import org.junit.*;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import java.util.List;
import static org.junit.Assert.*;

public class HunterGroupsTest {
    private ServerMock server;
    private SpeedrunnerSwap plugin;
    private GameManager game;
    private PlayerMock runner, a, b, c, d;

    @Before public void setup() {
        server = MockBukkit.mock();
        server.addSimpleWorld("world");
        plugin = MockBukkit.load(SpeedrunnerSwap.class);
        plugin.getConfig().set("freeze_mode", "SPECTATOR");
        plugin.getConfig().set("stats.enabled", false);
        plugin.getConfig().set("spawn.force_global", false);
        plugin.getConfigManager().loadConfig();
        plugin.getConfig().set("spawn.force_global", false);
        plugin.getConfig().set("freeze_mode", "SPECTATOR");
        plugin.getConfig().set("stats.enabled", false);
        runner = player("Runner");
        a = player("Alice"); b = player("Bob");
        c = player("Charlie"); d = player("Dave");
        game = plugin.getGameManager();
        game.setRunners(List.of(runner)); game.setHunters(List.of(a,b,c,d));
        game.getHunterGroups().configure("A", List.of("Alice", "Bob"));
        game.getHunterGroups().configure("B", List.of("Charlie", "Dave"));
        game.getHunterGroups().setInterval("A", 10);
        game.getHunterGroups().setInterval("B", 20);
    }
    @After public void cleanup() { MockBukkit.unmock(); }
    private PlayerMock player(String name) {
        // MockBukkit does not implement last-damage tracking; unrelated to swaps.
        PlayerMock p = new PlayerMock(server, name) {
            @Override public double getLastDamage() { return 0; }
        };
        server.addPlayer(p); return p;
    }
    private void start() {
        assertTrue(game.startGame()); server.getScheduler().performTicks(61);
        assertTrue(game.isGameRunning());
    }
    @Test public void twoBodiesRotateIndependentlyAndRunnerStaysActive() {
        start();
        a.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 7));
        c.getInventory().setItem(0, new ItemStack(Material.EMERALD, 11));
        a.setHealth(13); a.setFoodLevel(9); a.setLevel(8);
        a.teleport(new Location(a.getWorld(), 50, 70, 60));
        server.getScheduler().performTicks(201);
        assertTrue(game.isActiveHunter(b)); assertTrue(game.isActiveHunter(c));
        assertEquals(7, b.getInventory().getItem(0).getAmount());
        assertEquals(Material.DIAMOND, b.getInventory().getItem(0).getType());
        assertEquals(13, b.getHealth(), 0.01); assertEquals(9, b.getFoodLevel()); assertEquals(8,b.getLevel());
        assertEquals(50,b.getLocation().getX(),0.01);
        assertFalse(a.getInventory().contains(Material.DIAMOND));
        assertEquals(11,c.getInventory().getItem(0).getAmount());
        server.getScheduler().performTicks(201);
        assertTrue(game.isActiveHunter(a)); assertTrue(game.isActiveHunter(d));
        assertEquals(Material.EMERALD,d.getInventory().getItem(0).getType());
        server.getScheduler().performTicks(1000);
        assertFalse(game.isGamePaused()); assertEquals(runner,game.getActiveRunner());
        assertEquals(2,game.getHunterGroups().activePlayers().size());
    }
    @Test public void invalidAssignmentsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> game.getHunterGroups().configure("C",List.of("Alice")));
        game.getHunterGroups().remove("B");
        assertNotNull(game.getHunterGroups().validate(game.getHunters()));
        assertFalse(game.startGame());
    }
    @Test public void pauseStopsEveryGroupAndResumeRestartsThem() {
        start(); assertTrue(game.pauseGame());
        server.getScheduler().performTicks(1000);
        assertTrue(game.isActiveHunter(a)); assertTrue(game.isActiveHunter(c));
        assertTrue(game.resumeGame()); server.getScheduler().performTicks(401);
        assertTrue(game.isActiveHunter(a)); assertTrue(game.isActiveHunter(d));
    }
    @Test public void stopCancelsAllGroupTasksAndSupportsAnotherRound() {
        start(); game.stopGame();
        assertFalse(game.isGameRunning()); assertTrue(game.getHunterGroups().activePlayers().isEmpty());
        server.getScheduler().performTicks(500);
        start(); assertTrue(game.isActiveHunter(a)); assertTrue(game.isActiveHunter(c));
    }
    @Test public void setupIsLockedDuringCountdownAndRound() {
        assertTrue(game.startGame()); assertFalse(game.startGame());
        assertThrows(IllegalArgumentException.class, () -> game.getHunterGroups().remove("A"));
        game.stopGame(); server.getScheduler().performTicks(100);
        assertFalse(game.isGameRunning());
    }

    @Test public void disconnectHandsOffOnlyItsOwnBodyAndRejoinCannotDuplicateItems() {
        plugin.getConfig().set("swap.pause_on_disconnect", false);
        start();
        a.getInventory().setItem(0,new ItemStack(Material.DIAMOND,17));
        a.disconnect();
        assertTrue(game.isActiveHunter(b)); assertTrue(game.isActiveHunter(c));
        assertEquals(17,b.getInventory().getItem(0).getAmount());
        a.reconnect(); server.getScheduler().performTicks(2);
        assertFalse(game.isActiveHunter(a)); assertFalse(a.getInventory().contains(Material.DIAMOND));
        assertFalse(game.isGamePaused());
    }

    @Test public void disconnectPauseWaitsForEveryMissingActiveController() {
        plugin.getConfig().set("swap.pause_on_disconnect", true);
        start();
        a.getInventory().setItem(0,new ItemStack(Material.DIAMOND,17));
        a.disconnect(); c.disconnect();
        assertTrue(game.isGamePaused()); assertFalse(game.resumeGame());
        a.reconnect(); server.getScheduler().performTicks(2);
        assertTrue(game.isGamePaused());
        c.reconnect(); server.getScheduler().performTicks(2);
        assertFalse(game.isGamePaused());
        assertEquals(17,a.getInventory().getItem(0).getAmount());
    }

    @Test public void cancelledTeleportLeavesControllerAndInventoryUntouched() {
        start(); a.getInventory().setItem(0,new ItemStack(Material.DIAMOND,7));
        server.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler public void cancel(org.bukkit.event.player.PlayerTeleportEvent event) {
                if (event.getPlayer().equals(b)) event.setCancelled(true);
            }
        },plugin);
        server.getScheduler().performTicks(201);
        assertTrue(game.isActiveHunter(a)); assertFalse(game.isActiveHunter(b));
        assertEquals(7,a.getInventory().getItem(0).getAmount());
        assertFalse(b.getInventory().contains(Material.DIAMOND));
    }

    @Test public void stateCopiesAreIndependentAndIncludeEffectsAndMotion() {
        start();
        a.getInventory().setItem(0,new ItemStack(Material.DIAMOND,7));
        a.getInventory().setHeldItemSlot(4);
        a.setVelocity(new org.bukkit.util.Vector(0.1,0.2,0.3));
        a.setFreezeTicks(25);
        a.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED,1000,1));
        var snapshot=com.example.speedrunnerswap.utils.PlayerStateUtil.capturePlayerState(a);
        a.getInventory().getItem(0).setAmount(1);
        com.example.speedrunnerswap.utils.PlayerStateUtil.applyPlayerState(b,snapshot);
        assertEquals(7,b.getInventory().getItem(0).getAmount());
        assertEquals(4,b.getInventory().getHeldItemSlot()); assertEquals(25,b.getFreezeTicks());
        assertEquals(new org.bukkit.util.Vector(0.1,0.2,0.3),b.getVelocity());
        assertEquals(1,b.getPotionEffect(org.bukkit.potion.PotionEffectType.SPEED).getAmplifier());
        b.getInventory().getItem(0).setAmount(3);
        assertEquals(7,snapshot.getInventory()[0].getAmount());
    }

    @Test public void deathWaitsForRespawnAndNeverTransfersPreDeathInventory() {
        start(); a.getInventory().setItem(0,new ItemStack(Material.DIAMOND,7));
        a.setHealth(0);
        server.getScheduler().performTicks(201);
        assertTrue(game.isActiveHunter(a));
        a.respawn(); server.getScheduler().performTicks(21);
        assertTrue(game.isActiveHunter(b)); assertFalse(b.getInventory().contains(Material.DIAMOND));
    }

    @Test public void noGroupsPreservesLegacySingleHunterBody() {
        game.getHunterGroups().remove("A"); game.getHunterGroups().remove("B");
        start(); assertEquals(1,game.getHunterGroups().activePlayers().size());
        game.getHunterGroups().swapAll(); assertTrue(game.isActiveHunter(b));
    }

    @Test public void oneMemberGroupsRemainSeparateAndPlayable() {
        game.getHunterGroups().remove("A"); game.getHunterGroups().remove("B");
        for (PlayerMock p:List.of(a,b,c,d)) game.getHunterGroups().configure(p.getName(),List.of(p.getName()));
        start(); server.getScheduler().performTicks(1500);
        assertEquals(4,game.getHunterGroups().activePlayers().size()); assertFalse(game.isGamePaused());
    }

    @Test public void stoppingRestoresPreRoundInventoryAndCancelsPendingSwaps() {
        a.getInventory().setItem(0,new ItemStack(Material.GOLD_INGOT,3));
        start(); a.getInventory().setItem(0,new ItemStack(Material.DIAMOND,7));
        game.getHunterGroups().swapAll(); game.stopGame(); server.getScheduler().performTicks(500);
        assertEquals(Material.GOLD_INGOT,a.getInventory().getItem(0).getType());
        assertEquals(3,a.getInventory().getItem(0).getAmount());
        assertFalse(b.getInventory().contains(Material.DIAMOND));
    }

    @Test public void commandsPersistGroupsAndRejectUnauthorizedOrLiveChanges() {
        assertTrue(server.dispatchCommand(server.getConsoleSender(),"swap huntergroups interval A 15"));
        assertEquals(15,game.getHunterGroups().interval("A"));
        server.dispatchCommand(a,"swap huntergroups remove A");
        assertTrue(game.getHunterGroups().definitions().containsKey("A"));
        plugin.reloadConfig();
        assertEquals(List.of("Alice","Bob"),game.getHunterGroups().definitions().get("A"));
        assertEquals(15,game.getHunterGroups().interval("A"));
        start(); server.dispatchCommand(server.getConsoleSender(),"swap huntergroups remove A");
        assertTrue(game.getHunterGroups().definitions().containsKey("A"));
    }

    @Test public void groupMenuCyclesAssignmentsAndRemovesGroups() {
        a.setOp(true);
        plugin.getGuiManager().openHunterGroups(a);
        a.simulateInventoryClick(9); // Alice A -> B
        assertEquals(List.of("Bob"),game.getHunterGroups().definitions().get("A"));
        assertTrue(game.getHunterGroups().definitions().get("B").contains("Alice"));
        a.simulateInventoryClick(9); // B -> A
        assertTrue(game.getHunterGroups().definitions().get("A").contains("Alice"));
        a.simulateInventoryClick(36); // Group A settings
        a.simulateInventoryClick(a.getOpenInventory(),org.bukkit.event.inventory.ClickType.SHIFT_LEFT,15);
        assertFalse(game.getHunterGroups().definitions().containsKey("A"));
        assertNotNull(game.getHunterGroups().validate(game.getHunters()));
    }

    @Test public void allWaitingModesReleaseNextControllerWithoutLosingBodyEffects() {
        for (String mode:List.of("EFFECTS","LIMBO","CAGE","SPECTATOR")) {
            plugin.getConfig().set("freeze_mode",mode);
            start();
            a.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED,1000,1));
            a.getInventory().setItem(0,new ItemStack(Material.DIAMOND,7));
            game.getHunterGroups().swapAll();
            assertTrue(mode,game.isActiveHunter(b)); assertEquals(GameMode.SURVIVAL,b.getGameMode());
            assertTrue(b.hasPotionEffect(org.bukkit.potion.PotionEffectType.SPEED));
            assertFalse(b.hasPotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS));
            assertEquals(7,b.getInventory().getItem(0).getAmount());
            server.getScheduler().performTicks(21);
            assertEquals(GameMode.SURVIVAL,b.getGameMode());
            game.stopGame();
        }
    }

    @Test public void fullyOfflineGroupRecoversItsStateWithoutAffectingOtherGroup() {
        plugin.getConfig().set("swap.pause_on_disconnect",false); start();
        a.getInventory().setItem(0,new ItemStack(Material.DIAMOND,7));
        b.disconnect(); a.disconnect(); server.getScheduler().performTicks(201);
        b.reconnect(); server.getScheduler().performTicks(201);
        assertTrue(game.isActiveHunter(b)); assertEquals(7,b.getInventory().getItem(0).getAmount());
        assertEquals(2,game.getHunterGroups().activePlayers().size());
    }

    @Test public void runnerDeathEndsRoundAndRestoresAfterRespawn() {
        runner.getInventory().setItem(0,new ItemStack(Material.GOLD_INGOT,3));
        start(); runner.getInventory().setItem(0,new ItemStack(Material.DIAMOND,7));
        runner.setHealth(0); server.getScheduler().performTicks(2);
        assertFalse(game.isGameRunning());
        runner.respawn(); server.getScheduler().performTicks(2);
        assertEquals(Material.GOLD_INGOT,runner.getInventory().getItem(0).getType());
    }

    @Test public void disconnectedPlayerRestoresPreRoundStateAfterStop() {
        a.getInventory().setItem(0,new ItemStack(Material.GOLD_INGOT,3)); start();
        a.getInventory().setItem(0,new ItemStack(Material.DIAMOND,7));
        a.disconnect(); game.stopGame(); a.reconnect(); server.getScheduler().performTicks(2);
        assertEquals(Material.GOLD_INGOT,a.getInventory().getItem(0).getType());
    }

    @Test public void countdownCancelsWhenConfiguredMemberLeaves() {
        assertTrue(game.startGame()); b.disconnect(); server.getScheduler().performTicks(61);
        assertFalse(game.isGameRunning()); assertFalse(game.isSetupLocked());
    }

    @Test public void forcedModeChangeStopsExistingBodiesAndCancelsCountdown() {
        start();
        server.dispatchCommand(server.getConsoleSender(),"swap mode sapnap --force");
        assertFalse(game.isGameRunning()); assertTrue(game.getHunterGroups().activePlayers().isEmpty());
        assertEquals(SpeedrunnerSwap.SwapMode.SAPNAP,plugin.getCurrentMode());
    }

    @Test public void taskDuoKeepsItsOwnTwoBodiesAndIgnoresDreamGroups() {
        plugin.setCurrentMode(SpeedrunnerSwap.SwapMode.TASK_DUEL);
        game.setRunners(List.of(runner,a)); game.setHunters(List.of(c,d));
        plugin.getConfigManager().setSwapInterval(10);
        start();
        assertTrue(game.getHunterGroups().activePlayers().isEmpty());
        assertTrue(game.isActiveHunter(c)); assertEquals(runner,game.getActiveRunner());
        server.getScheduler().performTicks(201);
        assertTrue(game.isActiveHunter(d)); assertEquals(a,game.getActiveRunner());
        assertFalse(game.isGamePaused());
    }

    @Test public void taskRaceKeepsAllRunnersActiveWithoutHunterBodies() {
        plugin.setCurrentMode(SpeedrunnerSwap.SwapMode.TASK_RACE);
        game.setHunters(List.of()); game.setRunners(List.of(runner,a));
        start(); server.getScheduler().performTicks(401);
        assertEquals(GameMode.SURVIVAL,runner.getGameMode());
        assertEquals(GameMode.SURVIVAL,a.getGameMode());
        assertFalse(plugin.usesSharedRunnerControl());
        assertTrue(game.getHunterGroups().activePlayers().isEmpty()); assertFalse(game.isGamePaused());
    }

    @Test public void classicDreamKeepsAllHuntersWhenSharingIsDisabled() {
        plugin.getConfigManager().setSharedHunterControlEnabled(false); start();
        assertTrue(game.getHunterGroups().activePlayers().isEmpty());
        for (PlayerMock p:List.of(a,b,c,d)) assertEquals(GameMode.SURVIVAL,p.getGameMode());
        server.getScheduler().performTicks(1500); assertFalse(game.isGamePaused());
    }

    @Test public void pausePreservesRemainingTicksInsteadOfResettingInterval() {
        start(); server.getScheduler().performTicks(100);
        int remaining=game.getHunterGroups().seconds(a); game.pauseGame();
        server.getScheduler().performTicks(500); assertEquals(remaining,game.getHunterGroups().seconds(a));
        game.resumeGame(); server.getScheduler().performTicks(101);
        assertTrue(game.isActiveHunter(b)); assertTrue(game.isActiveHunter(c));
    }

    @Test public void portalDefersOnlyTheAffectedBody() {
        start();
        a.teleport(new Location(a.getWorld(),50,70,60));
        a.getLocation().getBlock().setType(Material.NETHER_PORTAL);
        server.getScheduler().performTicks(401);
        assertTrue(game.isActiveHunter(a)); assertTrue(game.isActiveHunter(d));
        a.getLocation().getBlock().setType(Material.AIR);
        server.getScheduler().performTicks(21); assertTrue(game.isActiveHunter(b));
    }

    @Test public void threeControllersPerBodyCompleteFullIndependentCycles() {
        PlayerMock e=player("Eve"), f=player("Frank");
        game.setHunters(List.of(a,b,c,d,e,f));
        game.getHunterGroups().configure("A",List.of("Alice","Bob","Eve"));
        game.getHunterGroups().configure("B",List.of("Charlie","Dave","Frank"));
        start();
        a.getInventory().setItem(0,new ItemStack(Material.DIAMOND,7));
        c.getInventory().setItem(0,new ItemStack(Material.EMERALD,11));
        for (int cycle=0;cycle<3;cycle++) {
            for (PlayerMock[] expected:List.of(new PlayerMock[]{b,d},new PlayerMock[]{e,f},new PlayerMock[]{a,c})) {
                game.getHunterGroups().swapAll();
                assertTrue(game.isActiveHunter(expected[0])); assertTrue(game.isActiveHunter(expected[1]));
                assertEquals(Material.DIAMOND,expected[0].getInventory().getItem(0).getType());
                assertEquals(Material.EMERALD,expected[1].getInventory().getItem(0).getType());
                assertEquals(2,game.getHunterGroups().activePlayers().size());
                assertEquals(runner,game.getActiveRunner());
            }
        }
    }

    @Test public void bodySpawnTransfersButOriginalSpawnAndEffectsReturnOnStop() {
        Location original=new Location(a.getWorld(),20,70,20);
        Location bodySpawn=new Location(a.getWorld(),40,70,40);
        a.setRespawnLocation(original,true);
        a.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.JUMP_BOOST,1200,2));
        start(); a.setRespawnLocation(bodySpawn,true);
        game.getHunterGroups().swapAll();
        assertEquals(bodySpawn,b.getRespawnLocation());
        assertNull(d.getRespawnLocation());
        game.stopGame();
        assertEquals(original,a.getRespawnLocation());
        assertNull(b.getRespawnLocation());
        assertEquals(2,a.getPotionEffect(org.bukkit.potion.PotionEffectType.JUMP_BOOST).getAmplifier());
    }

    @Test public void deadControllerDisconnectCannotResumeUntilRespawn() {
        start(); a.setHealth(0); a.disconnect();
        assertTrue(game.isGamePaused());
        a.reconnect(); server.getScheduler().performTicks(2);
        assertTrue(game.isGamePaused()); assertFalse(game.resumeGame());
        a.respawn(); server.getScheduler().performTicks(2);
        assertFalse(game.isGamePaused());
        server.getScheduler().performTicks(201); assertTrue(game.isActiveHunter(b));
    }

    @Test public void pluginDisableCancelsAllBodiesAndRestoresPlayers() {
        a.getInventory().setItem(0,new ItemStack(Material.GOLD_INGOT,3));
        start(); game.getHunterGroups().swapAll();
        server.getPluginManager().disablePlugin(plugin);
        assertFalse(game.isGameRunning()); assertTrue(game.getHunterGroups().activePlayers().isEmpty());
        assertEquals(Material.GOLD_INGOT,a.getInventory().getItem(0).getType());
        server.getScheduler().performTicks(500);
        assertFalse(game.isGameRunning());
    }
}
