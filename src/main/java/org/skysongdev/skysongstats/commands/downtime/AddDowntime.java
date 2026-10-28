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
        int amount = DowntimeTarget.parseAmount(sender, args[0]);
        DowntimeTarget target = amount < 0 ? null : DowntimeTarget.find(sender, args[1]);
        if (target != null) {
            add(sender, target, amount, args[2]);
        }
        return true;
    }

    private void add(CommandSender sender, DowntimeTarget target, int amount, String pool) {
        DowntimeChange change;
        if (pool.equalsIgnoreCase("global")) {
            change = downtime.addGlobal(target.uuid(), amount);
        } else {
            String profile = target.findProfile(sender, pool);
            if (profile == null) {
                return;
            }
            change = downtime.addCharacter(target.uuid(), profile, amount);
        }
        String where = change.profile() == null ? "global downtime" : "profile " + change.profile();
        String capped = change.delta() < amount ? " (capped at the maximum)" : "";
        sender.sendMessage(Utils.getMiniMessage().deserialize(PLUGIN_TAG + "<green>Added " + change.delta() + " downtime to player " + target.name() + "'s " + where + ", now " + change.newValue() + "." + capped));
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
