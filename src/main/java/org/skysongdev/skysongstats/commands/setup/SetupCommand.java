package org.skysongdev.skysongstats.commands.setup;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class SetupCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        Player target = (Player) commandSender;
        if(getPlugin().getUtils().getProfileManager().findActiveSetupProfile(target.getUniqueId().toString()).isSetUp()){
            getPlugin().getMessages().send(target, "setup.already-set-up");
            return true;
        }
        getPlugin().getMessages().send(target, "setup.started");
        getPlugin().getUtils().getProfileManager().findActiveSetupProfile(target.getUniqueId().toString()).getPointAllocGUI().setupInventory();
        target.openInventory(getPlugin().getUtils().getProfileManager().findActiveSetupProfile(target.getUniqueId().toString()).getPointAllocGUI().getInventory());
        return true;
    }
}
