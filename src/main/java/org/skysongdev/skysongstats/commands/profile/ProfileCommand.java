package org.skysongdev.skysongstats.commands.profile;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.skysongdev.skysongstats.SkysongStats;

import java.util.Arrays;

public class ProfileCommand implements CommandExecutor {
    SkysongStats plugin;
    CreateProfile createProfile;
    SwitchProfile switchProfile;
    DeleteProfile deleteProfile;

    public ProfileCommand(SkysongStats plugin) {
        this.plugin = plugin;
        this.createProfile = new CreateProfile();
        this.switchProfile = new SwitchProfile();
        this.deleteProfile = new DeleteProfile();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        String[] newArgs = Arrays.copyOfRange(strings, 1, strings.length);
        Player player = (Player) commandSender;

        if(strings.length < 1){
            commandSender.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "SkysongStats" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY + "Too Little arguments! (Usage: /profile (subcommand))");
            return true;
        }

        switch(strings[0].toLowerCase()) {
            case "create":
                if(commandSender.hasPermission("skysongstats.profile.create")){
                    createProfile.onCommand(commandSender, command, s, newArgs);
                } else
                    commandSender.sendMessage(ChatColor.of("DARK_GRAY") + "[" + ChatColor.of("GOLD") + "SkysongStats" + ChatColor.of("DARK_GRAY") + "]" +ChatColor.of("RED") + "You don't have permissions to run that command!");
                break;
            case "switch":
                if(commandSender.hasPermission("skysongstats.profile.switch")){
                    switchProfile.onCommand(commandSender, command, s, newArgs);
                } else
                    commandSender.sendMessage(ChatColor.of("DARK_GRAY") + "[" + ChatColor.of("GOLD") + "SkysongStats" + ChatColor.of("DARK_GRAY") + "]" +ChatColor.of("RED") + "You don't have permissions to run that command!");
                break;
            case "view":
                //TODO: View Profile

        }
        return true;
    }
}
