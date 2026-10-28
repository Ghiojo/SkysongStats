package org.skysongdev.skysongstats.commands.downtime;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongstats.Utils.Utils;
import org.skysongdev.skysongstats.service.downtime.DowntimeService;

import java.util.List;

import static org.skysongdev.skysongstats.Utils.Utils.PLUGIN_TAG;

public class ViewDowntime implements TabExecutor {
    private final DowntimeService downtime;

    public ViewDowntime(DowntimeService downtime) {
        this.downtime = downtime;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        boolean self = args.length == 0;
        if (args.length > 1 || (self && !(sender instanceof Player))) {
            sender.sendMessage(Utils.getMiniMessage().deserialize(PLUGIN_TAG + "<red>Usage: /downtime view <player>"));
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
            sender.sendMessage(Utils.getMiniMessage().deserialize(PLUGIN_TAG + "<red>You do not have permission to view other players' downtime."));
            return null;
        }
        return DowntimeTarget.find(sender, name);
    }

    private void show(CommandSender sender, DowntimeTarget target) {
        String activeProfile = target.activeProfile();
        if (activeProfile == null) {
            sender.sendMessage(Utils.getMiniMessage().deserialize(PLUGIN_TAG + "<red>" + target.name() + " has no active profile."));
            return;
        }
        sender.sendMessage(Utils.getMiniMessage().deserialize("<gold>" + target.name() + "<green>'s Downtime:"));
        sender.sendMessage(Utils.getMiniMessage().deserialize("<green>- Global Downtime: <gold>" + downtime.getGlobal(target.uuid())));
        sender.sendMessage(Utils.getMiniMessage().deserialize("<green>- Character Downtime: <gold>" + downtime.getCharacter(target.uuid(), activeProfile)));
        String preference = downtime.spendsGlobal(target.uuid()) ? "Global" : "Character";
        sender.sendMessage(Utils.getMiniMessage().deserialize("<green>- Active Downtime Spending Preference: <gold>" + preference));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 1) {
            return DowntimeTarget.onlineNames(args[0]);
        }
        return List.of();
    }
}
