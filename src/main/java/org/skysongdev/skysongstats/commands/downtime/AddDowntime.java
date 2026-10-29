package org.skysongdev.skysongstats.commands.downtime;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongstats.service.downtime.DowntimeChange;
import org.skysongdev.skysongstats.service.downtime.DowntimeService;

import java.util.List;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;


public class AddDowntime implements TabExecutor {
    private final DowntimeService downtime;

    public AddDowntime(DowntimeService downtime) {
        this.downtime = downtime;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length < 3) {
            getPlugin().getMessages().send(sender, "downtime.add.usage");
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
        String key = "downtime.add." + (change.profile() == null ? "global" : "profile") + (change.delta() < amount ? "-capped" : "");
        getPlugin().getMessages().send(sender, key, DowntimeTarget.changePlaceholders(target, change, change.delta()));
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
