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
        Player player = (Player) commandSender;
        if(strings.length < 1){
            viewCharacter.onCommand(commandSender, command, s, strings);
            return true;
        }
        String[] newArgs = Arrays.copyOfRange(strings, 1, strings.length);

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
        String[] newArgs = Arrays.copyOfRange(strings, 1, strings.length);
        if(strings.length == 1){
            return List.of("view", "set");
        }
        if(strings.length > 1){
            switch(strings[0].toLowerCase()){
                case "view":
                    return viewCharacter.onTabComplete(commandSender, command, s, newArgs);
                case "set":
                    return setField.onTabComplete(commandSender, command, s, newArgs);
            }
        }
        return List.of();
    }
}
