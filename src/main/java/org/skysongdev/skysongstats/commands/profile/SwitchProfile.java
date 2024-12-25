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
import org.skysongdev.skysongstats.Utils.Utils;
import org.skysongdev.skysongstats.database.PlayerSkills;
import org.skysongdev.skysongstats.database.PlayerStats;
import org.skysongdev.skysongstats.events.ProfileUpdateEvent;

import java.util.ArrayList;
import java.util.List;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class SwitchProfile implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        Player player = (Player) commandSender;
        if(strings.length < 1){
            commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>Too Little arguments! (Usage: /profile create (name))"));
            return true;
        }
        if(SkysongStats.getPlugin().getUtils().getStatsManager().findStats(player.getUniqueId().toString(), strings[0]) == null){
            commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>You have no profile with that name!"));
            return true;
        }
        PlayerStats stat = SkysongStats.getPlugin().getUtils().getStatsManager().findStats(player.getUniqueId().toString(), strings[0]);

        SkysongStats.getPlugin().getUtils().getProfileManager().setProfile(stat);
        try{
            getPlugin().getDatabase().updateActiveProfileData(getPlugin().getUtils().getProfileManager().findActiveProfile(player.getUniqueId().toString()));
        } catch (Exception e){
            e.printStackTrace();
        }

        ProfileUpdateEvent event = new ProfileUpdateEvent(player, player.getUniqueId().toString(), strings[0]);
        Bukkit.getPluginManager().callEvent(event);

        commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<green>Profile switched to " + strings[0]));

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        ArrayList<String> profiles = getPlugin().getUtils().getProfileManager().findAllProfiles(((Player) commandSender).getUniqueId().toString());
        return profiles.stream().filter(profile -> profile.startsWith(strings[0])).toList();
    }
}
