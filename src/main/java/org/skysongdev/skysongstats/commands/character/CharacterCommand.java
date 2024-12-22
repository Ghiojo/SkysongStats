package org.skysongdev.skysongstats.commands.character;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongstats.Utils.Utils;

import java.util.Arrays;
import java.util.List;

public class CharacterCommand implements TabExecutor {
    ViewCharacter viewCharacter = new ViewCharacter();
    SetCharacterField setField = new SetCharacterField();

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        String[] newArgs = Arrays.copyOfRange(strings, 1, strings.length);
        Player player = (Player) commandSender;
        if(strings.length < 1){
            commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>Too Little arguments! (Usage: /stats (subcommand))"));
            return true;
        }

        switch(strings[0].toLowerCase()){
            case "view":
                viewCharacter.onCommand(commandSender, command, s, newArgs);
                break;
            case "set":
                setField.onCommand(commandSender, command, s, newArgs);
                break;
            default:
                commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>Invalid subcommand!"));
                break;
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return List.of();
    }
}
