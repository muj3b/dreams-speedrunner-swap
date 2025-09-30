package com.example.speedrunnerswap.utils;

import com.example.speedrunnerswap.SpeedrunnerSwap;
import com.example.speedrunnerswap.models.Team;
import org.bukkit.entity.Player;

public class MessageManager {

    private final SpeedrunnerSwap plugin;

    public MessageManager(SpeedrunnerSwap plugin) {
        this.plugin = plugin;
    }

    public String getStartCountdownTitle(SpeedrunnerSwap.SwapMode mode, int count) {
        return switch (mode) {
            case DREAM -> "§b§lDream Swap starting in " + count;
            case SAPNAP -> "§d§lSapnap speedrunner swap in " + count;
            case TASK -> "§6§lTaskmaster starting in " + count;
        };
    }

    public String getStartCountdownSubtitle() {
        return "§7Made by muj3b";
    }

    public String getStartTitle(SpeedrunnerSwap.SwapMode mode) {
        return switch (mode) {
            case DREAM -> "§b§lDream Swap GO!";
            case SAPNAP -> "§d§lSapnap swap GO!";
            case TASK -> "§6§lTaskmaster GO!";
        };
    }

    public String getEndTitle(Team winner) {
        if (winner == Team.RUNNER) {
            return "§a§lRUNNERS WIN!";
        } else if (winner == Team.HUNTER) {
            return "§c§lHUNTERS WIN!";
        } else {
            return "§c§lGAME OVER";
        }
    }

    public String getEndSubtitle(Team winner, boolean isRunner) {
        if (winner == Team.RUNNER) {
            return "§eBro y'all are locked in, good stuff";
        } else if (winner == Team.HUNTER) {
            return isRunner ? "§eYou ain't the main character, unc" : "§eBro those speedrunners are trash";
        } else {
            return "§eNo winner declared.";
        }
    }

    public String getGameEndMessage(Team winner) {
        String winnerMessage = (winner != null) ? winner.name() + " team won!" : "Game ended!";
        return "§a[SpeedrunnerSwap] Game ended! " + winnerMessage;
    }

    public String getGameStartFailureMessage(SpeedrunnerSwap.SwapMode mode, boolean hasRunner, boolean hasHunter) {
        return switch (mode) {
            case DREAM -> hasRunner
                    ? "§cGame cannot start: Assign at least one hunter for Dream mode."
                    : "§cGame cannot start: Assign at least one speedrunner.";
            case SAPNAP -> hasRunner
                    ? (hasHunter
                        ? "§cGame cannot start: Sapnap mode does not allow hunters. Clear them and keep only speedrunners."
                        : "§cGame cannot start: Assign at least one speedrunner.")
                    : "§cGame cannot start: Assign at least one speedrunner.";
            case TASK -> hasRunner
                    ? (hasHunter
                        ? "§cGame cannot start: Task Master mode uses only speedrunners. Remove any hunters before starting."
                        : "§cGame cannot start: Assign at least one speedrunner.")
                    : "§cGame cannot start: Assign at least one speedrunner.";
        };
    }

    public String getGamePausedOnDisconnectMessage() {
        return "§e[SpeedrunnerSwap] Game paused: waiting for players to return.";
    }

    public String getSwapDeferredMessage(double delay, int attempt, int maxAttempts) {
        return String.format("§e[SpeedrunnerSwap] Swap deferred: runner is mid-portal. Retrying in %.1fs (attempt %d/%d).", delay, attempt, maxAttempts);
    }

    public String getHuntersSwappedMessage() {
        return "§c[SpeedrunnerSwap] Hunters have been swapped!";
    }

    public String getPowerupReceivedMessage(boolean isGood, String effectName, String effectLevel) {
        return String.format("§%sYou received a %s power-up: %s %s!",
                isGood ? "a" : "c",
                isGood ? "good" : "bad",
                effectName,
                effectLevel);
    }

    public String getGamePausedMessage() {
        return "§e§lGame paused by admin.";
    }

    public String getGameResumedMessage() {
        return "§a§lGame resumed.";
    }

    public String getQueuedMessage(int position) {
        return String.format("§6Queued (%d) - You're up next", position);
    }

    public String getSwapInMessage(int timeLeft) {
        return String.format("§eSwap in: §c%ds", Math.max(0, timeLeft));
    }

    public String getTitleSwapInMessage(int timeLeft) {
        return String.format("§6§lSwap in: %ds", Math.max(0, timeLeft));
    }

    public String getTitleStatusSubtitle(boolean isSneaking, boolean isSprinting) {
        return String.format("§eSneaking: %s  §7|  §eRunning: %s", isSneaking ? "Yes" : "No", isSprinting ? "Yes" : "No");
    }

    public void sendDonationMessage(Player player) {
        if (player == null) {
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                sendDonationMessage(p);
            }
            return;
        }
        player.sendMessage("");
        player.sendMessage("§6§l=== Support the Creator ===");
        player.sendMessage("§eEnjoyed the game? Help keep updates coming!");
        player.sendMessage("§d❤ Donate to support development");
        player.sendMessage("§b" + SpeedrunnerSwap.DONATION_URL);
        player.sendMessage("");
    }
}