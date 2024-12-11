package org.skysongdev.skysongstats.commands.stats;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.SkysongStats;

import java.util.Arrays;

public class StatsCommand implements CommandExecutor {
    ViewStats viewStats;
    SetStat setStat;
    AddStat addStat;

    public StatsCommand(SkysongStats plugin){
        this.viewStats = new ViewStats();
        this.setStat = new SetStat();
        this.addStat = new AddStat();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        String[] newArgs = Arrays.copyOfRange(strings, 1, strings.length);
        Player player = (Player) commandSender;
        if(strings.length < 1){
            commandSender.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "SkysongStats" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY + "Too Little arguments! (Usage: /stats (subcommand))");
            return true;
        }

        switch(strings[0].toLowerCase()){
            case "view":
                if(commandSender.hasPermission("skysongstats.view")){
                    viewStats.onCommand(commandSender, command, s, newArgs);
                } else
                    commandSender.sendMessage(ChatColor.of("DARK_GRAY") + "[" + ChatColor.of("GOLD") + "SkysongStats" + ChatColor.of("DARK_GRAY") + "]" +ChatColor.of("RED") + "You don't have permissions to run that command!");
                break;
            case "set":
                if(commandSender.hasPermission("skysongstats.set")){
                    setStat.onCommand(commandSender, command, s, newArgs);
                } else
                    commandSender.sendMessage(ChatColor.of("DARK_GRAY") + "[" + ChatColor.of("GOLD") + "SkysongStats" + ChatColor.of("DARK_GRAY") + "]" +ChatColor.of("RED") + "You don't have permissions to run that command!");
                break;
            case "add":
                if(commandSender.hasPermission("skysongstats.add")){
                    addStat.onCommand(commandSender, command, s, newArgs);
                } else
                    commandSender.sendMessage(ChatColor.of("DARK_GRAY") + "[" + ChatColor.of("GOLD") + "SkysongStats" + ChatColor.of("DARK_GRAY") + "]" +ChatColor.of("RED") + "You don't have permissions to run that command!");
                break;
        }

        return true;
    }
}
