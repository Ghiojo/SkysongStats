package org.skysongdev.skysongstats.commands.character;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongstats.Utils.CharacterManager;
import org.skysongdev.skysongstats.messages.Messages;
import org.skysongdev.skysongstats.database.CharacterProfile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class SetCharacterField implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if(!(commandSender instanceof Player)){
            getPlugin().getMessages().send(commandSender, "character.set.players-only");
            return true;
        }
        Player player = (Player) commandSender;
        if(strings.length < 2){
            getPlugin().getMessages().send(commandSender, "character.set.usage");
            return true;
        }
        CharacterProfile profile = getPlugin().getUtils().getCharacterManager().findActiveCharacter(player.getUniqueId().toString());
        StringBuilder field = new StringBuilder();
        for(int i = 1; i < strings.length; i++){
            field.append(strings[i]).append(" ");
        }
        String finalField = field.toString().replace("\"", "''");
        finalField = finalField.substring(0, finalField.length() - 1);
        switch(strings[0].toLowerCase()){
            case "name":
                if(field.length() > 100){
                    getPlugin().getMessages().send(commandSender, "character.set.name-too-long");
                    return true;
                }
                profile.setName(finalField);
                break;
            case "age":
                if(field.length() > 20){
                    getPlugin().getMessages().send(commandSender, "character.set.age-too-long");
                    return true;
                }
                profile.setAge(finalField);
                break;
            case "gender":
                if(field.length() > 20){
                    getPlugin().getMessages().send(commandSender, "character.set.gender-too-long");
                    return true;
                }
                profile.setGender(finalField);
                break;
            case "pronouns":
                if(field.length() > 20){
                    getPlugin().getMessages().send(commandSender, "character.set.pronouns-too-long");
                    return true;
                }
                profile.setPronouns(finalField);
                break;
            case "ancestry":
                if(field.length() > 50){
                    getPlugin().getMessages().send(commandSender, "character.set.ancestry-too-long");
                    return true;
                }
                profile.setAncestry(finalField);
                break;
            case "description":
                profile.setDescription(finalField);
                break;
            default:
                getPlugin().getMessages().send(commandSender, "character.set.invalid-field");
                break;
        }
        try{
            getPlugin().getDatabase().updateCharacterData(profile);
        } catch (Exception e){
            getPlugin().getMessages().send(commandSender, "character.set.failed");
            return true;
        }
        getPlugin().getMessages().send(commandSender, "character.set.updated", Map.of("field", strings[0]), Map.of("value", Messages.playerStyledText(finalField)));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if(strings.length == 1){
            return Arrays.asList("Name", "Age", "Gender", "Pronouns", "Ancestry", "Description").stream().filter(a -> a.startsWith(strings[0])).toList();
        }
        if(strings.length >= 2){
            return List.of("");
        }
        return null;
    }
}
