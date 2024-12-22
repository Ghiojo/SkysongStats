package org.skysongdev.skysongstats.commands.modifier;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.checkerframework.checker.units.qual.A;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongstats.SkysongStats;
import org.skysongdev.skysongstats.Utils.Utils;
import org.skysongdev.skysongstats.commands.stats.ViewStats;

import java.util.Arrays;
import java.util.List;

public class ModifierCommand implements TabExecutor {
    AddModifier addModifier;
    DelModifier delModifier;
    ViewModifiers viewModifiers;

    public ModifierCommand(){
        this.addModifier = new AddModifier();
        this.delModifier = new DelModifier();
        this.viewModifiers = new ViewModifiers();
    }
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        String[] newArgs = Arrays.copyOfRange(strings, 1, strings.length);
        Player player = (Player) commandSender;

        if(strings.length < 1){
            commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<gray>Too Little arguments! (Usage: /profile (subcommand))"));
            return true;
        }

        switch(strings[0].toLowerCase()){
            case "add":
                addModifier.onCommand(commandSender, command, s, newArgs);
                break;
            case "del":
            case "delete":
                delModifier.onCommand(commandSender, command, s, newArgs);
                break;
            case "view":
                viewModifiers.onCommand(commandSender, command, s, newArgs);
                break;
            default:
                commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<gray>Incorrect Arguments! Usage: /modifier (add/del/view) (modifier) (value)"));
                break;
        }


        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        String[] newstrings = Arrays.copyOfRange(strings, 1, strings.length);
        if(strings.length == 1){
            return Arrays.asList("add", "del");
        }
        if(strings.length > 1){
            switch(strings[0].toLowerCase()){
                case "add":
                    return addModifier.onTabComplete(commandSender, command, s, newstrings);
                case "del":
                case "delete":
                    return delModifier.onTabComplete(commandSender, command, s, newstrings);
                case "view":
                    return viewModifiers.onTabComplete(commandSender, command, s, newstrings);
                default:
                    return List.of("");
            }
        }
        return List.of("");
    }
}
