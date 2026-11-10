package org.skysongdev.skysongstats.definition;

import java.util.List;
import java.util.Objects;

// One option of a PackageChoice: its name and icon on the choices page, and the
// adjustments it gives (at least one, each stat at most once).
public record AncestryPackage(String name, Icon icon, List<AncestryAdjustment> adjustments) {
    public AncestryPackage {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(icon, "icon");
        adjustments = List.copyOf(adjustments);
        if (adjustments.isEmpty()) {
            throw new IllegalArgumentException("A package needs at least one adjustment");
        }
    }
}
