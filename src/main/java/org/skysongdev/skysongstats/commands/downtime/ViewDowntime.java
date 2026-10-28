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
        if (args.length > 1 || (args.length == 0 && !(sender instanceof Player))) {
            sender.sendMessage(Utils.getMiniMessage().deserialize(PLUGIN_TAG + "<red>Usage: /downtime view <player>"));
            return true;
        }
        DowntimeTarget target;
        if (args.length == 1) {
            if (!sender.hasPermission("skysongstats.downtime.view.others")) {
                sender.sendMessage(Utils.getMiniMessage().deserialize(PLUGIN_TAG + "<red>You do not have permission to view other players' downtime."));
                return true;
            }
            target = DowntimeTarget.find(args[0]);
            if (target == null) {
                sender.sendMessage(Utils.getMiniMessage().deserialize(PLUGIN_TAG + "<red>Player " + args[0] + " not found or has no profile."));
                return true;
            }
        } else {
            target = DowntimeTarget.of((Player) sender);
        }
        String activeProfile = target.activeProfile();
        if (activeProfile == null) {
            sender.sendMessage(Utils.getMiniMessage().deserialize(PLUGIN_TAG + "<red>" + target.name() + " has no active profile."));
            return true;
        }

        sender.sendMessage(Utils.getMiniMessage().deserialize("<gold>" + target.name() + "<green>'s Downtime:"));
        sender.sendMessage(Utils.getMiniMessage().deserialize("<green>- Global Downtime: <gold>" + downtime.getGlobal(target.uuid())));
        sender.sendMessage(Utils.getMiniMessage().deserialize("<green>- Character Downtime: <gold>" + downtime.getCharacter(target.uuid(), activeProfile)));
        String preference = downtime.spendsGlobal(target.uuid()) ? "Global" : "Character";
        sender.sendMessage(Utils.getMiniMessage().deserialize("<green>- Active Downtime Spending Preference: <gold>" + preference));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 1) {
            return DowntimeTarget.onlineNames(args[0]);
        }
        return List.of();
    }
}
