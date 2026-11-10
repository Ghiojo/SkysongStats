package org.skysongdev.skysongstats.storage;

import java.util.List;
import java.util.Objects;

// A character's ancestry as saved: the ancestry id, whether its choices are still
// to be made, and every adjustment it gave, oldest first.
public record StoredAncestry(String ancestry, boolean choicesPending, List<StoredAncestryAdjustment> adjustments) {
    public StoredAncestry {
        Objects.requireNonNull(ancestry, "ancestry");
        adjustments = List.copyOf(adjustments);
    }
}
