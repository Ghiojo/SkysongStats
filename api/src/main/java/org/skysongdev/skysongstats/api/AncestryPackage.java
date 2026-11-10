package org.skysongdev.skysongstats.api;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * One option of an {@link AncestryChoice.Packages} choice: a set of adjustments taken
 * together.
 *
 * @param name        its name, MiniMessage
 * @param icon        its icon on the choices page: a Material name or {@code model:<namespace:id>}
 * @param adjustments what it gives, in file order
 * @since 1.4.0
 */
public record AncestryPackage(@NotNull String name, @NotNull String icon, @NotNull List<AncestryAdjustment> adjustments) {

    /**
     * @throws NullPointerException when any part is null
     */
    public AncestryPackage {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(icon, "icon");
        adjustments = List.copyOf(adjustments);
    }
}
