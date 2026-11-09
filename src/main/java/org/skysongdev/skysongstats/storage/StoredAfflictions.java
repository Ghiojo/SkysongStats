package org.skysongdev.skysongstats.storage;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

// One character's saved injuries and conditions, as AfflictionRepository loads
// them. "Afflictions" is the code's word for injuries and conditions together.
//   injuries:   injury id -> when it was awarded (epoch millis)
//   conditions: condition id -> the direct award (conditions that come from an
//               injury or another condition are worked out, never saved)
//   known:      injury id -> when and how the character learnt it
public record StoredAfflictions(Map<String, Long> injuries, Map<String, StoredCondition> conditions,
                                Map<String, StoredKnownInjury> known) {
    public StoredAfflictions {
        injuries = Collections.unmodifiableMap(new LinkedHashMap<>(injuries));
        conditions = Collections.unmodifiableMap(new LinkedHashMap<>(conditions));
        known = Collections.unmodifiableMap(new LinkedHashMap<>(known));
    }

    public static StoredAfflictions empty() {
        return new StoredAfflictions(Map.of(), Map.of(), Map.of());
    }
}
