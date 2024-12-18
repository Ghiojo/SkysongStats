package org.skysongdev.skysongstats.commands.skills;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.Utils.Utils;

import java.util.Arrays;

public class SkillsCommand implements CommandExecutor {
    AddSkillXP addSkillXP;
    SetSkillXP setSkillXP;

    public SkillsCommand(){
        this.addSkillXP = new AddSkillXP();
        this.setSkillXP = new SetSkillXP();
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

        }

        return true;
    }
}
