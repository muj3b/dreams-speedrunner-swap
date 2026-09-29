package com.example.speedrunnerswap.game;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import com.example.speedrunnerswap.utils.*;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.junit.*;
import org.mockbukkit.mockbukkit.*;
import static org.junit.Assert.*;
import java.util.List;

/** The same tests execute against 1.21.11/Adventure 4 and 26.2/Adventure 5. */
public class CompatibilityTest {
    private ServerMock server;
    private SpeedrunnerSwap plugin;
    @Before public void setup() {
        server=MockBukkit.mock();server.addSimpleWorld("world");
        plugin=MockBukkit.load(SpeedrunnerSwap.class);
    }
    @After public void cleanup() { MockBukkit.unmock(); }

    private World arena() {
        // addSimpleWorld changes the name but retains the default key in MockBukkit.
        var world = new org.mockbukkit.mockbukkit.world.WorldMock() {
            @Override public NamespacedKey getKey() { return new NamespacedKey("test", "arena"); }
        };
        world.setName("arena"); server.addWorld(world); return world;
    }

    @Test public void legacyColorCodesAreParsedRatherThanDisplayed() {
        var component=TextUtil.component("§aHunter §lA");
        assertEquals("Hunter A",PlainTextComponentSerializer.plainText().serialize(component));
        assertEquals(NamedTextColor.GREEN,component.color());
        assertEquals("",PlainTextComponentSerializer.plainText().serialize(TextUtil.component(null)));
    }
    @Test public void inventoryMetadataPreservesColorsAndPlainLabels() {
        ItemStack item=new ItemStack(Material.COMPASS);
        var meta=item.getItemMeta();
        GuiCompat.setDisplayName(meta,"§aTracker");
        GuiCompat.setLore(meta,List.of("§eGroup A", "§7Next player"));
        assertEquals("Tracker",GuiCompat.getDisplayName(meta));
        assertEquals(NamedTextColor.GREEN,meta.displayName().color());
        assertEquals(List.of("Group A","Next player"),GuiCompat.getLore(meta));
    }
    @Test public void potionAliasesAndNamespacedKeysResolve() {
        assertEquals(PotionEffectType.STRENGTH,BukkitCompat.resolvePotionEffect("increase_damage"));
        assertEquals(PotionEffectType.SLOWNESS,BukkitCompat.resolvePotionEffect("minecraft:slowness"));
        assertNull(BukkitCompat.resolvePotionEffect("not a valid key"));
        assertNull(BukkitCompat.resolvePotionEffect("missing_effect"));
    }
    @Test public void maxHealthUsesServerAttributeNotHardcodedFallback() {
        var player=server.addPlayer();
        var attribute=BukkitCompat.resolveAttribute("GENERIC_MAX_HEALTH","MAX_HEALTH");
        assertNotNull(attribute);player.getAttribute(attribute).setBaseValue(40);
        assertEquals(40,BukkitCompat.getMaxHealthValue(player),0.001);
    }
    @Test public void keysWinOverStaleNamesAndNamesRemainBackwardCompatible() {
        World arena=arena();
        assertEquals(arena,WorldCompat.resolve(arena.getKey().toString(),"old_arena"));
        assertEquals(arena,WorldCompat.resolve(null,"arena"));
        assertEquals(arena,WorldCompat.resolve("invalid key!","arena"));
        assertEquals(arena,WorldCompat.resolve(null,arena.getKey().toString()));
        assertNull(WorldCompat.resolve("missing:world","old_arena"));
    }
    @Test public void oldConfigAndNewWorldKeysCoexistWithoutLosingSettings() {
        var config=plugin.getConfig();
        config.set("spawn.world","world");config.set("swap.interval",77);
        config.set("swap.shared_hunter_control.groups.A.players",List.of("Alice","Bob"));
        plugin.saveConfig();plugin.getConfigManager().loadConfig();
        assertEquals("world",plugin.getConfigManager().getSpawnLocation().getWorld().getName());
        World arena=arena();
        plugin.getConfigManager().setGlobalSpawn(new Location(arena,42,80,16),false);
        plugin.getConfigManager().loadConfig();
        assertEquals(arena,plugin.getConfigManager().getSpawnLocation().getWorld());
        assertEquals(42,plugin.getConfigManager().getSpawnLocation().getX(),0.01);
        assertEquals(77,plugin.getConfig().getInt("swap.interval"));
        assertEquals(List.of("Alice","Bob"),plugin.getGameManager().getHunterGroups().definitions().get("A"));
        plugin.getConfig().set("limbo.world_key",arena.getKey().toString());
        plugin.getConfig().set("limbo.world","renamed_world");
        assertEquals(arena,plugin.getConfigManager().getLimboLocation().getWorld());
    }
    @Test public void invalidKitEntriesDoNotDiscardValidItemsOrOtherArmorSlots() {
        var config=new YamlConfiguration();
        config.set("items",List.of("minecraft:diamond 2","NOT_A_REAL_ITEM 1","STONE -2","EMERALD 3"));
        var items=plugin.getKitManager().loadKitItems(config);
        assertEquals(2,items.size());assertEquals(Material.DIAMOND,items.get(0).getType());
        assertEquals(Material.EMERALD,items.get(1).getType());
        config.set("armor.boots","NOT_A_REAL_ITEM");config.set("armor.helmet","minecraft:diamond_helmet");
        var armor=plugin.getKitManager().loadKitArmor(config);
        assertNull(armor[0]);assertEquals(Material.DIAMOND_HELMET,armor[3].getType());
    }
    @Test public void fishingWildcardIsNotTreatedAsAnUnavailableMaterial() {
        var config = plugin.getTaskConfigManager();
        config.getConfig().set("tasks", List.of(java.util.Map.of(
                "id", "fish_any", "type", "FISH_ITEM", "enabled", true)));
        config.saveConfig();
        plugin.getTaskManagerMode().reloadTasksFromFile();
        assertTrue(plugin.getTaskManagerMode().getCandidateTaskIds().contains("fish_any"));
    }
    @Test public void unavailableTaskMaterialsAreExcludedWithoutRewritingSavedDefinitions() {
        var config = plugin.getTaskConfigManager();
        config.getConfig().set("tasks", List.of(
                java.util.Map.of("id", "craft_not_a_real_material", "type", "CRAFT_ITEM", "enabled", true),
                java.util.Map.of("id", "craft_diamond_sword", "type", "CRAFT_ITEM", "enabled", true),
                java.util.Map.of("id", "craft_special_challenge", "type", "COMPLEX_TASK", "enabled", true)));
        config.saveConfig();
        plugin.getTaskManagerMode().reloadTasksFromFile();
        var candidates = plugin.getTaskManagerMode().getCandidateTaskIds();
        assertFalse(candidates.contains("craft_not_a_real_material"));
        assertTrue(candidates.contains("craft_diamond_sword"));
        assertTrue(candidates.contains("craft_special_challenge"));
        plugin.getTaskManagerMode().setTaskEnabled("craft_not_a_real_material", true);
        assertFalse(plugin.getTaskManagerMode().getCandidateTaskIds().contains("craft_not_a_real_material"));
        assertEquals(Boolean.TRUE, config.getConfig().getMapList("tasks").get(0).get("enabled"));
    }
}
