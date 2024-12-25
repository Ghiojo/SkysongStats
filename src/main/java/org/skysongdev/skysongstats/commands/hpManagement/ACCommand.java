package org.skysongdev.skysongstats.commands.hpManagement;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.Utils.Utils;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class ACCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if(strings.length <= 0){
            commandSender.sendMessage(getPlugin().getUtils().getMiniMessage().deserialize(getPlugin().getUtils().PLUGIN_TAG + "<red>Too little arguments! (Usage: /ac <value>)"));
        }
        Player player = (Player) commandSender;
        int acAmount;
        try{
            acAmount = Integer.parseInt(strings[0]);
            if(acAmount < 0){
                commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<gray>Invalid HP!"));
                return true;
            }
        } catch (NumberFormatException e) {
                commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<gray>Invalid HP Amount!"));
            return true;
        }

        getPlugin().getUtils().getProfileManager().findActiveStats(player.getUniqueId().toString()).setAC(acAmount);
        commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<green>AC set to <gold>" + acAmount + "<green>"));

        try{
            getPlugin().getDatabase().updateStatData( getPlugin().getUtils().getProfileManager().findActiveStats(player.getUniqueId().toString()));
        } catch (Exception e){
            commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>Failed to update player stats!"));
            return true;
        }

        return true;
    }
}
