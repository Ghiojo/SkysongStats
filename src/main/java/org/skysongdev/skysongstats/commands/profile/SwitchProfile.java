package org.skysongdev.skysongstats.commands.profile;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.SkysongStats;
import org.skysongdev.skysongstats.database.PlayerStats;

public class SwitchProfile implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        Player player = (Player) commandSender;
        if(strings.length < 1){
            commandSender.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "SkysongStats" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY + "Too Little arguments! (Usage: /profile create (name))");
            return true;
        }
        if(SkysongStats.getPlugin().getUtils().getStatsManager().findStats(player.getUniqueId().toString(), strings[0]) == null){
            commandSender.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "SkysongStats" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY + "You have no profile with that name!");
            return true;
        }
        PlayerStats stat = SkysongStats.getPlugin().getUtils().getStatsManager().findStats(player.getUniqueId().toString(), strings[0]);

        SkysongStats.getPlugin().getUtils().getProfileManager().setProfile(stat);


        //Skills also need to be replaced

        return true;
    }
}
