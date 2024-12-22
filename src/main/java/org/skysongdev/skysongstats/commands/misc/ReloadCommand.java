package org.skysongdev.skysongstats.commands.misc;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.Utils.Utils;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class ReloadCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if(!commandSender.hasPermission("skysongstats.reload")){
            if(commandSender instanceof org.bukkit.entity.Player)
                commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>You don't have permission to use this command!"));
            return true;
        }
        getPlugin().reloadConfig();
        if(commandSender instanceof org.bukkit.entity.Player) {
            commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<gray>Config Reloaded!"));
        }else {
            getPlugin().getLogger().info("Config reloaded!");
        }
        return true;
    }
}
