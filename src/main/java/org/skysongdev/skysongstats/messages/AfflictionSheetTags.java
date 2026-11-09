package org.skysongdev.skysongstats.messages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.OfflinePlayer;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// The {injuries...} and {conditions...} sheet tags (see AfflictionLists), for
// the active character of the player a sheet is about. A player with no active
// character gets none, so the tag shows as written.
public final class AfflictionSheetTags implements SheetTags {
    private static final Pattern TAG = Pattern.compile("\\{((?:injuries|conditions)[a-z0-9_]*)}");
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final AfflictionLists lists;
    private final Function<OfflinePlayer, Optional<ProfileKey>> activeCharacter;

    public AfflictionSheetTags(AfflictionLists lists, Function<OfflinePlayer, Optional<ProfileKey>> activeCharacter) {
        this.lists = Objects.requireNonNull(lists, "lists");
        this.activeCharacter = Objects.requireNonNull(activeCharacter, "activeCharacter");
    }

    @Override
    public Map<String, Component> tags(OfflinePlayer player, String line) {
        Map<String, Component> tags = new HashMap<>();
        Matcher matcher = TAG.matcher(line);
        while (matcher.find()) {
            String name = matcher.group(1);
            if (AfflictionLists.isListName(name) && !tags.containsKey(name)) {
                activeCharacter.apply(player).flatMap(character -> lists.resolve(character, name))
                        .ifPresent(text -> tags.put(name, MINI_MESSAGE.deserialize(text)));
            }
        }
        return tags;
    }
}
