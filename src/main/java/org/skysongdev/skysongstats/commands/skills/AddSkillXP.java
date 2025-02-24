package org.skysongdev.skysongstats.commands.skills;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongstats.Utils.Utils;
import org.skysongdev.skysongstats.database.PlayerSkills;
import org.skysongdev.skysongstats.events.SkillUpdateEvent;

import java.util.List;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class AddSkillXP implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        Player player = null;
        Player target;
        int xpammount;
        if(commandSender instanceof Player){
            if(!commandSender.hasPermission("skysongstats.addxp")){
                commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>You do not have permission to use this command!"));
                return true;
            }
            player = (Player) commandSender;
        }
        if(strings.length < 2){
            if(player != null)
                player.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>Too Little arguments! (Usage: /skill addxp (skill) (xp) (player (optional)))"));
            return true;
        }
        if(strings.length < 3) {
            if(!(commandSender instanceof  Player)){
                Bukkit.getLogger().warning("You must specify a player if you are running this command on console!");
                return true;
            }
            target = (Player) commandSender;
        }
        else{
            target = Bukkit.getPlayer(strings[2]);
            if(target == null){
                if(player != null)
                    player.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>Player not found!"));
                return true;
            }
        }

        if(!Utils.isASkill(strings[0])){
            if(player != null)
                player.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>Invalid Skill!"));
            return true;
        }
        try{
            xpammount = Integer.parseInt(strings[1]);
            if(xpammount < 0){
                if(player != null)
                    player.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>Invalid XP!"));
                return true;
            }
        } catch (NumberFormatException e) {
            if(player != null)
                player.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>Invalid XP!"));
            return true;
        }

        getPlugin().getUtils().getSkillManager().addSkillXP(getPlugin().getUtils().getSkillManager().findSkills(target.getUniqueId().toString(), getPlugin().getUtils().getProfileManager().getActiveProfileName(target.getUniqueId().toString())), strings[0], xpammount);
        try{
            getPlugin().getDatabase().updateSkillsData(getPlugin().getUtils().getSkillManager().findSkills(target.getUniqueId().toString(), getPlugin().getUtils().getProfileManager().getActiveProfileName(target.getUniqueId().toString())));
        } catch (Exception e){
            e.printStackTrace();
        }


        if(commandSender instanceof Player){
            player.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>Added " + xpammount + " XP to " + target.getName() + "'s " + strings[0] + " skill!"));
        }
        Bukkit.getLogger().info("[SkysongStats] Added " + xpammount + " XP to " + target.getName() + "'s " + strings[0] + " skill!");
        if(target != player)
            target.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<green>You have gained <gold>" + xpammount + " XP <green>in your " + strings[0] + " skill!"));

        SkillUpdateEvent event = new SkillUpdateEvent(target, target.getUniqueId().toString(), getPlugin().getUtils().getSkillManager().getSkill(strings[0]));
        Bukkit.getPluginManager().callEvent(event);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if(strings.length == 1){
            return getPlugin().getUtils().getSkillManager().getSkillList().stream().filter(a -> a.startsWith(strings[0])).toList();
        }
        if(strings.length == 2){
            return List.of("");
        }
        if(strings.length == 3){
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(a -> a.startsWith(strings[2])).toList();
        }
        return null;
    }
}
