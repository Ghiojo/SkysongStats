package org.skysongdev.skysongstats.placeholders;

import org.skysongdev.skysongstats.character.StatCalculator;
import org.skysongdev.skysongstats.character.StatService;
import org.skysongdev.skysongstats.definition.StatDefinition;
import org.skysongdev.skysongstats.height.HeightText;
import org.skysongdev.skysongstats.storage.ProfileKey;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

// The stat placeholders, made for every stat in stats.yml (X is a stat id):
//   X                total: base + modifiers (+ item counts); a bar's current value
//   X_base           saved base value, without modifiers
//   X_modifiers      sum of the modifiers on X
//   max_X            a bar's max (with the modifiers on max_X)
//   max_X_base       a bar's max before its modifiers: the max formula alone
//   max_X_modifiers  sum of the modifiers on a bar's max
// The height (the stat with a "scale:" block) shows X and X_base in metres
// ("1.75") instead of centimetres, and also has:
//   X_feet           feet and inches, with modifiers ("5'9\"")
//   X_feet_base      feet and inches, before modifiers
//   X_scale          the Minecraft scale the height gives
// X_modifiers stays in centimetres, the unit modifiers are written in.
// Kept apart from PlaceholderAPI so it can be tested without it; the expansion
// passes the text after "skysongstats_".
public final class StatPlaceholders {
    private static final String BASE = "_base";
    private static final String MODIFIERS = "_modifiers";
    private static final String FEET = "_feet";
    private static final String SCALE = "_scale";

    private final StatService stats;

    public StatPlaceholders(StatService stats) {
        this.stats = Objects.requireNonNull(stats, "stats");
    }

    // The placeholder's value, or empty when the name is not a stat placeholder or
    // the character has no stats loaded.
    public Optional<String> resolve(ProfileKey character, String placeholder) {
        String name = placeholder.toLowerCase(Locale.ROOT);
        return stats.sheet(character).flatMap(sheet -> {
            StatCalculator calculator = stats.calculator(sheet);
            Optional<String> height = stats.definitions().height().flatMap(stat -> height(calculator, stat, name));
            if (height.isPresent()) {
                return height;
            }
            OptionalInt value = value(calculator, name);
            return value.isPresent() ? Optional.of(Integer.toString(value.getAsInt())) : Optional.empty();
        });
    }

    private static Optional<String> height(StatCalculator calculator, StatDefinition stat, String name) {
        String id = stat.id();
        if (name.equals(id)) {
            return Optional.of(HeightText.metres(calculator.total(stat)));
        }
        if (name.equals(id + BASE)) {
            return Optional.of(HeightText.metres(calculator.base(stat)));
        }
        if (name.equals(id + FEET)) {
            return Optional.of(HeightText.feet(calculator.total(stat)));
        }
        if (name.equals(id + FEET + BASE)) {
            return Optional.of(HeightText.feet(calculator.base(stat)));
        }
        if (name.equals(id + SCALE)) {
            return Optional.of(HeightText.scale(stat.scale().scaleFor(calculator.total(stat))));
        }
        return Optional.empty();
    }

    private OptionalInt value(StatCalculator calculator, String name) {
        OptionalInt total = calculator.value(name);
        if (total.isPresent()) {
            return total;
        }
        if (name.endsWith(BASE)) {
            String id = strip(name, BASE);
            OptionalInt maxBase = stats.definitions().findBarByMaxId(id)
                    .map(bar -> OptionalInt.of(calculator.maxBase(bar)))
                    .orElse(OptionalInt.empty());
            if (maxBase.isPresent()) {
                return maxBase;
            }
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
