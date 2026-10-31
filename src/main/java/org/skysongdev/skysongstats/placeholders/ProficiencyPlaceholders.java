package org.skysongdev.skysongstats.placeholders;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.skysongdev.skysongstats.character.ProficiencyFocus;
import org.skysongdev.skysongstats.character.ProficiencyService;
import org.skysongdev.skysongstats.definition.ProficiencyDefinition;
import org.skysongdev.skysongstats.definition.ProficiencyTier;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

// The proficiency placeholders (X is a proficiency id from proficiencies.yml):
//   proficiency_X               the tier's name in the tier's colour ("Adept")
//   proficiency_X_tier          the tier's id ("adept"), for conditions in other plugins
//   proficiency_focus           the focused proficiency's name, empty without a focus
//   proficiency_focus_points    the focus's points, 0 without a focus
//   proficiency_focus_cost      the points the focus needs for its next tier, 0 without a focus
// Colours are written as § codes, which every PlaceholderAPI user reads. Kept
// apart from PlaceholderAPI so it can be tested without it; the expansion passes
// the text after "skysongstats_".
public final class ProficiencyPlaceholders {
    private static final String PREFIX = "proficiency_";
    private static final String TIER = "_tier";
    private static final String FOCUS = "focus";
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final ProficiencyService proficiencies;

    public ProficiencyPlaceholders(ProficiencyService proficiencies) {
        this.proficiencies = Objects.requireNonNull(proficiencies, "proficiencies");
    }

    // The placeholder's value, or empty when the name is not a proficiency
    // placeholder or the character has no proficiencies loaded.
    public Optional<String> resolve(ProfileKey character, String placeholder) {
        String name = placeholder.toLowerCase(Locale.ROOT);
        if (!name.startsWith(PREFIX) || !proficiencies.hasCharacter(character)) {
            return Optional.empty();
        }
        String rest = name.substring(PREFIX.length());
        return focusValue(character, rest).or(() -> tierValue(character, rest));
    }

    // The validator keeps "focus" from being a proficiency id, so these never hide one.
    private Optional<String> focusValue(ProfileKey character, String name) {
        Optional<ProficiencyFocus> focus = proficiencies.focus(character);
        return switch (name) {
            case FOCUS -> Optional.of(focus.map(current -> current.proficiency().name()).orElse(""));
            case FOCUS + "_points" -> Optional.of(Integer.toString(focus.map(ProficiencyFocus::points).orElse(0)));
            case FOCUS + "_cost" -> Optional.of(Integer.toString(
                    focus.map(current -> proficiencies.pointsNeeded(character, current)).orElse(0)));
            default -> Optional.empty();
        };
    }

    // An id that itself ends in _tier is matched as an id first.
    private Optional<String> tierValue(ProfileKey character, String name) {
        Optional<ProficiencyDefinition> exact = proficiencies.config().proficiencies().get(name);
        if (exact.isPresent()) {
            return exact.map(proficiency -> colored(proficiencies.tier(character, proficiency)));
        }
        if (!name.endsWith(TIER)) {
            return Optional.empty();
        }
        return proficiencies.config().proficiencies().get(name.substring(0, name.length() - TIER.length()))
                .map(proficiency -> proficiencies.tier(character, proficiency).id());
    }

    private static String colored(ProficiencyTier tier) {
        return LegacyComponentSerializer.legacySection().serialize(
                MINI_MESSAGE.deserialize(tier.color() + "<text>", Placeholder.unparsed("text", tier.name())));
    }
}
