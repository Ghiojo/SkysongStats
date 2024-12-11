package org.skysongdev.skysongstats.commands.profile;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.SkysongStats;
import org.skysongdev.skysongstats.Utils.Utils;
import org.skysongdev.skysongstats.database.PlayerSkills;
import org.skysongdev.skysongstats.database.PlayerStats;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class CreateProfile implements CommandExecutor {

    public CreateProfile() { }
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        Player player = (Player) commandSender;
        if(strings.length < 1){
            commandSender.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "SkysongStats" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY + "Too Little arguments! (Usage: /profile create (name))");
            return true;
        }
        if(getPlugin().getUtils().getStatsManager().findStats(player.getUniqueId().toString(), strings[0]) != null){
            commandSender.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "SkysongStats" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY + "You already have a profile with this name!");
            return true;
        }
        if(strings[0].length() > 20){
            commandSender.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "SkysongStats" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY + "Profile name is too long! (Max 20 characters)");
            return true;
        }

        //We add the Stat Profile to the list of profiles
        PlayerStats profile = new PlayerStats(player.getUniqueId().toString(), strings[0]);
        getPlugin().getUtils().getStatsManager().addStatProfile(profile);
        try {
            getPlugin().getDatabase().createStatData(profile);
        }catch(Exception e){
            e.printStackTrace();
        }

        //We add the Skill profile to the list
        PlayerSkills skillProfile = new PlayerSkills(player.getUniqueId().toString(), strings[0]);
        getPlugin().getUtils().getSkillManager().addSkillProfile(skillProfile);
        try {
            getPlugin().getDatabase().createSkillsData(skillProfile);
        }catch(Exception e){
            e.printStackTrace();
        }


        player.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "SkysongStats" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY + " Profile \"" + strings[0] + "\" has been created!");

        return true;
    }
}
