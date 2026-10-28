package org.skysongdev.skysongstats.commands.downtime;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongstats.SkysongStats;
import org.skysongdev.skysongstats.Utils.DowntimeManager;
import org.skysongdev.skysongstats.Utils.Utils;
import org.skysongdev.skysongstats.database.CharacterDowntime;
import org.skysongdev.skysongstats.database.PlayerDowntime;

import java.util.List;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;
import static org.skysongdev.skysongstats.Utils.Utils.PLUGIN_TAG;

public class AddDowntime implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length < 3) {
            sender.sendMessage(Utils.getMiniMessage().deserialize(PLUGIN_TAG + "<red>Usage: /downtime add <amount> <player> <global/profile>"));
            return true;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Utils.getMiniMessage().deserialize(PLUGIN_TAG + "<red>Amount must be a valid number."));
            return true;
        }
        String target = args[1];
        if (Bukkit.getPlayer(target) == null) {
            sender.sendMessage(Utils.getMiniMessage().deserialize(PLUGIN_TAG + "<red>Player " + target + " not found or not online."));
            return true;
        }
        String type = args[2];
        if (type.equals("global")) {
            getPlugin().getUtils().getDowntimeManager().updatePlayerDowntime(new PlayerDowntime(Bukkit.getPlayer(target).getUniqueId().toString(), SkysongStats.getPlugin().getUtils().getDowntimeManager().findPlayerDowntime(Bukkit.getPlayer(target).getUniqueId().toString()).getCurrentDowntime() + amount));
            sender.sendMessage(Utils.getMiniMessage().deserialize(PLUGIN_TAG + "<green>Added " + amount + " downtime to player " + target + "'s global downtime."));
            return true;
        } else {
            if (!getPlugin().getUtils().getProfileManager().findAllProfiles(Bukkit.getPlayer(target).getUniqueId().toString()).contains(type)) {
                sender.sendMessage(Utils.getMiniMessage().deserialize(PLUGIN_TAG + "<red>Profile " + type + " not found for player " + target + "."));
                return true;
            }
            getPlugin().getUtils().getDowntimeManager().updateCharacterDowntime(new CharacterDowntime(type, Bukkit.getPlayer(target).getUniqueId().toString(), SkysongStats.getPlugin().getUtils().getDowntimeManager().findPlayerDowntime(Bukkit.getPlayer(target).getUniqueId().toString()).getCurrentDowntime() + amount));
            sender.sendMessage(Utils.getMiniMessage().deserialize(PLUGIN_TAG + "<green>Added " + amount + " downtime to player " + target + "'s profile " + type + "."));
            return true;
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if(args.length == 1) {
            return null;
        }
        if(args.length == 2) {
            return List.of(Bukkit.getOnlinePlayers().stream().map(player -> player.getName()).toArray(String[]::new)).stream().filter(s -> s.startsWith(args[1].toLowerCase())).toList();
        }
        if(args.length == 3) {
            return List.of("global").stream().filter(s -> s.startsWith(args[2].toLowerCase())).toList();
        }
        return null;
    }
}
