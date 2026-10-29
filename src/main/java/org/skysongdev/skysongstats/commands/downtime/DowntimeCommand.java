package org.skysongdev.skysongstats.commands.downtime;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongstats.service.downtime.DowntimeService;

import java.util.Arrays;
import java.util.List;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

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
            getPlugin().getMessages().send(sender, "downtime.usage");
            return true;
        }
        String[] newArgs = Arrays.copyOfRange(args, 1, args.length);

        switch(args[0].toLowerCase()){
            case "add":
                runIfManager(addDowntime, sender, command, label, newArgs);
                break;
            case "remove":
                runIfManager(removeDowntime, sender, command, label, newArgs);
                break;
            case "view":
                viewDowntime.onCommand(sender, command, label, newArgs);
                break;
            case "preference":
                if(sender instanceof Player){
                    downtimePreference.onCommand(sender, command, label, newArgs);
                } else {
                    getPlugin().getMessages().send(sender, "downtime.players-only");
                }
                break;
            case "resetall":
                runIfManager(resetAllDowntimes, sender, command, label, newArgs);
                break;
            default:
                getPlugin().getMessages().send(sender, "downtime.unknown-subcommand");
                break;
        }


        return true;
    }

    /** Players need skysongstats.downtime.manage; the console always may. */
    private static void runIfManager(CommandExecutor subcommand, CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player) || sender.hasPermission("skysongstats.downtime.manage")){
            subcommand.onCommand(sender, command, label, args);
        } else {
            getPlugin().getMessages().send(sender, "downtime.no-permission");
        }
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
