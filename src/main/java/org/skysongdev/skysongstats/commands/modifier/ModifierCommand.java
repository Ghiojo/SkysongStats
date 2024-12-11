package org.skysongdev.skysongstats.commands.modifier;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.units.qual.A;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.SkysongStats;
import org.skysongdev.skysongstats.Utils.Utils;

import java.util.Arrays;

public class ModifierCommand implements CommandExecutor {
    AddModifier addModifier;
    DelModifier delModifier;

    public ModifierCommand(){
        this.addModifier = new AddModifier();
        this.delModifier = new DelModifier();
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
                //TODO: Add all Modifier Commands
                break;
        }


        return true;
    }
}
