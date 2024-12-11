package org.skysongdev.skysongstats.commands.skills;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.Utils.Utils;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class SetSkillXP implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        Player player;
        Player target;
        int xpammount;

        if(commandSender instanceof Player){
            player = (Player) commandSender;
        }
        if(strings.length < 2){
            commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<gray>Too Little arguments! (Usage: /skill addxp (skill) (xp) (player (optional)))"));
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
                commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<gray>Player not found!"));
                return true;
            }
        }

        if(!Utils.isASkill(strings[0])){
            commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<gray>Invalid Skill!"));
            return true;
        }
        try{
            xpammount = Integer.parseInt(strings[1]);
            if(xpammount < 0){
                commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<gray>Invalid XP!"));
                return true;
            }
        } catch (NumberFormatException e) {
            commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<gray>Invalid XP!"));
            return true;
        }

        getPlugin().getUtils().getSkillManager().setSkillXP(getPlugin().getUtils().getSkillManager().findSkills(target.getUniqueId().toString(), getPlugin().getUtils().getProfileManager().getActiveProfileName(target.getUniqueId().toString())), strings[0], xpammount);

        if(commandSender instanceof Player){
            commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<gray>Set " + strings[0] + " XP to " + xpammount + " for " + target.getName()));
        }
        Bukkit.getLogger().info("[SkysongStats] Set " + strings[0] + " XP to " + xpammount + " for " + target.getName());

        return true;
    }
}
