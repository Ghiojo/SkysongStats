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
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class DeleteProfile implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        Player player = (Player) commandSender;
        if(strings.length < 1){
            getPlugin().getMessages().send(commandSender, "profile.delete.usage");
            return true;
        }
        if(!getPlugin().getUtils().getProfileManager().hasProfile(player.getUniqueId(), strings[0])){
            getPlugin().getMessages().send(commandSender, "profile.delete.not-found");
            return true;
        }
        String activeProfile = getPlugin().getUtils().getProfileManager().getActiveProfileName(player.getUniqueId().toString());
        String targetProfile = strings[0];
        if(activeProfile.equals(targetProfile)){
            getPlugin().getMessages().send(commandSender, "profile.delete.active");
            return true;
        }

        getPlugin().getUtils().getProfileManager().deleteSetupProfile(getPlugin().getUtils().getProfileManager().findSetupProfile(player.getUniqueId().toString(), strings[0]));
        getPlugin().getUtils().getCharacterManager().deleteCharacterProfile(getPlugin().getUtils().getCharacterManager().findCharacterProfile(player.getUniqueId().toString(), strings[0]));
        getPlugin().getDowntimeService().deleteCharacter(player.getUniqueId(), targetProfile);
        getPlugin().getStatService().deleteCharacter(new ProfileKey(player.getUniqueId(), targetProfile));
        getPlugin().getSkillService().deleteCharacter(new ProfileKey(player.getUniqueId(), targetProfile));

        getPlugin().getUtils().getProfileManager().findAllProfiles(player.getUniqueId().toString()).remove(strings[0]);

        getPlugin().getMessages().send(player, "profile.delete.deleted", Map.of("profile", strings[0]));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        ArrayList<String> profiles = getPlugin().getUtils().getProfileManager().findAllProfiles(((Player) commandSender).getUniqueId().toString());
        return profiles.stream().filter(profile -> profile.startsWith(strings[0])).toList();
    }
}
