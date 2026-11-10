package org.skysongdev.skysongstats.definition;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

// One ancestry of ancestries.yml: how it shows in the /setup ancestry list, who
// may pick it (permission empty = everyone), the adjustments it always gives and
// the choices the player makes, in file order.
public record AncestryDefinition(String id, String name, Icon icon, int slot, List<String> lore,
                                 Optional<String> permission, List<AncestryAdjustment> adjustments,
                                 List<AncestryChoice> choices) {
    public AncestryDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(icon, "icon");
        Objects.requireNonNull(permission, "permission");
        lore = List.copyOf(lore);
        adjustments = List.copyOf(adjustments);
        choices = List.copyOf(choices);
    }

    public boolean hasChoices() {
        return !choices.isEmpty();
    }

    // What the fixed adjustments give this stat, 0 when nothing.
    public int fixedAmount(String stat) {
        return adjustments.stream().filter(adjustment -> adjustment.stat().equals(stat))
                .mapToInt(AncestryAdjustment::amount).sum();
    }
}
