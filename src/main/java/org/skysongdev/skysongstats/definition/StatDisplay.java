package org.skysongdev.skysongstats.definition;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

// How a stat is named: display name, MiniMessage colour, the other names
// commands accept for it (lowercase), and its short name ("STR"). customShortName
// is true when stats.yml sets short-name; otherwise the short name is the name.
// Only a custom short name is also accepted by commands, see shortNameInput().
public record StatDisplay(String name, String color, List<String> aliases, String shortName,
                          boolean customShortName) {
    public StatDisplay {
        aliases = List.copyOf(aliases);
    }

    // A stat without a short-name: its short name is its name.
    public StatDisplay(String name, String color, List<String> aliases) {
        this(name, color, aliases, name, false);
    }

    // The word commands accept for the short name ("str" for "STR"), lowercase.
    // Empty when stats.yml doesn't set one, or when it has a space or starts with
    // "max_" and so can't be typed as one stat name.
    public Optional<String> shortNameInput() {
        return customShortName ? inputOf(shortName) : Optional.empty();
    }

    // The same display with another short name, which commands then accept too.
    public StatDisplay withShortName(String newShortName) {
        return new StatDisplay(name, color, aliases, newShortName, true);
    }

    // The word commands would accept for this short name, if any.
    public static Optional<String> inputOf(String shortName) {
        String input = shortName.trim().toLowerCase(Locale.ROOT);
        if (input.isEmpty() || input.chars().anyMatch(Character::isWhitespace)
                || input.startsWith(StatDefinition.MAX_PREFIX)) {
            return Optional.empty();
        }
        return Optional.of(input);
    }
}
