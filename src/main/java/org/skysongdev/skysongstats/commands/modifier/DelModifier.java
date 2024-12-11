package org.skysongdev.skysongstats.commands.modifier;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.SkysongStats;
import org.skysongdev.skysongstats.Utils.Utils;

public class DelModifier implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        Player target;
        if(strings.length < 1){
            commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<gray>Not enough arguments! (correct usage: /stats set [stat] [value])"));
        }
        if(strings.length < 2)
            target = (Player) commandSender;
        else{
            if(!commandSender.hasPermission("skysongstats.modifier.del.other")){
                commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<gray>You don't have permissions to set other players' stats!"));
                return true;
            }
            target = Bukkit.getServer().getPlayer(strings[1]);
            if(target == null){
                commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<gray>No Player found with that username!"));
                return true;
            }
        }
        int id;
        try{
            id = Integer.parseInt(strings[0]);
        }catch (NumberFormatException e){
            commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<gray>\"" + strings[1] + "\" is not a number!"));
            return true;
        }
        if(id > SkysongStats.getPlugin().getUtils().getProfileManager().findActiveStats(target.getUniqueId().toString()).getModifiers().size() || id <= 0){
            commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<gray>index \"" + strings[1] + "\" is out of bounds! There's no modifier with that id"));
            return true;
        }

        SkysongStats.getPlugin().getUtils().getProfileManager().findActiveStats(target.getUniqueId().toString()).getModifiers().remove(id-1);
        return true;
    }
}
