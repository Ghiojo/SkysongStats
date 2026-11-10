package org.skysongdev.skysongstats.messages;

import org.skysongdev.skysongstats.character.AncestryService;
import org.skysongdev.skysongstats.definition.AncestryDefinition;
import org.skysongdev.skysongstats.definition.StatDefinition;
import org.skysongdev.skysongstats.definition.StatDefinitions;
import org.skysongdev.skysongstats.storage.ProfileKey;
import org.skysongdev.skysongstats.storage.StoredAncestry;
import org.skysongdev.skysongstats.storage.StoredAncestryAdjustment;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

// A character's ancestry as MiniMessage text, for the placeholders
// (%skysongstats_ancestry...%) and the sheet tags ({ancestry_...}):
//   ancestry               its name; empty when it has none (placeholder only, since
//                          {ancestry} on the character sheet is the ancestry field)
//   ancestry_name          its name, or placeholders.ancestry.none
//   ancestry_id            its id from ancestries.yml; empty when it has none
//   ancestry_pending       true while its choices are still to be made, else false
//   ancestry_adjustments   what it gave, "+1 Constitution, -1 Speed", followed by
//                          placeholders.ancestry.pending while choices are pending
//   ancestry_<stat>        what it gave that stat (id or alias) in all, 0 for nothing
// An ancestry that left ancestries.yml is named by its id.
public final class AncestryPlaceholders {
    private static final String PREFIX = "ancestry";
    private static final String KEYS = "placeholders.ancestry.";

    private final Supplier<AncestryService> ancestries;
    private final Supplier<StatDefinitions> stats;
    private final Supplier<Messages> messages;

    // ancestries and stats may give null while the plugin is starting.
    public AncestryPlaceholders(Supplier<AncestryService> ancestries, Supplier<StatDefinitions> stats,
                                Supplier<Messages> messages) {
        this.ancestries = Objects.requireNonNull(ancestries, "ancestries");
        this.stats = Objects.requireNonNull(stats, "stats");
        this.messages = Objects.requireNonNull(messages, "messages");
    }

    // Whether this could be one of the names above (any stat name), ignoring case.
    public static boolean isAncestryName(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return PREFIX.equals(lower) || lower.startsWith(PREFIX + "_");
    }

    // The text for one of the names above; empty for any other name, and for
    // ancestry_<stat> of a stat stats.yml doesn't have.
    public Optional<String> resolve(ProfileKey character, String name) {
        AncestryService service = ancestries.get();
        StatDefinitions definitions = stats.get();
        String lower = name.toLowerCase(Locale.ROOT);
        if (service == null || definitions == null || !isAncestryName(lower)) {
            return Optional.empty();
        }
        Optional<StoredAncestry> saved = service.of(character);
        return switch (lower) {
            case PREFIX -> Optional.of(saved.map(ancestry -> name(service, ancestry)).orElse(""));
            case PREFIX + "_name" -> Optional.of(saved.map(ancestry -> name(service, ancestry)).orElse(raw("none")));
            case PREFIX + "_id" -> Optional.of(saved.map(StoredAncestry::ancestry).orElse(""));
            case PREFIX + "_pending" -> Optional.of(Boolean.toString(
                    saved.filter(StoredAncestry::choicesPending).isPresent()));
            case PREFIX + "_adjustments" -> Optional.of(adjustments(definitions, saved));
            default -> definitions.get(lower.substring(PREFIX.length() + 1))
                    .map(stat -> Integer.toString(total(definitions, saved, stat)));
        };
    }

    private static String name(AncestryService service, StoredAncestry saved) {
        return service.definitions().get(saved.ancestry()).map(AncestryDefinition::name).orElse(saved.ancestry());
    }

    private String adjustments(StatDefinitions definitions, Optional<StoredAncestry> saved) {
        String given = saved.map(StoredAncestry::adjustments).orElse(List.of()).stream()
                .map(adjustment -> adjustment(definitions, adjustment))
                .collect(Collectors.joining(raw("separator")));
        String text = given.isEmpty() ? raw("no-adjustments") : given;
        return saved.filter(StoredAncestry::choicesPending).isPresent() ? text + raw("pending") : text;
    }

    private String adjustment(StatDefinitions definitions, StoredAncestryAdjustment adjustment) {
        String stat = definitions.get(adjustment.stat()).map(found -> found.display().name()).orElse(adjustment.stat());
        String amount = adjustment.amount() > 0 ? "+" + adjustment.amount() : Integer.toString(adjustment.amount());
        return raw("adjustment").replace("{amount}", amount).replace("{stat}", stat);
    }

    private static int total(StatDefinitions definitions, Optional<StoredAncestry> saved, StatDefinition stat) {
        return saved.map(StoredAncestry::adjustments).orElse(List.of()).stream()
                .filter(adjustment -> definitions.get(adjustment.stat()).filter(stat::equals).isPresent())
                .mapToInt(StoredAncestryAdjustment::amount).sum();
    }

    private String raw(String key) {
        return String.join("", messages.get().rawLines(KEYS + key));
    }
}
