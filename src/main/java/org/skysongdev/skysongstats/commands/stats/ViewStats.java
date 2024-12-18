package org.skysongdev.skysongstats.commands.stats;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.SkysongStats;
import org.skysongdev.skysongstats.Utils.Utils;
import org.skysongdev.skysongstats.database.PlayerStats;

public class ViewStats implements CommandExecutor {

    public ViewStats() {}
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        Player player = (Player) commandSender;
        Player target;

        if(strings.length > 1){
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
        player.sendMessage(Utils.getMiniMessage().deserialize("                <#F6DA23>"+ player.getName() + "' s <#864811>Stat Sheet:"));
        player.sendMessage("");
        player.sendMessage(Utils.getMiniMessage().deserialize("<#D0530C><bold>Main Stats</bold>"));
        player.sendMessage(Utils.getMiniMessage().deserialize("<#B1262C><bold>Str:</bold> <#B1262C>" + stats.getStrength() + "<8a2c30>(" + stats.getStrengthModifiers() + ") <#864811>| <#55B141><bold>Dex:</bold> <#55B141>" + stats.getDexterity() + " <#4e8243>(" + stats.getDexterityModifiers() + ") <#864811>| <#D4992C><bold>Con:</bold> <#D4992C>" + stats.getConstitution() + "<#b08a43>(" + stats.getConstitutionModifiers() + ")"));
        player.sendMessage(Utils.getMiniMessage().deserialize("<#2B8EDE><bold>Foc:</bold> <#2B8EDE>" + stats.getFocus() + "<#4086bf>("+ stats.getFocusModifiers() +") <#864811>| <#F2F39B><bold>Spd:</bold> <#F2F39B>" + stats.getSpeed() + "<#dedf76>("+ stats.getSpeedModifiers() + ")"));
        player.sendMessage("");
        player.sendMessage(Utils.getMiniMessage().deserialize("<#D0530C><bold>Health and AC</bold>"));
        player.sendMessage(Utils.getMiniMessage().deserialize("<#EE5B79><bold>HP:</bold> <#EE5B79>" + stats.getHp() + "<#831F34>/<#EE5B79>" + stats.getHp_max() + "<#ba415a>(" + stats.getMaxHPModifiers() + ") <#864811>| <#513E3E><bold>AC:</bold> <#513E3E>" + stats.getAc()));
        player.sendMessage("");
        player.sendMessage(Utils.getMiniMessage().deserialize("<#D0530C><bold>Afflictions</bold>"));
        player.sendMessage(Utils.getMiniMessage().deserialize("<#21ABA5><bold>Arcrot:</bold> <#21ABA5>" + stats.getArcrot() +"<#0D5653>/<#21ABA5>"+ stats.getArcrot_max() + "<#368a87>(" + stats.getMaxArcrotModifiers() + ") <#864811>| <#2EB12C><bold>Potion Sickness:</bold> <#2EB12C>" + stats.getPotion() + "<#094C08>/<#2EB12C>" + stats.getPotion_max() + "<#449443>(" + stats.getMaxPotionModifiers() + ")"));
        player.sendMessage(Utils.getMiniMessage().deserialize("<#710E86><bold>Fictis Apotheosis:</bold> <#710E86>" + stats.getFictis() + "<#410F4C>/<#710E86>" + stats.getFictis_max() + "<#572861>(" + stats.getMaxFictisModifiers() + ")"));
        player.sendMessage(Utils.getMiniMessage().deserialize("<#864811>|====================<<<#63C934>₪<#864811>>>====================|"));

        return true;
    }
}
