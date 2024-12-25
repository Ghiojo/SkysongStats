package org.skysongdev.skysongstats.commands.stats;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongstats.SkysongStats;
import org.skysongdev.skysongstats.Utils.Utils;

import java.util.Arrays;
import java.util.List;

public class StatsCommand implements TabExecutor {
    ViewStats viewStats;
    SetStat setStat;
    AddStat addStat;

    public StatsCommand(){
        this.viewStats = new ViewStats();
        this.setStat = new SetStat();
        this.addStat = new AddStat();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        Player player = (Player) commandSender;
        if(strings.length < 1){
            commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>Too Little arguments! (Usage: /stats (subcommand))"));
            return true;
        }
        String[] newArgs = Arrays.copyOfRange(strings, 1, strings.length);

        switch(strings[0].toLowerCase()){
            case "view":
                viewStats.onCommand(commandSender, command, s, newArgs);
                break;
            case "set":
                if(!commandSender.hasPermission("skysongstats.stats.set")){
                    commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>You don't have permission to set stats!"));
                    return true;
                }
                setStat.onCommand(commandSender, command, s, newArgs);
                break;
            case "add":
                if(!commandSender.hasPermission("skysongstats.stats.add")){
                    commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>You don't have permission to add stats!"));
                    return true;
                }
                addStat.onCommand(commandSender, command, s, newArgs);
                break;
            default:
                commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>Incorrect Arguments! Usage: /stats (view/set/add) (stat) (value)"));
                break;
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        String[] newstrings = Arrays.copyOfRange(strings, 1, strings.length);
        if(strings.length == 1){
            return Arrays.asList("view", "set", "add").stream().filter(a -> a.startsWith(strings[0])).toList();
        } if(strings.length > 1){
            switch(strings[0].toLowerCase()){
                case "view":
                    return List.of("");
                case "set":
                    return setStat.onTabComplete(commandSender, command, s, newstrings);
                case "add":
                    return addStat.onTabComplete(commandSender, command, s, newstrings);
            }
        }
        return null;
    }
}
