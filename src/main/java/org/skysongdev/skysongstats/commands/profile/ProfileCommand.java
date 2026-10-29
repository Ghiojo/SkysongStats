package org.skysongdev.skysongstats.commands.profile;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

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
        if(strings.length < 1){
            getPlugin().getMessages().send(commandSender, "profile.usage");
            return true;
        }
        // Profiles belong to players: the console has none to create, switch or delete.
        if(!(commandSender instanceof Player player)){
            getPlugin().getMessages().send(commandSender, "general.players-only");
            return true;
        }
        String[] newArgs = Arrays.copyOfRange(strings, 1, strings.length);

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
                getPlugin().getMessages().send(commandSender, "profile.current",
                        Map.of("profile", getPlugin().getUtils().getProfileManager().getActiveProfileName(player.getUniqueId().toString())));
                break;
            default:
                getPlugin().getMessages().send(commandSender, "profile.invalid-subcommand");
                break;


        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if(!(commandSender instanceof Player)){
            return List.of();
        }
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
