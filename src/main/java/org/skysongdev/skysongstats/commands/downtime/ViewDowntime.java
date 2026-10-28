package org.skysongdev.skysongstats.commands.downtime;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongstats.SkysongStats;
import org.skysongdev.skysongstats.Utils.DowntimeManager;
import org.skysongdev.skysongstats.Utils.ProfileManager;
import org.skysongdev.skysongstats.Utils.Utils;
import org.skysongdev.skysongstats.database.CharacterDowntime;

import java.util.List;

import static java.awt.Color.green;
import static org.skysongdev.skysongstats.Utils.Utils.PLUGIN_TAG;

public class ViewDowntime implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if(args.length < 0 || args.length > 1){
            sender.sendMessage(Utils.getMiniMessage().deserialize(PLUGIN_TAG + "<red>Usage: /downtime view <player>"));
            return true;
        }
        Player target = null;
        if(args.length == 1){
            if(sender.hasPermission("skysongstats.downtime.view.others")){
                target = Bukkit.getPlayer(args[1]);
            } else {
                sender.sendMessage(Utils.getMiniMessage().deserialize(PLUGIN_TAG + "<red>You do not have permission to view other players' downtime."));
            }
        } else {
            target = (Player) sender;
        }
        if(target == null) {
            sender.sendMessage(Utils.getMiniMessage().deserialize(PLUGIN_TAG + "<red>Player " + args[0] + " not found or not online."));
            return true;
        }

        sender.sendMessage(Utils.getMiniMessage().deserialize("<gold>" + target.getName() + "<green>'s Downtime:"));
        sender.sendMessage(Utils.getMiniMessage().deserialize("<green>- Global Downtime: <gold>" + SkysongStats.getPlugin().getUtils().getDowntimeManager().findPlayerDowntime(target.getUniqueId().toString()).getCurrentDowntime()));
        sender.sendMessage(Utils.getMiniMessage().deserialize("<green>- Character Downtime: <gold>" + SkysongStats.getPlugin().getUtils().getDowntimeManager().findCharacterDowntime(target.getUniqueId().toString(), SkysongStats.getPlugin().getUtils().getProfileManager().getActiveProfileName(target.getUniqueId().toString())).getCurrentDowntime()));
        if( ProfileManager.downtimePreferences.get(target.getUniqueId().toString())){
            sender.sendMessage(Utils.getMiniMessage().deserialize("<green>- Active Downtime Spending Preference: <gold>Global"));
        } else {
            sender.sendMessage(Utils.getMiniMessage().deserialize("<green>- Active Downtime Spending Preference: <gold>Character"));
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        return List.of(Bukkit.getOnlinePlayers().stream().map(player -> player.getName()).toArray(String[]::new)).stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
    }
}
