package org.skysongdev.skysongstats.commands.character;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skysongdev.skysongstats.database.CharacterProfile;
import org.skysongdev.skysongstats.messages.Messages;
import org.skysongdev.skysongstats.messages.SheetPart;

import java.util.ArrayList;
import java.util.List;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class ViewCharacter implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        Player target;
        if(strings.length < 1){
            if(commandSender instanceof Player){
                target = (Player) commandSender;
            } else {
                getPlugin().getMessages().send(commandSender, "character.view.needs-player");
                return true;
            }
        }else {
            target = commandSender.getServer().getPlayer(strings[0]);
            if (target == null) {
                getPlugin().getMessages().send(commandSender, "character.view.player-not-online");
                return true;
            }
        }
        CharacterProfile profile = getPlugin().getUtils().getCharacterManager().findActiveCharacter(target.getUniqueId().toString());
        showSheet(commandSender, target, profile);
        return true;
    }

    // Other plugins' %placeholders% in the sheet's lines are filled in for its player.
    private static void showSheet(CommandSender viewer, Player player, CharacterProfile profile) {
        Messages messages = getPlugin().getMessages();
        for (SheetPart part : new CharacterSheetLines(getPlugin().getStatService()).parts(profile, player.getName())) {
            messages.send(viewer, part.key(), part.placeholders(), part.components(), player);
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if(strings.length == 1){
            List<String> players = new ArrayList<String>();
            for(Player player : Bukkit.getOnlinePlayers()){
                players.add(player.getName());
            }
            return players;
        }
        return null;
    }
}
