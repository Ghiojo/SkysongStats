package org.skysongdev.skysongstats.commands.character;

import net.kyori.adventure.text.Component;
import org.skysongdev.skysongstats.character.StatService;
import org.skysongdev.skysongstats.database.CharacterProfile;
import org.skysongdev.skysongstats.definition.StatDefinition;
import org.skysongdev.skysongstats.messages.Messages;
import org.skysongdev.skysongstats.messages.SheetPart;
import org.skysongdev.skysongstats.placeholders.StatPlaceholders;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

// The character sheet /char view shows: character.view.header with the sheet
// fields, the description wrapped into character.view.description-line lines of
// about 60 characters, then character.view.footer. {player} is the name given;
// {height} and {height_feet} come from the stats.yml stat with a "scale:" block.
public final class CharacterSheetLines {
    static final String HEADER = "character.view.header";
    static final String DESCRIPTION_LINE = "character.view.description-line";
    static final String FOOTER = "character.view.footer";
    private static final int DESCRIPTION_LINE_LENGTH = 60;

    private final StatService stats;

    public CharacterSheetLines(StatService stats) {
        this.stats = Objects.requireNonNull(stats, "stats");
    }

    // The sheet of the profile's character.
    public List<SheetPart> parts(CharacterProfile profile, String name) {
        List<SheetPart> parts = new ArrayList<>();
        parts.add(new SheetPart(HEADER, placeholders(profile, name), fields(profile)));
        for (String line : wrapDescription(profile.getDescription())) {
            parts.add(new SheetPart(DESCRIPTION_LINE, Map.of(), Map.of("line", Messages.playerStyledText(line))));
        }
        parts.add(new SheetPart(FOOTER, Map.of()));
        return parts;
    }

    // {player} (set to name), {height} and {height_feet}.
    public Map<String, String> placeholders(CharacterProfile profile, String name) {
        Map<String, String> placeholders = new HashMap<>(height(
                new ProfileKey(UUID.fromString(profile.getUuid()), profile.getProfile())));
        placeholders.put("player", name);
        return placeholders;
    }

    // The sheet fields the header shows, as players styled them.
    public Map<String, Component> fields(CharacterProfile profile) {
        return Map.of(
                "name", Messages.playerStyledText(profile.getName()),
                "age", Messages.playerStyledText(profile.getAge()),
                "gender", Messages.playerStyledText(profile.getGender()),
                "pronouns", Messages.playerStyledText(profile.getPronouns()),
                "ancestry", Messages.playerStyledText(profile.getAncestry()));
    }

    // {height} in metres and {height_feet}; empty when there is no height stat or
    // the character has no stats.
    private Map<String, String> height(ProfileKey character) {
        Optional<StatDefinition> height = stats.definitions().height();
        StatPlaceholders placeholders = new StatPlaceholders(stats);
        String metres = height.flatMap(stat -> placeholders.resolve(character, stat.id())).orElse("");
        String feet = height.flatMap(stat -> placeholders.resolve(character, stat.id() + "_feet")).orElse("");
        return Map.of("height", metres, "height_feet", feet);
    }

    /** Splits the description into lines of about 60 characters, between words. */
    static List<String> wrapDescription(String description) {
        List<String> lines = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        for (String word : description.split(" ")) {
            if (buffer.length() + word.length() > DESCRIPTION_LINE_LENGTH) {
                lines.add(buffer.toString());
                buffer = new StringBuilder();
            }
            buffer.append(word).append(" ");
        }
        lines.add(buffer.toString());
        return lines;
    }
}
