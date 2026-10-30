package org.skysongdev.skysongstats.commands.profile;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongstats.SkysongStats;
import org.skysongdev.skysongstats.events.ProfileUpdateEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class SwitchProfile implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        Player player = (Player) commandSender;
        if(strings.length < 1){
            getPlugin().getMessages().send(commandSender, "profile.switch.usage");
            return true;
        }
        if(!getPlugin().getUtils().getProfileManager().hasProfile(player.getUniqueId(), strings[0])){
            getPlugin().getMessages().send(commandSender, "profile.switch.not-found");
            return true;
        }
        getPlugin().getUtils().getProfileManager().setProfile(player.getUniqueId().toString(), strings[0]);
        try{
            getPlugin().getDatabase().updateActiveProfileData(getPlugin().getUtils().getProfileManager().findActiveProfile(player.getUniqueId().toString()));
        } catch (Exception e){
            e.printStackTrace();
        }

        ProfileUpdateEvent event = new ProfileUpdateEvent(player, player.getUniqueId().toString(), strings[0]);
        Bukkit.getPluginManager().callEvent(event);

        getPlugin().getMessages().send(commandSender, "profile.switch.switched", Map.of("profile", strings[0]));

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        ArrayList<String> profiles = getPlugin().getUtils().getProfileManager().findAllProfiles(((Player) commandSender).getUniqueId().toString());
        return profiles.stream().filter(profile -> profile.startsWith(strings[0])).toList();
    }
}
