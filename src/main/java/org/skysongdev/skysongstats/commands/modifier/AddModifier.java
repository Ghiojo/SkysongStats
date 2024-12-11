package org.skysongdev.skysongstats.commands.modifier;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.SkysongStats;
import org.skysongdev.skysongstats.Utils.Utils;
import org.skysongdev.skysongstats.database.Modifier;

public class AddModifier implements CommandExecutor {
    int num;
    String stat;
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        Player target;
        if(strings.length < 2){
            commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<gray>Not enough arguments! (correct usage: /stats set [stat] [value])"));
        }
        if(strings.length < 3)
            target = (Player) commandSender;
        else{
            if(!commandSender.hasPermission("skysongstats.modifier.add.other")){
                commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<gray>You don't have permissions to set other players' stats!"));
                return true;
            }
            target = Bukkit.getServer().getPlayer(strings[2]);
            if(target == null){
                commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<gray>No Player found with that username!"));
                return true;
            }
        }

        if(!Utils.isAStat(strings[0])){
            commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<gray>\"" + strings[0] + "\"" + " is not a stat!"));
            return true;
        } else
            stat = strings[0];
        try{
            num = Integer.parseInt(strings[1]);
        }catch (NumberFormatException e){
            commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<gray>\"" + strings[1] + "\" is not a number!"));
            return true;
        }

        switch(stat.toLowerCase()){
            case "strength":
            case "str":
                SkysongStats.getPlugin().getUtils().getStatsManager().addModifier(SkysongStats.getPlugin().getUtils().getProfileManager().findActiveStats(target.getUniqueId().toString()), new Modifier(Utils.StaticStats.STRENGTH, num));
                break;
            case "dexterity":
            case "dex":
                SkysongStats.getPlugin().getUtils().getStatsManager().addModifier(SkysongStats.getPlugin().getUtils().getProfileManager().findActiveStats(target.getUniqueId().toString()), new Modifier(Utils.StaticStats.DEXTERITY, num));
                break;
            case "constitution":
            case "con":
                SkysongStats.getPlugin().getUtils().getStatsManager().addModifier(SkysongStats.getPlugin().getUtils().getProfileManager().findActiveStats(target.getUniqueId().toString()), new Modifier(Utils.StaticStats.CONSTITUTION, num));
                break;
            case "focus":
            case "foc":
                SkysongStats.getPlugin().getUtils().getStatsManager().addModifier(SkysongStats.getPlugin().getUtils().getProfileManager().findActiveStats(target.getUniqueId().toString()), new Modifier(Utils.StaticStats.FOCUS, num));
                break;
            case "speed":
            case "spd":
                SkysongStats.getPlugin().getUtils().getStatsManager().addModifier(SkysongStats.getPlugin().getUtils().getProfileManager().findActiveStats(target.getUniqueId().toString()), new Modifier(Utils.StaticStats.SPEED, num));
                break;
            case "arcrot-max":
                SkysongStats.getPlugin().getUtils().getStatsManager().addModifier(SkysongStats.getPlugin().getUtils().getProfileManager().findActiveStats(target.getUniqueId().toString()), new Modifier(Utils.StaticStats.ARCROT_MAX, num));
                break;
            case "fictis-max":
                SkysongStats.getPlugin().getUtils().getStatsManager().addModifier(SkysongStats.getPlugin().getUtils().getProfileManager().findActiveStats(target.getUniqueId().toString()), new Modifier(Utils.StaticStats.FICTIS_MAX, num));
                break;
            case "potion-max":
                SkysongStats.getPlugin().getUtils().getStatsManager().addModifier(SkysongStats.getPlugin().getUtils().getProfileManager().findActiveStats(target.getUniqueId().toString()), new Modifier(Utils.StaticStats.POTION_MAX, num));
                break;
            case "maxhp":
                SkysongStats.getPlugin().getUtils().getStatsManager().addModifier(SkysongStats.getPlugin().getUtils().getProfileManager().findActiveStats(target.getUniqueId().toString()), new Modifier(Utils.StaticStats.MAXHP, num));
                break;
        }

        commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + " <gray> "+ strings[1] + " has been modified"));
        

        return true;
    }
}
