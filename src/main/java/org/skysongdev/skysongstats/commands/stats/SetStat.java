package org.skysongdev.skysongstats.commands.stats;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.SkysongStats;
import org.skysongdev.skysongstats.Utils.Utils;
import org.skysongdev.skysongstats.database.PlayerStats;

public class SetStat implements CommandExecutor {
    int num;
    String stat;
    public SetStat(){

    }
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        Player target;
        if(strings.length < 2){
            commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>Not enough arguments! (correct usage: /stats set [stat] [value])"));
        }
        if(!(strings.length < 3))
            target = (Player) commandSender;
        else{
            if(!commandSender.hasPermission("skysongstats.set.other")){
                commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>You don't have permissions to set other players' stats!"));
                return true;
            }
            target = Bukkit.getServer().getPlayer(strings[0]);
            if(target == null){
                commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>No Player found with that username!"));
                return true;
            }
        }

        if(!Utils.isAStat(strings[0])){
            commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<yellow>\"" + strings[0] + "\"" + " <red>is not a stat!"));
            return true;
        } else
            stat = strings[0];
        try{
            num = Integer.parseInt(strings[1]);
        }catch (NumberFormatException e){
            commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<yellow>\"" + strings[1] + "\" <red>is not a number!"));
            return true;
        }

        PlayerStats stats = SkysongStats.getPlugin().getUtils().getProfileManager().findActiveStats(target.getUniqueId().toString());

        switch(stat.toLowerCase()){
            case "strength":
                stats.setStrength(num);
                break;
            case "dexterity":
                stats.setDexterity(num);
                break;
            case "constitution":
                stats.setConstitution(num);
                break;
            case "focus":
                stats.setFocus(num);
                break;
            case "speed":
                stats.setSpeed(num);
                break;
            case "arcrot":
                stats.setArcrot(num);
                break;
            case "potion":
                stats.setPotion(num);
                break;
            case "fictis":
                stats.setFictis(num);
                break;
            case "hp":
                stats.setHP(num);
                break;
            case "temphp":
                stats.setTemp_hp(num);
                break;
            case "ac":
                stats.setAC(num);
                break;
        }

        SkysongStats.getPlugin().getUtils().getStatsManager().updateStats(stats);
        commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<yellow>" +strings[1] + " <red>has been set to <yellow>" + strings[0]));

        return true;
    }
}
