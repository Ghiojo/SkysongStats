package org.skysongdev.skysongstats.commands.downtime;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongstats.SkysongStats;
import org.skysongdev.skysongstats.Utils.Utils;
import org.skysongdev.skysongstats.service.downtime.DowntimeService;

import java.util.Arrays;
import java.util.List;

import static org.skysongdev.skysongstats.Utils.Utils.PLUGIN_TAG;

public class DowntimeCommand implements TabExecutor {
    private final AddDowntime addDowntime;
    private final RemoveDowntime removeDowntime;
    private final ViewDowntime viewDowntime;
    private final DowntimePreference downtimePreference;
    private final ResetAllDowntimes resetAllDowntimes;

    public DowntimeCommand(DowntimeService downtime) {
        addDowntime = new AddDowntime(downtime);
        removeDowntime = new RemoveDowntime(downtime);
        viewDowntime = new ViewDowntime(downtime);
        downtimePreference = new DowntimePreference(downtime);
        resetAllDowntimes = new ResetAllDowntimes(downtime);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if(args.length < 1){
            if(sender instanceof Player)
                sender.sendMessage(Utils.getMiniMessage().deserialize(PLUGIN_TAG + " <red>Invalid arguments! Use /downtime <subcommand>"));
            else
                Bukkit.getLogger().warning("[SkysongStats] Invalid arguments! Use /downtime <subcommand>");
            return true;
        }
        String[] newArgs = Arrays.copyOfRange(args, 1, args.length);

        switch(args[0].toLowerCase()){
            case "add":
                if(sender instanceof Player && sender.hasPermission("skysongstats.downtime.manage")){
                    addDowntime.onCommand(sender, command, label, newArgs);
                } else if(!(sender instanceof Player)){
                    addDowntime.onCommand(sender, command, label, newArgs);
                } else {
                    sender.sendMessage(Utils.getMiniMessage().deserialize(PLUGIN_TAG + " <red>You do not have permission to use this command."));
                }
                break;
            case "remove":
                if(sender instanceof Player && sender.hasPermission("skysongstats.downtime.manage")){
                    removeDowntime.onCommand(sender, command, label, newArgs);
                } else if(!(sender instanceof Player)){
                    removeDowntime.onCommand(sender, command, label, newArgs);
                } else {
                    sender.sendMessage(Utils.getMiniMessage().deserialize(PLUGIN_TAG + " <red>You do not have permission to use this command."));
                }
                break;
            case "view":
                viewDowntime.onCommand(sender, command, label, newArgs);
                break;
            case "preference":
                if(sender instanceof Player){
                    downtimePreference.onCommand(sender, command, label, newArgs);
                } else {
                    Bukkit.getLogger().warning("[SkysongStats] Only players can set downtime preferences!");
                }
                break;
            case "resetall":
                if(sender instanceof Player && sender.hasPermission("skysongstats.downtime.manage")){
                    resetAllDowntimes.onCommand(sender, command, label, newArgs);
                } else if(!(sender instanceof Player)){
                    resetAllDowntimes.onCommand(sender, command, label, newArgs);
                } else {
                    sender.sendMessage(Utils.getMiniMessage().deserialize(PLUGIN_TAG + " <red>You do not have permission to use this command."));
                }
                break;
            default:
                if(sender instanceof Player)
                    sender.sendMessage(Utils.getMiniMessage().deserialize(PLUGIN_TAG + " <red>Unknown subcommand!"));
                else
                    Bukkit.getLogger().warning("[SkysongStats] Unknown subcommand!");
                break;
        }


        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        String[] newstrings = Arrays.copyOfRange(args, 1, args.length);
        if(args.length == 1){
            return Arrays.asList("add", "remove", "view", "preference", "resetall").stream().filter(a -> a.startsWith(args[0])).toList();
        }
        if(args.length > 1){
            switch(args[0].toLowerCase()){
                case "add":
                    return addDowntime.onTabComplete(sender, command, label, newstrings);
                case "remove":
                    return removeDowntime.onTabComplete(sender, command, label, newstrings);
                case "view":
                    return viewDowntime.onTabComplete(sender, command, label, newstrings);
                default:
                    return null;
            }
        }
        return null;
    }
}
