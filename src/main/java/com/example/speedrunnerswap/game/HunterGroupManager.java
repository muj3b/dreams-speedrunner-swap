package com.example.speedrunnerswap.game;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import com.example.speedrunnerswap.models.PlayerState;
import com.example.speedrunnerswap.utils.PlayerStateUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;

/** One independent shared body per configured group. Membership is fixed for a round. */
public final class HunterGroupManager {
    private final SpeedrunnerSwap plugin;
    private final Map<String, Body> bodies = new LinkedHashMap<>();
    private final Set<UUID> disconnected = new HashSet<>();
    private static final String ROOT = "swap.shared_hunter_control.groups";

    private static final class Body {
        final String name;
        final List<UUID> members;
        final int interval;
        UUID active;
        PlayerState saved;
        Location spawn;
        BukkitTask timer;
        long due, remaining;
        boolean awaitingRespawn;
        Body(String name, List<UUID> members, int interval) {
            this.name = name;
            this.members = List.copyOf(members);
            this.active = members.getFirst();
            this.interval = interval;
        }
    }

    public HunterGroupManager(SpeedrunnerSwap plugin) { this.plugin = plugin; }
    public Map<String, List<String>> definitions() {
        Map<String, List<String>> result = new LinkedHashMap<>();
        var section = plugin.getConfig().getConfigurationSection(ROOT);
        if (section != null) for (String key : section.getKeys(false))
            result.put(key, section.getStringList(key + ".players"));
        return result;
    }
    public int interval(String name) {
        return Math.min(3600, Math.max(1, plugin.getConfig().getInt(ROOT + "." + name + ".interval",
                plugin.getConfigManager().getSharedHunterControlInterval())));
    }
    public void configure(String name, List<String> players) {
        requireEditable();
        if (!name.matches("[A-Za-z0-9_-]{1,24}")) throw new IllegalArgumentException("Use a group name of 1–24 letters, numbers, - or _.");
        if (players.isEmpty()) throw new IllegalArgumentException("Select at least one player.");
        Set<String> unique = new HashSet<>();
        for (String player : players) {
            if (!unique.add(player.toLowerCase(Locale.ROOT))) throw new IllegalArgumentException("Duplicate player: " + player);
            for (var entry : definitions().entrySet()) if (!entry.getKey().equals(name)
                    && entry.getValue().stream().anyMatch(player::equalsIgnoreCase))
                throw new IllegalArgumentException(player + " already belongs to " + entry.getKey());
        }
        plugin.getConfig().set(ROOT + "." + name + ".players", players);
        plugin.getConfigManager().setSharedHunterControlEnabled(true);
        plugin.saveConfig();
    }
    private void requireEditable() {
        if (plugin.getGameManager().isSetupLocked()) throw new IllegalArgumentException("Stop the round before editing hunter groups.");
    }
    public void remove(String name) {
        requireEditable();
        if (!definitions().containsKey(name)) throw new IllegalArgumentException("Unknown hunter group: " + name);
        plugin.getConfig().set(ROOT + "." + name, null); plugin.saveConfig();
    }
    public void setInterval(String name, int seconds) {
        requireEditable();
        if (!definitions().containsKey(name) || seconds < 1 || seconds > 3600)
            throw new IllegalArgumentException("Choose an existing group and an interval from 1–3600 seconds.");
        plugin.getConfig().set(ROOT + "." + name + ".interval", seconds); plugin.saveConfig();
    }
    public String validate(List<Player> hunters) {
        var defs = definitions();
        if (defs.isEmpty()) return null;
        Set<String> selected = new HashSet<>(), seen = new HashSet<>();
        for (Player p : hunters) selected.add(p.getName().toLowerCase(Locale.ROOT));
        for (var entry : defs.entrySet()) {
            if (!entry.getKey().matches("[A-Za-z0-9_-]{1,24}")) return "Invalid hunter group name: " + entry.getKey();
            if (entry.getValue().isEmpty()) return "Hunter group " + entry.getKey() + " is empty.";
            for (String name : entry.getValue()) {
                String key = name.toLowerCase(Locale.ROOT);
                if (!selected.contains(key)) return name + " must be online and selected as a hunter.";
                if (!seen.add(key)) return name + " belongs to more than one hunter group.";
            }
        }
        return seen.equals(selected) ? null : "Assign every selected hunter to a group, or clear all groups for one shared body.";
    }
    public void start(List<Player> hunters) {
        stop();
        var defs = definitions();
        if (defs.isEmpty()) defs.put("Hunters", hunters.stream().map(Player::getName).toList());
        for (var entry : defs.entrySet()) {
            List<UUID> ids = entry.getValue().stream().map(Bukkit::getPlayerExact)
                    .filter(Objects::nonNull).map(Player::getUniqueId).toList();
            if (ids.isEmpty()) continue;
            Body body = new Body(entry.getKey(), ids, interval(entry.getKey()));
            bodies.put(body.name, body);
            body.spawn = Bukkit.getPlayer(body.active).getRespawnLocation();
            schedule(body, body.interval * 20L);
        }
    }
    public boolean contains(Player p) { return body(p) != null; }
    private Body body(Player p) {
        if (p == null) return null;
        return bodies.values().stream().filter(b -> b.members.contains(p.getUniqueId())).findFirst().orElse(null);
    }
    public boolean isActive(Player p) { Body b = body(p); return b != null && b.active.equals(p.getUniqueId()); }
    public List<Player> activePlayers() {
        return bodies.values().stream().map(b -> Bukkit.getPlayer(b.active)).filter(Objects::nonNull)
                .filter(Player::isOnline).toList();
    }
    public Player activeFor(Player p) { Body b = body(p); return b == null ? null : Bukkit.getPlayer(b.active); }
    public String label(Player p) { Body b = body(p); return b == null ? "Hunters" : b.name; }
    public int seconds(Player p) {
        Body b = body(p);
        return b == null ? 0 : (int)Math.max(0, (plugin.getGameManager().isGamePaused() ? b.remaining : b.due - Bukkit.getCurrentTick()) / 20);
    }
    public List<Player> membersFor(Player p) {
        Body b = body(p);
        return b == null ? List.of() : b.members.stream().map(Bukkit::getPlayer).filter(Objects::nonNull).toList();
    }
    private void schedule(Body b, long ticks) {
        if (b.timer != null) b.timer.cancel();
        b.remaining = ticks;
        b.due = Bukkit.getCurrentTick() + ticks;
        b.timer = Bukkit.getScheduler().runTaskLater(plugin, () -> swap(b), Math.max(1, ticks));
    }
    public void swapAll() { for (Body b : bodies.values()) swap(b); }
    private void swap(Body b) {
        var game = plugin.getGameManager();
        if (!game.isGameRunning() || game.isGamePaused()) return;
        Player old = Bukkit.getPlayer(b.active);
        if (b.awaitingRespawn) { schedule(b, 20); return; }
        if (old != null && !disconnected.contains(old.getUniqueId()) && (old.isDead() || old.getLocation().getBlock().getType() == Material.NETHER_PORTAL
                || old.getLocation().getBlock().getType() == Material.END_PORTAL
                || old.getLocation().getBlock().getType() == Material.END_GATEWAY)) { schedule(b, 20); return; }
        Player next = null;
        int current = b.members.indexOf(b.active);
        for (int offset = 1; offset <= b.members.size(); offset++) {
            Player candidate = Bukkit.getPlayer(b.members.get((current + offset) % b.members.size()));
            if (candidate != null && candidate.isOnline() && !candidate.isDead()
                    && !disconnected.contains(candidate.getUniqueId())) { next = candidate; break; }
        }
        if (next == null || next.getUniqueId().equals(b.active)) { schedule(b, b.interval * 20L); return; }
        if (old != null && old.isOnline() && !disconnected.contains(old.getUniqueId())) {
            game.prepareHunterHandoff(old);
            b.saved = PlayerStateUtil.capturePlayerState(old);
            b.spawn = old.getRespawnLocation();
        }
        if (b.saved == null) { schedule(b, 20); return; }
        // Move first. A cancelled teleport must leave the old controller and inventory intact.
        if (!next.teleport(b.saved.getLocation())) { schedule(b, 20); return; }
        game.releaseHunterController(next);
        PlayerStateUtil.applyPlayerState(next, b.saved, false);
        next.setRespawnLocation(b.spawn, true);
        b.active = next.getUniqueId();
        if (old != null) { old.closeInventory(); old.getInventory().clear(); }
        if (b.saved.isInVehicle() && b.saved.getVehicle() != null && b.saved.getVehicle().isValid()) {
            if (old != null) old.leaveVehicle();
            b.saved.getVehicle().addPassenger(next);
        }
        game.applyHunterGroupRestrictions();
        if (plugin.getVoiceChatIntegration() != null) plugin.getVoiceChatIntegration().updateRunnerMuteStatus();
        if (plugin.getConfigManager().isTrackerEnabled()) plugin.getTrackerManager().giveTrackingCompass(next);
        int grace = plugin.getConfigManager().getGracePeriodTicks();
        next.setNoDamageTicks(Math.max(next.getNoDamageTicks(), grace));
        if (old != null) old.sendMessage("§eHunter " + b.name + ": " + next.getName() + " is now playing.");
        next.sendMessage("§aYou now control hunter " + b.name + ".");
        schedule(b, b.interval * 20L);
    }
    public void quit(Player p) {
        Body b = body(p);
        if (b == null) return;
        disconnected.add(p.getUniqueId());
        if (!isActive(p)) return;
        if (p.isDead()) { b.awaitingRespawn = true; b.saved = null; plugin.getGameManager().pauseForHunterDisconnect(); return; }
        plugin.getGameManager().prepareHunterHandoff(p); b.saved = PlayerStateUtil.capturePlayerState(p); b.spawn = p.getRespawnLocation();
        // Clear the disconnected controller so state cannot be duplicated on rejoin.
        p.getInventory().clear();
        if (plugin.getConfigManager().isPauseOnDisconnect()) plugin.getGameManager().pauseForHunterDisconnect();
        else swap(b);
    }
    public void join(Player p) {
        disconnected.remove(p.getUniqueId());
        Body b = body(p);
        if (b != null && isActive(p) && !p.isDead() && b.saved != null) {
            plugin.getGameManager().releaseHunterController(p);
            PlayerStateUtil.applyPlayerState(p, b.saved);
            p.setRespawnLocation(b.spawn, true);
        }
    }
    public boolean ready() {
        return bodies.values().stream().allMatch(b -> Bukkit.getPlayer(b.active) != null
            && !Bukkit.getPlayer(b.active).isDead() && !b.awaitingRespawn && !disconnected.contains(b.active));
    }
    public void died(Player p) { Body b = body(p); if (b != null && isActive(p)) { b.awaitingRespawn = true; b.saved = null; } }
    public void respawned(Player p) {
        Body b = body(p);
        if (b == null) return;
        if (isActive(p)) {
            b.awaitingRespawn = false;
            b.saved = PlayerStateUtil.capturePlayerState(p);
            b.spawn = p.getRespawnLocation();
            if (plugin.getConfigManager().isTrackerEnabled()) plugin.getTrackerManager().giveTrackingCompass(p);
            plugin.getGameManager().resumeAfterHunterReconnect();
        } else plugin.getGameManager().applyHunterGroupRestrictions();
    }
    public void pause() {
        for (Body b : bodies.values()) { b.remaining = Math.max(1, b.due - Bukkit.getCurrentTick()); if (b.timer != null) b.timer.cancel(); }
    }
    public void resume() { for (Body b : bodies.values()) schedule(b, Math.max(1, b.remaining)); }
    public void stop() { for (Body b : bodies.values()) if (b.timer != null) b.timer.cancel(); bodies.clear(); disconnected.clear(); }
}
