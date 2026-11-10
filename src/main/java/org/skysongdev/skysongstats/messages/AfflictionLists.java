package org.skysongdev.skysongstats.messages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.skysongdev.skysongstats.character.ActiveCondition;
import org.skysongdev.skysongstats.character.AfflictionService;
import org.skysongdev.skysongstats.character.CharacterInjury;
import org.skysongdev.skysongstats.definition.ConditionDefinition;
import org.skysongdev.skysongstats.definition.InjuryDefinition;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

// A character's current injuries or conditions as one MiniMessage text, for the
// placeholders (%skysongstats_injuries...%) and the sheet tags ({injuries...}):
//   injuries, conditions                  names joined by placeholders.<list>.separator
//   ..._hover                             each name as its tag, the description on hover
//   ..._<number>, ..._hover_<number>      broken into lines of that many characters
//   injuries_count, conditions_count      how many
// A condition above level 1 shows its level (condition.level-format). In the
// _hover lists, a condition an injury or another condition gives is
// condition.given-tag, which names them. With none,
// the list is placeholders.<list>.none.
public final class AfflictionLists {
    private static final Pattern NAME = Pattern.compile("(injuries|conditions)(?:_(count)|(_hover)?(?:_(\\d{1,4}))?)");
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final Supplier<AfflictionService> afflictions;
    private final AfflictionTexts texts;
    private final Supplier<Messages> messages;

    // afflictions may give null while the plugin is starting.
    public AfflictionLists(Supplier<AfflictionService> afflictions, AfflictionTexts texts, Supplier<Messages> messages) {
        this.afflictions = Objects.requireNonNull(afflictions, "afflictions");
        this.texts = Objects.requireNonNull(texts, "texts");
        this.messages = Objects.requireNonNull(messages, "messages");
    }

    // Whether this is one of the names above, ignoring case.
    public static boolean isListName(String name) {
        return NAME.matcher(name.toLowerCase(Locale.ROOT)).matches();
    }

    // The text for one of the names above; empty for any other name.
    public Optional<String> resolve(ProfileKey character, String name) {
        Matcher matcher = NAME.matcher(name.toLowerCase(Locale.ROOT));
        AfflictionService service = afflictions.get();
        if (!matcher.matches() || service == null) {
            return Optional.empty();
        }
        boolean injuries = "injuries".equals(matcher.group(1));
        List<String> entries = injuries ? injuries(service, character, matcher.group(3) != null)
                : conditions(service, character, matcher.group(3) != null);
        if (matcher.group(2) != null) {
            return Optional.of(Integer.toString(entries.size()));
        }
        int width = matcher.group(4) == null ? 0 : Integer.parseInt(matcher.group(4));
        return Optional.of(list(matcher.group(1), entries, width));
    }

    private List<String> injuries(AfflictionService service, ProfileKey character, boolean hover) {
        return service.injuries(character).stream().map(CharacterInjury::injury)
                .map(injury -> hover ? MINI_MESSAGE.serialize(texts.injuryTag(injury)) : injury.name())
                .toList();
    }

    private List<String> conditions(AfflictionService service, ProfileKey character, boolean hover) {
        return service.conditions(character).stream().map(condition -> MINI_MESSAGE.serialize(hover
                ? conditionTag(service, condition)
                : texts.conditionName(condition.condition(), condition.level()))).toList();
    }

    // A condition an injury or another condition gives names them in its hover.
    private Component conditionTag(AfflictionService service, ActiveCondition condition) {
        Stream<String> injuryNames = condition.fromInjuries().stream()
                .flatMap(id -> service.injuryDefinitions().get(id).stream()).map(InjuryDefinition::name);
        Stream<String> conditionNames = condition.fromConditions().stream()
                .flatMap(id -> service.conditionDefinitions().get(id).stream()).map(ConditionDefinition::name);
        List<Component> givers = Stream.concat(injuryNames, conditionNames).map(MINI_MESSAGE::deserialize).toList();
        if (givers.isEmpty()) {
            return texts.conditionTag(condition.condition(), condition.level());
        }
        return texts.givenConditionTag(condition.condition(), condition.level(),
                Component.join(JoinConfiguration.commas(true), givers));
    }

    private String list(String kind, List<String> entries, int width) {
        String keys = "placeholders." + kind + ".";
        if (entries.isEmpty()) {
            return raw(keys + "none");
        }
        return LineWrapper.wrap(String.join(raw(keys + "separator"), entries), width);
    }

    private String raw(String key) {
        return String.join("", messages.get().rawLines(key));
    }
}
