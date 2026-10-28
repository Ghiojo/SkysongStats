package org.skysongdev.skysongstats.commands.downtime;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongstats.Utils.Utils;
import org.skysongdev.skysongstats.service.downtime.DowntimeChange;
import org.skysongdev.skysongstats.service.downtime.DowntimeService;

import java.util.List;

import static org.skysongdev.skysongstats.Utils.Utils.PLUGIN_TAG;

public class AddDowntime implements TabExecutor {
    private final DowntimeService downtime;

    public AddDowntime(DowntimeService downtime) {
        this.downtime = downtime;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length < 3) {
            sender.sendMessage(Utils.getMiniMessage().deserialize(PLUGIN_TAG + "<red>Usage: /downtime add <amount> <player> <global/profile>"));
            return true;
        }
        int amount = DowntimeTarget.parseAmount(args[0]);
        if (amount < 0) {
            sender.sendMessage(Utils.getMiniMessage().deserialize(PLUGIN_TAG + "<red>Amount must be a whole number of 1 or more."));
            return true;
        }
        DowntimeTarget target = DowntimeTarget.find(args[1]);
        if (target == null) {
            sender.sendMessage(Utils.getMiniMessage().deserialize(PLUGIN_TAG + "<red>Player " + args[1] + " not found or has no profile."));
            return true;
        }
        DowntimeChange change;
        String where;
        if (args[2].equalsIgnoreCase("global")) {
            change = downtime.addGlobal(target.uuid(), amount);
            where = "global downtime";
        } else {
            String profile = target.findProfile(args[2]);
            if (profile == null) {
                sender.sendMessage(Utils.getMiniMessage().deserialize(PLUGIN_TAG + "<red>Profile " + args[2] + " not found for player " + target.name() + "."));
                return true;
            }
            change = downtime.addCharacter(target.uuid(), profile, amount);
            where = "profile " + profile;
        }
        String capped = change.delta() < amount ? " (capped at the maximum)" : "";
        sender.sendMessage(Utils.getMiniMessage().deserialize(PLUGIN_TAG + "<green>Added " + change.delta() + " downtime to player " + target.name() + "'s " + where + ", now " + change.newValue() + "." + capped));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 2) {
            return DowntimeTarget.onlineNames(args[1]);
        }
        if (args.length == 3) {
            return DowntimeTarget.poolNames(args[1], args[2]);
        }
        return List.of();
    }
}
