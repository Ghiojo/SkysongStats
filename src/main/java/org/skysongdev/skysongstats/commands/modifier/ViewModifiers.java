package org.skysongdev.skysongstats.commands.modifier;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongstats.Utils.Utils;
import org.skysongdev.skysongstats.database.Modifier;
import org.skysongdev.skysongstats.database.PlayerStats;

import java.util.ArrayList;
import java.util.List;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class ViewModifiers implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        Player target;
        int page = 0;
        if(!(commandSender instanceof Player)){
            if(strings.length < 1){
                Bukkit.getLogger().info("Too Little arguments! (Usage: /modifier view (player))");
                return true;
            }
            target = Bukkit.getPlayer(strings[0]);
            if(target == null){
                Bukkit.getLogger().info("Player not found!");
                return true;
            }
        }
        if(strings.length < 1){
            commandSender.sendMessage("Too Little arguments! (Usage: /modifier view (player))");
            return true;
        }
        try{
            page = Integer.parseInt(strings[0]);
        }catch (NumberFormatException e){
            commandSender.sendMessage("Invalid page number!");
            return true;
        }
        if(page < 1){
            commandSender.sendMessage("Invalid page number!");
            return true;
        }
        if(strings.length < 2){
            if(!(commandSender instanceof Player)) {
                commandSender.sendMessage("Too Little arguments! (Usage: /modifier view (player))");
                return true;
            }
            target = (Player) commandSender;
        } else{
            if(!commandSender.hasPermission("skysongstats.modifier.view.other")){
                commandSender.sendMessage("You don't have permissions to view other players' modifiers!");
                return true;
            }
            target = Bukkit.getServer().getPlayer(strings[1]);
            if(target == null){
                commandSender.sendMessage("Player not found!");
                return true;
            }
        }

        ArrayList<Modifier> modifiers = getPlugin().getUtils().getProfileManager().findActiveStats(target.getUniqueId().toString()).getModifiers();

        if(commandSender instanceof Player){
            if(!commandSender.hasPermission("skysongstats.modifier.view.other") && !commandSender.getName().equals(target.getName())){
                commandSender.sendMessage("You don't have permissions to view other players' modifiers!");
                return true;
            }
            commandSender.sendMessage(Utils.getMiniMessage().deserialize("<#864811>|====================<<<#63C934>₪<#864811>>>====================|"));
            commandSender.sendMessage(Utils.getMiniMessage().deserialize("                <#F6DA23>"+ target.getName() + "' s <#864811>Modifiers (page " + page +"):"));
            if(modifiers.size() == 0){
                commandSender.sendMessage(Utils.getMiniMessage().deserialize("<#F6DA23>No modifiers found!"));
                return true;
            } else
                for(int i = ((page-1)*10); i < 10+((page-1)*10) && i < modifiers.size(); i++){
                    commandSender.sendMessage(Utils.getMiniMessage().deserialize("<#F6DA23>" + (i+1) + ". <#864811>" + Utils.StaticStats.getStatName(modifiers.get(i).getStat()) + ": <#63C934>" + modifiers.get(i).getModifier()));
                }
            commandSender.sendMessage(Utils.getMiniMessage().deserialize("<#864811>|====================<<<#63C934>₪<#864811>>>====================|"));
        } else {
            Bukkit.getLogger().info("|====================<<<₪>>>====================|");
            Bukkit.getLogger().info("                " + target.getName() + "' s Modifiers (page " + page +"):");
            if(modifiers.size() == 0){
                Bukkit.getLogger().info("No modifiers found!");
                return true;
            } else
                for(int i = ((page-1)*10); i < 10+((page-1)*10) && i < modifiers.size(); i++){
                Bukkit.getLogger().info(i+1 + ". " + Utils.StaticStats.getStatName(modifiers.get(i).getStat()) + ": " + modifiers.get(i).getModifier());
                }
            Bukkit.getLogger().info("|====================<<<₪>>>====================|");
        }

        return true;

    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if(strings.length == 1){
            return List.of("");
        }
        ArrayList<String> players = new ArrayList<>();
        for(Player player : Bukkit.getOnlinePlayers()){
            players.add(player.getName());
        }
        return players;
    }
}
