package org.skysongdev.skysongstats.commands.profile;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongstats.SkysongStats;
import org.skysongdev.skysongstats.Utils.Utils;

import java.util.Arrays;
import java.util.List;

public class ProfileCommand implements TabExecutor {
    CreateProfile createProfile;
    SwitchProfile switchProfile;
    DeleteProfile deleteProfile;

    public ProfileCommand() {
        this.createProfile = new CreateProfile();
        this.switchProfile = new SwitchProfile();
        this.deleteProfile = new DeleteProfile();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        String[] newArgs = Arrays.copyOfRange(strings, 1, strings.length);
        Player player = (Player) commandSender;

        if(strings.length < 1){
            commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>Too Little arguments! (Usage: /profile (subcommand))"));
            return true;
        }

        switch(strings[0].toLowerCase()) {
            case "create":
                createProfile.onCommand(commandSender, command, s, newArgs);
                break;
            case "switch":
                switchProfile.onCommand(commandSender, command, s, newArgs);
                break;
            case "delete":
            case "del":
                deleteProfile.onCommand(commandSender, command, s, newArgs);
                break;
            case "view":
                commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<gray> Your current profile is <yellow>" + SkysongStats.getPlugin().getUtils().getProfileManager().findActiveStats(player.getUniqueId().toString()).getProfile()));
                break;
            default:
                commandSender.sendMessage(Utils.getMiniMessage().deserialize(Utils.PLUGIN_TAG + "<red>Incorrect Arguments! Usage: /profile (create/switch/delete) (profile)"));
                break;


        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        String[] newstrings = Arrays.copyOfRange(strings, 1, strings.length);
        if(strings.length == 1){
            return Arrays.asList("create", "switch", "delete", "view").stream().filter(a -> a.startsWith(strings[0])).toList();
        }
        if(strings.length > 1){
            switch(strings[0].toLowerCase()){
                case "create":
                    return createProfile.onTabComplete(commandSender, command, s, newstrings);
                case "switch":
                    return switchProfile.onTabComplete(commandSender, command, s, newstrings);
                case "delete":
                    return deleteProfile.onTabComplete(commandSender, command, s, newstrings);
            }
        }
        return null;
    }
}
