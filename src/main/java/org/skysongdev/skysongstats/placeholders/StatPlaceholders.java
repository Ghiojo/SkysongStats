package org.skysongdev.skysongstats.placeholders;

import org.skysongdev.skysongstats.character.StatCalculator;
import org.skysongdev.skysongstats.character.StatService;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

// The stat placeholders, made for every stat in stats.yml (X is a stat id):
//   X                total: base + modifiers (+ item counts); a bar's current value
//   X_base           saved base value, without modifiers
//   X_modifiers      sum of the modifiers on X
//   max_X            a bar's max
//   max_X_modifiers  sum of the modifiers on a bar's max
// Kept apart from PlaceholderAPI so it can be tested without it; the expansion
// passes the text after "skysongstats_".
public final class StatPlaceholders {
    private static final String BASE = "_base";
    private static final String MODIFIERS = "_modifiers";

    private final StatService stats;

    public StatPlaceholders(StatService stats) {
        this.stats = Objects.requireNonNull(stats, "stats");
    }

    // The placeholder's value, or empty when the name is not a stat placeholder or
    // the character has no stats loaded.
    public Optional<String> resolve(ProfileKey character, String placeholder) {
        String name = placeholder.toLowerCase(Locale.ROOT);
        return stats.sheet(character)
                .map(sheet -> value(stats.calculator(sheet), name))
                .filter(OptionalInt::isPresent)
                .map(value -> Integer.toString(value.getAsInt()));
    }

    private OptionalInt value(StatCalculator calculator, String name) {
        OptionalInt total = calculator.value(name);
        if (total.isPresent()) {
            return total;
        }
        if (name.endsWith(BASE)) {
            String id = strip(name, BASE);
            return stats.definitions().get(id)
                    .map(stat -> OptionalInt.of(calculator.base(stat)))
                    .orElse(OptionalInt.empty());
        }
        if (name.endsWith(MODIFIERS)) {
            String target = strip(name, MODIFIERS);
            return stats.isModifierTarget(target) ? OptionalInt.of(calculator.modifiers(target)) : OptionalInt.empty();
        }
        return OptionalInt.empty();
    }

    private static String strip(String name, String suffix) {
        return name.substring(0, name.length() - suffix.length());
    }
}
