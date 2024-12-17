package org.skysongdev.skysongstats.commands.hpManagement;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.Utils.Utils;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class DamageHP implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        Player sender = null;
        Player target;
        int hpAmount;
        if (commandSender instanceof Player) {
            sender = (Player) commandSender;
        }
        if(strings.length < 2){
            if(sender != null)
                sender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<gray>Too Little arguments! (Usage: /skill addxp (skill) (xp) (player (optional)))"));
            else
                Bukkit.getLogger().warning("[Skysong Stats] Too Little arguments! (Usage: /skill addxp (skill) (xp) (player (optional)))");
            return true;
        }
        if(strings.length < 3) {
            if(!(commandSender instanceof  Player)){
                Bukkit.getLogger().warning("You must specify a player if you are running this command on console!");
                return true;
            }
            target = (Player) commandSender;
        } else{
            if(!commandSender.hasPermission("skysongstats.heal.other")){
                if(sender != null)
                    sender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<gray>You don't have permissions to set other players' stats!"));
                else
                    Bukkit.getLogger().warning("[Skysong Stats] You don't have permissions to heal other players!");
                return true;
            }
            target = Bukkit.getPlayer(strings[2]);
            if(target == null){
                if(sender != null)
                    sender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<gray>Player not found!"));
                else
                    Bukkit.getLogger().warning("[Skysong Stats] Player not found!");
                return true;
            }
        }

        try{
            hpAmount = Integer.parseInt(strings[1]);
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

        getPlugin().getUtils().getProfileManager().findActiveStats(target.getUniqueId().toString()).dealDamage(-hpAmount);
        if(sender != null)
            sender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<gray>Damaged " + target.getName() + " for " + hpAmount + " hp!"));
        Bukkit.getLogger().info("[SkysongStats] Damaged " + target.getName() + " for " + hpAmount + " hp!");

        return true;
    }
}
