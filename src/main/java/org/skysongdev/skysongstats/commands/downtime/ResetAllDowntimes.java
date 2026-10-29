package org.skysongdev.skysongstats.commands.downtime;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.Utils.StatsManager;
import org.skysongdev.skysongstats.database.PlayerStats;
import org.skysongdev.skysongstats.service.downtime.DowntimeService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class ResetAllDowntimes implements CommandExecutor {
    private final DowntimeService downtime;

    public ResetAllDowntimes(DowntimeService downtime) {
        this.downtime = downtime;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        downtime.resetAll(knownProfiles());
        getPlugin().getMessages().send(sender, "downtime.reset-all");
        return true;
    }

    /** Every player with a stats profile, with all their profile names, so players without downtime rows get refilled too. */
    static Map<UUID, List<String>> knownProfiles() {
        Map<UUID, List<String>> profiles = new HashMap<>();
        if (StatsManager.statsProfileList == null) {
            return profiles;
        }
        for (PlayerStats stats : StatsManager.statsProfileList) {
            try {
                profiles.computeIfAbsent(UUID.fromString(stats.getUuid()), uuid -> new ArrayList<>()).add(stats.getProfile());
            } catch (IllegalArgumentException e) {
                // A malformed UUID in skysong_stats: skip it, its downtime cannot be stored either.
            }
        }
        return profiles;
    }
}
