package org.skysongdev.skysongstats.commands.stats;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongstats.SkysongStats;
import org.skysongdev.skysongstats.Utils.Utils;
import org.skysongdev.skysongstats.database.PlayerStats;

import java.util.List;

public class ViewStats implements TabExecutor {

    public ViewStats() {}
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        Player player = (Player) commandSender;
        Player target;

        if(strings.length > 0){
            target = Bukkit.getServer().getPlayer(strings[0]);
            if(target == null){
                player.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>No Player found with that username!"));
                return true;
            }
        } else {
            target = player;
        }
        PlayerStats stats = SkysongStats.getPlugin().getUtils().getProfileManager().findActiveStats(target.getUniqueId().toString());

        player.sendMessage(Utils.getMiniMessage().deserialize("<#864811>|====================<<<#63C934>₪<#864811>>>====================|"));
        player.sendMessage(Utils.getMiniMessage().deserialize("                <#F6DA23>"+ target.getName() + "' s <#864811>Stat Sheet:"));
        player.sendMessage("");
        player.sendMessage(Utils.getMiniMessage().deserialize("<#D0530C><bold>Main Stats</bold>"));
        player.sendMessage(Utils.getMiniMessage().deserialize("<#B1262C><bold>Str:</bold> <#B1262C>" + stats.getStrength() + "<#71272a> (" + stats.getStrengthModifiers() + ") <#864811>| <#55B141><bold>Dex:</bold> <#55B141>" + stats.getDexterity() + " <#3c5936> (" + stats.getDexterityModifiers() + ") <#864811>| <#D4992C><bold>Con:</bold> <#D4992C>" + stats.getConstitution() + "<#766035> (" + stats.getConstitutionModifiers() + ")"));
        player.sendMessage(Utils.getMiniMessage().deserialize("<#2B8EDE><bold>Foc:</bold> <#2B8EDE>" + stats.getFocus() + "<#3b678a> ("+ stats.getFocusModifiers() +") <#864811>| <#F2F39B><bold>Spd:</bold> <#F2F39B>" + stats.getSpeed() + "<#adad5d> ("+ stats.getSpeedModifiers() + ")"));
        player.sendMessage("");
        player.sendMessage(Utils.getMiniMessage().deserialize("<#D0530C><bold>Health and AC</bold>"));
        player.sendMessage(Utils.getMiniMessage().deserialize("<#EE5B79><bold>HP:</bold> <#EE5B79>" + stats.getHp() + "<#831F34>/<#EE5B79>" + (stats.getHp_max() + stats.getMaxHPModifiers()) + "<#8d394a> (" + stats.getMaxHPModifiers() + ") <#864811>| <#513E3E><bold>AC:</bold> <#513E3E>" + stats.getAc()));
        player.sendMessage(Utils.getMiniMessage().deserialize("<#67e4e1><bold>Temp HP:</bold> <#67e4e1>" + stats.getTemp_hp()));
        player.sendMessage("");
        player.sendMessage(Utils.getMiniMessage().deserialize("<#D0530C><bold>Afflictions</bold>"));
        player.sendMessage(Utils.getMiniMessage().deserialize("<#21ABA5><bold>Arcrot:</bold> <#21ABA5>" + stats.getArcrot() +"<#0D5653>/<#21ABA5>"+ stats.getArcrot_max() + "<#2d6765> (" + stats.getArcrotModifiers() + "/" + stats.getMaxArcrotModifiers() + ") <#864811>| <#2EB12C><bold>Potion Sickness:</bold> <#2EB12C>" + stats.getPotion() + "<#094C08>/<#2EB12C>" + stats.getPotion_max() + "<#3c733b> (" + stats.getMaxPotionModifiers() + ")"));
        player.sendMessage(Utils.getMiniMessage().deserialize("<#710E86><bold>Fictis Apotheosis:</bold> <#710E86>" + stats.getFictis() + "<#410F4C>/<#710E86>" + stats.getFictis_max() + "<#422947> (" + stats.getFictisModifiers() + "/"  + stats.getMaxFictisModifiers() + ")"));
        player.sendMessage(Utils.getMiniMessage().deserialize("<#864811>|====================<<<#63C934>₪<#864811>>>====================|"));

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (strings.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(strings[0].toLowerCase()))
                    .toList();
        }
        return null;
    }
}
