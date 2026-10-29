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
import java.util.Map;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class AddSkillXP implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        Player player = null;
        Player target;
        int xpammount;
        if(commandSender instanceof Player){
            if(!commandSender.hasPermission("skysongstats.addxp")){
                getPlugin().getMessages().send(commandSender, "general.no-permission");
                return true;
            }
            player = (Player) commandSender;
        }
        if(strings.length < 2){
            getPlugin().getMessages().send(commandSender, "skills.addxp.usage");
            return true;
        }
        if(strings.length < 3) {
            if(!(commandSender instanceof  Player)){
                getPlugin().getMessages().send(commandSender, "general.console-needs-player");
                return true;
            }
            target = (Player) commandSender;
        }
        else{
            target = Bukkit.getPlayer(strings[2]);
            if(target == null){
                getPlugin().getMessages().send(commandSender, "general.player-not-found", Map.of("player", strings[2]));
                return true;
            }
        }

        if(!Utils.isASkill(strings[0])){
            getPlugin().getMessages().send(commandSender, "skills.invalid-skill");
            return true;
        }
        try{
            xpammount = Integer.parseInt(strings[1]);
            if(xpammount < 0){
                getPlugin().getMessages().send(commandSender, "skills.invalid-xp");
                return true;
            }
        } catch (NumberFormatException e) {
            getPlugin().getMessages().send(commandSender, "skills.invalid-xp");
            return true;
        }

        getPlugin().getUtils().getSkillManager().addSkillXP(getPlugin().getUtils().getSkillManager().findSkills(target.getUniqueId().toString(), getPlugin().getUtils().getProfileManager().getActiveProfileName(target.getUniqueId().toString())), strings[0], xpammount);
        try{
            getPlugin().getDatabase().updateSkillsData(getPlugin().getUtils().getSkillManager().findSkills(target.getUniqueId().toString(), getPlugin().getUtils().getProfileManager().getActiveProfileName(target.getUniqueId().toString())));
        } catch (Exception e){
            e.printStackTrace();
        }


        if(commandSender instanceof Player){
            getPlugin().getMessages().send(player, "skills.addxp.added", Map.of("xp", String.valueOf(xpammount), "player", target.getName(), "skill", strings[0]));
        }
        Bukkit.getLogger().info("[SkysongStats] Added " + xpammount + " XP to " + target.getName() + "'s " + strings[0] + " skill!");
        if(target != player)
            getPlugin().getMessages().send(target, "skills.addxp.gained", Map.of("xp", String.valueOf(xpammount), "skill", strings[0]));

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
