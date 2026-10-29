package org.skysongdev.skysongstats.commands.downtime;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.storage.ProfileKey;
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

    /** Every player's profiles, so players without downtime rows get refilled too. */
    static Map<UUID, List<String>> knownProfiles() {
        Map<UUID, List<String>> profiles = new HashMap<>();
        for (ProfileKey character : getPlugin().getStatService().characters()) {
            profiles.computeIfAbsent(character.player(), uuid -> new ArrayList<>()).add(character.profile());
        }
        return profiles;
    }
}
