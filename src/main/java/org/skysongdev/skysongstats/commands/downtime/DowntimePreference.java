package org.skysongdev.skysongstats.commands.downtime;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.service.downtime.DowntimeService;

import java.util.Map;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class DowntimePreference implements CommandExecutor {
    private final DowntimeService downtime;

    public DowntimePreference(DowntimeService downtime) {
        this.downtime = downtime;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (sender instanceof Player player) {
            String preference = DowntimeTarget.preferenceName(downtime.togglePreference(player.getUniqueId()));
            getPlugin().getMessages().send(player, "downtime.preference-set", Map.of("preference", preference));
        }
        return true;
    }
}
