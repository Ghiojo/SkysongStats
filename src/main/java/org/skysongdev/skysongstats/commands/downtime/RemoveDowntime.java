package org.skysongdev.skysongstats.commands.downtime;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongstats.Utils.ProfileManager;
import org.skysongdev.skysongstats.Utils.Utils;
import org.skysongdev.skysongstats.database.CharacterDowntime;
import org.skysongdev.skysongstats.database.PlayerDowntime;

import java.util.List;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;
import static org.skysongdev.skysongstats.Utils.Utils.PLUGIN_TAG;

public class RemoveDowntime implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if(args.length < 2){
            sender.sendMessage(Utils.getMiniMessage().deserialize(PLUGIN_TAG + "<red>Usage: /downtime remove <amount> <player> [global/profile]"));
            return true;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[0]);
        } catch (NumberFormatException e){
            sender.sendMessage(Utils.getMiniMessage().deserialize(PLUGIN_TAG + "<red>Amount must be a valid number."));
            return true;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if(target == null) {
            sender.sendMessage(Utils.getMiniMessage().deserialize(PLUGIN_TAG + "<red>Player " + args[1] + " not found or not online."));
            return true;
        }
        boolean isGlobal;
        String targetProfile = "lmao";
        if(args.length >= 3){
            String type = args[2].toLowerCase();
            if(type.equals("global")){
                isGlobal = true;
            } else {
                targetProfile = args[2];
                getPlugin().getUtils().getDowntimeManager().updateCharacterDowntime(new CharacterDowntime(targetProfile, target.getUniqueId().toString(), getPlugin().getUtils().getDowntimeManager().findCharacterDowntime(target.getUniqueId().toString(), targetProfile).getCurrentDowntime() - amount));
                return true;
            }
        } else {
            isGlobal = ProfileManager.downtimePreferences.get(target.getUniqueId().toString());
        }
        if(isGlobal){
            getPlugin().getUtils().getDowntimeManager().updatePlayerDowntime(new PlayerDowntime(target.getUniqueId().toString(), getPlugin().getUtils().getDowntimeManager().findPlayerDowntime(target.getUniqueId().toString()).getCurrentDowntime() - amount));
            sender.sendMessage(Utils.getMiniMessage().deserialize(PLUGIN_TAG + "<green>Successfully removed " + amount + " downtime from " + target.getName() + "'s global downtime."));
        } else {
            getPlugin().getUtils().getDowntimeManager().updateCharacterDowntime(new CharacterDowntime(getPlugin().getUtils().getProfileManager().getActiveProfileName(target.getUniqueId().toString()), target.getUniqueId().toString(), getPlugin().getUtils().getDowntimeManager().findCharacterDowntime(target.getUniqueId().toString(), getPlugin().getUtils().getProfileManager().getActiveProfileName(target.getUniqueId().toString())).getCurrentDowntime() - amount));
            sender.sendMessage(Utils.getMiniMessage().deserialize(PLUGIN_TAG + "<green>Successfully removed " + amount + " downtime from " + target.getName() + "'s " + targetProfile + " profile downtime."));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if(args.length == 1) {
            return List.of();
        }
        if(args.length == 2) {
            return List.of(Bukkit.getOnlinePlayers().stream().map(player -> player.getName()).toArray(String[]::new)).stream().filter(s -> s.startsWith(args[1].toLowerCase())).toList();
        }
        if(args.length == 3) {
            List<String> profiles = List.of("global");
            if(Bukkit.getPlayer(args[1]) != null)
                profiles = getPlugin().getUtils().getProfileManager().findAllProfiles(Bukkit.getPlayerExact(args[1]).getUniqueId().toString());
            return profiles.stream().filter(s -> s.startsWith(args[2].toLowerCase())).toList();
        }

        return null;
    }
}
