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
import org.skysongdev.skysongstats.Utils.Utils;
import org.skysongdev.skysongstats.database.CharacterProfile;
import org.skysongdev.skysongstats.database.PlayerSkills;
import org.skysongdev.skysongstats.database.PlayerStats;
import org.skysongdev.skysongstats.database.SetupProfile;

import java.util.List;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class CreateProfile implements TabExecutor {

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

        //We add the Setup Profile to the list
        SetupProfile setupProfile = new SetupProfile(player.getUniqueId().toString(), strings[0], false);
        getPlugin().getUtils().getProfileManager().addSetupProfile(setupProfile);

        //We add the Skill profile to the list
        PlayerSkills skillProfile = new PlayerSkills(player.getUniqueId().toString(), strings[0]);
        getPlugin().getUtils().getSkillManager().addSkillProfile(skillProfile);

        CharacterProfile characterProfile = new CharacterProfile(player.getUniqueId().toString(), strings[0]);
        getPlugin().getUtils().getCharacterManager().addCharacter(characterProfile);


        player.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG +  "<gray>Profile \"" + strings[0] + "\" has been created!"));

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return List.of("");
    }
}
