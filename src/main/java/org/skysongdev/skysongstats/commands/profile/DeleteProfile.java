package org.skysongdev.skysongstats.commands.profile;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.SkysongStats;
import org.skysongdev.skysongstats.Utils.Utils;

import java.util.Objects;

public class DeleteProfile implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        Player player = (Player) commandSender;
        if(strings.length < 1){
            commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>Too Little arguments! (Usage: /profile create (name))"));
            return true;
        }
        if(SkysongStats.getPlugin().getUtils().getStatsManager().findStats(player.getUniqueId().toString(), strings[0]) == null){
            commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>There's no profile with that name!"));
            return true;
        }
        if(Objects.equals(SkysongStats.getPlugin().getUtils().getStatsManager().findStats(player.getUniqueId().toString(), strings[0]).getProfile(), SkysongStats.getPlugin().getUtils().getProfileManager().findActiveStats(player.getUniqueId().toString()).getProfile())){
            commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>You cannot delete the profile you are currently on, switch profiles before doing so!"));
            return true;
        }

        SkysongStats.getPlugin().getUtils().getStatsManager().deleteStat(SkysongStats.getPlugin().getUtils().getStatsManager().findStats(player.getUniqueId().toString(), strings[0]));
        SkysongStats.getPlugin().getUtils().getSkillManager().deleteSkillProfile(SkysongStats.getPlugin().getUtils().getSkillManager().findSkills(player.getUniqueId().toString(), strings[0]));
        SkysongStats.getPlugin().getUtils().getProfileManager().deleteSetupProfile(SkysongStats.getPlugin().getUtils().getProfileManager().findSetupProfile(player.getUniqueId().toString(), strings[0]));

        player.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<gray>Profile \"" + strings[0] + "\" has been deleted!"));
        return true;
    }
}
