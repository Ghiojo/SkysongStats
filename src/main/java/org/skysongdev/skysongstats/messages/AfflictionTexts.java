package org.skysongdev.skysongstats.messages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.skysongdev.skysongstats.definition.ConditionDefinition;
import org.skysongdev.skysongstats.definition.InjuryDefinition;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

// How injuries and conditions look in text, so chat, messages, menus and
// placeholders all show them the same way:
//   injury tag      injury.tag in messages.yml: the name with the description on
//                   hover, what [injury_x] shows in chat
//   condition tag   condition.tag: the same for a condition at a level, its
//                   description's $ shown as the level
//   announcement    an injury's message: with {character} and {injury} filled in
// Owner-written texts (names, descriptions, messages) are MiniMessage. A
// character's name keeps every format its player gave it, as on the sheets.
public final class AfflictionTexts {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final String LEVEL_MARK = "$";

    private final Supplier<Messages> messages;
    private final Function<ProfileKey, String> characterName;

    // characterName gives the name on a character's sheet, or the player's name
    // when the sheet has none.
    public AfflictionTexts(Supplier<Messages> messages, Function<ProfileKey, String> characterName) {
        this.messages = Objects.requireNonNull(messages, "messages");
        this.characterName = Objects.requireNonNull(characterName, "characterName");
    }

    public Component injuryTag(InjuryDefinition injury) {
        return messages.get().get("injury.tag", Map.of(), Map.of(
                "name", MINI_MESSAGE.deserialize(injury.name()),
                "description", MINI_MESSAGE.deserialize(injury.description())));
    }

    public Component conditionTag(ConditionDefinition condition, int level) {
        return messages.get().get("condition.tag", Map.of(), Map.of(
                "name", conditionName(condition, level),
                "description", conditionDescription(condition, level)));
    }

    // The condition's name, with its level after it when above 1 (condition.level-format).
    public Component conditionName(ConditionDefinition condition, int level) {
        Component name = MINI_MESSAGE.deserialize(condition.name());
        if (level <= 1) {
            return name;
        }
        return messages.get().get("condition.level-format", Map.of("level", Integer.toString(level)),
                Map.of("name", name));
    }

    // The description with each $ shown as the level (condition.level-in-description).
    public Component conditionDescription(ConditionDefinition condition, int level) {
        String levelText = MINI_MESSAGE.serialize(messages.get().get("condition.level-in-description",
                Map.of("level", Integer.toString(level))));
        return MINI_MESSAGE.deserialize(condition.description().replace(LEVEL_MARK, levelText));
    }

    // The injury's message for this character, or empty when the injury has none.
    public Optional<Component> announcement(ProfileKey character, InjuryDefinition injury) {
        if (injury.message().isBlank()) {
            return Optional.empty();
        }
        String template = injury.message().replace("{character}", "<character>").replace("{injury}", "<injury>");
        return Optional.of(MINI_MESSAGE.deserialize(template,
                Placeholder.component("character", Messages.playerStyledText(characterName.apply(character))),
                Placeholder.component("injury", injuryTag(injury))));
    }

    // A text as a MiniMessage string, for other plugins that work with strings.
    public static String miniMessage(Component text) {
        return MINI_MESSAGE.serialize(text);
    }
}
