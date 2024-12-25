package org.skysongdev.skysongstats.commands.hpManagement;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.Utils.Utils;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class TempHp implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        Player sender = null;
        Player target;
        int tHpAmount;
        if (commandSender instanceof Player) {
            sender = (Player) commandSender;
        }
        if(strings.length < 1){
            if(sender != null)
                sender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<gray>Too Little arguments! (Usage: /skill addxp (skill) (xp) (player (optional)))"));
            else
                Bukkit.getLogger().warning("[Skysong Stats] Too Little arguments! (Usage: /skill addhp (skill) (xp) (player (optional)))");
            return true;
        }
        if(strings.length < 2) {
            if(!(commandSender instanceof  Player)){
                Bukkit.getLogger().warning("You must specify a player if you are running this command on console!");
                return true;
            }
            target = (Player) commandSender;
        } else{
            if(commandSender instanceof Player && !commandSender.hasPermission("skysongstats.temphp.other")){
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
            tHpAmount = Integer.parseInt(strings[0]);
            if(tHpAmount < 0){
                if(sender != null)
                    sender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<gray>Invalid HP!"));
                else
                    Bukkit.getLogger().warning("[Skysong Stats] Invalid HP!");
                return true;
            }
        } catch (NumberFormatException e) {
            if(sender != null)
                sender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<gray>Invalid HP!"));
            else
                Bukkit.getLogger().warning("[Skysong Stats] Invalid HP!");
            return true;
        }

        getPlugin().getUtils().getProfileManager().findActiveStats(target.getUniqueId().toString()).setTemp_hp(tHpAmount);
        try{
            getPlugin().getDatabase().updateStatData( getPlugin().getUtils().getProfileManager().findActiveStats(target.getUniqueId().toString()));
        } catch (Exception e){
            if(sender != null)
                sender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>Failed to update player stats!"));
            else
                Bukkit.getLogger().warning("[Skysong Stats] Failed to update player stats!");
            return true;
        }

        target.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<#67e4e1>Your temp HP has been set to <gold>" + tHpAmount + "<#67e4e1>!"));

        if(sender != null && sender != target)
            sender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<#67e4e1>Set <yellow>" + target.getName() + "<#67e4e1>'s temp HP to <gold>" + tHpAmount + "<#67e4e1>!"));
        else
            Bukkit.getLogger().info("[Skysong Stats] Set " + target.getName() + "'s temp HP to " + tHpAmount + "!");

        return true;
    }
}
