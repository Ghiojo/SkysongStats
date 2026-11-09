package org.skysongdev.skysongstats.apiimpl;

import net.kyori.adventure.text.Component;
import org.skysongdev.skysongstats.api.SheetLayout;
import org.skysongdev.skysongstats.character.ProficiencyService;
import org.skysongdev.skysongstats.character.StatService;
import org.skysongdev.skysongstats.commands.character.CharacterSheetLines;
import org.skysongdev.skysongstats.commands.proficiency.ProficiencySheet;
import org.skysongdev.skysongstats.commands.stat.StatSheet;
import org.skysongdev.skysongstats.database.CharacterProfile;
import org.skysongdev.skysongstats.definition.SkillsConfig;
import org.skysongdev.skysongstats.messages.Messages;
import org.skysongdev.skysongstats.messages.SheetPart;
import org.skysongdev.skysongstats.messages.SheetText;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

// The messages.yml sheets rendered for any character, through the same classes the
// commands use, about the character instead of a player: the sheet tags are the
// character's, and PlaceholderAPI is left out.
final class SheetRendering implements SheetRenderer {
    private final StatService stats;
    private final Supplier<ProficiencyService> proficiencies;
    private final Supplier<SkillsConfig> skills;
    private final Supplier<Messages> messages;
    // (player UUID as text, profile name) -> the loaded sheet fields, or null.
    private final BiFunction<String, String, CharacterProfile> profiles;
    private final StatSheet statSheet;
    private final ProficiencySheet proficiencySheet;
    private final CharacterSheetLines characterSheet;

    // Each one the plugin has, looked up on every use since /ssreload replaces some.
    record Sources(StatService stats, Supplier<ProficiencyService> proficiencies, Supplier<SkillsConfig> skills,
                   Supplier<Messages> messages, BiFunction<String, String, CharacterProfile> profiles) {
    }

    SheetRendering(Sources sources, StatSheet statSheet, ProficiencySheet proficiencySheet) {
        this.stats = Objects.requireNonNull(sources.stats(), "stats");
        this.proficiencies = Objects.requireNonNull(sources.proficiencies(), "proficiencies");
        this.skills = Objects.requireNonNull(sources.skills(), "skills");
        this.messages = Objects.requireNonNull(sources.messages(), "messages");
        this.profiles = Objects.requireNonNull(sources.profiles(), "profiles");
        this.statSheet = Objects.requireNonNull(statSheet, "statSheet");
        this.proficiencySheet = Objects.requireNonNull(proficiencySheet, "proficiencySheet");
        this.characterSheet = new CharacterSheetLines(stats);
    }

    // Empty for a character without stats: one that doesn't exist.
    @Override
    public Optional<List<Component>> render(ProfileKey character, SheetLayout layout, String name) {
        if (stats.sheet(character).isEmpty()) {
            return Optional.empty();
        }
        SheetText text = messages.get().about(character);
        if (layout == SheetLayout.STATS) {
            return Optional.of(lines(text, statSheet.parts(character, name)));
        }
        if (layout == SheetLayout.CHARACTER) {
            return Optional.of(lines(text, characterSheet.parts(profile(character), name)));
        }
        return Optional.of(proficiencySheet.lines(text, name, proficiencies.get(), skills.get(), character));
    }

    // Every placeholder any of the sheets has, for a plugin's own lines.
    @Override
    public Optional<List<Component>> render(ProfileKey character, List<String> lines, String name) {
        if (stats.sheet(character).isEmpty()) {
            return Optional.empty();
        }
        SheetText text = messages.get().about(character);
        CharacterProfile profile = profile(character);
        Map<String, String> placeholders = new HashMap<>(statSheet.placeholders(character, name));
        placeholders.putAll(characterSheet.placeholders(profile, name));
        Map<String, Component> components = new HashMap<>(characterSheet.fields(profile));
        components.put("description", Messages.playerStyledText(profile.getDescription()));
        components.putAll(proficiencySheet.components(text, proficiencies.get(), skills.get(), character));
        return Optional.of(text.renderLines(lines, placeholders, components));
    }

    private static List<Component> lines(SheetText text, List<SheetPart> parts) {
        return parts.stream()
                .flatMap(part -> text.getLines(part.key(), part.placeholders(), part.components()).stream())
                .toList();
    }

    // A character whose sheet fields weren't saved (the old profile table was down
    // when it was made) shows them as a new profile does.
    private CharacterProfile profile(ProfileKey character) {
        CharacterProfile profile = profiles.apply(character.player().toString(), character.profile());
        return profile != null ? profile : new CharacterProfile(character.player().toString(), character.profile());
    }
}
