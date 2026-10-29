package org.skysongdev.skysongstats.commands.downtime;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.Utils.Utils;
import org.skysongdev.skysongstats.service.downtime.DowntimeService;

public class DowntimePreference implements CommandExecutor {
    private final DowntimeService downtime;

    public DowntimePreference(DowntimeService downtime) {
        this.downtime = downtime;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (sender instanceof Player player) {
            String newPreference = downtime.togglePreference(player.getUniqueId()) ? "Global" : "Character";
            player.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<green>Your downtime spending preference has been set to <gold>" + newPreference + "<green>."));
        }
        return true;
    }
}
