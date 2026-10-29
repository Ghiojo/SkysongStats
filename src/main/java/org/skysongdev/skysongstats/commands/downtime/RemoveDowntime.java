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

public class RemoveDowntime implements TabExecutor {
    private final DowntimeService downtime;

    public RemoveDowntime(DowntimeService downtime) {
        this.downtime = downtime;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length < 2) {
            sender.sendMessage(Utils.getMiniMessage().deserialize(PLUGIN_TAG + "<red>Usage: /downtime remove <amount> <player> [global/profile]"));
            return true;
        }
        int amount = DowntimeTarget.parseAmount(sender, args[0]);
        DowntimeTarget target = amount < 0 ? null : DowntimeTarget.find(sender, args[1]);
        if (target != null) {
            remove(sender, target, amount, args.length >= 3 ? args[2] : null);
        }
        return true;
    }

    /** Removes from {@code pool}: "global", a profile name, or {@code null} for the player's preference. */
    private void remove(CommandSender sender, DowntimeTarget target, int amount, String pool) {
        DowntimeChange change;
        if (pool == null) {
            change = downtime.removeFromPreferred(target.uuid(), target.activeProfile(), amount);
        } else if (pool.equalsIgnoreCase("global")) {
            change = downtime.removeGlobal(target.uuid(), amount);
        } else {
            String profile = target.findProfile(sender, pool);
            if (profile == null) {
                return;
            }
            change = downtime.removeCharacter(target.uuid(), profile, amount);
        }
        String where = change.profile() == null ? "global downtime" : change.profile() + " profile downtime";
        String clamped = -change.delta() < amount ? " (stopped at 0)" : "";
        sender.sendMessage(Utils.getMiniMessage().deserialize(PLUGIN_TAG + "<green>Successfully removed " + (-change.delta()) + " downtime from " + target.name() + "'s " + where + ", now " + change.newValue() + "." + clamped));
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
