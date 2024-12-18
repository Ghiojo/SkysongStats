package org.skysongdev.skysongstats.commands.setup;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.Utils.Utils;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class SetupCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        Player target = (Player) commandSender;
        if(getPlugin().getUtils().getProfileManager().findActiveSetupProfile(target.getUniqueId().toString()).isSetUp()){
            target.sendMessage(getPlugin().getUtils().getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>You have already set up your profile!"));
            return true;
        }
        target.sendMessage(getPlugin().getUtils().getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>You are now setting up your profile!"));
        target.openInventory(getPlugin().getUtils().getProfileManager().findActiveSetupProfile(target.getUniqueId().toString()).getStatSetupGUI().getInventory());
        return true;
    }
}
