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

// The {ancestry_...} sheet tags (see AncestryPlaceholders), for the character a
// sheet is about (for a player, their active character). {ancestry} itself is not
// one: on the character sheet it is the ancestry field. A player with no active
// character gets none, so the tag shows as written.
public final class AncestrySheetTags implements SheetTags {
    private static final Pattern TAG = Pattern.compile("\\{(ancestry_[a-z0-9_]+)}");
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final AncestryPlaceholders ancestries;
    private final Function<OfflinePlayer, Optional<ProfileKey>> activeCharacter;

    public AncestrySheetTags(AncestryPlaceholders ancestries,
                             Function<OfflinePlayer, Optional<ProfileKey>> activeCharacter) {
        this.ancestries = Objects.requireNonNull(ancestries, "ancestries");
        this.activeCharacter = Objects.requireNonNull(activeCharacter, "activeCharacter");
    }

    @Override
    public Map<String, Component> tags(OfflinePlayer player, String line) {
        return activeCharacter.apply(player).map(character -> tags(character, line)).orElse(Map.of());
    }

    @Override
    public Map<String, Component> tags(ProfileKey character, String line) {
        Map<String, Component> tags = new HashMap<>();
        Matcher matcher = TAG.matcher(line);
        while (matcher.find()) {
            String name = matcher.group(1);
            if (!tags.containsKey(name)) {
                ancestries.resolve(character, name).ifPresent(text -> tags.put(name, MINI_MESSAGE.deserialize(text)));
            }
        }
        return tags;
    }
}
