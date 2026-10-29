package org.skysongdev.skysongstats.commands.profile;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongstats.SkysongStats;
import org.skysongdev.skysongstats.database.*;

import java.util.List;
import java.util.Map;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class CreateProfile implements TabExecutor {

    public CreateProfile() { }
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        Player player = (Player) commandSender;
        if(strings.length < 1){
            getPlugin().getMessages().send(commandSender, "profile.create.usage");
            return true;
        }
        if(getPlugin().getUtils().getStatsManager().findStats(player.getUniqueId().toString(), strings[0]) != null){
            getPlugin().getMessages().send(commandSender, "profile.create.already-exists");
            return true;
        }
        if(strings[0].length() > 20){
            getPlugin().getMessages().send(commandSender, "profile.create.name-too-long");
            return true;
        }

        //We add the Stat Profile to the list of profiles
        PlayerStats profile = new PlayerStats(player.getUniqueId().toString(), strings[0]);
        getPlugin().getUtils().getStatsManager().addStatProfile(profile);

        //We add the Setup Profile to the list
        SetupProfile setupProfile = new SetupProfile(player.getUniqueId().toString(), strings[0], false);
        getPlugin().getUtils().getProfileManager().addSetupProfile(setupProfile);

        //We add the Skill profile to the list
        PlayerSkills skillProfile = new PlayerSkills(player.getUniqueId().toString(), strings[0]);
        getPlugin().getUtils().getSkillManager().addSkillProfile(skillProfile);

        CharacterProfile characterProfile = new CharacterProfile(player.getUniqueId().toString(), strings[0]);
        getPlugin().getUtils().getCharacterManager().addCharacter(characterProfile);

        //We create the profile's character downtime
        getPlugin().getDowntimeService().getCharacter(player.getUniqueId(), strings[0]);


        getPlugin().getMessages().send(player, "profile.create.created", Map.of("profile", strings[0]));

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return List.of("");
    }
}
