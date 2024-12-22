package org.skysongdev.skysongstats.commands.character;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongstats.SkysongStats;
import org.skysongdev.skysongstats.Utils.Utils;
import org.skysongdev.skysongstats.database.CharacterProfile;

import java.util.ArrayList;
import java.util.List;

public class ViewCharacter implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        Player target;
        if(strings.length < 1){
            if(commandSender instanceof Player){
                target = (Player) commandSender;
            } else {
                commandSender.sendMessage("You must specify a player to view their character!");
                return true;
            }
        }else {
            target = commandSender.getServer().getPlayer(strings[0]);
            if (target == null) {
                commandSender.sendMessage("That player is not online!");
                return true;
            }
        }
        CharacterProfile profile = SkysongStats.getPlugin().getUtils().getCharacterManager().findActiveCharacter(target.getUniqueId().toString());
        if(commandSender instanceof Player){
            Player player = (Player) commandSender;
            player.sendMessage(Utils.getMiniMessage().deserialize("<#864811>|====================<<<#63C934>₪<#864811>>>====================|"));
            player.sendMessage(Utils.getMiniMessage().deserialize("                <#F6DA23>"+ player.getName() + "' s <#864811>Character Sheet:"));
            player.sendMessage("");
            player.sendMessage(Utils.getMiniMessage().deserialize("<#D0530C><bold>Name:</bold> <#f8e886>" + profile.getName()));
            player.sendMessage(Utils.getMiniMessage().deserialize("<#D0530C><bold>Age:</bold> <#f8e886>" + profile.getAge()));
            player.sendMessage(Utils.getMiniMessage().deserialize("<#D0530C><bold>Gender:</bold> <#f8e886>" + profile.getGender() + "<#e7d462>("+ profile.getPronouns() + ")"));
            player.sendMessage(Utils.getMiniMessage().deserialize("<#D0530C><bold>Ancestry:</bold> <#f8e886>" + profile.getAncestry()));
            player.sendMessage(Utils.getMiniMessage().deserialize("<#D0530C><bold>Description:</bold>"));
            StringBuilder buffer = new StringBuilder();
            String[] input = profile.getDescription().split(" ");
            for(String word : input){
                if(buffer.length() + word.length() > 60){
                    player.sendMessage(Utils.getMiniMessage().deserialize("<#f8e886>" + buffer));
                    buffer = new StringBuilder();
                }
                buffer.append(word).append(" ");
            }
            player.sendMessage(Utils.getMiniMessage().deserialize("<#864811>|====================<<<#63C934>₪<#864811>>>====================|"));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if(strings.length == 1){
            List<String> players = new ArrayList<String>();
            for(Player player : Bukkit.getOnlinePlayers()){
                players.add(player.getName());
            }
            return players;
        }
        return null;
    }
}
