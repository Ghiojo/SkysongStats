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
import org.skysongdev.skysongstats.database.SetupProfile;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class CreateProfile implements CommandExecutor {

    public CreateProfile() { }
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        Player player = (Player) commandSender;
        if(strings.length < 1){
            commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<gray>Too Little arguments! (Usage: /profile create (name))"));
            return true;
        }
        if(getPlugin().getUtils().getStatsManager().findStats(player.getUniqueId().toString(), strings[0]) != null){
            commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG  + "<gray> already have a profile with this name!"));
            return true;
        }
        if(strings[0].length() > 20){
            commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG +  "<gray>Profile name is too long! (Max 20 characters)"));
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

        //We add the Setup Profile to the list
        SetupProfile setupProfile = new SetupProfile(player.getUniqueId().toString(), strings[0], false);
        getPlugin().getUtils().getProfileManager().addSetupProfile(setupProfile);
        try {
            getPlugin().getDatabase().createSetupData(setupProfile);
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


        player.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG +  "<gray>Profile \"" + strings[0] + "\" has been created!"));

        return true;
    }
}
