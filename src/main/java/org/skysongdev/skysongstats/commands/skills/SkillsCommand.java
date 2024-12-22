package org.skysongdev.skysongstats.commands.skills;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongstats.Utils.Utils;

import java.util.Arrays;
import java.util.List;

public class SkillsCommand implements TabExecutor {
    AddSkillXP addSkillXP;
    SetSkillXP setSkillXP;
    ViewSkills viewSkills;

    public SkillsCommand(){
        this.addSkillXP = new AddSkillXP();
        this.setSkillXP = new SetSkillXP();
        this.viewSkills = new ViewSkills();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {


        String[] newArgs = Arrays.copyOfRange(strings, 1, strings.length);
        if(strings.length < 1){
            commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>Too Little arguments! (Usage: /skill (subcommand))"));
            return true;
        }

        switch(strings[0].toLowerCase()) {
            case "addxp":
                addSkillXP.onCommand(commandSender, command, s, newArgs);
                break;
            case "setxp":
                setSkillXP.onCommand(commandSender, command, s, newArgs);
                break;
            case "view":
                viewSkills.onCommand(commandSender, command, s, newArgs);
                break;
            default:
                commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>Incorrect Arguments! Usage: /skill (addxp/setxp/view) (skill) (value)"));
                break;
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if(strings.length == 1){
            return Arrays.asList("addxp", "setxp", "view").stream().filter(a -> a.startsWith(strings[0])).toList();
        }
        if(strings.length > 1){
            switch(strings[0].toLowerCase()){
                case "addxp":
                    return addSkillXP.onTabComplete(commandSender, command, s, strings);
                case "setxp":
                    return setSkillXP.onTabComplete(commandSender, command, s, strings);
                case "view":
                    return viewSkills.onTabComplete(commandSender, command, s, strings);
            }
        }
        return List.of("");
    }
}
