package org.skysongdev.skysongstats.commands.misc;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class ReloadCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if(!commandSender.hasPermission("skysongstats.reload")){
            getPlugin().getMessages().send(commandSender, "general.no-permission");
            return true;
        }
        List<String> notReloaded = getPlugin().reloadSettings();
        getPlugin().getMessages().send(commandSender, "general.reloaded");
        notReloaded.forEach(file -> getPlugin().getMessages().send(commandSender, "general.not-reloaded", Map.of("file", file)));
        return true;
    }
}
