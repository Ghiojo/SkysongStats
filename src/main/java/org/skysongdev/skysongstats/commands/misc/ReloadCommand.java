package org.skysongdev.skysongstats.commands.misc;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class ReloadCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if(!commandSender.hasPermission("skysongstats.reload")){
            getPlugin().getMessages().send(commandSender, "general.no-permission");
            return true;
        }
        getPlugin().reloadSettings();
        getPlugin().getMessages().send(commandSender, "general.reloaded");
        return true;
    }
}
