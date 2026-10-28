package org.skysongdev.skysongstats.commands.downtime;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class DowntimePreference implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if(sender instanceof org.bukkit.entity.Player player){
            boolean currentPreference;
            if(org.skysongdev.skysongstats.Utils.ProfileManager.downtimePreferences.get(player.getUniqueId().toString()) != null)
                currentPreference = org.skysongdev.skysongstats.Utils.ProfileManager.downtimePreferences.get(player.getUniqueId().toString());
            else
                currentPreference = true;
            org.skysongdev.skysongstats.Utils.ProfileManager.downtimePreferences.put(player.getUniqueId().toString(), !currentPreference);
            String newPreferenceString = !currentPreference ? "Global" : "Character";
            player.sendMessage(org.skysongdev.skysongstats.Utils.Utils.getMiniMessage().deserialize(org.skysongdev.skysongstats.Utils.Utils.PLUGIN_TAG + "<green>Your downtime spending preference has been set to <gold>" + newPreferenceString + "<green>."));
        }

        return true;
    }
}
