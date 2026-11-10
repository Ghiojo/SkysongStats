package org.skysongdev.skysongstats.definition;

import java.util.List;
import java.util.Objects;

// "Take one of these packages": the player picks exactly one option.
public record PackageChoice(String name, List<AncestryPackage> options, boolean stack) implements AncestryChoice {
    public PackageChoice {
        Objects.requireNonNull(name, "name");
        options = List.copyOf(options);
        if (options.size() < 2) {
            throw new IllegalArgumentException("A package choice needs at least two options");
        }
    }
}
