package org.skysongdev.skysongstats.commands.downtime;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.Utils.DowntimeManager;
import org.skysongdev.skysongstats.Utils.Utils;
import org.skysongdev.skysongstats.database.CharacterDowntime;
import org.skysongdev.skysongstats.database.PlayerDowntime;

import static org.skysongdev.skysongstats.Utils.Utils.*;

public class ResetAllDowntimes implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {

        for(CharacterDowntime downtime : DowntimeManager.characterDowntimes){
            downtime.setCurrentDowntime(MAX_CHARACTER_DOWNTIME);
        }

        for(PlayerDowntime playerDowntime : DowntimeManager.playerDowntimes) {
            playerDowntime.setCurrentDowntime(Math.min(playerDowntime.getCurrentDowntime() + GLOBAL_DOWNTIME_RECOVERY, MAX_GLOBAL_DOWNTIME));
        }

        if(sender instanceof Player){
            sender.sendMessage(Utils.getMiniMessage().deserialize(PLUGIN_TAG + "<green>All downtimes have been reset successfully."));
        }

        return true;
    }
}
