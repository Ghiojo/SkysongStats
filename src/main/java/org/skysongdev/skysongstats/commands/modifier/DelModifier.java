package org.skysongdev.skysongstats.commands.modifier;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongstats.SkysongStats;
import org.skysongdev.skysongstats.Utils.Utils;

import java.sql.SQLException;
import java.util.List;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class DelModifier implements TabExecutor {
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
        if(id > getPlugin().getUtils().getProfileManager().findActiveStats(target.getUniqueId().toString()).getModifiers().size() || id <= 0){
            commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<gray>index \"" + strings[1] + "\" is out of bounds! There's no modifier with that id"));
            return true;
        }
        try {
            getPlugin().getDatabase().deleteModifierData(getPlugin().getUtils().getProfileManager().findActiveStats(target.getUniqueId().toString()), id - 1);
        } catch (SQLException e){
            e.printStackTrace();
            commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<gray>Failed to remove modifier!"));
            return true;
        }
        getPlugin().getUtils().getProfileManager().findActiveStats(target.getUniqueId().toString()).getModifiers().remove(id-1);
        getPlugin().getUtils().getStatsManager().updateStats(getPlugin().getUtils().getProfileManager().findActiveStats(target.getUniqueId().toString()));
        commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<gray>Modifier removed!"));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return List.of();
    }
}
