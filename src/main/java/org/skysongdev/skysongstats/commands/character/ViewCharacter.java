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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.skysongdev.skysongstats.SkysongStats.getPlugin;

public class ViewCharacter implements TabExecutor {
    private static final int DESCRIPTION_LINE_LENGTH = 60;

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
        showSheet(commandSender, target.getName(), profile);
        return true;
    }

    private static void showSheet(CommandSender viewer, String playerName, CharacterProfile profile) {
        Messages messages = getPlugin().getMessages();
        messages.send(viewer, "character.view.header", Map.of("player", playerName), Map.of(
                "name", Messages.playerStyledText(profile.getName()),
                "age", Messages.playerStyledText(profile.getAge()),
                "gender", Messages.playerStyledText(profile.getGender()),
                "pronouns", Messages.playerStyledText(profile.getPronouns()),
                "ancestry", Messages.playerStyledText(profile.getAncestry())));
        for(String line : wrapDescription(profile.getDescription())){
            messages.send(viewer, "character.view.description-line", Map.of(), Map.of("line", Messages.playerStyledText(line)));
        }
        messages.send(viewer, "character.view.footer");
    }

    /** Splits the description into lines of about 60 characters, between words. */
    static List<String> wrapDescription(String description) {
        List<String> lines = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        for(String word : description.split(" ")){
            if(buffer.length() + word.length() > DESCRIPTION_LINE_LENGTH){
                lines.add(buffer.toString());
                buffer = new StringBuilder();
            }
            buffer.append(word).append(" ");
        }
        lines.add(buffer.toString());
        return lines;
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
