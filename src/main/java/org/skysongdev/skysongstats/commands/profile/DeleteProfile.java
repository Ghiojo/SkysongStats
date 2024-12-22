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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class DeleteProfile implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        Player player = (Player) commandSender;
        if(strings.length < 1){
            commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>Too Little arguments! (Usage: /profile create (name))"));
            return true;
        }
        if(getPlugin().getUtils().getStatsManager().findStats(player.getUniqueId().toString(), strings[0]) == null){
            commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>There's no profile with that name!"));
            return true;
        }
        if(Objects.equals(getPlugin().getUtils().getStatsManager().findStats(player.getUniqueId().toString(), strings[0]).getProfile(), getPlugin().getUtils().getProfileManager().findActiveStats(player.getUniqueId().toString()).getProfile())){
            commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>You cannot delete the profile you are currently on, switch profiles before doing so!"));
            return true;
        }

        getPlugin().getUtils().getStatsManager().deleteStat(getPlugin().getUtils().getStatsManager().findStats(player.getUniqueId().toString(), strings[0]));
        getPlugin().getUtils().getSkillManager().deleteSkillProfile(getPlugin().getUtils().getSkillManager().findSkills(player.getUniqueId().toString(), strings[0]));
        getPlugin().getUtils().getProfileManager().deleteSetupProfile(getPlugin().getUtils().getProfileManager().findSetupProfile(player.getUniqueId().toString(), strings[0]));

        player.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<gray>Profile \"" + strings[0] + "\" has been deleted!"));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        ArrayList<String> profiles = getPlugin().getUtils().getProfileManager().findAllProfiles(((Player) commandSender).getUniqueId().toString());
        return profiles.stream().filter(profile -> profile.startsWith(strings[0])).toList();
    }
}
