package org.skysongdev.skysongstats.commands.character;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongstats.Utils.CharacterManager;
import org.skysongdev.skysongstats.Utils.Utils;
import org.skysongdev.skysongstats.database.CharacterProfile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class SetCharacterField implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if(!(commandSender instanceof Player)){
            Bukkit.getLogger().warning("Only players can use this command!");
            return true;
        }
        Player player = (Player) commandSender;
        if(strings.length < 2){
            commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>Too Little arguments! (Usage: /character set (field) (value))"));
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
                    commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>Name is too long!"));
                    return true;
                }
                profile.setName(finalField);
                break;
            case "age":
                if(field.length() > 20){
                    commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>Age is too long!"));
                    return true;
                }
                profile.setAge(finalField);
                break;
            case "gender":
                if(field.length() > 20){
                    commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>Gender is too long!"));
                    return true;
                }
                profile.setGender(finalField);
                break;
            case "pronouns":
                if(field.length() > 20){
                    commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>Pronouns are too long!"));
                    return true;
                }
                profile.setPronouns(finalField);
                break;
            case "ancestry":
                if(field.length() > 50){
                    commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>Ancestry is too long!"));
                    return true;
                }
                profile.setAncestry(finalField);
                break;
            case "description":
                profile.setDescription(finalField);
                break;
            default:
                commandSender.sendMessage("Invalid field!");
                break;
        }
        try{
            getPlugin().getDatabase().updateCharacterData(profile);
        } catch (Exception e){
            commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>Failed to update character data!"));
            return true;
        }
        commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<green>Updated " + strings[0] + " to " + finalField));
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
