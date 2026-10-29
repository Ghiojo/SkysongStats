package org.skysongdev.skysongstats.commands.downtime;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongstats.service.downtime.DowntimeService;

import java.util.List;
import java.util.Map;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class ViewDowntime implements TabExecutor {
    private final DowntimeService downtime;

    public ViewDowntime(DowntimeService downtime) {
        this.downtime = downtime;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        boolean self = args.length == 0;
        if (args.length > 1 || (self && !(sender instanceof Player))) {
            getPlugin().getMessages().send(sender, "downtime.view.usage");
            return true;
        }
        DowntimeTarget target = self ? DowntimeTarget.of((Player) sender) : findOther(sender, args[0]);
        if (target != null) {
            show(sender, target);
        }
        return true;
    }

    private static DowntimeTarget findOther(CommandSender sender, String name) {
        if (!sender.hasPermission("skysongstats.downtime.view.others")) {
            getPlugin().getMessages().send(sender, "downtime.view.no-permission-others");
            return null;
        }
        return DowntimeTarget.find(sender, name);
    }

    private void show(CommandSender sender, DowntimeTarget target) {
        String activeProfile = target.activeProfile();
        if (activeProfile == null) {
            getPlugin().getMessages().send(sender, "downtime.view.no-active-profile", Map.of("player", target.name()));
            return;
        }
        getPlugin().getMessages().send(sender, "downtime.view.lines", Map.of(
                "player", target.name(),
                "global", String.valueOf(downtime.getGlobal(target.uuid())),
                "character", String.valueOf(downtime.getCharacter(target.uuid(), activeProfile)),
                "preference", DowntimeTarget.preferenceName(downtime.spendsGlobal(target.uuid()))));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 1) {
            return DowntimeTarget.onlineNames(args[0]);
        }
        return List.of();
    }
}
