package org.skysongdev.skysongstats.commands.hpManagement;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.Utils.Utils;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class AddHP implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        Player sender = null;
        Player target;
        int hpAmount;
        if (commandSender instanceof Player) {
            sender = (Player) commandSender;
        }
        if(strings.length < 1){
            if(sender != null)
                sender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<gray>Too Little arguments! (Usage: /heal (number) (player (optional)))"));
            else
                Bukkit.getLogger().warning("[Skysong Stats] Too Little arguments! (Usage: /heal (number) (player (optional)))");
            return true;
        }
        if(strings.length < 2) {
            if(!(commandSender instanceof  Player)){
                Bukkit.getLogger().warning("You must specify a player if you are running this command on console!");
                return true;
            }
            target = (Player) commandSender;
        } else{
            if(commandSender instanceof Player && !commandSender.hasPermission("skysongstats.heal.other")){
                sender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<gray>You don't have permissions to set other players' stats!"));
                return true;
            }
            target = Bukkit.getPlayer(strings[1]);
            if(target == null){
                if(sender != null)
                    sender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<gray>Player not found!"));
                else
                    Bukkit.getLogger().warning("[Skysong Stats] Player not found!");
                return true;
            }
        }

        try{
            hpAmount = Integer.parseInt(strings[0]);
            if(hpAmount < 0){
                if(sender != null)
                    sender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<gray>Invalid HP!"));
                else
                    Bukkit.getLogger().warning("[Skysong Stats] Invalid HP!");
                return true;
            }
        } catch (NumberFormatException e) {
            if(sender != null)
                sender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<gray>Invalid HP Amount!"));
            else
                Bukkit.getLogger().warning("[Skysong Stats] Invalid HP!");
            return true;
        }

        getPlugin().getUtils().getProfileManager().findActiveStats(target.getUniqueId().toString()).addHP(hpAmount);
        try{
            getPlugin().getDatabase().updateStatData( getPlugin().getUtils().getProfileManager().findActiveStats(target.getUniqueId().toString()));
        } catch (Exception e){
            if(sender != null)
                sender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>Failed to update player stats!"));
            else
                Bukkit.getLogger().warning("[Skysong Stats] Failed to update player stats!");
            return true;
        }

        target.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<green>You have been healed for<gold> " + hpAmount + " HP<green>! Your health is now <gold>" + getPlugin().getUtils().getProfileManager().findActiveStats(target.getUniqueId().toString()).getHp() + "<green>!"));

        if(sender != null && sender != target)
            sender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<gray>Added " + hpAmount + " HP to " + target.getName() + "!"));
        Bukkit.getLogger().info("[SkysongStats] Added " + hpAmount + " HP to " + target.getName() + "!");

        return true;
    }
}
