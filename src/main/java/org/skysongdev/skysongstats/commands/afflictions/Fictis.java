package org.skysongdev.skysongstats.commands.afflictions;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.Utils.Utils;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class Fictis implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Player player = (Player) sender;
        Player target = null;
        int num = 0;
        if(args.length == 0) {
            sender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>Too Little arguments! (Usage: /fictis (amount) (player)"));
            return true;
        }
        if(args.length < 2) {
            target = player;
        } else {
            target = getPlugin().getServer().getPlayer(args[1]);
            if(target == null) {
                player.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>Player not found!"));
                return true;
            }
        }
        try{
            num = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>Invalid number!"));
            return true;
        }
        if (num < 0) {
            player.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>Invalid number!"));
            return true;
        }
        getPlugin().getUtils().getProfileManager().findActiveStats(target.getUniqueId().toString()).setFictis(num);

        player.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<green>Set " + target.getName() + "'s Fictis Apotheosis to " + num));
        return true;

    }
}
